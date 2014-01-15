package org.esa.cci.sst.util;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: THIS FILE HAS BEEN MODIFIED BY BC TO SUIT PARTICULAR NEEDS.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Implementation of a Sobol sequence.
 * <p/>
 * A Sobol sequence is a low-discrepancy sequence with the property that for all values of N,
 * its subsequence (x1, ... xN) has a low discrepancy. It can be used to generate pseudo-random
 * points in a space S, which are equi-distributed.
 * <p/>
 * The implementation already comes with support for up to 21201 dimensions with direction numbers
 * calculated from <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Stephen Joe and Frances Kuo</a>.
 * <p/>
 * The generator supports two modes:
 * <ul>
 * <li>sequential generation of points: {@link #nextVector()}</li>
 * <li>random access to the i-th point in the sequence: {@link #skipTo(int)}</li>
 * </ul>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Sobol_sequence">Sobol sequence (Wikipedia)</a>
 * @see <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Sobol sequence direction numbers</a>
 */
public class SobolSequenceGenerator {

    /**
     * The number of bits to use.
     */
    private static final int BITS = 52;

    /**
     * The scaling factor.
     */
    private static final double SCALE = (double) (1L << BITS); //Math.pow(2, BITS);

    /**
     * The maximum supported space dimension.
     */
    private static final int MAX_DIMENSION = 1000;

    /**
     * The resource containing the direction numbers.
     */
    private static final String DIRECTION_NUMBERS_RESOURCE_NAME = "new-joe-kuo-6.21201.txt";

    /**
     * Character set for file input.
     */
    private static final String FILE_CHARSET = "US-ASCII";

    /**
     * Space dimension.
     */
    private final int dimension;

    /**
     * The current index in the sequence.
     */
    private int count = 0;

    /**
     * The direction vector for each component.
     */
    private final long[][] direction;

    /**
     * The current state.
     */
    private final long[] x;

    /**
     * Construct a new Sobol sequence generator for the given space dimension.
     *
     * @param dimension the space dimension
     */
    public SobolSequenceGenerator(final int dimension) {
        if (dimension < 1 || dimension > MAX_DIMENSION) {
            throw new IllegalArgumentException("dimension < 1 || dimension > MAX_DIMENSION"); // TODO - message
        }

        // initialize the other dimensions with direction numbers from a resource
        final InputStream is = getClass().getResourceAsStream(DIRECTION_NUMBERS_RESOURCE_NAME);
        if (is == null) {
            throw new IllegalStateException("The internal resource file could not be read.");
        }

        this.dimension = dimension;

        // init data structures
        direction = new long[dimension][BITS + 1];
        x = new long[dimension];

        try {
            initFromStream(is);
        } catch (IOException e) {
            // the internal resource file could not be read; should not happen
            throw new IllegalStateException("The internal resource file could not be read.");
        } catch (NoSuchElementException | NumberFormatException e) {
            // the internal resource file could not be parsed; should not happen
            throw new IllegalStateException("The internal resource file could not be parsed.");
        } finally {
            try {
                is.close();
            } catch (IOException e) { // NOPMD
                // ignore
            }
        }
    }

    /**
     * Load the direction vector for each dimension from the given stream.
     * <p/>
     * The input stream <i>must</i> be an ASCII text containing one
     * valid direction vector per line.
     *
     * @param is the input stream to read the direction vector from
     *
     * @return the last dimension that has been read from the input stream
     *
     * @throws java.io.IOException if the stream could not be read
     */
    private int initFromStream(final InputStream is) throws IOException {
        // special case: dimension 1 -> use unit initialization
        for (int i = 1; i <= BITS; i++) {
            direction[0][i] = 1l << (BITS - i);
        }

        final Charset charset = Charset.forName(FILE_CHARSET);
        int dim = -1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            // ignore first line
            reader.readLine();

            int lineNumber = 2;
            int index = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " ");
                try {
                    dim = Integer.parseInt(st.nextToken());
                    if (dim >= 2 && dim <= dimension) { // we have found the right dimension
                        final int s = Integer.parseInt(st.nextToken());
                        final int a = Integer.parseInt(st.nextToken());
                        final int[] m = new int[s + 1];
                        for (int i = 1; i <= s; i++) {
                            m[i] = Integer.parseInt(st.nextToken());
                        }
                        initDirectionVector(index++, a, m);
                    }

                    if (dim > dimension) {
                        return dim;
                    }
                } catch (NoSuchElementException | NumberFormatException e) {
                    throw new NoSuchElementException(
                            "Could not parse line '" + line + "' in line number " + lineNumber);
                }
                lineNumber++;
            }
        }

        return dim;
    }

    /**
     * Calculate the direction numbers from the given polynomial.
     *
     * @param d the dimension, zero-based
     * @param a the coefficients of the primitive polynomial
     * @param m the initial direction numbers
     */
    private void initDirectionVector(final int d, final int a, final int[] m) {
        final int s = m.length - 1;
        for (int i = 1; i <= s; i++) {
            direction[d][i] = ((long) m[i]) << (BITS - i);
        }
        for (int i = s + 1; i <= BITS; i++) {
            direction[d][i] = direction[d][i - s] ^ (direction[d][i - s] >> s);
            for (int k = 1; k <= s - 1; k++) {
                direction[d][i] ^= ((a >> (s - 1 - k)) & 1) * direction[d][i - k];
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public double[] nextVector() {
        final double[] v = new double[dimension];
        if (count == 0) {
            count++;
            return v;
        }

        // find the index c of the rightmost 0
        int c = 1;
        int value = count - 1;
        while ((value & 1) == 1) {
            value >>= 1;
            c++;
        }

        for (int i = 0; i < dimension; i++) {
            x[i] = x[i] ^ direction[i][c];
            v[i] = (double) x[i] / SCALE;
        }
        count++;
        return v;
    }

    /**
     * Skip to the i-th point in the Sobol sequence.
     * <p/>
     * This operation can be performed in O(1).
     *
     * @param index the index in the sequence to skip to
     *
     * @return the i-th point in the Sobol sequence
     */
    public double[] skipTo(final int index) {
        if (index == 0) {
            // reset x vector
            Arrays.fill(x, 0);
        } else {
            final int i = index - 1;
            final long grayCode = i ^ (i >> 1); // compute the gray code of i = i XOR floor(i / 2)
            for (int j = 0; j < dimension; j++) {
                long result = 0;
                for (int k = 1; k <= BITS; k++) {
                    final long shift = grayCode >> (k - 1);
                    if (shift == 0) {
                        // stop, as all remaining bits will be zero
                        break;
                    }
                    // the k-th bit of i
                    final long ik = shift & 1;
                    result ^= ik * direction[j][k];
                }
                x[j] = result;
            }
        }
        count = index;
        return nextVector();
    }

    /**
     * Returns the index i of the next point in the Sobol sequence that will be returned
     * by calling {@link #nextVector()}.
     *
     * @return the index of the next point
     */
    public int getNextIndex() {
        return count;
    }
}
