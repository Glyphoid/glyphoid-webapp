/**
 * Created by scai on 3/25/2015.
 */

package me.scai.plato.helpers;

import me.scai.plato.serverutils.PropertiesHelper;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.clienttypes.ClientTypeMajor;
import me.scai.utilities.clienttypes.ClientTypeMinor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

public class TestHandwritingEnginePool {
    private WorkerClientInfo wkrClientInfo;

    private int getMaxNumHandwritingEngines() {
        Properties props = PropertiesHelper.getProperties();

        return Integer.parseInt(props.getProperty("maxNumHandwritingEngines"));
    }

    @Before
    public void beforeTest() {
        wkrClientInfo = null;
        try {
            wkrClientInfo = new WorkerClientInfo(InetAddress.getLocalHost(),
                                                 InetAddress.getLocalHost().getHostName(),
                                                 ClientTypeMajor.API,
                                                 ClientTypeMinor.API_UnitTest);
        } catch (UnknownHostException uhExc) {
            wkrClientInfo = new WorkerClientInfo(null, null, ClientTypeMajor.API, ClientTypeMinor.API_UnitTest);
        }
    }

    @Test
    public void test1() {
        HandwritingEnginePool hwEngPool = null;
        try {
            hwEngPool = new HandwritingEnginePool();
        } catch (IOException exc) {

        }

        int maxNumEngines = getMaxNumHandwritingEngines();
        assertTrue(maxNumEngines > 0);

        /* Successful creation of new handwriting engines */
        for (int i = 0; i < maxNumEngines; ++i) {
            assertEquals(hwEngPool.numEngines(), i);
            String newEngUuid = hwEngPool.genNewHandwritingEngine(wkrClientInfo);
            assertNotNull(newEngUuid);
            assertEquals(hwEngPool.numEngines(), i + 1);

            /* Try getting the reference to the newly created engine */
            HandwritingEngineImpl hwEng = hwEngPool.getHandwritingEngine(newEngUuid);
            assertNotNull(hwEng);
            assertNotNull(hwEng.tokenSetParser);
            assertNotNull(hwEng.strokeCurator);
            assertNotNull(hwEng.stringizer);
            assertNotNull(hwEng.evaluator);
        }

        /* Creation of new engine is unsuccessful, due to the fact that count limit has been reached */
        String newEngUuid = hwEngPool.genNewHandwritingEngine(wkrClientInfo);
        assertNull(newEngUuid);
        assertEquals(hwEngPool.numEngines(), maxNumEngines);

        /* Clear all the engines */
        hwEngPool.clear();
        assertEquals(hwEngPool.numEngines(), 0);

        /* Try creating a new engine after clearing the pool. This should succeed */
        newEngUuid = hwEngPool.genNewHandwritingEngine(wkrClientInfo);
        assertNotNull(newEngUuid);
        assertTrue(hwEngPool.engineExists(newEngUuid));
        assertEquals(hwEngPool.numEngines(), 1);

        /* Test the removal of a non-existent engine */
        hwEngPool.remove(UUID.randomUUID().toString());
        assertEquals(hwEngPool.numEngines(), 1);
        assertTrue(hwEngPool.engineExists(newEngUuid));

        /* Test the removal of the just-created engine */
        hwEngPool.remove(newEngUuid);
        assertEquals(hwEngPool.numEngines(), 0);

        try {
            assertFalse(hwEngPool.engineExists(newEngUuid));
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {}
    }

}
