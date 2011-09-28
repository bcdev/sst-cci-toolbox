package org.esa.cci.sst.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A yyyy/MM/dd organized product tree.
 *
 * @author Norman Fomferra
 */
public class ProductTree {
    private static final File[] EMPTY = new File[0];
    private Map<Integer, Map<Integer, Map<Integer, List<File>>>> yearMaps;

    public ProductTree() {
        this.yearMaps = new HashMap<Integer, Map<Integer, Map<Integer, List<File>>>>(32);
    }

    public void add(Date date, File file) {
        Calendar calendar = UTC.createCalendar(date);

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

        System.out.println("file = " + file);
        dayList.add(file);
    }

    public File[] get(Date date) {
        Calendar calendar = UTC.createCalendar(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        return get(year, month, day);
    }

    public File[] get(int year) {
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return EMPTY;
        }

        ArrayList<File> entries = new ArrayList<File>();
        for (Map<Integer, List<File>> dayLists : monthMaps.values()) {
            for (List<File> dayList : dayLists.values()) {
                entries.addAll(dayList);
            }
        }

        return entries.toArray(new File[0]);
    }

    public File[] get(int year, int month) {
        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return EMPTY;
        }

        Map<Integer, List<File>> dayLists = monthMaps.get(month);
        if (dayLists == null) {
            return EMPTY;
        }

        ArrayList<File> entries = new ArrayList<File>();
        for (List<File> dayList : dayLists.values()) {
            entries.addAll(dayList);
        }

        return entries.toArray(new File[0]);
    }

    public File[] get(int year, int month, int day) {

        Map<Integer, Map<Integer, List<File>>> monthMaps = yearMaps.get(year);
        if (monthMaps == null) {
            return EMPTY;
        }

        Map<Integer, List<File>> dayLists = monthMaps.get(month);
        if (dayLists == null) {
            return EMPTY;
        }

        List<File> dayList = dayLists.get(day);
        if (dayList == null) {
            return EMPTY;
        }

        return dayList.toArray(new File[0]);
    }

}
