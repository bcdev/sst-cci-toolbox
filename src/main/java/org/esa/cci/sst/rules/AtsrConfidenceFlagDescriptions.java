/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;

/**
 * Sets the bit mask descriptions and the mask attributes for the ATSR 3 cloud nadir variable.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
public class AtsrConfidenceFlagDescriptions extends AbstractAttributeModification {

    private static final byte[] FLAG_VALUES = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    private static final String FLAG_MEANINGS = "Pixel_is_over_land " +
                                                "Pixel_is_cloudy " +
                                                "Sunglint_detected_in_pixel " +
                                                "1.6_micron_reflectance_histogram_test_shows_pixel_cloudy " +
                                                "1.6_micron_spatial_coherence_test_shows_pixel_cloudy " +
                                                "11_micron_spatial_coherence_test_shows_pixel_cloudy " +
                                                "12_micron_gross_cloud_test_shows_pixel_cloudy " +
                                                "11_12_micron_thin_cirrus_test_shows_pixel_cloudy " +
                                                "3.7_12_micron_medium_high_level_test_shows_pixel_cloudy " +
                                                "11_3.7_micron_fog_low_stratus_test_shows_pixel_cloudy " +
                                                "11_12_micron_view_difference_test_shows_pixel_cloudy " +
                                                "3.7_11_micron_view_difference_test_shows_pixel_cloudy " +
                                                "11_12_micron_thermal_histogram_test_shows_pixel_cloudy";

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder
                .flagMasks(FLAG_VALUES)
                .flagMeanings(FLAG_MEANINGS);
    }
}
