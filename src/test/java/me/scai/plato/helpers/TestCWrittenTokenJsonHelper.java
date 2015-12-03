package me.scai.plato.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.scai.handwriting.CStroke;
import me.scai.handwriting.CWrittenToken;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by scai on 3/25/2015.
 */
public class TestCWrittenTokenJsonHelper {
    private static final JsonParser jsonParser = new JsonParser();
    private static final double epsilon = 1e-6;

    @Test
    public void testCWrittenToken2JsonObjectWithStrokes() {
        CWrittenToken wt = new CWrittenToken();

        /* First stroke */
        CStroke s0 = new CStroke(0.0f, 0.0f);
        s0.addPoint(1.0f, 1.0f);
        wt.addStroke(s0);

        /* Second stroke */
        CStroke s1 = new CStroke(0.0f, 1.0f);
        s1.addPoint(1.0f, 0.0f);
        s1.addPoint(2.0f, 0.0f);
        wt.addStroke(s1);

        JsonObject wtObj = CWrittenTokenJsonHelper.CWrittenToken2JsonObject(wt);

        assertEquals(wtObj.get("numStrokes").getAsInt(), 2);
        assertTrue(wtObj.get("strokes").isJsonObject());
        assertTrue(wtObj.get("strokes").getAsJsonObject().get("0").isJsonObject());

        JsonObject js0 = wtObj.get("strokes").getAsJsonObject().get("0").getAsJsonObject();
        assertEquals(js0.get("numPoints").getAsInt(), 2);
        assertEquals(js0.get("x").getAsJsonArray().size(), js0.get("numPoints").getAsInt());
        assertEquals(js0.get("y").getAsJsonArray().size(), js0.get("numPoints").getAsInt());

        JsonObject js1 = wtObj.get("strokes").getAsJsonObject().get("1").getAsJsonObject();
        assertEquals(js1.get("numPoints").getAsInt(), 3);
        assertEquals(js1.get("x").getAsJsonArray().size(), js1.get("numPoints").getAsInt());
        assertEquals(js1.get("y").getAsJsonArray().size(), js1.get("numPoints").getAsInt());
    }

    @Test
    public void testCWrittenToken2JsonNoStroke() {
        String testJSON = "{\"numStrokes\":2,\"strokes\":{\"0\":{\"numPoints\":22,\"x\":[106,109,120,127,136,150,168,205,246,267,285,325,342,357,370,384,415,427,439,441,448,443],\"y\":[182,184,185,187,188,190,193,199,205,206,209,212,214,215,217,217,218,218,218,220,220,220]},\"1\":{\"numPoints\":23,\"x\":[284,282,279,278,276,276,276,276,276,276,277,277,279,279,280,280,280,282,282,282,281,281,281],\"y\":[75,75,82,89,98,110,124,151,164,181,196,212,242,257,271,281,292,307,310,314,323,328,329]}}}";

        CWrittenToken wt = new CWrittenToken(testJSON);

        /* 1. No recogWinner */
        String wtJson = CWrittenTokenJsonHelper.CWrittenToken2JsonNoStroke(wt);

        JsonObject wtJsonObj = jsonParser.parse(wtJson).getAsJsonObject();
        assertEquals(wtJsonObj.get("width").getAsFloat(), 342.0, epsilon);
        assertEquals(wtJsonObj.get("height").getAsFloat(), 254.0, epsilon);

        assertEquals(wtJsonObj.get("bounds").getAsJsonArray().size(), 4);

        /* 2. Has recogWinner */
        wt.setRecogResult("w");
        wtJson = CWrittenTokenJsonHelper.CWrittenToken2JsonNoStroke(wt);

        wtJsonObj = jsonParser.parse(wtJson).getAsJsonObject();
        assertEquals(wtJsonObj.get("width").getAsFloat(), 342.0, epsilon);
        assertEquals(wtJsonObj.get("height").getAsFloat(), 254.0, epsilon);

        assertEquals(wtJsonObj.get("bounds").getAsJsonArray().size(), 4);
        assertEquals(wtJsonObj.get("recogWinner").getAsString(), "w");

        /* 3. Has recogPVals */
        double [] dummyRecogPs = {0.1, 0.2, 0.5};
        wt.setRecogPs(dummyRecogPs);
        wtJson = CWrittenTokenJsonHelper.CWrittenToken2JsonNoStroke(wt);

        wtJsonObj = jsonParser.parse(wtJson).getAsJsonObject();
        assertEquals(wtJsonObj.get("width").getAsFloat(), 342.0, epsilon);
        assertEquals(wtJsonObj.get("height").getAsFloat(), 254.0, epsilon);

        assertEquals(wtJsonObj.get("bounds").getAsJsonArray().size(), 4);
        assertEquals(wtJsonObj.get("recogWinner").getAsString(), "w");

        JsonArray verify_recogPs = wtJsonObj.get("recogPs").getAsJsonArray();
        assertEquals(verify_recogPs.size(), dummyRecogPs.length);
        for (int i = 0; i < dummyRecogPs.length; ++i) {
            assertEquals(verify_recogPs.get(i).getAsDouble(), dummyRecogPs[i], epsilon);
        }
    }




}
