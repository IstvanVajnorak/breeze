package io.maverick.database.breeze.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 *  A simple context in which a transaction is registered so we can treat all requests in it as atomic units
 */
public class TransactionContext<K,V> {

    //The user supplied unique id of a transaction
    private final String id;

    //The time when the transaction got created,
    // any chances after that committed will be a conflict should this batch be committed later
    private final long transactionStartTime;

    //The list of changes registered. If the value is empty, that means we want to delete the key from the system
    private final Map<K,VersionedValue<K,V>> valueChanges = new HashMap<>();

    //Since right now nothing prevents us from trying to write into a transaction on multiple threads or by multiple users,
    // we need to protect the values inside by some mechanism
    private final Lock lock = new ReentrantLock();

    /**
     * Default constructor that bootraps a transaction context
     */
    public TransactionContext(final String id){
        this.id = id;
        this.transactionStartTime = System.nanoTime();
    }

    /**
     * Notes that a deletion for a given key was requested
     * @param key
     */
    public V getValue(final K key){
        try {
            lock.lock();
            return valueChanges.containsKey(key) ? valueChanges.get(key).getValue() : null;
        }finally {
            lock.unlock();;
        }
    }

    /**
     * Notes that a deletion for a given key was requested
     * @param key
     */
    public void registerDelete(final K key){
        try {
            lock.lock();
            valueChanges.put(key,null);
        }finally {
            lock.unlock();;
        }
    }

    /**
     * Notes that there is an item to be inserted or updated
     *
     * @param versionedValue
     */
    public void registerUpsert(final VersionedValue<K,V> versionedValue){
        try {
            lock.lock();
            valueChanges.put(versionedValue.getKey(), versionedValue);
        }finally {
            lock.unlock();;
        }
    }

    /**
     * The id of this particular transaction
     * @return
     */
    public String getId(){
        return id;
    }

    /**
     * To be able to work with the delta the transaction want to handle as an atomic unit
     * @return
     */
    public Map<K,VersionedValue<K,V>> getChanges(){
        return Collections.unmodifiableMap(valueChanges);
    }

    /**
     * When we wanted to start the transaction
     * @return
     */
    public long getTimestamp() {
        return transactionStartTime;
    }
}
