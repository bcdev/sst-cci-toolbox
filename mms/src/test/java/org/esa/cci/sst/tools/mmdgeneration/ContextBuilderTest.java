package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.tools.Configuration;
import org.junit.Test;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ContextBuilderTest {

    @Test
    public void testBuild_empty() {
        final ContextBuilder contextBuilder = new ContextBuilder();

        final Context context = contextBuilder.build();
        assertNotNull(context);
        assertNull(context.getConfiguration());

        final Map<String, Integer> dimensionConfiguration = context.getDimensionConfiguration();
        assertNotNull(dimensionConfiguration);
        assertEquals(0, dimensionConfiguration.size());

        assertNull(context.getMatchup());
        assertNull(context.getObservation());
        assertNull(context.getObservationReader());
        assertNull(context.getReferenceObservationReader());
        assertNull(context.getTargetVariable());
    }

    @Test
    public void testBuild_matchup() {
        final ContextBuilder contextBuilder = new ContextBuilder();
        final Matchup matchup = new Matchup();
        final Context context = contextBuilder.matchup(matchup).build();

        assertSame(matchup, context.getMatchup());
    }

    @Test
    public void testBuild_observation() {
        final ContextBuilder contextBuilder = new ContextBuilder();
        final Observation observation = new Observation();
        final Context context = contextBuilder.observation(observation).build();

        assertSame(observation, context.getObservation());
    }

    @Test
    public void testBuild_targetVariable() {
        final ContextBuilder contextBuilder = new ContextBuilder();
        final Variable variable = mock(Variable.class);
        final Context context = contextBuilder.targetVariable(variable).build();

        assertSame(variable, context.getTargetVariable());
    }

    @Test
    public void testBuild_dimensionConfiguration() {
        final ContextBuilder contextBuilder = new ContextBuilder();
        final Map<String, Integer> dimensionMap = new HashMap<>();
        final Context context = contextBuilder.dimensionConfiguration(dimensionMap).build();

        final Map<String, Integer> dimensionConfiguration = context.getDimensionConfiguration();
        assertNotNull(dimensionConfiguration);
        assertEquals(0, dimensionConfiguration.size());
    }

    @Test
    public void testBuild_configuration() {
        final ContextBuilder contextBuilder = new ContextBuilder();
        final Configuration configuration = new Configuration();
        final Context context = contextBuilder.configuration(configuration).build();

        assertSame(configuration, context.getConfiguration());
    }
}