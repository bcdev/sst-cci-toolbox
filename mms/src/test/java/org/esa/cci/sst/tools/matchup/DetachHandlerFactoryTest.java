package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tool.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DetachHandlerFactoryTest {

    @Test
    public void testCreate_noOpHandler() {
        final Configuration configuration = new Configuration();

        final DetachHandler handler = DetachHandlerFactory.create(configuration, null);
        assertNotNull(handler);
        assertTrue(handler instanceof NoOpDetachHandler);
    }

    @Test
    public void testCreate_dbDetachHandler() {
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_MMD_DETACH_MATCHUPS, "true");

        final DetachHandler handler = DetachHandlerFactory.create(configuration, persistenceManager);
        assertNotNull(handler);
        assertTrue(handler instanceof DbDetachHandler);
    }
}
