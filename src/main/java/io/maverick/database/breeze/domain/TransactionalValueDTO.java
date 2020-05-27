package io.maverick.database.breeze.domain;

/**
 * Created by istvanvajnorak on 2020. 05. 27..
 *
 * A simple DTO that ships key / value / transaction info as a response
 *
 * @param <K>
 * @param <V>
 */
public class TransactionalValueDTO<K,V> {

    private K key;
    private V value;
    private String transactionId;

    public TransactionalValueDTO(String transactionId,K key, V value){
        this.value = value;
        this.key = key;
        this.transactionId = transactionId;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }
}
