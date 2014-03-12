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

    @Test
    public void testBuildWithIntFlagMasks() {
        final Item item = builder.flagMasks(8, 2, 7, 1).dimensions("a b").build();
        assertEquals("8 2 7 1", item.getFlagMasks());
    }

    @Test
    public void testBuildWithByteFlagMasks() {
        final Item item = builder.flagMasks((byte)3, (byte)4, (byte)1).dimensions("a b").build();
        assertEquals("3 4 1", item.getFlagMasks());
    }
}
