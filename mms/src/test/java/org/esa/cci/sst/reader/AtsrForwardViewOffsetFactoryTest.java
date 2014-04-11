package org.esa.cci.sst.reader;

import org.junit.Before;
import org.junit.Test;

import static org.esa.cci.sst.reader.AtsrForwardViewOffsetFactory.Offset;
import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class AtsrForwardViewOffsetFactoryTest {

    private AtsrForwardViewOffsetFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new AtsrForwardViewOffsetFactory();
    }

    @Test
    public void testCreateOffset_ATSR1_1990() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1990);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1991() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1991);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1992() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1992);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1993() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1993);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1994() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1994);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1995() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1995);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1996() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1996);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1997() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1997);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1998() throws Exception {
        final Offset offset = factory.createOffset("ATS1", 1998);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    // TODO - ATSR2 and ATSR3 tests
}
