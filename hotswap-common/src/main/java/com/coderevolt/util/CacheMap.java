package com.coderevolt.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存map
 *
 * @param <K>
 * @param <V>
 */
public class CacheMap<K, V> {

    private final Map<Thread, Map<Object, Node<Object>>> map = new ConcurrentHashMap<>();

    public void put(K key, V val, long expireTime, TimeUnit unit) {
        Map<Object, Node<Object>> nodeMap = map.computeIfAbsent(Thread.currentThread(), k -> new ConcurrentHashMap<>());
        nodeMap.put(key, new Node<>(val, unit.toNanos(expireTime)));
    }

    public V get(K key) {
        Map<Object, Node<Object>> nodeMap = map.get(Thread.currentThread());
        if (nodeMap == null) return null;
        Node<V> node = (Node<V>) nodeMap.get(key);
        if (node == null || node.isExpired()) {
            nodeMap.remove(key);
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
