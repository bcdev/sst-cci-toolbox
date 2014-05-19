package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ForwardingGeoCodingTest {

    private GeoCoding mockGeoCoding;
    private ForwardingGeoCoding fwdCoding;

    @Before
    public void setUp(){
        mockGeoCoding = mock(GeoCoding.class);
        fwdCoding = new ForwardingGeoCoding(mockGeoCoding);
    }

    @Test
    public void testIsCrossingMeridianAt180() {
        when(mockGeoCoding.isCrossingMeridianAt180()).thenReturn(true);

        assertTrue(fwdCoding.isCrossingMeridianAt180());

        verify(mockGeoCoding, times(1)).isCrossingMeridianAt180();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testCanGetPixelPos() {
        when(mockGeoCoding.canGetPixelPos()).thenReturn(false);

        assertFalse(fwdCoding.canGetPixelPos());

        verify(mockGeoCoding, times(1)).canGetPixelPos();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testCanGetGeoPos() {
        when(mockGeoCoding.canGetGeoPos()).thenReturn(true);

        assertTrue(fwdCoding.canGetGeoPos());

        verify(mockGeoCoding, times(1)).canGetGeoPos();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetPixelPos() {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();

        when(mockGeoCoding.getPixelPos(geoPos, pixelPos)).thenReturn(pixelPos);

        final PixelPos result = fwdCoding.getPixelPos(geoPos, pixelPos);
        assertNotNull(result);

        verify(mockGeoCoding, times(1)).getPixelPos(geoPos, pixelPos);
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetGeoPos() {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();

        when(mockGeoCoding.getGeoPos(pixelPos, geoPos)).thenReturn(geoPos);

        final GeoPos result = fwdCoding.getGeoPos(pixelPos, geoPos);
        assertNotNull(result);

        verify(mockGeoCoding, times(1)).getGeoPos(pixelPos, geoPos);
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetDatum() {
        final Datum datum = mock(Datum.class);
        when(mockGeoCoding.getDatum()).thenReturn(datum);

        final Datum result = fwdCoding.getDatum();
        assertNotNull(result);

        verify(mockGeoCoding, times(1)).getDatum();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testDispose() {
        fwdCoding.dispose();

        verify(mockGeoCoding, times(1)).dispose();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetImageCRS() {
        final CoordinateReferenceSystem crs = mock(CoordinateReferenceSystem.class);
        when(mockGeoCoding.getImageCRS()).thenReturn(crs);

        final CoordinateReferenceSystem imageCRS = fwdCoding.getImageCRS();
        assertNotNull(imageCRS);

        verify(mockGeoCoding, times(1)).getImageCRS();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetMapCRS() {
        final CoordinateReferenceSystem crs = mock(CoordinateReferenceSystem.class);
        when(mockGeoCoding.getMapCRS()).thenReturn(crs);

        final CoordinateReferenceSystem mapCRS = fwdCoding.getMapCRS();
        assertNotNull(mapCRS);

        verify(mockGeoCoding, times(1)).getMapCRS();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetGeoCRS() {
        final CoordinateReferenceSystem crs = mock(CoordinateReferenceSystem.class);
        when(mockGeoCoding.getGeoCRS()).thenReturn(crs);

        final CoordinateReferenceSystem geoCRS = fwdCoding.getGeoCRS();
        assertNotNull(geoCRS);

        verify(mockGeoCoding, times(1)).getGeoCRS();
        verifyNoMoreInteractions(mockGeoCoding);
    }

    @Test
    public void testGetImageToMapTransform() {
        final MathTransform mathTransform = mock(MathTransform.class);
        when(mockGeoCoding.getImageToMapTransform()).thenReturn(mathTransform);

        final MathTransform imageToMapTransform = fwdCoding.getImageToMapTransform();
        assertNotNull(imageToMapTransform);

        verify(mockGeoCoding, times(1)).getImageToMapTransform();
        verifyNoMoreInteractions(mockGeoCoding);
    }
}

