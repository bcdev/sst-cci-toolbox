/*
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

package org.esa.cci.sst.util;

import gov.nasa.gsfc.seadas.watermask.operator.WatermaskClassifier;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Tests for using subrandom sequences for sampling 'dummy locations' in SST CCI.
 *
 * @author Ralf Quast
 */
@Ignore
public class SubrandomTest {

    @Ignore
    @Test
    public void testSubrandom() throws Exception {
        final double c = Math.sqrt(2.0) % 1.0;

        assertTrue(c > 0.0 && c < 1.0);
        final Subrandom subrandom = new Subrandom(c, 27182);

        double s;
        double t;

        s = subrandom.nextDouble();
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

    }

    @Ignore
    @Test
    public void testSubrandom2D() throws Exception {
        final int w = 800;
        final int h = 400;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        final JLabel label = new JLabel(new ImageIcon(image));

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(w, h);
        frame.setVisible(true);

        final Subrandom2D subrandom2D = new SobolSubrandom2D();
        final Random random = new Random(27182);
        final Graphics2D graphics = image.createGraphics();

        final WatermaskClassifier watermaskClassifier = new WatermaskClassifier(WatermaskClassifier.RESOLUTION_10km,
                                                                                WatermaskClassifier.Mode.GSHHS,
                                                                                "GSHHS_water_mask_10km.zip");

        for (int i = 0, j = 0; i <= 10000; i++) {
            final Point2D p = subrandom2D.nextPoint();
            final double x = p.getX();
            final double y = p.getY();
            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;
            final double f = Math.abs(lat) < 30.0 ? 1.0 : Math.cos(Math.toRadians(Math.abs(lat) - 30.0));
            final double z = random.nextDouble();
            if (z <= f) {
                if (watermaskClassifier.isWater((float) lat, (float) lon)) {
                    final int l = (int) (y * h);
                    final int m = (int) (x * w);
                    graphics.draw(new Rectangle(m, l, 1, 1));
                    label.repaint();

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    if (j % 10 == 0) {
                        final File imageFile = new File("/Users/ralf/tmp/png", String.format("%04d", j) + ".png");
                        ImageIO.write(image, "png", imageFile);
                    }
                    j++;
                }
            }
        }
    }

    private static class Subrandom {

        private final double c;
        private double s;

        public Subrandom(double c, long seed) {
            if (c <= 0.0 || c >= 1.0) {
                throw new IllegalArgumentException("Argument c is not in (0, 1).");
            }
            final Random random = new Random(seed);

            this.c = c;
            this.s = random.nextDouble();
        }

        public double nextDouble() {
            return s = (s + c) % 1.0;
        }
    }

    private static class SimpleSubrandom2D implements Subrandom2D {

        private final Point2D.Double c;
        private Point2D.Double s;

        public SimpleSubrandom2D(Point2D.Double c, long seed) {
            if (c.x <= 0.0 || c.x >= 1.0) {
                throw new IllegalArgumentException("Argument c.x is not in (0, 1).");
            }
            if (c.y <= 0.0 || c.y >= 1.0) {
                throw new IllegalArgumentException("Argument c.y is not in (0, 1).");
            }
            final Random random = new Random(seed);

            this.c = new Point2D.Double(c.x, c.y);
            this.s = new Point2D.Double(random.nextDouble(), random.nextDouble());
        }

        @Override
        public Point2D nextPoint() {
            s.x = (s.x + c.x) % 1.0;
            s.y = (s.y + c.y) % 1.0;

            return new Point2D.Double(s.x, s.y);
        }
    }

    private static class SobolSubrandom2D implements Subrandom2D {

        private boolean b;
        private final SobolSequenceGenerator sobolSequenceGenerator;

        public SobolSubrandom2D() throws IOException {
            sobolSequenceGenerator = new SobolSequenceGenerator(201);
        }

        @Override
        public Point2D nextPoint() {
            final double[] doubles = sobolSequenceGenerator.nextVector();

            return new Point2D.Double(doubles[6], doubles[7]);
        }
    }

    private static interface Subrandom2D {

        Point2D nextPoint();
    }
}

