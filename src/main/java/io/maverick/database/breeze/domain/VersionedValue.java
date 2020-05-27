package io.maverick.database.breeze.domain;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A simple representation of a key value pair that is timestamped.
 */
public class VersionedValue<K,V> {

    private final K key;
    private final V value;
    private final long timestamp;

    /**
     * Only be created through the builder, so we can validate
     * @param builder
     */
    private VersionedValue(VersionedValueBuilder<K,V> builder){
        this.key = builder.key;
        this.value = builder.value;
        this.timestamp = builder.timestamp;
    }

    /**
     * A private constructor to be able to clone an object with a new timestamp
     *
     * @param key
     * @param value
     * @param timestamp
     */
    private VersionedValue(K key,V value, long timestamp){
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }
    public K getKey(){
        return key;
    }

    public V getValue(){
        return value;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public  VersionedValue<K,V> cloneWithTime(long time){
        return new VersionedValue<K,V>(getKey(),getValue(),time);
    }
    /**
     * Tells if the value has been changed more recently than a given time
     *
     * @param time
     * @return
     */
    public boolean hasChangedSince(long time){
        return time < getTimestamp();
    }

    /**
     * A builder to provide a fluent API for building
     * <code>CreditCardDetails</code> objects and enforcing the mandatory input
     * of a credit card number at a minimum..
     *
     * @author istvanvajnorak
     *
     */
    public static final class VersionedValueBuilder<K,V> {

        private K key;
        private V value;
        private long timestamp;

        private VersionedValueBuilder(K key) {
            this.key = key;
        }

        public VersionedValueBuilder<K,V> withValue(V value) {
            this.value = value;
            return this;
        }

        public VersionedValueBuilder<K,V> atTime(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public VersionedValue<K,V> build() {
            return new VersionedValue<>(this);
        }
    }

    /**
     * A static factory method for those who like the fluent API.
     * Bit sad as it fails to use the generic correctly, could be enhanced.
     *
     * @param key
     * @return a builder
     */
    public static VersionedValueBuilder<String,String> builderFor(String key) {
        return new VersionedValueBuilder<String,String>(key);
    }

}
