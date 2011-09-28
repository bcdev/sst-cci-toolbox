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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MatchupPatternNoPmwTest {

    @Test
    public void testRemovePmwPatterns() throws Exception {
        assertEquals(1170654428143681538L, MatchupPatternNoPmw.removePmwPatterns(1170654428156264450L));
        assertEquals(5782340446571069473L, MatchupPatternNoPmw.removePmwPatterns(5782340446583652385L));
        assertEquals(1170654428143681569L, MatchupPatternNoPmw.removePmwPatterns(1170654428152070177L));
        assertEquals(5782340446571069442L, MatchupPatternNoPmw.removePmwPatterns(5782340446583652354L));
    }
}
