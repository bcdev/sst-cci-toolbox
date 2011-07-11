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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic cache.
 *
 * @author Ralf Quast.
 */
public class Cache<K, V> {

    private final int capacity;
    private final List<V> itemList;
    private final Map<K, V> itemMap;

    public Cache(int capacity) {
        this.capacity = capacity;
        itemList = new ArrayList<V>(capacity);
        itemMap = new HashMap<K, V>(capacity);
    }

    public V add(K key, V item) {
        if (!itemMap.containsKey(key)) {
            itemMap.put(key, item);
            itemList.add(item);
            if (itemList.size() > capacity) {
                final V removedItem = itemList.remove(0);
                itemMap.values().remove(removedItem);
                return removedItem;
            }
        }
        return null;
    }

    public boolean contains(K key) {
        return itemMap.containsKey(key);
    }

    public V get(K key) {
        return itemMap.get(key);
    }

    public Collection<V> clear() {
        final Collection<V> collection = new ArrayList<V>(itemList);
        itemList.clear();
        itemMap.clear();

        return collection;
    }

    public V remove(K key) {
        V item = itemMap.get(key);
        if (item != null) {
            itemList.remove(item);
            itemMap.remove(key);
        }
        return item;
    }
}
