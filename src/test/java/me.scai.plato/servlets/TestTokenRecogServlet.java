package me.scai.plato.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TestTokenRecogServlet {
    /* Constants */
    private static final JsonParser jsonParser = new JsonParser();

    /* Member variables */
    private TokenRecogServlet tokenRecogServlet;

    @Before
    public void setUp() {
        tokenRecogServlet = new TokenRecogServlet();

        try {
            tokenRecogServlet.init();
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testTokenRecogServlet() {
        final String reqBody = "{\n" +
                "  \"numStrokes\": 1,\n" +
                "  \"strokes\": {\n" +
                "    \"0\": {\n" +
                "      \"numPoints\": 43,\n" +
                "      \"x\": [\n" +
                "        257,\n" +
                "        258,\n" +
                "        261,\n" +
                "        263,\n" +
                "        265,\n" +
                "        268,\n" +
                "        270,\n" +
                "        274,\n" +
                "        276,\n" +
                "        281,\n" +
                "        283,\n" +
                "        287,\n" +
                "        289,\n" +
                "        292,\n" +
                "        295,\n" +
                "        299,\n" +
                "        302,\n" +
                "        306,\n" +
                "        308,\n" +
                "        309,\n" +
                "        311,\n" +
                "        313,\n" +
                "        314,\n" +
                "        314,\n" +
                "        314,\n" +
                "        313,\n" +
                "        312,\n" +
                "        311,\n" +
                "        309,\n" +
                "        309,\n" +
                "        307,\n" +
                "        306,\n" +
                "        304,\n" +
                "        302,\n" +
                "        301,\n" +
                "        300,\n" +
                "        299,\n" +
                "        298,\n" +
                "        297,\n" +
                "        296,\n" +
                "        296,\n" +
                "        296,\n" +
                "        296\n" +
                "      ],\n" +
                "      \"y\": [\n" +
                "        61.00000000000001,\n" +
                "        60,\n" +
                "        60,\n" +
                "        60,\n" +
                "        60,\n" +
                "        60,\n" +
                "        60,\n" +
                "        59,\n" +
                "        58.00000000000001,\n" +
                "        58.00000000000001,\n" +
                "        56.99999999999999,\n" +
                "        56,\n" +
                "        56,\n" +
                "        55,\n" +
                "        53.99999999999999,\n" +
                "        53.99999999999999,\n" +
                "        53.99999999999999,\n" +
                "        53.99999999999999,\n" +
                "        53.99999999999999,\n" +
                "        53,\n" +
                "        53,\n" +
                "        53,\n" +
                "        53,\n" +
                "        53.99999999999999,\n" +
                "        56,\n" +
                "        58.00000000000001,\n" +
                "        62,\n" +
                "        66,\n" +
                "        69,\n" +
                "        72,\n" +
                "        76,\n" +
                "        79,\n" +
                "        84,\n" +
                "        88,\n" +
                "        91,\n" +
                "        93,\n" +
                "        97,\n" +
                "        101,\n" +
                "        103,\n" +
                "        105,\n" +
                "        106,\n" +
                "        107,\n" +
                "        107.99999999999999\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";


        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        req.setContent(reqBody.getBytes());

        try {
            tokenRecogServlet.doPost(req, resp);
        } catch (IOException exc) {
            fail(exc.getMessage());
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(resp.getContentAsString()).getAsJsonObject();
        } catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertNotNull(respObj);
        assertEquals("7", respObj.get("winnerTokenName").getAsString());

        JsonArray recogPVals = respObj.get("recogPVals").getAsJsonArray();

        assertTrue(recogPVals.size() > 0);

        final int nCandidates = recogPVals.size();
        Set<String> uniqueTokenNames = new HashSet<>();

        double currPVal = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nCandidates; ++i) {
            uniqueTokenNames.add(recogPVals.get(i).getAsJsonArray().get(0).getAsString());

            final double p = recogPVals.get(i).getAsJsonArray().get(1).getAsDouble();

            assertTrue(p >= 0.0);
            assertTrue(currPVal >= p);  // Check the descending order of the p-values

            currPVal = p;
        }

        // Assert that there is no duplicate token name in recogPVals
        assertEquals(nCandidates, uniqueTokenNames.size());
    }

}
