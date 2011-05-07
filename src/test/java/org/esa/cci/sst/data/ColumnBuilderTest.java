package org.esa.cci.sst.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColumnBuilderTest {

    private ColumnBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new ColumnBuilder().setRank(2);
    }

    @Test
    public void testSetDimensions() {
        builder.setDimensions("");
        builder.setDimensions("a");
        builder.setDimensions("a b");
        builder.setDimensions("a b c");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithLeadingWhitespace() {
        builder.setDimensions(" a b");
        builder.setDimensions("a  b");
        builder.setDimensions("a b ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithTooMuchWhitespace() {
        builder.setDimensions("a  b");
        builder.setDimensions("a b ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithTrailingWhitespace() {
        builder.setDimensions("a b ");
    }

    @Test
    public void testBuild() {
        assertEquals(2, builder.setDimensions("a b").build().getRank());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithEmptyDimensionsString() {
        builder.setDimensions("").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithTooFewDimensions() {
        builder.setDimensions("a").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithTooManyDimensions() {
        builder.setDimensions("a b c").build();
    }
}
