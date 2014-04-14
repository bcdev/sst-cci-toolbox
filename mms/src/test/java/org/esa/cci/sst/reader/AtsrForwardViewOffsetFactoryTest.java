package org.esa.cci.sst.reader;

import org.junit.Before;
import org.junit.Test;

import static org.esa.cci.sst.reader.AtsrForwardViewOffsetFactory.Offset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        try {
            factory.createOffset("AT1", 1990);
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testCreateOffset_ATSR1_1991() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1991);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1992() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1992);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1993() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1993);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1994() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1994);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1995() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1995);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1996() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1996);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1997() throws Exception {
        final Offset offset = factory.createOffset("AT1", 1997);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR1_1998() throws Exception {
        try {
            factory.createOffset("AT1", 1998);
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testCreateOffset_ATSR2_1994() throws Exception {
        try {
            factory.createOffset("AT2", 1994);
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testCreateOffset_ATSR2_1995() throws Exception {
        final Offset offset = factory.createOffset("AT2", 1995);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_1996() throws Exception {
        final Offset offset = factory.createOffset("AT2", 1996);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_1997() throws Exception {
        final Offset offset = factory.createOffset("AT2", 1997);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_1998() throws Exception {
        final Offset offset = factory.createOffset("AT2", 1998);

        assertEquals(-1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_1999() throws Exception {
        final Offset offset = factory.createOffset("AT2", 1999);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_2000() throws Exception {
        final Offset offset = factory.createOffset("AT2", 2000);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_2001() throws Exception {
        final Offset offset = factory.createOffset("AT2", 2001);

        assertEquals(1, offset.getAcrossTrackOffset());
        assertEquals(1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_2002() throws Exception {
        final Offset offset = factory.createOffset("AT2", 2002);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_2003() throws Exception {
        final Offset offset = factory.createOffset("AT2", 2003);

        assertEquals(1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR2_2004() throws Exception {
        try {
            factory.createOffset("AT2", 2004);
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }


    @Test
    public void testCreateOffset_ATSR3_2001() throws Exception {
        try {
            factory.createOffset("ATS", 2001);
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testCreateOffset_ATSR3_2002() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2002);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2003() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2003);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2004() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2004);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2005() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2005);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2006() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2006);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2007() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2007);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2008() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2008);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2009() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2009);

        assertEquals(1, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2010() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2010);

        assertEquals(0, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2011() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2011);

        assertEquals(1, offset.getAcrossTrackOffset());
        assertEquals(-1, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2012() throws Exception {
        final Offset offset = factory.createOffset("ATS", 2012);

        assertEquals(1, offset.getAcrossTrackOffset());
        assertEquals(0, offset.getAlongTrackOffset());
    }

    @Test
    public void testCreateOffset_ATSR3_2013() throws Exception {
        try {
            factory.createOffset("ATS", 2013);
        } catch (IllegalArgumentException expected) {
            //
        }
    }

}
