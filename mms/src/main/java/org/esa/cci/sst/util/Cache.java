/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic cache.
 *
 * @author Ralf Quast.
 */
@SuppressWarnings("CollectionDeclaredAsConcreteClass")
public class Cache<K, V> {

    private final int capacity;
    private final LinkedList<K> keyList;
    private Map<K, V> itemMap;

    public Cache(int capacity) {
        this.capacity = capacity;
        keyList = new LinkedList<>();
        itemMap = new ConcurrentHashMap<>(capacity);
    }

    public V add(K key, V item) {
        if (!itemMap.containsKey(key)) {
            itemMap.put(key, item);
            keyList.addLast(key);
            if (keyList.size() > capacity) {
                final K removedKey = keyList.removeFirst();
                final V removedItem = itemMap.get(removedKey);
                itemMap.remove(removedKey);
                return removedItem;
            }
        }
        return null;
    }

    public boolean contains(K key) {
        return itemMap.containsKey(key);
    }

    public V get(K key) {
        final V result = itemMap.get(key);
        if (result == null) {
            throw new IllegalArgumentException("Object with key '" + key + "' not contained in cache");
        }
        return result;
    }

    public Collection<V> clear() {
        final Collection<V> removedItems = itemMap.values();

        keyList.clear();
        itemMap = new ConcurrentHashMap<>(capacity);

        return removedItems;
    }

    public V remove(K key) {
        V item = itemMap.get(key);
        if (item != null) {
            keyList.remove(key);
            itemMap.remove(key);
        }
        return item;
    }
}
