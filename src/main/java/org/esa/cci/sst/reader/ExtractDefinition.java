package org.esa.cci.sst.reader;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ralf
 * Date: 5/18/11
 * Time: 18:50
 * To change this template use File | Settings | File Templates.
 */
public interface ExtractDefinition {

    String getRole();

    double getLat();

    double getLon();

    int[] getOrigin();

    int[] getShape();

    Date getDate();
}
