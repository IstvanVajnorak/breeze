package io.maverick.database.breeze.service.impl;

import io.maverick.database.breeze.domain.TransactionContext;
import io.maverick.database.breeze.domain.VersionedValue;
import io.maverick.database.breeze.exception.BreezeActionException;
import io.maverick.database.breeze.exception.ErrorCode;
import io.maverick.database.breeze.service.BreezeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A simple implementation of the breeze service that holds every bit of the work in memory.
 *
 */
@Component
public class BreezeServiceImpl implements BreezeService<String,String> {

    // Our one and only logger class to have some idea what we do in the code
    private static final Logger LOG = LoggerFactory.getLogger(BreezeServiceImpl.class);

    //The main store that contains the values in their versioned format so we can use to compare timings
    private final Map<String,VersionedValue<String,String>> store = new ConcurrentHashMap<>();

    // The ongoing transactions started
    // (TODO: later provide some user context for a transaction so people can't eavesdrop on each other's transactions)
    private final  Map<String, TransactionContext<String,String>> activeTransactions = new HashMap<>();

    //The locking context making sure that only one write and multiple reads can be active at a time
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    //A separate lock for manipulating transactions
    private final Lock transactionsLock = new ReentrantLock();

    @Override
    public void put(String key, String value) throws BreezeActionException {
        Lock writeLock = lock.writeLock();
        try{
            writeLock.lock();
            VersionedValue<String,String> entry = VersionedValue.builderFor(key)
                    .withValue(value)
                    .atTime(System.nanoTime())
                    .build();
            LOG.info("Saving the value ["+entry.getValue()+"] for key ["+entry.getKey()+"] with the update time of ["+entry.getTimestamp()+"]");
            store.put(entry.getKey(),entry);
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public void put(String key, String value, String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(!activeTransactions.containsKey(transactionId)) {
                LOG.info("No transaction with id ["+transactionId+"] found for transactional put request.");
                throw new BreezeActionException(ErrorCode.UNKNOWN_TRANSACTION,
                        "There is no transaction with the id of [" + transactionId + "], hence no way to delete from it.");
            }

            //so the name is still not taken, let's create a context
            TransactionContext<String,String> transaction = activeTransactions.get(transactionId);

            VersionedValue<String,String> entry = VersionedValue.builderFor(key)
                    .withValue(value)
                    .atTime(System.nanoTime())
                    .build();
            LOG.info("Saving the value ["+entry.getValue()+"] for key ["+entry.getKey()+"] in transaction  ["+transactionId+"]");
            transaction.registerUpsert(entry);

        }finally {
            transactionsLock.unlock();
        }
    }

    @Override
    public String get(String key) throws BreezeActionException {
        Lock readLock = lock.readLock();
        try{
            readLock.lock();
            String result = store.containsKey(key) ? store.get(key).getValue() : null;
            LOG.info("Returning ["+result+"] as the value for key ["+key+"]");
            return result;
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public String get(String key, String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(!activeTransactions.containsKey(transactionId)) {
                LOG.info("No transaction with id ["+transactionId+"] found for transactional get request.");
                throw new BreezeActionException(ErrorCode.UNKNOWN_TRANSACTION,
                        "There is no transaction with the id of [" + transactionId + "], hence no way to delete from it.");
            }

            //so the name is still not taken, let's create a context
            TransactionContext<String,String> transaction = activeTransactions.get(transactionId);
            String result = transaction.getValue(key);
            LOG.info("Returning ["+result+"] as the value for key ["+key+"] from active transaction ["+transactionId+"]");
            return result;

        }finally {
            transactionsLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws BreezeActionException {
        Lock writeLock = lock.writeLock();
        try{
            writeLock.lock();
            LOG.info("Removing ["+key+"] in one atomic step from the store.");
            store.remove(key);
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key, String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(!activeTransactions.containsKey(transactionId))
                throw new BreezeActionException(ErrorCode.UNKNOWN_TRANSACTION,
                        "There is no transaction with the id of ["+transactionId+"], hence no way to delete from it.");

            //so the name is still not taken, let's create a context
            TransactionContext<String,String> transaction = activeTransactions.get(transactionId);
            transaction.registerDelete(key);

        }finally {
            transactionsLock.unlock();
        }
    }

    @Override
    public void createTransaction(String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(activeTransactions.containsKey(transactionId))
                throw new BreezeActionException(ErrorCode.TRANSACTION_ALREADY_EXISTS,
                        "There is already an active transaction with the id of ["+transactionId+"]");

            //so the name is still not taken, let's create a context
            TransactionContext<String,String> transaction = new TransactionContext<>(transactionId);
            activeTransactions.put(transaction.getId(),transaction);

        }finally {
            transactionsLock.unlock();
        }
    }

    @Override
    public void rollbackTransaction(String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(!activeTransactions.containsKey(transactionId)) {
                LOG.info(" Trying to rollback non existing transaction with id [" + transactionId + "].");
                throw new BreezeActionException(ErrorCode.UNKNOWN_TRANSACTION,
                        "There is no ongoing transaction with the id of [" + transactionId + "], cannot roll back.");
            }
        }finally {
            //it is safe to try to remove this entry even if there are no entries
            activeTransactions.remove(transactionId);
            transactionsLock.unlock();
        }
    }

    @Override
    public void commitTransaction(String transactionId) throws BreezeActionException {
        try{
            transactionsLock.lock();

            if(!activeTransactions.containsKey(transactionId)) {
                LOG.info(" Trying to commit non existing transaction with id ["+transactionId+"].");
                throw new BreezeActionException(ErrorCode.UNKNOWN_TRANSACTION,
                        "There is no ongoing transaction with the id of [" + transactionId + "], cannot roll back.");
            }

            //now we need to ensure that the write is performed atomically
            Lock writeLock = lock.writeLock();
            try{
                writeLock.lock();
                performTransaction(activeTransactions.get(transactionId));
            }finally{
                //m,ake sure whatever happens we are unlocking the store for further reads / writes
                writeLock.unlock();
            }

        }finally {
            //whether the transaction succeeds or fails it actually concludes and we remove it from the active ones
            activeTransactions.remove(transactionId);
            transactionsLock.unlock();
        }
    }

    /**
     * The bulk of the transaction handling
     *
     * @param transaction
     * @throws BreezeActionException
     */
    private void performTransaction(TransactionContext<String,String> transaction) throws BreezeActionException {

        LOG.info("Committing transaction ["+transaction.getId()+"] with ["+transaction.getChanges().size()+"] items in it");

        //for each entry check if any of them got updated
        //TODO check if some sort of notification would work better so we could keep the transactions up to date without a final run on values
        for( Entry<String,VersionedValue<String,String>> entry : transaction.getChanges().entrySet()){

            //whatever we want to do with the record, we need to ensure the record did not change since the time our transaction started
            //every other scenario like updating an existing record that did not have any changes yet, deleting a non existent record, inserting a new one are fine
            //TODO if another transaction deleted the record, we won't see it, that might be a problem?
            if(hasChangedSince(entry.getKey(),transaction.getTimestamp())){
                throw new BreezeActionException(ErrorCode.UNCOMMITABLE_TRANSACTION,"The value for key ["+entry.getKey()+"] has been modified after the transaction started." +
                        " Transaction with id ["+transaction.getId()+"] will be discarded. " +
                        "Please open a new transaction and try to change the values again.");
            }
        }

        //grabbing the exact time for this update
        long transactionCommitTime = System.currentTimeMillis();

        //kinda ugly double loop, if we would track changes with events it would not be needed
        for( Entry<String,VersionedValue<String,String>> entry : transaction.getChanges().entrySet()){

            if(entry.getValue() == null){
                store.remove(entry.getKey());
            }else{
                //creating a new value that captures the commit time for this object, and storing that
                store.put(entry.getKey(),entry.getValue().cloneWithTime(transactionCommitTime));
            }

        }
    }

    /**
     * Convenience method to better read the commit logic's decision making
     *
     * @param key
     * @param timeMillies
     * @return
     */
    private boolean hasChangedSince(String key,long timeMillies){
        LOG.info("Checking if [" + key + "] is contained by the store");
        if(store.containsKey(key)) {
            LOG.info("Checking if commit time [" + timeMillies + "] is lesser than last modified time [" + store.get(key).getTimestamp() + "] , which yields  [" + store.get(key).hasChangedSince(timeMillies) + "]");
        }
        return store.containsKey(key) && store.get(key).hasChangedSince(timeMillies);
    }
}
