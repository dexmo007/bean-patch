package com.dexmohq.annotation.processing;

import java.util.*;

import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;

abstract class TypeMirrorLikeMap<K, V> implements Map<K, V> {

    private final Utils utils;
    private final List<Map.Entry<K, V>> entries;

    TypeMirrorLikeMap(Utils utils) {
        this.utils = utils;
        this.entries = new ArrayList<>();
    }

    protected abstract TypeMirrorLike toTypeMirrorLike(K key);

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key);
        final TypeMirrorLike typeMirrorLikeKey = toTypeMirrorLike((K) key);
        return entries.stream().anyMatch(e -> utils.areEqual(toTypeMirrorLike(e.getKey()), typeMirrorLikeKey));
    }

    @Override
    public boolean containsValue(Object value) {
        Objects.requireNonNull(value);
        return entries.stream().anyMatch(e -> e.getValue().equals(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        Objects.requireNonNull(key);
        final TypeMirrorLike typeMirrorLikeKey = toTypeMirrorLike((K) key);
        return entries.stream()
                .filter(e -> utils.areEqual(toTypeMirrorLike(e.getKey()), typeMirrorLikeKey))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    @Override
    public V put(K key, V value) {
        final Iterator<Map.Entry<K, V>> it = entries.iterator();
        V previous = null;
        final TypeMirrorLike typeMirrorLikeKey = toTypeMirrorLike(key);
        while (it.hasNext()) {
            final Map.Entry<K, V> next = it.next();
            if (utils.areEqual(typeMirrorLikeKey, toTypeMirrorLike(next.getKey()))) {
                previous = next.setValue(value);
                break;
            }
        }
        if (previous == null) {
            entries.add(new Entry<>(key, value));
        }
        return previous;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        final TypeMirrorLike typeMirrorLikeKey = toTypeMirrorLike((K) key);
        final Iterator<Map.Entry<K, V>> it = entries.iterator();
        V removed = null;
        while (it.hasNext()) {
            final Map.Entry<K, V> next = it.next();
            if (utils.areEqual(toTypeMirrorLike(next.getKey()), typeMirrorLikeKey)) {
                removed = next.getValue();
                it.remove();
                break;
            }
        }
        return removed;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public Set<K> keySet() {
        return entries.stream().map(Map.Entry::getKey).collect(toUnmodifiableSet());
    }

    @Override
    public Collection<V> values() {
        return entries.stream().map(Map.Entry::getValue).collect(toUnmodifiableList());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return entries.stream().collect(toUnmodifiableSet());
    }

    private static class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            final V previous = this.value;
            this.value = value;
            return previous;
        }
    }

}
