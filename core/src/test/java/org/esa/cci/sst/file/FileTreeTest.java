package org.esa.cci.sst.file;

import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Norman Fomferra
 */
public class FileTreeTest {

    @Test
    public void testPutAndGetWithDate() throws Exception {
        FileTree fileTree = new FileTree();

        fileTree.add(date("2007-05-11"), new File("a.nc"));
        assertArrayEquals(new Object[]{
                        new File("a.nc")},
                fileTree.get(date("2007-05-11")).toArray());

        fileTree.add(date("2007-05-11"), new File("b.nc"));
        assertArrayEquals(new Object[]{
                        new File("a.nc"),
                        new File("b.nc")},
                fileTree.get(date("2007-05-11")).toArray());
    }

    @Test
    public void testPutAndGetByIndexes() throws Exception {
        FileTree fileTree = new FileTree();
        fileTree.add(date("2007-05-11"), new File("a.nc"));
        fileTree.add(date("2007-05-11"), new File("b.nc"));
        fileTree.add(date("2007-06-04"), new File("c.nc"));
        fileTree.add(date("2008-01-01"), new File("d.nc"));
        fileTree.add(date("2008-01-01"), new File("e.nc"));

        assertArrayEquals(new Object[]{
                        new File("a.nc"),
                        new File("b.nc"),
                        new File("c.nc")},
                fileTree.get(2007).toArray());

        assertArrayEquals(new Object[]{
                        new File("d.nc"),
                        new File("e.nc")},
                fileTree.get(2008).toArray());

        assertArrayEquals(new Object[]{
                        new File("a.nc"),
                        new File("b.nc")},
                fileTree.get(2007, 4).toArray());
    }

    private Date date(String source) throws ParseException {
        return TimeUtil.parseShortUtcFormat(source);
    }
}
