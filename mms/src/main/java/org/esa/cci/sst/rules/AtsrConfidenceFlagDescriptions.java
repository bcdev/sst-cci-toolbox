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
final class AtsrConfidenceFlagDescriptions extends AbstractAttributeModification {

    private static final byte[] FLAG_VALUES = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final String FLAG_MEANINGS = "Blanking_Pulse Cosmetic_Fill_Pixel " +
                                                "Entire_scan_absent_from_telemetry " +
                                                "Pixel_absent_from_telemetry " +
                                                "Pixel_not_decompressed_owing_to_error_in_packet_validation " +
                                                "No_signal_in_some_channel " +
                                                "Saturation_in_some_channel " +
                                                "Derived_radiance_of_some_channel_outside_range_of_calibration " +
                                                "Calibration_Parameters_unavailable_for_pixel " +
                                                "Pixel_unfilled";

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder
                .flagMasks(FLAG_VALUES)
                .flagMeanings(FLAG_MEANINGS);
    }
}
