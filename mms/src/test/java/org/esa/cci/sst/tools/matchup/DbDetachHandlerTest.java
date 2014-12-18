package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.orm.PersistenceManager;
import org.junit.Test;

import static org.mockito.Mockito.*;

@SuppressWarnings("UnnecessaryBoxing")
public class DbDetachHandlerTest {

    @Test
    public void testDetach() {
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);
        final Double objectToDetach = new Double(99.6);

        final DbDetachHandler detachHandler = new DbDetachHandler(persistenceManager);
        detachHandler.detach(objectToDetach);

        verify(persistenceManager, times(1)).detach(objectToDetach);
        verifyNoMoreInteractions(persistenceManager);
    }
}
