package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

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

import static org.junit.Assert.assertEquals;

public class SamplingToolTest {

    @Test
    public void testCreateSamples() throws Exception {
        final SamplingTool tool = new SamplingTool();

        tool.setCommandLineArgs(new String[0]);
        tool.initialize();

        final List<SamplingPoint> sampleList = tool.createSamples();

        assertEquals(10000, sampleList.size());
    }

    @Test
    public void testRemoveLandSamples() throws Exception {
        final SamplingTool tool = new SamplingTool();

        tool.setCommandLineArgs(new String[0]);
        tool.initialize();

        final List<SamplingPoint> sampleList = tool.createSamples();
        tool.removeLandSamples(sampleList);

        assertEquals(6612, sampleList.size());
    }

    @Test
    public void testReduceClearSamples() throws Exception {
        final SamplingTool tool = new SamplingTool();

        tool.setCommandLineArgs(new String[0]);
        tool.initialize();

        final List<SamplingPoint> sampleList = tool.createSamples();
        tool.removeLandSamples(sampleList);
        tool.reduceClearSamples(sampleList);

        assertEquals(2960, sampleList.size());
    }

    public static void main(String[] args) throws IOException {
        final SamplingTool tool = new SamplingTool();

        tool.setCommandLineArgs(new String[]{"-Dmms.sampling.count=100000"});
        tool.initialize();

        System.out.println("Creating samples...");
        final List<SamplingPoint> sampleList = tool.createSamples();
        System.out.println("Removing land samples...");
        tool.removeLandSamples(sampleList);
        System.out.println("Reducing clear samples...");
        tool.reduceClearSamples(sampleList);

        final int w = 800;
        final int h = 400;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        final JLabel label = new JLabel(new ImageIcon(image));

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(w, h);
        frame.setVisible(true);

        final Graphics2D graphics = image.createGraphics();

        for (SamplingPoint p : sampleList) {
            final double x = (p.getLon() + 180.0) / 360.0;
            final double y = (90.0 - p.getLat()) / 180.0;
            final int i = (int) (y * h);
            final int k = (int) (x * w);
            graphics.draw(new Rectangle(k, i, 1, 1));
            label.repaint();

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
                // ignore
            }
        }

        ImageIO.write(image, "png", new File("sampling.png"));
    }
}
