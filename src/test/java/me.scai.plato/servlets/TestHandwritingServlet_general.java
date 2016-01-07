package me.scai.plato.servlets;

import com.google.gson.*;
import me.scai.handwriting.HandwritingEngineUserAction;
import me.scai.parsetree.MathHelper;
import me.scai.parsetree.evaluation.ValueUnion;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestHandwritingServlet_general {


    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    private HandwritingServlet hwServlet;
    HandwritingServletTestHelper helper;

    private static final int uuidStringLen = 36;

    private static final float FLOAT_TOL = 1e-9f;
    private static final double DOUBLE_TOL = 1e-9;

    private static final double epsilon = 1e-6;
    private static final float floatTol = 1e-6f;

    private String engineUuid;

    @Before
    public void setUp() {
        hwServlet = new HandwritingServlet();
        helper = new HandwritingServletTestHelper(hwServlet);

        try {
            hwServlet.init();
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        engineUuid = helper.createEngineClear();
    }

    @After
    public void tearDown() {
        if (engineUuid != null) {
            MockHttpServletRequest reqRemoveEngine = new MockHttpServletRequest();
            MockHttpServletResponse respRemoveEngine = new MockHttpServletResponse();

            String reqBodyRemoveEngine = "{\"action\":\"remove-engine\",\"engineUuid\":\"" + engineUuid + "\"}";
            reqRemoveEngine.setContent(reqBodyRemoveEngine.getBytes());
            try {
                hwServlet.doPost(reqRemoveEngine, respRemoveEngine);
            } catch (IOException exc) {
                fail(exc.getMessage());
            } catch (ServletException exc) {
                fail(exc.getMessage());
            } catch (IllegalArgumentException exc) {
                fail(exc.getMessage());
            }
        }

    }

    @Test
    public void testCreateEngine() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String reqBody = "{\"action\":\"create-engine\",\"ClientTypeMajor\":\"API\",\"ClientTypeMinor\":\"API_UnitTest\"}";
        req.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(req, resp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(resp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respObj.get("errors").getAsJsonArray().size(), 0);
        String uuidString = respObj.get("engineUuid").getAsString();
        assertEquals(uuidString.length(), uuidStringLen);
    }

    @Test
    public void testClearEngines() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String reqBody = "{\"action\" : \"clear-engines\"}";
        req.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(req, resp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(resp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respObj.get("errors").getAsJsonArray().size(), 0);
        assertEquals(respObj.get("actionTaken").getAsString(), "clear-engines");

        engineUuid = null;
    }

    @Test
    public void testGetAllTokenNames() {
        /* Obtain new engine handle */
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String reqBody = "{\"action\" : \"get-all-token-names\", \"engineUuid\": \"" + engineUuid + "\"}";
        req.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(req, resp);
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

        assertTrue(respObj.has("errors"));
        assertTrue(respObj.get("errors").isJsonArray());
        assertEquals(0, respObj.get("errors").getAsJsonArray().size());

        assertTrue(respObj.has("allTokenNames"));
        assertTrue(respObj.get("allTokenNames").isJsonArray());

        JsonArray allTokenNames = respObj.get("allTokenNames").getAsJsonArray();
        assertTrue(allTokenNames.size() > 0);
    }

    @Test
    public void testAddClearStroke0() {
        /* Add a new stroke */
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String reqBody = "{\"action\" : \"add-stroke\", \"engineUuid\": \"" + engineUuid + "\", " +
                         "\"stroke\": {\"numPoints\": 34, \"x\": [202,202,202,201,200,199,197,197,197,197,197,197,196,196,196,196,196,194,194,194,192,192,191,191,190,190,189,189,187,186,186,185,184,183], \"y\": [55,58,64,68,76,83,95,103,112,122,129,134,141,147,153,158,165,171,175,181,187,190,195,197,200,203,209,212,214,217,219,222,223,224]}}";
        req.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(req, resp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(resp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respObj.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObj.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");

        JsonArray constituentStrokes = respObj.get("constituentStrokes").getAsJsonArray();
        assertEquals(constituentStrokes.size(), 1);

        JsonArray constStrokes0 = constituentStrokes.get(0).getAsJsonArray();
        assertEquals(constStrokes0.size(), 1);
        assertEquals(constStrokes0.get(0).getAsInt(), 0);

        assertEquals(HandwritingEngineUserAction.AddStroke.toString(), respObj.get("lastStrokeCuratorUserAction").getAsString());
        assertTrue(respObj.get("canUndoStrokeCuratorUserAction").getAsBoolean());
        assertFalse(respObj.get("canRedoStrokeCuratorUserAction").getAsBoolean());

        /* Clear strokes */
        MockHttpServletRequest reqClear = new MockHttpServletRequest();
        MockHttpServletResponse respClear = new MockHttpServletResponse();

        String reqBodyClear = "{\"action\" : \"clear\", \"engineUuid\": \"" + engineUuid + "\"}";
        reqClear.setContent(reqBodyClear.getBytes());
        try {
            hwServlet.doPost(reqClear, respClear);
        } catch (IOException exc) {
            fail(exc.getMessage());
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObjClear = null;
        try {
            respObjClear = jsonParser.parse(respClear.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respObjClear.get("errors").getAsJsonArray().size(), 0);

        JsonObject tokenSet = respObjClear.get("tokenSet").getAsJsonObject();
        tokens = tokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 0);

        writtenTokenSet = respObjClear.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 0);

        constituentStrokes = respObjClear.get("constituentStrokes").getAsJsonArray();
        assertEquals(constituentStrokes.size(), 0);

        assertEquals(HandwritingEngineUserAction.ClearStrokes.toString(), respObjClear.get("lastStrokeCuratorUserAction").getAsString());
        assertTrue(respObjClear.get("canUndoStrokeCuratorUserAction").getAsBoolean());
        assertFalse(respObjClear.get("canRedoStrokeCuratorUserAction").getAsBoolean());
    }

    // TODO: Get token bounds for token sets with node tokens
    @Test
    public void testGetTokenBounds() {
        /* Add a new stroke */
        MockHttpServletRequest addStrokeReq = new MockHttpServletRequest();
        MockHttpServletResponse addStrokeResp = new MockHttpServletResponse();

        String addStrokeReqBody = "{\"action\" : \"add-stroke\", \"engineUuid\": \"" + engineUuid + "\", " +
                         "\"stroke\": {\"numPoints\": 34, \"x\": [202,202,202,201,200,199,197,197,197,197,197,197,196,196,196,196,196,194,194,194,192,192,191,191,190,190,189,189,187,186,186,185,184,183], \"y\": [55,58,64,68,76,83,95,103,112,122,129,134,141,147,153,158,165,171,175,181,187,190,195,197,200,203,209,212,214,217,219,222,223,224]}}";
        addStrokeReq.setContent(addStrokeReqBody.getBytes());

        try {
            hwServlet.doPost(addStrokeReq, addStrokeResp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject addStrokeRespObj = null;
        try {
            addStrokeRespObj = jsonParser.parse(addStrokeResp.getContentAsString()).getAsJsonObject();
        } catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(addStrokeRespObj.get("errors").getAsJsonArray().size(), 0);

        /* Get token bounds */
        JsonObject getTokenBoundsRespObj = helper.getTokenBounds(engineUuid, 0);

        assertEquals(getTokenBoundsRespObj.get("errors").getAsJsonArray().size(), 0);
        assertTrue(getTokenBoundsRespObj.has("tokenBounds"));
        assertTrue(getTokenBoundsRespObj.get("tokenBounds").isJsonArray());
        JsonArray tokenBoundsJson = getTokenBoundsRespObj.get("tokenBounds").getAsJsonArray();
        assertEquals(4, tokenBoundsJson.size());
        assertEquals(183.0f, tokenBoundsJson.get(0).getAsFloat(), floatTol);
        assertEquals(55.0f, tokenBoundsJson.get(1).getAsFloat(), floatTol);
        assertEquals(202.0f, tokenBoundsJson.get(2).getAsFloat(), floatTol);
        assertEquals(224.0f, tokenBoundsJson.get(3).getAsFloat(), floatTol);
    }

    @Test
    public void testMoveToken() {
        /* Add a new stroke */
        MockHttpServletRequest addStrokeReq = new MockHttpServletRequest();
        MockHttpServletResponse addStrokeResp = new MockHttpServletResponse();

        String addStrokeReqBody = "{\"action\" : \"add-stroke\", \"engineUuid\": \"" + engineUuid + "\", " +
                "\"stroke\": {\"numPoints\": 34, \"x\": [202,202,202,201,200,199,197,197,197,197,197,197,196,196,196,196,196,194,194,194,192,192,191,191,190,190,189,189,187,186,186,185,184,183], \"y\": [55,58,64,68,76,83,95,103,112,122,129,134,141,147,153,158,165,171,175,181,187,190,195,197,200,203,209,212,214,217,219,222,223,224]}}";
        addStrokeReq.setContent(addStrokeReqBody.getBytes());

        try {
            hwServlet.doPost(addStrokeReq, addStrokeResp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject addStrokeRespObj = null;
        try {
            addStrokeRespObj = jsonParser.parse(addStrokeResp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(addStrokeRespObj.get("errors").getAsJsonArray().size(), 0);

        /* Move token */
        JsonObject moveTokenRespObj = helper.moveToken(engineUuid, 0, new float[] {193f, 65f, 212f, 234f});

        assertEquals(moveTokenRespObj.get("errors").getAsJsonArray().size(), 0);

        /* Get token bounds: Verify the effect of the move */
        JsonObject getTokenBoundsRespObj = helper.getTokenBounds(engineUuid, 0);

        assertEquals(getTokenBoundsRespObj.get("errors").getAsJsonArray().size(), 0);
        assertTrue(getTokenBoundsRespObj.has("tokenBounds"));
        assertTrue(getTokenBoundsRespObj.get("tokenBounds").isJsonArray());
        JsonArray tokenBoundsJson = getTokenBoundsRespObj.get("tokenBounds").getAsJsonArray();
        assertEquals(4, tokenBoundsJson.size());
        assertEquals(193.0f, tokenBoundsJson.get(0).getAsFloat(), floatTol);
        assertEquals(65.0f, tokenBoundsJson.get(1).getAsFloat(), floatTol);
        assertEquals(212.0f, tokenBoundsJson.get(2).getAsFloat(), floatTol);
        assertEquals(234.0f, tokenBoundsJson.get(3).getAsFloat(), floatTol);
    }

    private float[] jsonArray2FloatArray(JsonArray a) {
        float[] r = new float[a.size()];

        for (int i = 0; i < a.size(); ++i) {
            r[i] = a.get(i).getAsFloat();
        }

        return r;
    }

    @Test
    public void testMoveMultipleTokens() {
        /* Add 1st stroke */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\": 34, \"x\": [202,202,202,201,200,199,197,197,197,197,197,197,196,196,196,196,196,194,194,194,192,192,191,191,190,190,189,189,187,186,186,185,184,183], \"y\": [55,58,64,68,76,83,95,103,112,122,129,134,141,147,153,158,165,171,175,181,187,190,195,197,200,203,209,212,214,217,219,222,223,224]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");

        /* Add 2nd stroke */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":26,\"x\":[205,206,207,208,211,212,215,216,219,221,224,227,231,235,241,246,248,252,257,263,268,274,281,284,286,289],\"y\":[139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,141,142,142,143,144,144,144,144]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "-");

        /* Add 3rd stroke */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":22,\"x\":[283,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284],\"y\":[68,71,78,86,97,105,114,125,134,145,156,164,174,183,190,198,208,215,220,223,225,226]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "+");

        /* Obtain the original bounds of the two tokens */
        float[] token0Bounds = jsonArray2FloatArray(respObjAdd2.get("writtenTokenSet").getAsJsonObject()
                .get("tokens").getAsJsonArray().get(0).getAsJsonObject().get("bounds").getAsJsonArray());
        float[] token1Bounds = jsonArray2FloatArray(respObjAdd2.get("writtenTokenSet").getAsJsonObject()
                .get("tokens").getAsJsonArray().get(1).getAsJsonObject().get("bounds").getAsJsonArray());

        assertEquals(4, token0Bounds.length);
        assertEquals(4, token1Bounds.length);

        /* Move multiple tokens */
        MockHttpServletRequest moveTokensReq = new MockHttpServletRequest();
        MockHttpServletResponse moveTokensResp = new MockHttpServletResponse();

        float[] token0NewBounds = MathHelper.floatArrayPlusFloat(token0Bounds, -10f);
        float[] token1NewBounds = MathHelper.floatArrayPlusFloat(token1Bounds, 20f);

        String token0NewBoundsStr = "[" + MathHelper.floatArray2String(token0NewBounds) + "]";
        String token1newBoundsStr = "[" +MathHelper.floatArray2String(token1NewBounds) + "]";
        String newBoundsArrayStr = "[" + token0NewBoundsStr + "," + token1newBoundsStr + "]";

        String moveTokenReqBody = "{\"action\" : \"move-multiple-tokens\", \"engineUuid\": \"" + engineUuid + "\"" +
                ", \"tokenIndices\": [0, 1], \"newBoundsArray\": " + newBoundsArrayStr + "}";
        moveTokensReq.setContent(moveTokenReqBody.getBytes());

        try {
            hwServlet.doPost(moveTokensReq, moveTokensResp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject moveTokenRespObj = null;
        try {
            moveTokenRespObj = jsonParser.parse(moveTokensResp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(moveTokenRespObj.get("errors").getAsJsonArray().size(), 0);

        float[] token0BoundsAfterMove = jsonArray2FloatArray(moveTokenRespObj.get("writtenTokenSet").getAsJsonObject()
                .get("tokens").getAsJsonArray().get(0).getAsJsonObject().get("bounds").getAsJsonArray());
        float[] token1BoundsAfterMove = jsonArray2FloatArray(moveTokenRespObj.get("writtenTokenSet").getAsJsonObject()
                .get("tokens").getAsJsonArray().get(1).getAsJsonObject().get("bounds").getAsJsonArray());

        assertArrayEquals(token0NewBounds, token0BoundsAfterMove, floatTol);
        assertArrayEquals(token1NewBounds, token1BoundsAfterMove, floatTol);
    }


    private JsonObject getVarMap(String engineUuid) {
        MockHttpServletRequest getVarMapReq = new MockHttpServletRequest();
        MockHttpServletResponse getVarMapResp = new MockHttpServletResponse();

        String addStrokeReqBody = "{\"action\" : \"get-var-map\", \"engineUuid\": \"" + engineUuid + "\"}";
        getVarMapReq.setContent(addStrokeReqBody.getBytes());

        try {
            hwServlet.doPost(getVarMapReq, getVarMapResp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(getVarMapResp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        return respObj;
    }



    @Test
    public void testMergeStrokes0() {

        /* Add 1st stroke */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                                           "{\"numPoints\": 34, \"x\": [202,202,202,201,200,199,197,197,197,197,197,197,196,196,196,196,196,194,194,194,192,192,191,191,190,190,189,189,187,186,186,185,184,183], \"y\": [55,58,64,68,76,83,95,103,112,122,129,134,141,147,153,158,165,171,175,181,187,190,195,197,200,203,209,212,214,217,219,222,223,224]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");

        /* Add 2nd stroke */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":26,\"x\":[205,206,207,208,211,212,215,216,219,221,224,227,231,235,241,246,248,252,257,263,268,274,281,284,286,289],\"y\":[139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,139,141,142,142,143,144,144,144,144]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "-");

        /* Add 3rd stroke */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":22,\"x\":[283,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284,284],\"y\":[68,71,78,86,97,105,114,125,134,145,156,164,174,183,190,198,208,215,220,223,225,226]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "1");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "+");

        /* Merge all three strokes */
        MockHttpServletRequest reqMerge = new MockHttpServletRequest();
        MockHttpServletResponse respMerge = new MockHttpServletResponse();

        String reqBody = "{\"action\" : \"merge-strokes-as-token\", \"engineUuid\": \"" + engineUuid + "\", " +
                  "\"strokeIndices\": [0, 1, 2]}";
        reqMerge.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(reqMerge, respMerge);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respMergeObj = null;
        try {
            respMergeObj = jsonParser.parse(respMerge.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respMergeObj.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respMergeObj.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);

        JsonObject token0 = tokens.get(0).getAsJsonObject();
        assertEquals(token0.get("width").getAsDouble(), 106.0, epsilon);
        assertEquals(token0.get("height").getAsDouble(), 171.0, epsilon);
        assertEquals(token0.get("recogWinner").getAsString(), "H");

        JsonArray constituentStrokes = respMergeObj.get("constituentStrokes").getAsJsonArray();
        assertEquals(constituentStrokes.size(), 1);
        assertEquals(constituentStrokes.get(0).getAsJsonArray().size(), 3);
    }

    @Test
    public void testMergeStrokes1() {

        /* Add 1st stroke */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":62,\"x\":[234,234,234,235,237,240,244,248,255,258,263,265,268,270,271,274,277,281,287,291,295,299,302,304,306,307,307,307,307,307,306,305,302,299,295,289,284,278,271,264,257,250,244,238,234,228,227,225,226,229,234,240,249,260,271,283,296,304,312,320,323,325],\"y\":[143,142,141,140,139,136,133,131,128,126,124,124,124,124,124,124,127,130,136,142,149,156,162,165,169,172,173,176,177,179,181,183,185,188,192,194,195,197,201,203,205,207,208,208,209,209,210,210,210,210,210,210,210,209,208,208,208,207,207,207,207,207]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "2");

        /* Add 2nd stroke */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":39,\"x\":[400,400,400,399,397,393,391,389,387,385,383,382,381,380,380,380,380,380,382,382,383,383,385,388,391,394,400,406,411,418,424,430,437,444,453,460,462,463,465],\"y\":[113,116,118,122,129,137,144,152,158,165,171,176,182,187,194,198,202,203,204,205,205,206,207,208,209,210,211,211,211,211,211,211,211,211,210,209,209,209,209]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "2");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "L");

        /* Add 3rd stroke */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":23,\"x\":[403,404,406,408,411,412,414,416,421,424,428,433,437,443,446,450,453,457,460,462,463,464,464],\"y\":[94,94,94,94,94,93,93,93,93,93,93,93,93,93,93,93,93,93,93,94,94,94,95]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 3);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "2");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "L");
        assertEquals(tokens.get(2).getAsJsonObject().get("recogWinner").getAsString(), "-");

        /* Add 4th stroke */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":14,\"x\":[391,394,397,402,410,416,426,432,441,446,453,458,460,462],\"y\":[158,158,157,157,157,157,157,157,157,156,156,156,155,155]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 3);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "2");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "L");
        assertEquals(tokens.get(2).getAsJsonObject().get("recogWinner").getAsString(), "=");

        /* Merge the last three of the four strokes */
        MockHttpServletRequest reqMerge = new MockHttpServletRequest();
        MockHttpServletResponse respMerge = new MockHttpServletResponse();

        String reqBody = "{\"action\" : \"merge-strokes-as-token\", \"engineUuid\": \"" + engineUuid + "\", " +
                         "\"strokeIndices\": [1, 2, 3]}";
        reqMerge.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(reqMerge, respMerge);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respMergeObj = null;
        try {
            respMergeObj = jsonParser.parse(respMerge.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(respMergeObj.get("errors").getAsJsonArray().size(), 0);
        writtenTokenSet = respMergeObj.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "2");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "E");

        JsonArray constituentStrokes = respMergeObj.get("constituentStrokes").getAsJsonArray();
        assertEquals(constituentStrokes.size(), 2);
        assertEquals(constituentStrokes.get(0).getAsJsonArray().size(), 1);
        assertEquals(constituentStrokes.get(1).getAsJsonArray().size(), 3);
    }

    @Test
    public void testInvalidAction() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        final String invalidAction = "foo-bar-qux";
        String reqBody = "{\"action\" : \"" + invalidAction + "\"}";
        req.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(req, resp);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(resp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        JsonArray errors = respObj.get("errors").getAsJsonArray();
        assertEquals(errors.size(), 1);
        assertEquals(errors.get(0).getAsString(), "Request contains unrecognized action \"" + invalidAction + "\"");
    }

    /**
     * Test concurrent parsing
     */
    @Test
    public void testParsingConcurrent() {

        /* Add 1st stroke: 1 */
        for (int i = 0; i < 10; ++i) {
            float x1 = 20.1f + i * 2;
            float x2 = 20.2f + i * 2;
            float x3 = 20.3f + i * 2;
            float x4 = 20.4f + i * 2;
            float x5 = 20.5f + i * 2;

            JsonObject respObjAdd = helper.addStroke(engineUuid,
                    String.format("{\"numPoints\":5,\"x\":[%f, %f, %f, %f, %f],\"y\":[0, 5, 10, 15, 20]}",
                            x1, x2, x3, x4, x5));

            assertEquals(respObjAdd.get("errors").getAsJsonArray().size(), 0);
        }

        // Concurrent parsing requests
        final int nConcurrentRequests = 4;
        for (int i = 0; i < nConcurrentRequests; ++i) {
            new Runnable() {
                @Override
                public void run() {
                    JsonObject respObjParseTokenSet = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

                    assertEquals(respObjParseTokenSet.get("errors").getAsJsonArray().size(), 0);
                    assertEquals("1111111111", respObjParseTokenSet.get("parseResult").getAsJsonObject().get("stringizerOutput").getAsString());
                }
            }.run();
        }

    }

    @Test
    public void testRemoveToken_validIdxToken_first() {

        /* Add 1st token: 1 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        /* Add 2nd token: - */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        /* Verify state before remove-token request */
        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        assertEquals(2, writtenTokenSet.get("tokens").getAsJsonArray().size());

        JsonObject token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject token2 = writtenTokenSet.get("tokens").getAsJsonArray().get(1).getAsJsonObject();

        assertEquals("1", token1.get("recogWinner").getAsString());
        assertEquals("-", token2.get("recogWinner").getAsString());

        /* Remove the 1st token */
        JsonObject removeTokenRespObj = helper.removeToken(engineUuid, 0);

        /* Verify state after remove-token request */
        writtenTokenSet = removeTokenRespObj.get("writtenTokenSet").getAsJsonObject();
        assertEquals(1, writtenTokenSet.get("tokens").getAsJsonArray().size());

        token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();

        assertEquals("-", token1.get("recogWinner").getAsString());

        /* Verify that the constituent stroke indices have been updated */
        JsonArray constStrokes = removeTokenRespObj.getAsJsonArray("constituentStrokes");
        assertEquals(1, constStrokes.size());

        JsonArray constStrokesToken1 = constStrokes.get(0).getAsJsonArray();
        assertEquals(1, constStrokesToken1.size());
        assertEquals(0, constStrokesToken1.get(0).getAsInt());
    }


    @Test
    public void testRemoveToken_validIdxToken_second() {

        /* Add 1st token: 1 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        /* Add 2nd token: - */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        /* Verify state before remove-token request */
        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        assertEquals(2, writtenTokenSet.get("tokens").getAsJsonArray().size());

        JsonObject token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject token2 = writtenTokenSet.get("tokens").getAsJsonArray().get(1).getAsJsonObject();

        assertEquals("1", token1.get("recogWinner").getAsString());
        assertEquals("-", token2.get("recogWinner").getAsString());

        /* Remove the 1st token */
        JsonObject removeTokenRespObj = helper.removeToken(engineUuid, 1);

        /* Verify state after remove-token request */
        writtenTokenSet = removeTokenRespObj.get("writtenTokenSet").getAsJsonObject();
        assertEquals(1, writtenTokenSet.get("tokens").getAsJsonArray().size());

        token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();

        assertEquals("1", token1.get("recogWinner").getAsString());

        /* Verify that the constituent stroke indices */
        JsonArray constStrokes = removeTokenRespObj.getAsJsonArray("constituentStrokes");
        assertEquals(1, constStrokes.size());

        JsonArray constStrokesToken1 = constStrokes.get(0).getAsJsonArray();
        assertEquals(1, constStrokesToken1.size());
        assertEquals(0, constStrokesToken1.get(0).getAsInt());
    }

    @Test
    public void testRemoveToken_invalidIdxToken() {

        /* Add 1st token: 1 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        /* Add 2nd token: - */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        /* Verify state before remove-token request */
        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        assertEquals(2, writtenTokenSet.get("tokens").getAsJsonArray().size());

        JsonObject token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject token2 = writtenTokenSet.get("tokens").getAsJsonArray().get(1).getAsJsonObject();

        assertEquals("1", token1.get("recogWinner").getAsString());
        assertEquals("-", token2.get("recogWinner").getAsString());

        /* Remove the 1st token */
        JsonObject removeTokenRespObj = helper.removeToken(engineUuid, 2);

        /* Verify state after remove-token request */
        /* Since the token index was invalid, the two tokens should still be there */
        writtenTokenSet = removeTokenRespObj.get("writtenTokenSet").getAsJsonObject();
        assertEquals(2, writtenTokenSet.get("tokens").getAsJsonArray().size());

        token1 = writtenTokenSet.get("tokens").getAsJsonArray().get(0).getAsJsonObject();
        token2 = writtenTokenSet.get("tokens").getAsJsonArray().get(1).getAsJsonObject();

        assertEquals("1", token1.get("recogWinner").getAsString());
        assertEquals("-", token2.get("recogWinner").getAsString());

        JsonArray errors = removeTokenRespObj.getAsJsonArray("errors");
        assertEquals(1, errors.size());

    }

    @Test
    public void testParsingIncorrectSyntax() {

        /* Add 1st stroke: 1 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        /* Add 2nd stroke: - */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(2, tokens.size());
        assertEquals("1", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        JsonObject respObjParseTokenSet = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertTrue(respObjParseTokenSet.get("errors").isJsonArray());
        JsonArray errors = respObjParseTokenSet.get("errors").getAsJsonArray();
        assertEquals(errors.size(), 1);
        assertTrue(errors.get(0).getAsString().startsWith(HandwritingServlet.TOKEN_SET_PARSING_FAILURE_ERROR_MESSAGE));
    }

    @Test
    public void testParsingTimeoutShortTimeout() {

        /* Add a lot of strokes "1"s, so that the parsing will for sure taking longer than 1000 ms */
        for (int i = 0; i < 50; ++i) {
            float xOffset = i * 1.0f;

            float[] x =  {20f, 20.1f, 20.2f, 20.3f, 20.4f};
            for (int j = 0; j < x.length; ++j) {
                x[j] += xOffset;
            }

            JsonObject respObjAdd = helper.addStroke(engineUuid,
                    "{\"numPoints\":5,\"x\":[" + x[0] + ", " + x[1] + ", " + x[2] + ", " + x[3] + ", " + x[4] + " ],\"y\":[0, 5, 10, 15, 20]}");

            assertEquals(respObjAdd.get("errors").getAsJsonArray().size(), 0);
        }

        JsonObject respObjParseTokenSet = helper.parseTokenSet(engineUuid, 20L);

        assertTrue(respObjParseTokenSet.get("errors").isJsonArray());
        JsonArray errors = respObjParseTokenSet.get("errors").getAsJsonArray();

        assertEquals(2, errors.size());
        assertTrue(errors.get(0).getAsString().startsWith(HandwritingServlet.TOKEN_SET_PARSING_FAILUE_TIMEOUT));
        assertTrue(errors.get(1).getAsString().toLowerCase().contains("interrupted"));
    }

    @Test
    public void testGetGraphicalProductions() {
        MockHttpServletRequest getProdsReq   = new MockHttpServletRequest();
        MockHttpServletResponse getProdsResp = new MockHttpServletResponse();

        String addStrokeReqBody = "{\"action\" : \"get-graphical-productions\", \"engineUuid\": \"" + engineUuid + "\"}";
        getProdsReq.setContent(addStrokeReqBody.getBytes());

        try {
            hwServlet.doPost(getProdsReq, getProdsResp);
        } catch (IOException exc) {
            fail(exc.getMessage());
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(getProdsResp.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertEquals(0, respObj.get("errors").getAsJsonArray().size());

        JsonArray gpArray = respObj.get("graphicalProductions").getAsJsonArray();
        assertNotNull(gpArray);

    }

    @Test
    public void testGetVarMap1() {

        /* Add 1st stroke: V */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[0, 10, 20, 10, 0]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");

        /* Add 2nd and 3rd strokes: = */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[5, 5, 5]}");
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[15, 15, 15]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);
        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "=");

        /* Add 4th strokes: = */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                                           "{\"numPoints\":5,\"x\":[70, 71, 71, 71, 71],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 3);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        assertEquals(tokens.get(1).getAsJsonObject().get("recogWinner").getAsString(), "=");
        assertEquals(tokens.get(2).getAsJsonObject().get("recogWinner").getAsString(), "1");

        // There should be no "varMap" before the parsing, because the new variable has not been parsed out yet.
        assertFalse(respObjAdd3.has("varMap"));

        // Issue parse-token-set request
        JsonObject respObjParseTokenSet = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        // Verify that V has been added to the varMap
        assertTrue(respObjParseTokenSet.has("varMap"));
        assertTrue(respObjParseTokenSet.get("varMap").getAsJsonObject().has("V"));

        JsonObject varMap = respObjParseTokenSet.getAsJsonObject("varMap");
        assertEquals(ValueUnion.ValueType.Double.toString(),
                varMap.getAsJsonObject("V").getAsJsonObject().get("type").getAsString());
        assertEquals(1.0, varMap.getAsJsonObject("V").getAsJsonObject().get("value").getAsDouble(), 1E-9);

        JsonObject respObjGetVarMap = getVarMap(engineUuid);

        varMap = respObjGetVarMap.getAsJsonObject("varMap");

        assertTrue(varMap.has("V"));

        // Parse again and test if there is no varMap in the new response object.
        respObjParseTokenSet = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        // The 2nd parsing should have led to the same value of "V". As such, there should be no "varMap" in the new
        // response object.
        assertFalse(respObjParseTokenSet.has("varMap"));

    }

    @Test
    public void testGetVarMapPredefined() {

        /* Add 1st stroke: V */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[0, 10, 20, 10, 0]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals("V", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());

        /* Add 2nd and 3rd strokes: = */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[5, 5, 5]}");
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[15, 15, 15]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);
        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 2);
        assertEquals("V", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("=", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Add 4th strokes: c */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":7,\"x\":[81.2, 80, 75, 73, 75, 80, 81],\"y\":[2, 0, 0, 10, 20, 20, 18]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 3);
        assertEquals("V", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("=", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("c", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());

        // Issue parse-token-set request
        JsonObject respObjParseTokenSet = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        JsonObject respObjGetVarMap = getVarMap(engineUuid);

        JsonObject varMap = respObjGetVarMap.getAsJsonObject("varMap");
        assertTrue(varMap.has("V"));
        assertEquals(299792458.0, varMap.getAsJsonObject("V").get("value").getAsDouble(), 1E-9);
    }

    @Test
    public void testRemoveEngine() {

        MockHttpServletRequest reqRemoveEngine = new MockHttpServletRequest();
        MockHttpServletResponse respRemoveEngine = new MockHttpServletResponse();

        String reqBodyRemoveEngine = "{\"action\":\"remove-engine\",\"engineUuid\":\"" + engineUuid + "\"}";
        reqRemoveEngine.setContent(reqBodyRemoveEngine.getBytes());
        try {
            hwServlet.doPost(reqRemoveEngine, respRemoveEngine);
        }
        catch (IOException exc) {
            fail(exc.getMessage());
        }
        catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respRemoveEngineObj = null;
        try {
            respRemoveEngineObj = jsonParser.parse(respRemoveEngine.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        JsonObject resRemoveEngineObj = null;
        try {
            resRemoveEngineObj = jsonParser.parse(respRemoveEngine.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        assertTrue(resRemoveEngineObj.get("errors").isJsonArray());
        assertEquals(0, resRemoveEngineObj.get("errors").getAsJsonArray().size());
        assertTrue(resRemoveEngineObj.get("removedEngineUuid").isJsonPrimitive());

        String removedEngineUuid = resRemoveEngineObj.get("removedEngineUuid").getAsString();

        assertTrue(removedEngineUuid.equals(engineUuid));

        engineUuid = null;
    }



    @Test
    public void testInjectState() {
        final String stateDataString = "{\"strokes\":[{\"numPoints\":4,\"x\":[0.0,10.0,20.0,30.0],\"y\":[0.0,0.1,0.0,0.05]},{\"numPoints\":4,\"x\":[0.0,10.0,20.0,30.0],\"y\":[10.0,10.95,10.0,10.02]},{\"numPoints\":4,\"x\":[40.0,50.0,60.0,70.0],\"y\":[-10.0,-10.1,-10.2,-10.1]},{\"numPoints\":4,\"x\":[55.0,55.1,55.0,55.1],\"y\":[-10.0,0.0,10.0,20.1]}],\"strokes_un\":[{\"numPoints\":4,\"x\":[0.0,10.0,20.0,30.0],\"y\":[0.0,0.1,0.0,0.05]},{\"numPoints\":4,\"x\":[0.0,10.0,20.0,30.0],\"y\":[10.0,10.95,10.0,10.02]},{\"numPoints\":4,\"x\":[40.0,50.0,60.0,70.0],\"y\":[-10.0,-10.1,-10.2,-10.1]},{\"numPoints\":4,\"x\":[55.0,55.1,55.0,55.1],\"y\":[-10.0,0.0,10.0,20.1]}],\"tokenBounds\":[[0.0,0.0,30.0,10.95],[40.0,-10.2,70.0,20.1]],\"tokenSet\":{\"tokens\":[{\"bounds\":[0.0,0.0,30.0,10.95],\"width\":30.0,\"height\":10.95,\"recogWinner\":\"\\u003d\",\"recogPs\":[1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0796486170378275E-4,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,8.548614396284052E-16,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,3.9514225622652507E-11,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,9.84765389942582E-4,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20]},{\"bounds\":[40.0,-10.2,70.0,20.1],\"width\":30.0,\"height\":30.3,\"recogWinner\":\"+\"}]},\"wtConstStrokeIndices\":[[0,1],[2,3]],\"wtRecogWinners\":[\"\\u003d\",\"+\"],\"wtRecogPs\":[[1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0796486170378275E-4,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,8.548614396284052E-16,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,3.9514225622652507E-11,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,9.84765389942582E-4,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20,1.0E-20],null],\"wtRecogMaxPs\":[1.0,1.0],\"strokeState\":[0,0,1,2],\"wtCtrXs\":[15.0,55.0],\"wtCtrYs\":[5.475,4.9500003],\"tokenUuids\":[\"Kgs8hhAM\",\"FauPbRLV\"]}";

        JsonObject stateData = jsonParser.parse(stateDataString).getAsJsonObject();

        String reqBodyInjectState = "{\"action\" : \"inject-state\", \"engineUuid\": \"" + engineUuid + "\", \"stateData\": " + stateData + "}";

        MockHttpServletRequest reqInjectState = new MockHttpServletRequest();
        MockHttpServletResponse respInjectState = new MockHttpServletResponse();
        reqInjectState.setContent(reqBodyInjectState.getBytes());

        try {
            hwServlet.doPost(reqInjectState, respInjectState);
        } catch (IOException exc) {
            fail(exc.getMessage());
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObjInjectState = null;
        try {
            respObjInjectState = jsonParser.parse(respInjectState.getContentAsString()).getAsJsonObject();
        } catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        // Verify injected tokens
        JsonArray injTokens = stateData.get("tokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        JsonArray resTokens = respObjInjectState.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();

        assertEquals(injTokens.size(), resTokens.size());

        for (int i = 0; i < injTokens.size(); ++i) {
            JsonObject injToken = injTokens.get(i).getAsJsonObject();
            JsonObject resToken = resTokens.get(i).getAsJsonObject();

            JsonArray injBounds = injToken.get("bounds").getAsJsonArray();
            JsonArray resBounds = resToken.get("bounds").getAsJsonArray();

            assertEquals(injBounds.size(), resBounds.size());
            for (int j = 0; j < injBounds.size(); ++j) {
                assertEquals(injBounds.get(j).getAsFloat(), resBounds.get(j).getAsFloat(), FLOAT_TOL);
            }

            assertEquals(injToken.get("width").getAsFloat(), resToken.get("width").getAsFloat(), FLOAT_TOL);
            assertEquals(injToken.get("height").getAsFloat(), resToken.get("height").getAsFloat(), FLOAT_TOL);
            assertEquals(injToken.get("recogWinner").getAsJsonPrimitive(), resToken.get("recogWinner").getAsJsonPrimitive());
        }

        // Verify constStrokeIndices
        JsonArray injConstStrokeIndices = stateData.get("wtConstStrokeIndices").getAsJsonArray();
        JsonArray resConstStrokeIndices = respObjInjectState.get("constituentStrokes").getAsJsonArray();

        assertEquals(injConstStrokeIndices.size(), resConstStrokeIndices.size());

        for (int i = 0; i < injConstStrokeIndices.size(); ++i) {
            JsonArray injIndices = injConstStrokeIndices.get(i).getAsJsonArray();
            JsonArray resIndices = resConstStrokeIndices.get(i).getAsJsonArray();

            assertEquals(injIndices.size(), resIndices.size());

            for (int j = 0; j < injIndices.size(); ++j) {
                assertEquals(injIndices.get(j).getAsInt(), resIndices.get(j).getAsInt());
            }
        }

    }


    @Test
    public void testUndoRedo() {

        /* Add 1st stroke: V */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[0, 10, 20, 10, 0]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd0.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        JsonArray tokenBounds = tokens.get(0).getAsJsonObject().get("bounds").getAsJsonArray();
        assertEquals(4, tokenBounds.size());
        assertEquals(0.0f, tokenBounds.get(0).getAsFloat(), FLOAT_TOL);
        assertEquals(0.0f, tokenBounds.get(1).getAsFloat(), FLOAT_TOL);
        assertEquals(40.0f, tokenBounds.get(2).getAsFloat(), FLOAT_TOL);
        assertEquals(20.0f, tokenBounds.get(3).getAsFloat(), FLOAT_TOL);

        String state0 = gson.toJson(respObjAdd0.get("writtenTokenSet").getAsJsonObject());

        /* Verify get-last-stroke-curator-user-action */
        JsonObject respGetAction = getLastStrokeCuratorUserAction(engineUuid);

        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     respGetAction.get("lastStrokeCuratorUserAction").getAsString());

        /* Undo add stroke */
        JsonObject respUndo = undoStrokeCuratorUserAction(engineUuid);

        /* After the undo, the last user action should be null */
        respGetAction = getLastStrokeCuratorUserAction(engineUuid);
        assertNull(respGetAction.get("lastStrokeCuratorUserAction"));

        /* Redo add stroke */
        JsonObject respRedo = redoStrokeCuratorUserAction(engineUuid);

        assertEquals(state0, gson.toJson(respRedo.get("writtenTokenSet").getAsJsonObject()));   // After the redo, the state should have been restored to the state right after the first add-stroke request
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Move the token */
        JsonObject respMoveToken = helper.moveToken(engineUuid, 0, new float[] {10, 10, 50, 30});

        respGetAction = getLastStrokeCuratorUserAction(engineUuid);
        assertEquals(HandwritingEngineUserAction.MoveToken.toString(),
                     respGetAction.get("lastStrokeCuratorUserAction").getAsString());

        tokens = respMoveToken.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        tokenBounds = tokens.get(0).getAsJsonObject().get("bounds").getAsJsonArray();
        assertEquals(4, tokenBounds.size());
        assertEquals(10.0f, tokenBounds.get(0).getAsFloat(), FLOAT_TOL);
        assertEquals(10.0f, tokenBounds.get(1).getAsFloat(), FLOAT_TOL);
        assertEquals(50.0f, tokenBounds.get(2).getAsFloat(), FLOAT_TOL);
        assertEquals(30.0f, tokenBounds.get(3).getAsFloat(), FLOAT_TOL);

        /* Undo the token move */
        respUndo = undoStrokeCuratorUserAction(engineUuid);

        respGetAction = getLastStrokeCuratorUserAction(engineUuid);
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     respGetAction.get("lastStrokeCuratorUserAction").getAsString());

        tokens = respUndo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        tokenBounds = tokens.get(0).getAsJsonObject().get("bounds").getAsJsonArray();
        assertEquals(4, tokenBounds.size());
        assertEquals(0.0f, tokenBounds.get(0).getAsFloat(), FLOAT_TOL);
        assertEquals(0.0f, tokenBounds.get(1).getAsFloat(), FLOAT_TOL);
        assertEquals(40.0f, tokenBounds.get(2).getAsFloat(), FLOAT_TOL);
        assertEquals(20.0f, tokenBounds.get(3).getAsFloat(), FLOAT_TOL);

        /* Redo the token move */
        respRedo = redoStrokeCuratorUserAction(engineUuid);

        respGetAction = getLastStrokeCuratorUserAction(engineUuid);
        assertEquals(HandwritingEngineUserAction.MoveToken.toString(),
                     respGetAction.get("lastStrokeCuratorUserAction").getAsString());

        tokens = respRedo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).getAsJsonObject().get("recogWinner").getAsString(), "V");
        tokenBounds = tokens.get(0).getAsJsonObject().get("bounds").getAsJsonArray();
        assertEquals(4, tokenBounds.size());
        assertEquals(10.0f, tokenBounds.get(0).getAsFloat(), FLOAT_TOL);
        assertEquals(10.0f, tokenBounds.get(1).getAsFloat(), FLOAT_TOL);
        assertEquals(50.0f, tokenBounds.get(2).getAsFloat(), FLOAT_TOL);
        assertEquals(30.0f, tokenBounds.get(3).getAsFloat(), FLOAT_TOL);

        /* Undo the token move */
        undoStrokeCuratorUserAction(engineUuid);

        /* Force set token name */
        JsonObject respForceSet = helper.forceSetTokenName(engineUuid, 0, "W");

        /* Verify that the token name has been force-set to "W" */
        JsonArray tokens1 = respForceSet.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals("W", tokens1.get(0).getAsJsonObject().get("recogWinner").getAsString());

        assertEquals(HandwritingEngineUserAction.ForceSetTokenName.toString(),
                getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Undo force set token name */
        respUndo = undoStrokeCuratorUserAction(engineUuid);

        /* After the undo, the last user action should be add-stroke */
        respGetAction = getLastStrokeCuratorUserAction(engineUuid);
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* The undo should have set the token name back to "V" */
        tokens1 = respUndo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals("V", tokens1.get(0).getAsJsonObject().get("recogWinner").getAsString());

        /* Add 2nd and 3rd strokes: = */
        JsonObject respAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[5, 5, 5]}");
        String state1 = gson.toJson(respAdd1.get("writtenTokenSet").getAsJsonObject());

        JsonObject respAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":3,\"x\":[50, 55, 60],\"y\":[15, 15, 15]}");
        String state2 = gson.toJson(respAdd2.get("writtenTokenSet").getAsJsonObject());

        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        tokens1 = respAdd2.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());
        assertEquals("V", tokens1.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("=", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Undo the 3rd stroke */
        respUndo = undoStrokeCuratorUserAction(engineUuid);

        assertEquals(state1, gson.toJson(respUndo.get("writtenTokenSet").getAsJsonObject()));
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Redo the 3rd stroke */
        respRedo = redoStrokeCuratorUserAction(engineUuid);

        assertEquals(state2, gson.toJson(respRedo.get("writtenTokenSet").getAsJsonObject()));
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Force unmerge the "=" token */
        JsonObject respUnmerge = helper.mergeStrokesAsToken(engineUuid, new int[] {2});

        tokens1 = respUnmerge.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());
        assertEquals("V", tokens1.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertEquals(HandwritingEngineUserAction.MergeStrokesAsToken.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Undo the unmerge */
        respUndo = undoStrokeCuratorUserAction(engineUuid);

        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());
        assertEquals(state2, gson.toJson(respUndo.get("writtenTokenSet").getAsJsonObject()));

        /* Go anal! Let's go back and forth with the redo and undo for a few times */
        for (int i = 0; i < 3; ++i) {
            respRedo = redoStrokeCuratorUserAction(engineUuid);

            assertEquals(HandwritingEngineUserAction.MergeStrokesAsToken.toString(),
                    getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

            respUndo = undoStrokeCuratorUserAction(engineUuid);

            assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                         getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());
            assertEquals(state2, gson.toJson(respUndo.get("writtenTokenSet").getAsJsonObject()));
        }

        /* Clear all strokes */
        JsonObject respClear = clearStrokes(engineUuid);

        tokens1 = respClear.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(0, tokens1.size());

        assertEquals(HandwritingEngineUserAction.ClearStrokes.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Undo the clear */
        respUndo = undoStrokeCuratorUserAction(engineUuid);

        assertEquals(state2, gson.toJson(respUndo.get("writtenTokenSet").getAsJsonObject()));
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Add the 4th stroke: c */
        JsonObject respAdd3 = helper.addStroke(engineUuid, "{\"numPoints\":7,\"x\":[81.2, 80, 75, 75, 75, 80, 81],\"y\":[2, 0, 0, 10, 20, 20, 18]}");

        tokens1 = respAdd3.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());
        assertEquals(HandwritingEngineUserAction.AddStroke.toString(),
                     getLastStrokeCuratorUserAction(engineUuid).get("lastStrokeCuratorUserAction").getAsString());

        /* Clear all strokes, again */
        respClear = clearStrokes(engineUuid);
        tokens1 = respClear.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(0, tokens1.size());

        /* Undo the clear, again */
        respUndo = undoStrokeCuratorUserAction(engineUuid);
        tokens1 = respUndo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        /* Remove the last token */
        JsonObject respRemoveToken = helper.removeToken(engineUuid, 2);
        tokens1 = respRemoveToken.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        /* Undo the remove token */
        respUndo = undoStrokeCuratorUserAction(engineUuid);
        tokens1 = respUndo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        /* Redo the remove token */
        respRedo = redoStrokeCuratorUserAction(engineUuid);
        tokens1 = respRedo.get("writtenTokenSet").getAsJsonObject().get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        /* Attempt to redo should run into an error */
        respRedo = redoStrokeCuratorUserAction(engineUuid);

        JsonArray errors = respRedo.get("errors").getAsJsonArray();
        assertEquals(1, errors.size());
        assertEquals("Cannot redo stroke curator user action", errors.get(0).getAsString());
    }


    /* Private methods */
    private JsonObject getLastStrokeCuratorUserAction(String engineUuid) {
        return helper.sendRequest(engineUuid, "get-last-stroke-curator-user-action", null);
    }


    /* Test method: clear all strokes (and tokens) */
    private JsonObject clearStrokes(String engineUuid) {
        return helper.sendRequest(engineUuid, "clear", null);
    }

    private JsonObject undoStrokeCuratorUserAction(String engineUuid) {
        return helper.sendRequest(engineUuid, "undo-stroke-curator-user-action", null);
    }

    private JsonObject redoStrokeCuratorUserAction(String engineUuid) {
        return helper.sendRequest(engineUuid, "redo-stroke-curator-user-action", null);
    }




}