/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
