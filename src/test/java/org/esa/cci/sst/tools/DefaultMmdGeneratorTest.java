/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.VariableDescriptor;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class DefaultMmdGeneratorTest {

    @Test
    public void testCreateDimString() throws Exception {
        final VariableDescriptor descriptor1 = new VariableDescriptor();
        descriptor1.setDimensionRoles("match_up ni nj");
        descriptor1.setDimensions("matchup_id ni nj");
        final String dimString1 = DefaultMmdGenerator.createDimensionString(descriptor1, SensorType.ATSR_MD);

        VariableDescriptor descriptor2 = new VariableDescriptor();
        descriptor2.setDimensionRoles("match_up ni nj");
        descriptor2.setDimensions("matchup_id ni nj");
        final String dimString2 = DefaultMmdGenerator.createDimensionString(descriptor2, SensorType.AAI);

        VariableDescriptor descriptor3 = new VariableDescriptor();
        descriptor3.setDimensionRoles("match_up length");
        descriptor3.setDimensions("matchup_id kasperkopp");
        final String dimString3 = DefaultMmdGenerator.createDimensionString(descriptor3, SensorType.ATSR_MD);

        VariableDescriptor descriptor4 = new VariableDescriptor();
        descriptor4.setDimensionRoles("match_up length");
        descriptor4.setDimensions("match_up atsr_md.ui_length");
        final String dimString4 = DefaultMmdGenerator.createDimensionString(descriptor4, SensorType.ATSR_MD);

        assertEquals("match_up atsr_md.ni atsr_md.nj", dimString1);
        assertEquals("match_up aai.ni aai.nj", dimString2);
        assertEquals("match_up atsr_md.kasperkopp", dimString3);
        assertEquals("match_up atsr_md.ui_length", dimString4);
    }
}
