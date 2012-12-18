package org.esa.cci.sst.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 18.12.12 09:19
 */
public class TemporalResolutionTest {

    @Test
    public void testValuesForAveraging() throws Exception {
        assertEquals("[daily, monthly, seasonal, annual]", TemporalResolution.valuesForAveraging());
    }
}
