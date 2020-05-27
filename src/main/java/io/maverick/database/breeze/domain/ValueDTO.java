package io.maverick.database.breeze.domain;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A simple DTO that ships key / value  requests
 *
 * @param <K>
 * @param <V>
 */
public class ValueDTO<K,V> {

    private K key;
    private V value;

    public ValueDTO(){
    }

    public ValueDTO(K key, V value){
        this.value = value;
        this.key = key;
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
