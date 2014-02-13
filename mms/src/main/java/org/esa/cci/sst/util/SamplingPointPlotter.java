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

package org.esa.cci.sst.util;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Ralf Quast
 */
public final class SamplingPointPlotter {

    private List<SamplingPoint> samples;
    private String filePath;
    private boolean show = true;
    private String windowTitle;

    public SamplingPointPlotter() {
    }

    public SamplingPointPlotter samples(List<SamplingPoint> samples) {
        this.samples = samples;
        return this;
    }

    public SamplingPointPlotter filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public SamplingPointPlotter show(boolean show) {
        this.show = show;
        return this;
    }

    public SamplingPointPlotter windowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
        return this;
    }

    public BufferedImage plot() throws IOException {
        final BufferedImage image = drawImage(samples);
        if (show) {
            showImage(image, windowTitle);
        }
        if (filePath != null) {
            writeImage(image, filePath);
        }

        return image;
    }

    static BufferedImage drawImage(List<SamplingPoint> samples) {
        final int w = 800;
        final int h = 400;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D graphics = image.createGraphics();

        if (samples != null) {
            for (final SamplingPoint p : samples) {
                final double x = (p.getLon() + 180.0) / 360.0;
                final double y = (90.0 - p.getLat()) / 180.0;
                final int i = (int) (y * h);
                final int k = (int) (x * w);
                graphics.fill(new Rectangle(k, i, 1, 1));
            }
        }
        return image;
    }

    static BufferedImage showImage(BufferedImage image, String windowTitle) {
        final JLabel label = new JLabel(new ImageIcon(image));

        final JFrame frame = new JFrame();
        frame.setTitle(windowTitle);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(image.getWidth(), image.getHeight());
        frame.setVisible(true);

        return image;
    }

    private static void writeImage(BufferedImage image, String filePath) throws IOException {
        ImageIO.write(image, "png", new File(filePath));
    }

}
