package org.esa.cci.sst.regavg.util;

import org.esa.cci.sst.util.FileTree;
import org.esa.cci.sst.util.UTC;
import org.junit.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class FileTreeTest {

    private static final DateFormat DATE_FORMAT = UTC.getDateFormat("yyyy-MM-dd");

    @Test
    public void testPutAndGetWithDate() throws Exception {
        FileTree fileTree = new FileTree();

        fileTree.add(date("2007-05-11"), new File("a.nc"));
        assertArrayEquals(new File[]{
                new File("a.nc")},
                          fileTree.get(date("2007-05-11")));

        fileTree.add(date("2007-05-11"), new File("b.nc"));
        assertArrayEquals(new File[]{
                new File("a.nc"),
                new File("b.nc")},
                          fileTree.get(date("2007-05-11")));
    }

    @Test
    public void testPutAndGetByIndexes() throws Exception {
        FileTree fileTree = new FileTree();
        fileTree.add(date("2007-05-11"), new File("a.nc"));
        fileTree.add(date("2007-05-11"), new File("b.nc"));
        fileTree.add(date("2007-06-04"), new File("c.nc"));
        fileTree.add(date("2008-01-01"), new File("d.nc"));
        fileTree.add(date("2008-01-01"), new File("e.nc"));

        assertArrayEquals(new File[]{
                new File("a.nc"),
                new File("b.nc"),
                new File("c.nc")},
                          fileTree.get(2007));

        assertArrayEquals(new File[]{
                new File("d.nc"),
                new File("e.nc")},
                          fileTree.get(2008));

        assertArrayEquals(new File[]{
                new File("a.nc"),
                new File("b.nc")},
                          fileTree.get(2007, 4));
    }

    private Date date(String source) throws ParseException {
        return DATE_FORMAT.parse(source);
    }
}
