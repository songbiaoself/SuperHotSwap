package com.coderevolt.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存map
 * ThreadUnSafe 不是线程安全
 * @param <K>
 * @param <V>
 */
public class CacheMap<K, V> {

    private final LinkedHashMap<K, Node<V>> map;
    private final int capacity;

    private static final float loadFactor = 0.75f;

    public CacheMap() {
        this(128);
    }

    public CacheMap(int capacity) {
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(capacity, loadFactor, true);
    }

    public void put(K key, V value) {
        this.put(key, value, Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    /**
     * 大于容量移除队首元素
     * @param key
     * @param val
     * @param expireTime
     * @param unit
     */
    public void put(K key, V val, long expireTime, TimeUnit unit) {
        Node<V> value = new Node<>(val, unit.toNanos(expireTime));
        map.put(key, value);
        if (map.size() > capacity) {
            synchronized (map) {
                Iterator<K> iterator = map.keySet().iterator();
                int c = (int) (capacity * loadFactor);
                while (map.size() > c && iterator.hasNext()) {
                    map.remove(iterator.next());
                }
            }
        }
    }

    public V load(K key, Supplier<V> supplier) {
        V r = get(key);
        if (r == null) {
            r = supplier.get();
        }
        return r;
    }

    public V get(K key) {
        Node<V> node = map.get(key);
        if (node == null || node.isExpired()) {
            map.remove(key);
            return null;
        }
        return node.value;
    }

    private static class Node<V> {
        private final V value;
        private final long createTimeNanos;
        private final long expireTimeNanos;

        public Node(V value, long expireTimeMills) {
            this.value = value;
            this.createTimeNanos = System.nanoTime();
            this.expireTimeNanos = expireTimeMills;
        }

        private boolean isExpired() {
            return (System.nanoTime() - this.createTimeNanos) >= this.expireTimeNanos;
        }
    }


}
