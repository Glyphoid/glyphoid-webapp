package me.scai.plato.publishers;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.clienttypes.ClientTypeMajor;
import me.scai.utilities.clienttypes.ClientTypeMinor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestPlatoRequestPublisherS3 {
    private static Map<String, WorkerClientInfo> mockWorkersClientInfo;

    private static final String TIME_ZONE = "UTC";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    static PlatoRequestPublisher publisher;

    @BeforeClass
    public static void setUp() {
        mockWorkersClientInfo = new HashMap<>();

        final TimeZone utcTimeZone = TimeZone.getTimeZone(TIME_ZONE);
        dateFormat.setTimeZone(utcTimeZone);

        final String engineId = "mock-engine-id-1";
        WorkerClientInfo workerClientInfo = null;
        try {
            workerClientInfo = new WorkerClientInfo(InetAddress.getLocalHost(),
                    "localhost.localdomain",
                    ClientTypeMajor.API,
                    ClientTypeMinor.API_UnitTest);
        } catch (UnknownHostException exc) {
            fail();
        }

        mockWorkersClientInfo.put(engineId, workerClientInfo);

        /* Obtain S3 publisher instance */
        publisher = PlatoRequestPublisherS3.createOrGetInstance(mockWorkersClientInfo);
    }

    @AfterClass
    public static void tearDown() {
        if (publisher != null) {
            publisher.destroy();
        }
    }

    @Test
    public void testPublisherS3_createEngineRequest() {

        JsonObject reqObj = new JsonObject();
        reqObj.add("timestamp", new JsonPrimitive(dateFormat.format(new Date())));

        reqObj.add("action", new JsonPrimitive("foo-action"));

        String publishedKey = null;
        try {
            publishedKey = publisher.publishCreateEngineRequest(reqObj,
                    InetAddress.getLocalHost(),
                    "localhost.localdomain",
                    "mock-engine-id-1");
        } catch (UnknownHostException exc) {
            fail();
        }

//        assertNotNull(publishedKey);
        assertFalse(reqObj.has("engineUuid"));

    }

    @Test
    public void testPublisherS3_generalRequest() {
        JsonObject reqObj = new JsonObject();
        reqObj.add("engineUuid", new JsonPrimitive("mock-engine-id-1"));
        reqObj.add("timestamp", new JsonPrimitive(dateFormat.format(new Date())));

        reqObj.add("action", new JsonPrimitive("foo-action"));

        String publishedKey = publisher.publishGeneralRequest(reqObj);
//        assertNotNull(publishedKey);

    }
}
