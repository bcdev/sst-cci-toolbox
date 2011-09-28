package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.FileTree;

import java.io.File;
import java.io.FileFilter;

/**
 * A product store.
 *
 * @author Norman Fomferra
 */
public class ProductStore {

    private ProductType productType;
    private String[] inputPaths;
    private FileTree fileTree;

    public static ProductStore create(ProductType productType, String... inputPaths) {
        return new ProductStore(productType, inputPaths, scanFiles(productType, inputPaths));
    }

    private ProductStore(ProductType productType, String[] inputPaths, FileTree fileTree) {
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

    public FileTree getFileTree() {
        return fileTree;
    }

    private static FileTree scanFiles(ProductType productType, String... inputPaths) {
        FileTree fileTree = new FileTree();

        for (String inputPath : inputPaths) {
            scanFiles(productType, new File(inputPath), fileTree);
        }

        return fileTree;
    }

    private static void scanFiles(ProductType productType, File entry, FileTree fileTree) {
        if (entry.isDirectory()) {
            File[] files = entry.listFiles(new InputFileFilter());
            if (files != null) {
                for (File file : files) {
                    scanFiles(productType, file, fileTree);
                }
            }
        } else if (entry.isFile()) {
            fileTree.add(productType.getDate(entry.getName()), entry);
        }
    }

    private static class InputFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory()
                    || file.getName().endsWith(".nc")
                    || file.getName().endsWith(".nc.gz");
        }
    }
}
