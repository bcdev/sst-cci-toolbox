/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.ingestion;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * For testing filename patterns used in configuration files for ingesting satellite data.
 *
 * @author Ralf Quast
 */
public class FilenamePatternTest {

    private static final String EXAMPLE_FILENAME_AMSR2_1 = "GW1AM2_201212010013_176A_L1SGRTBR_1110110.h5";
    private static final String EXAMPLE_FILENAME_AMSR2_2 = "GW1AM2_201212010013_176A_L1SGRTBR_1110110.h5.gz";

    private static final String EXAMPLE_FILENAME_AVHRR_F_M01_1 = "AVHR_xxx_1B_M01_20121224003103Z_20121224021303Z_N_C_20121224021356Z.nat";
    private static final String EXAMPLE_FILENAME_AVHRR_F_M01_2 = "AVHR_xxx_1B_M01_20121224003103Z_20121224021303Z_N_C_20121224021356Z.nat.gz";

    private static final String EXAMPLE_FILENAME_AVHRR_F_M02_1 = "AVHR_xxx_1B_M02_20121201005503Z_20121201023703Z_N_O_20121201014146Z.nat";
    private static final String EXAMPLE_FILENAME_AVHRR_F_M02_2 = "AVHR_xxx_1B_M02_20121201005503Z_20121201023703Z_N_O_20121201014146Z.nat.gz";

    private static final String FILENAME_PATTERN_AMSR2 = "GW1AM2_[0-9]{12}_.{4}_L1SGRTBR_[0-9]{7}\\.h5(\\.gz)?";
    private static final String FILENAME_PATTERN_AVHRR_F_M01 = "AVHRR_xxx_1B_M01_[0-9]{14}Z_[0-9]{14}Z_._._[0-9]{14}\\.nat(\\.gz)?";
    private static final String FILENAME_PATTERN_AVHRR_F_M02 = "AVHRR_xxx_1B_M02_[0-9]{14}Z_[0-9]{14}Z_._._[0-9]{14}\\.nat(\\.gz)?";

    @Test
    public void testFilenamePattern_AMSR2() throws Exception {
        assertTrue(EXAMPLE_FILENAME_AMSR2_1.matches(
                FILENAME_PATTERN_AMSR2));
        assertTrue(EXAMPLE_FILENAME_AMSR2_2.matches(
                FILENAME_PATTERN_AMSR2));
    }

    @Test
    public void testFilenamePattern_AVHRR_F_M01() throws Exception {
        assertTrue(EXAMPLE_FILENAME_AVHRR_F_M01_1.matches(
                FILENAME_PATTERN_AVHRR_F_M01));
        assertTrue(EXAMPLE_FILENAME_AVHRR_F_M01_2.matches(
                FILENAME_PATTERN_AVHRR_F_M01));
    }

    @Test
    public void testFilenamePattern_AVHRR_F_M02() throws Exception {
        assertTrue(EXAMPLE_FILENAME_AVHRR_F_M02_1.matches(
                FILENAME_PATTERN_AVHRR_F_M02));
        assertTrue(EXAMPLE_FILENAME_AVHRR_F_M02_2.matches(
                FILENAME_PATTERN_AVHRR_F_M02));
    }
}
