package org.esa.cci.sst.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColumnBuilderTest {

    private ColumnBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new ColumnBuilder().rank(2);
    }

    @Test
    public void testSetDimensions() {
        builder.dimensions("");
        builder.dimensions("a");
        builder.dimensions("a b");
        builder.dimensions("a b c");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithLeadingWhitespace() {
        builder.dimensions(" a b");
        builder.dimensions("a  b");
        builder.dimensions("a b ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithTooMuchWhitespace() {
        builder.dimensions("a  b");
        builder.dimensions("a b ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDimensions_WithTrailingWhitespace() {
        builder.dimensions("a b ");
    }

    @Test
    public void testBuild() {
        assertEquals(2, builder.dimensions("a b").build().getRank());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithEmptyDimensionsString() {
        builder.dimensions("").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithTooFewDimensions() {
        builder.dimensions("a").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_WithTooManyDimensions() {
        builder.dimensions("a b c").build();
    }
}
