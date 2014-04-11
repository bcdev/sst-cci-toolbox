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

import org.esa.cci.sst.tools.samplepoint.GenerateSobolPointsWorkflow;
import org.esa.cci.sst.tools.samplepoint.WorkflowContext;

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
import java.util.List;

/**
 * @author Ralf Quast
 */
public final class SamplingPointPlotter {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;

    private List<SamplingPoint> samples;
    private String filePath;
    private boolean show = false;
    private boolean live = false;
    private boolean series = false;
    private String windowTitle;
    private String mapStrategyName;

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

    private SamplingPointPlotter series(boolean series) {
        this.series = series;
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

    public SamplingPointPlotter mapStrategyName(String mapStrategyName) {
        this.mapStrategyName = mapStrategyName;
        return this;
    }

    public BufferedImage plot() throws IOException {
        final MapStrategy strategy = getMapStrategy();
        strategy.initialize(samples);

        final BufferedImage image = drawImage(strategy);
        if (!live && show) {
            showImage(image);
        }
        if (filePath != null) {
            writeImage(image);
        }

        return image;
    }

    private BufferedImage drawImage(MapStrategy strategy) {
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        final JComponent component;
        if (live) {
            component = showImage(image);
        } else {
            component = null;
        }
        final Graphics2D graphics = image.createGraphics();

        for (int i = 0, k = 0; i < samples.size(); i++) {
            final SamplingPoint p = samples.get(i);
            final PlotPoint mapPoint = strategy.map(p);

            graphics.fill(new Rectangle(mapPoint.getX(), mapPoint.getY(), 1, 1));
            if (series) {
                if (i % 50 == 0 || i == samples.size() - 1) {
                    try {
                        final File file = createImageFile(k, false);
                        ImageIO.write(image, "png", file);
                        k++;
                    } catch (IOException ignored) {
                    }
                }
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

    private File createImageFile(int count, boolean deleteOnExit) throws IOException {
        final String filename = String.format("%04d.png", count);
        final File file = new File(filename);
        if (deleteOnExit) {
            file.deleteOnExit();
        }
        return file;
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

    private void writeImage(BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(filePath));
    }

    private MapStrategy getMapStrategy() {
        //noinspection IfCanBeSwitch
        if ("timlat".equals(mapStrategyName)) {
            return new TimeLatMapStrategy(WIDTH, HEIGHT);
        } else if ("lonlat".equals(mapStrategyName)) {
            return new LonLatMapStrategy(WIDTH, HEIGHT);
        } else {
            return new LonLatMapStrategy(WIDTH, HEIGHT);
        }
    }

    // for creating a movie
    public static void main(String[] args) throws IOException {
        final WorkflowContext context = new WorkflowContext();
        context.setSampleCount(100000);
        context.setSampleSkip(0);
        context.setStartTime(0L);
        context.setStopTime(2592000000L); // 30 days

        final GenerateSobolPointsWorkflow workflow = new GenerateSobolPointsWorkflow(context);
        final List<SamplingPoint> samples = workflow.execute();

        new SamplingPointPlotter()
                .samples(samples)
                .mapStrategyName("lonlat")
                .series(true)
                .plot();
        // a movie can be created from the image series with 'ffmpeg -i %04d.png sampling.mpg'
    }
}
