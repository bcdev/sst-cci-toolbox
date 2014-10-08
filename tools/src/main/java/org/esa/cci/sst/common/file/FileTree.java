/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.file;

import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.util.*;

/**
 * A yyyy/MM/dd organized file tree.
 *
 * @author Norman Fomferra
 */
public class FileTree {

    private Map<Integer, Map<Integer, Map<Integer, List<File>>>> yearMaps;

    public FileTree() {
        this.yearMaps = new HashMap<Integer, Map<Integer, Map<Integer, List<File>>>>(32);
    }

    public void add(Date date, File file) {
        Calendar calendar = TimeUtil.createUtcCalendar(date);

        int year = calendar.get(Calendar.YEAR);
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            monthMaps = new HashMap<Integer, Map<Integer, List<File>>>(16);
            yearMaps.put(year, monthMaps);
        }

        int month = calendar.get(Calendar.MONTH);
        Map<Integer, List<File>> dayLists = monthMaps.get(month);
        if (dayLists == null) {
            dayLists = new HashMap<Integer, List<File>>(16);
            monthMaps.put(month, dayLists);
        }

        int day = calendar.get(Calendar.DATE);
        List<File> dayList = dayLists.get(day);
        if (dayList == null) {
            dayList = new ArrayList<File>(32);
            dayLists.put(day, dayList);
        }

        dayList.add(file);
    }

    public List<File> get(Date date) {
        Calendar calendar = TimeUtil.createUtcCalendar(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        return get(year, month, day);
    }

    public List<File> get(int year) {
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return Collections.emptyList();
        }

        List<File> entries = new ArrayList<>();
        for (Map<Integer, List<File>> dayLists : monthMaps.values()) {
            for (List<File> dayList : dayLists.values()) {
                entries.addAll(dayList);
            }
        }

        return entries;
    }

    public List<File> get(int year, int month) {
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return Collections.emptyList();
        }

        Map<Integer, List<File>> dayLists = monthMaps.get(month);
        if (dayLists == null) {
            return Collections.emptyList();
        }

        List<File> entries = new ArrayList<>();
        for (List<File> dayList : dayLists.values()) {
            entries.addAll(dayList);
        }

        return entries;
    }

    public List<File> get(int year, int month, int day) {
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return Collections.emptyList();
        }

        Map<Integer, List<File>> dayLists = monthMaps.get(month);
        if (dayLists == null) {
            return Collections.emptyList();
        }

        List<File> dayList = dayLists.get(day);
        if (dayList == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(dayList);
    }

}
