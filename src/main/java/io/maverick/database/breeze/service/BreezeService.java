package io.maverick.database.breeze.service;

import io.maverick.database.breeze.exception.BreezeActionException;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A simple interface representing the actions a user can take on a database.
 *
 */
public interface BreezeService<K,V> {

    /**
     * A simple action that creates / updates a value in a db
     *
     * @param key
     * @param value
     */
    void put(K key,V value) throws BreezeActionException;

    /**
     * A simple action that registers a request for the creation of a key / value
     * @param key
     * @param value
     * @param transactionId
     */
    void put(K key, V value, String transactionId) throws BreezeActionException;


    /**
     * A simple action that retrieves a value for a given key.
     *
     * @param key
     * @throws  throws an exception of the key is not found
     */
    String get(K key) throws BreezeActionException;


    /**
     * A simple action that retrieves a value for a given key as it is seen in a given transaction
     *
     * @param key
     * @param transactionId
     * @throws  BreezeActionException  when the transaction does not exist or the key does not exist
     */
    String get(K key,String transactionId) throws BreezeActionException;


    /**
     * A simple action that removes a value for a given key.
     *
     * @param key
     * @throws  throws an exception of the key is not found
     */
    void delete(K key) throws BreezeActionException;


    /**
     * A simple action that registers a removal of a given key for an open transaction
     *
     * @param key
     * @param transactionId
     * @throws BreezeActionException when
     */
    void delete(K key,String transactionId) throws BreezeActionException;

    /**
     *
     * Starts a transaction with the specified ID. The ID must not be an active transaction ID.
     * Throws an exception or returns an error on failure
     *
     * @param transactionId
     * @throws BreezeActionException
     */
    void createTransaction(String transactionId) throws BreezeActionException;

    /**
     * Aborts the transaction and invalidates the transaction with the specified transaction ID.
     * Throws an exception or returns an error on failure
     * @param transactionId
     */
    void rollbackTransaction(String transactionId) throws BreezeActionException;

    /**
     *  Commits the transaction and invalidates the ID.
     *  If there is a conflict (meaning the transaction attempts to change a value for a key that was mutated after the transaction was created),
     *  the transaction always fails with an exception or an error is returned.
     *
     * @param transactionId
     */
    void commitTransaction(String transactionId) throws BreezeActionException;

}
