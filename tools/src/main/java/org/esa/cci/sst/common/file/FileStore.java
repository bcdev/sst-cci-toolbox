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

import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.ProductType;
import org.esa.cci.sst.util.UTC;

import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A product store.
 *
 * @author Norman Fomferra
 */
public class FileStore {

    private static final Logger LOGGER = Tool.LOGGER;

    private ProductType productType;
    private String[] inputPaths;
    private FileTree fileTree;

    public static FileStore create(ProductType productType, String filenameRegex, String... inputPaths) {
        return new FileStore(productType, inputPaths, scanFiles(productType, new InputFileFilter(filenameRegex), inputPaths));
    }

    private FileStore(ProductType productType, String[] inputPaths, FileTree fileTree) {
        this.productType = productType;
        this.inputPaths = inputPaths;
        this.fileTree = fileTree;
    }

    public ProductType getProductType() {
        return productType;
    }

    public String[] getInputPaths() {
        return inputPaths;
    }

    public List<File> getFiles(Date date1, Date date2) {
        Calendar calendar = UTC.createCalendar(date1);
        ArrayList<File> files = new ArrayList<File>();
        while (calendar.getTime().before(date2)) {
            files.addAll(fileTree.get(calendar.getTime()));
            calendar.add(Calendar.DATE, 1);
        }
        return files;
    }

    private static FileTree scanFiles(ProductType productType, FileFilter fileFilter, String... inputPaths) {

        FileTree fileTree = new FileTree();

        for (String inputPath : inputPaths) {
            scanFiles(productType, fileFilter, new File(inputPath), fileTree);
        }
        return fileTree;
    }

    private static void scanFiles(ProductType productType, FileFilter fileFilter, File entry, FileTree fileTree) {
        if (entry.isDirectory()) {
            File[] files = entry.listFiles(fileFilter);
            if (files != null) {
                for (File file : files) {
                    scanFiles(productType, fileFilter, file, fileTree);
                }
            }
        } else if (entry.isFile()) {
            try {
                Date date = productType.parseDate(entry);
                if (date != null) {
                    fileTree.add(date, entry);
                } else {
                    LOGGER.warning("Ignoring input file with unknown naming convention: " + entry.getPath());
                }
            } catch (ParseException e) {
                LOGGER.warning("Ignoring input file because date can't be parsed from filename: " + entry.getPath());
            }
        }
    }

    private static class InputFileFilter implements FileFilter {
        private final Pattern filenamePattern;

        private InputFileFilter(String filenameRegex) {
            this.filenamePattern = Pattern.compile(filenameRegex);
        }

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.isFile() &&
                    filenamePattern.matcher(file.getName()).matches();
        }
    }
}
