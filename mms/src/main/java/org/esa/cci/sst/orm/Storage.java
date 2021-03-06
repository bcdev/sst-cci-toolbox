package org.esa.cci.sst.orm;/*
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

import org.esa.cci.sst.data.*;

import java.util.Date;
import java.util.List;

public interface Storage {

    DataFile getDatafile(int id);

    DataFile getDatafile(String path);

    DataFile getDatafileWithTransaction(String path);

    void store(DataFile dataFile);

    int storeWithTransaction(DataFile dataFile);

    Observation getObservation(int id);

    RelatedObservation getRelatedObservation(int id);

    List<RelatedObservation> getRelatedObservations(String sensorName, Date startDate, Date stopDate);

    ReferenceObservation getReferenceObservation(int id);

    Sensor getSensor(String sensorName);

    Sensor getSensorWithTransaction(String sensorName);

    void storeWithTransaction(Sensor sensor);

    void store(InsituObservation insituObservation);
}
