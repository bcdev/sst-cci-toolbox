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

package org.esa.cci.sst.tools.arcprocessing;

/**
* Simple container class for holding infos about AVHRR GAC files. To be used by ARC1 and ARC2 processing tools.
*
* @author Thomas Storm
*/
class AvhrrInfo {

    private String matchupId;
    private String filename;
    private String point;

    public String getPoint() {
        return point;
    }

    public void setPoint(final String point) {
        this.point = point;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getMatchupId() {
        return matchupId;
    }

    public void setMatchupId(final String matchupId) {
        this.matchupId = matchupId;
    }
}
