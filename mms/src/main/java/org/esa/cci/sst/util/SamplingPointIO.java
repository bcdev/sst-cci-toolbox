package org.esa.cci.sst.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SamplingPointIO {


    public static void write(List<SamplingPoint> samplingPoints, OutputStream outputStream) throws IOException {
        final SamplingPointList samplingPointList = new SamplingPointList(samplingPoints);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, samplingPointList);
    }

    @SuppressWarnings("unchecked")
    public static List<SamplingPoint> read(InputStream inputStream) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final SamplingPointList samplingPointList = objectMapper.readValue(inputStream, SamplingPointList.class);

        return samplingPointList.getSamplingPoints();
    }

    private static final class SamplingPointList {

        @JsonProperty
        private List<SamplingPoint> samplingPoints;

        @SuppressWarnings("UnusedDeclaration")
        public SamplingPointList() {
            // needed by json tb 2012-02-14
        }

        public SamplingPointList(List<SamplingPoint> samplingPoints) {
            this.samplingPoints = samplingPoints;
        }

        public List<SamplingPoint> getSamplingPoints() {
            return samplingPoints;
        }
    }
}
