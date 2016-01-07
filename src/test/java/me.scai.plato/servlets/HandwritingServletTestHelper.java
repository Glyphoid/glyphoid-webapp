package me.scai.plato.servlets;

import com.google.gson.*;
import me.scai.parsetree.MathHelper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HandwritingServletTestHelper {
    /* Constants */
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    public static final long DEFAULT_PARSING_TIMEOUT = 10000L;

    /* Members */
    private final HandwritingServlet hwServlet;

    /* Constructor */
    public HandwritingServletTestHelper(HandwritingServlet hwServlet) {
        this.hwServlet = hwServlet;
    }

    /* Methods */
    public String createEngineClear() {
        MockHttpServletRequest reqClear = new MockHttpServletRequest();
        MockHttpServletResponse respClear = new MockHttpServletResponse();
        MockHttpServletRequest reqEngineCreation = new MockHttpServletRequest();
        MockHttpServletResponse respEngineCreation = new MockHttpServletResponse();

        /* Clear any existing engines */
        String reqBodyClear = "{\"action\" : \"clear-engines\"}";
        reqClear.setContent(reqBodyClear.getBytes());
        try {
            hwServlet.doPost(reqClear, respClear);
        }
        catch (IOException exc) {
        }
        catch (ServletException exc) {
        }

        /* Get the engine */
        String reqBodyEngineCreation = "{\"action\":\"create-engine\",\"ClientTypeMajor\":\"API\",\"ClientTypeMinor\":\"API_UnitTest\"}";
        reqEngineCreation.setContent(reqBodyEngineCreation.getBytes());
        try {
            hwServlet.doPost(reqEngineCreation, respEngineCreation);
        }
        catch (IOException exc) {
        }
        catch (ServletException exc) {
        }

        JsonObject respObjEngineCreation = null;
        try {
            respObjEngineCreation = jsonParser.parse(respEngineCreation.getContentAsString()).getAsJsonObject();
        }
        catch (UnsupportedEncodingException exc) {
        }

        // The create-engine response should include initial var map.
        assertTrue(respObjEngineCreation.has("varMap"));
        assertTrue(respObjEngineCreation.get("varMap").isJsonObject());

        return respObjEngineCreation.get("engineUuid").getAsString();



    }

    /* Add a stroke. Returns: the response JSON object */
    public JsonObject addStroke(String engineUuid,
                                        String strokeJson) {
        JsonObject additionalData = new JsonObject();
        additionalData.add("stroke",  jsonParser.parse(strokeJson).getAsJsonObject());

        return sendRequest(engineUuid, "add-stroke", additionalData);
    }

    /* Remove a token */
    public JsonObject removeToken(String engineUuid, int idxToken) {
        JsonObject additionalData = new JsonObject();
        additionalData.add("idxToken", new JsonPrimitive(idxToken));

        return sendRequest(engineUuid, "remove-token", additionalData);
    }

    /* Get the bounds of a token (abstract token) */
    public JsonObject getTokenBounds(String engineUuid, int idxToken) {
        JsonObject additionalData = new JsonObject();
        additionalData.add("tokenIdx", new JsonPrimitive(idxToken));

        return sendRequest(engineUuid, "get-token-bounds", additionalData);
    }

    /* Move a token */
    public JsonObject moveToken(String engineUuid, int idxToken, float[] newBounds) {
        assert(newBounds.length == 4);

        JsonObject additionalData = new JsonObject();
        additionalData.add("tokenIdx", new JsonPrimitive(idxToken));

        JsonArray newBoundsArray = new JsonArray();
        for (float b : newBounds) {
            newBoundsArray.add(new JsonPrimitive(b));
        }
        additionalData.add("newBounds", newBoundsArray);

        return sendRequest(engineUuid, "move-token", additionalData);
    }

    /* Force set the name of a token */
    public JsonObject forceSetTokenName(String engineUuid, int idxToken, String tokenName) {
        JsonObject additionalData = new JsonObject();
        additionalData.add("tokenIdx", new JsonPrimitive(idxToken));
        additionalData.add("tokenRecogWinner", new JsonPrimitive(tokenName));

        return sendRequest(engineUuid, "force-set-token-name", additionalData);
    }

    /* Merge stroke(s) as token */
    public JsonObject mergeStrokesAsToken(String engineUuid, int[] strokeIndices) {
        JsonObject additionalData = new JsonObject();
        JsonArray jsonStrokeIndices = new JsonArray();

        for (int strokeIndex : strokeIndices) {
            jsonStrokeIndices.add(new JsonPrimitive(strokeIndex));
        }

        additionalData.add("strokeIndices", jsonStrokeIndices);

        return sendRequest(engineUuid, "merge-strokes-as-token", additionalData);
    }

    public JsonObject sendRequest(String engineUuid,
                                  String actionName,
                                  JsonObject additionalData) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        JsonObject reqBody = new JsonObject();
        reqBody.add("engineUuid", new JsonPrimitive(engineUuid));
        reqBody.add("action", new JsonPrimitive(actionName));

        if (additionalData != null) {
            for (Map.Entry<String, JsonElement> entry : additionalData.entrySet()) {
                reqBody.add(entry.getKey(), entry.getValue());
            }
        }

        req.setContentType("application/json");
        req.setContent(gson.toJson(reqBody).getBytes());

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

        return respObj;
    }

    public JsonObject parseTokenSet(String engineUuid, long timeoutMillis) {
        return parseTokenSet(engineUuid, null, timeoutMillis);
    }

    public JsonObject parseTokenSubset(String engineUuid, int[] subsetTokenIndices, long timeoutMillis) {
        return parseTokenSet(engineUuid, subsetTokenIndices, timeoutMillis);
    }

    /**
     *
     * @param engineUuid
     * @param subsetTokenIndices  For full token set parsing, use null.
     * @param timeoutMillis
     * @return
     */
    private JsonObject parseTokenSet(String engineUuid, int[] subsetTokenIndices, long timeoutMillis) {
        hwServlet.setParsingTimeoutMillis(timeoutMillis);

        MockHttpServletRequest parsingReq = new MockHttpServletRequest();
        MockHttpServletResponse parsingResp = new MockHttpServletResponse();

        final String reqBody;
        if (subsetTokenIndices == null) { // Full token set parsing
            reqBody = "{\"action\" : \"parse-token-set\", \"engineUuid\": \"" + engineUuid + "\"}";
        } else {
            String tokenIndicesStr = "[" + MathHelper.intArray2String(subsetTokenIndices) + "]";
            reqBody = "{\"action\" : \"parse-token-subset\", \"engineUuid\": \"" + engineUuid +
                    "\", \"tokenIndices\": " + tokenIndicesStr + "}";
        }
        parsingReq.setContent(reqBody.getBytes());

        try {
            hwServlet.doPost(parsingReq, parsingResp);
        } catch (IOException exc) {
            fail(exc.getMessage());
        } catch (ServletException exc) {
            fail(exc.getMessage());
        }

        JsonObject respObj = null;
        try {
            respObj = jsonParser.parse(parsingResp.getContentAsString()).getAsJsonObject();
        } catch (UnsupportedEncodingException exc) {
            fail(exc.getMessage());
        }

        return respObj;
    }


}
