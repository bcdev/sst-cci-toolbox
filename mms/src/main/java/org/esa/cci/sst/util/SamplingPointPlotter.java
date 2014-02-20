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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author Ralf Quast
 */
public final class SamplingPointPlotter {

    private List<SamplingPoint> samples;
    private String filePath;
    private boolean show = true;
    private boolean live = false;
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

    public SamplingPointPlotter live(boolean live) {
        this.live = live;
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
        final BufferedImage image = drawImage();
        if (!live && show) {
            showImage(image);
        }
        if (filePath != null) {
            writeImage(image);
        }

        return image;
    }

    private BufferedImage drawImage() {
        final int w = 800;
        final int h = 400;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final JComponent component;
        if (live) {
            component = showImage(image);
        } else {
            component = null;
        }
        final Graphics2D graphics = image.createGraphics();
        final LonLatMapStrategy strategy = new LonLatMapStrategy(w, h);

        for (final SamplingPoint p : samples) {
            final PlotPoint mapPoint = strategy.map(p);

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        graphics.fill(new Rectangle(mapPoint.getX(), mapPoint.getY(), 1, 1));
                    }
                });
            } catch (InterruptedException | InvocationTargetException ignored) {
            }

            if (component != null) {
                component.repaint();

                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return image;
    }

    private JComponent showImage(BufferedImage image) {
        final JLabel label = new JLabel(new ImageIcon(image));
        final JFrame frame = new JFrame();
        frame.setTitle(windowTitle);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(image.getWidth(), image.getHeight());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });

        return label;
    }

    void writeImage(BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(filePath));
    }

}
