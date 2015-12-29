package me.scai.plato.servlets;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestHandwritingServlet_subsetParsing {
    private HandwritingServlet hwServlet;
    private me.scai.plato.servlets.HandwritingServletTestHelper helper;

    private String engineUuid;

    @Before
    public void setUp() {
        hwServlet = new HandwritingServlet();
        helper = new me.scai.plato.servlets.HandwritingServletTestHelper(hwServlet);

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
    public void testTokenSubsetParsing() {

        /* Add 1st stroke: 1 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd0.get("errors").getAsJsonArray().size(), 0);

        /* Add 2nd stroke: - */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        /* Add 3rd stroke: 7 */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");

        /* Add 4th stroke: 2 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("1", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("7", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{2, 3}, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj0);
        assertEquals(0, respObj0.get("errors").getAsJsonArray().size());

        assertTrue(respObj0.get("parseResult").isJsonObject());

        JsonObject parseResult = respObj0.get("parseResult").getAsJsonObject();
        assertEquals("72", parseResult.get("stringizerOutput").getAsString());
        assertEquals("72", parseResult.get("evaluatorOutput").getAsString());

        assertTrue(respObj0.get("tokenSet").isJsonObject());
        JsonObject tokenSet0 = respObj0.get("tokenSet").getAsJsonObject();

        JsonArray tokens0 = tokenSet0.get("tokens").getAsJsonArray();
        assertEquals(3, tokens0.size());

        // The node token should have been put at the head of the list
        assertTrue(tokens0.get(0).getAsJsonObject().get("node").isJsonObject());
        assertFalse(tokens0.get(1).getAsJsonObject().has("node"));
        assertFalse(tokens0.get(2).getAsJsonObject().has("node"));

        assertTrue(respObj0.get("writtenTokenSet").isJsonObject());
        JsonObject writtenTokenSet0 = respObj0.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(4, tokens0.size()); // The written token set should maintain the original number of tokens

        // Second parsing: Parse the entire token set, on top of the first subset parsing
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj1);
        assertEquals(0, respObj1.get("errors").getAsJsonArray().size());

        assertTrue(respObj1.get("parseResult").isJsonObject());

        parseResult = respObj1.get("parseResult").getAsJsonObject();
        assertEquals("(1 / 72)", parseResult.get("stringizerOutput").getAsString());

        assertTrue(respObj1.get("writtenTokenSet").isJsonObject());
        writtenTokenSet0 = respObj1.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(4, tokens0.size());

    }

    // Add more strokes after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByAddStroke() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        assertEquals(2, respObjAdd1.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd1.get("constituentWrittenTokenUuids").getAsJsonArray().size());

        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(2, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj0);
        assertEquals(0, respObj0.get("errors").getAsJsonArray().size());

        // TODO: Refactor
        assertEquals(2, respObj0.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        assertTrue(respObj0.get("parseResult").isJsonObject());

        JsonObject parseResult = respObj0.get("parseResult").getAsJsonObject();
        assertEquals("72", parseResult.get("stringizerOutput").getAsString());
        assertEquals("72", parseResult.get("evaluatorOutput").getAsString());

        assertTrue(respObj0.get("tokenSet").isJsonObject());
        JsonObject tokenSet0 = respObj0.get("tokenSet").getAsJsonObject();

        JsonArray tokens0 = tokenSet0.get("tokens").getAsJsonArray();
        assertEquals(1, tokens0.size());

        // The node token should have been put at the head of the list
        assertTrue(tokens0.get(0).getAsJsonObject().get("node").isJsonObject());

        assertTrue(respObj0.get("writtenTokenSet").isJsonObject());
        JsonObject writtenTokenSet0 = respObj0.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(2, tokens0.size()); // The written token set should maintain the original number of tokens

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        // TODO: Refactor and make more succinct
        assertEquals(3, respObjAdd2.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(3, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd2.get("tokenSet").isJsonObject());
        JsonObject tokenSet1 = respObjAdd2.get("tokenSet").getAsJsonObject();

        JsonArray tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd3.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd3.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        // Second parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj1);
        assertEquals(0, respObj1.get("errors").getAsJsonArray().size());

        assertTrue(respObj1.get("parseResult").isJsonObject());

        parseResult = respObj1.get("parseResult").getAsJsonObject();
        assertEquals("(1 / 72)", parseResult.get("stringizerOutput").getAsString());

    }

    // Remove a token after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByRemoveToken() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        assertEquals(2, respObjAdd1.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd1.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(respObjAdd1.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObjAdd1.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObjAdd1.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObjAdd1.get("constituentWrittenTokenUuids").getAsJsonArray().get(1).getAsJsonArray().get(0).getAsString());

        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(2, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj0);
        assertEquals(0, respObj0.get("errors").getAsJsonArray().size());

        // TODO: Refactor
        assertEquals(2, respObj0.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        assertTrue(respObj0.get("parseResult").isJsonObject());

        JsonObject parseResult = respObj0.get("parseResult").getAsJsonObject();
        assertEquals("72", parseResult.get("stringizerOutput").getAsString());
        assertEquals("72", parseResult.get("evaluatorOutput").getAsString());

        assertTrue(respObj0.get("tokenSet").isJsonObject());
        JsonObject tokenSet0 = respObj0.get("tokenSet").getAsJsonObject();

        JsonArray tokens0 = tokenSet0.get("tokens").getAsJsonArray();
        assertEquals(1, tokens0.size());

        // The node token should have been put at the head of the list
        assertTrue(tokens0.get(0).getAsJsonObject().get("node").isJsonObject());

        assertTrue(respObj0.get("writtenTokenSet").isJsonObject());
        JsonObject writtenTokenSet0 = respObj0.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(2, tokens0.size()); // The written token set should maintain the original number of tokens

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(3, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd2.get("tokenSet").isJsonObject());
        JsonObject tokenSet1 = respObjAdd2.get("tokenSet").getAsJsonObject();

        JsonArray tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd3.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd3.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 5th stroke: 1 */
        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[22, 22.1, 22.2, 22.3, 22.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd4.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd4.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(5, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(4).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd4.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd4.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(4, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(3).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(3).getAsJsonObject().get("recogWinner").getAsString());

        // TODO: Refactor
        assertEquals(5, respObjAdd4.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, respObjAdd4.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd4.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObjAdd4.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObjAdd4.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObjAdd4.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        /* Remove the last token: 2nd "1" */
        // Note: Token ID is for the abstract token set. It'll be translated into the written token set internally by the
        // implementation of the HandwritingEngine
        JsonObject removeTokenRespObj = helper.removeToken(engineUuid, 3);

        assertEquals(removeTokenRespObj.get("errors").getAsJsonArray().size(), 0);

        // TODO: Refactor
        assertEquals(4, removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        writtenTokenSet = removeTokenRespObj.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(removeTokenRespObj.get("tokenSet").isJsonObject());
        tokenSet1 = removeTokenRespObj.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        // Second parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj1);
        assertEquals(0, respObj1.get("errors").getAsJsonArray().size());

        assertTrue(respObj1.get("parseResult").isJsonObject());

        parseResult = respObj1.get("parseResult").getAsJsonObject();
        assertEquals("(1 / 72)", parseResult.get("stringizerOutput").getAsString());

    }

    // Force set the name of a written token after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByForceSetWrittenTokenName() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(2, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj0);
        assertEquals(0, respObj0.get("errors").getAsJsonArray().size());

        assertTrue(respObj0.get("parseResult").isJsonObject());

        JsonObject parseResult = respObj0.get("parseResult").getAsJsonObject();
        assertEquals("72", parseResult.get("stringizerOutput").getAsString());
        assertEquals("72", parseResult.get("evaluatorOutput").getAsString());

        assertTrue(respObj0.get("tokenSet").isJsonObject());
        JsonObject tokenSet0 = respObj0.get("tokenSet").getAsJsonObject();

        JsonArray tokens0 = tokenSet0.get("tokens").getAsJsonArray();
        assertEquals(1, tokens0.size());

        // The node token should have been put at the head of the list
        assertTrue(tokens0.get(0).getAsJsonObject().get("node").isJsonObject());

        assertTrue(respObj0.get("writtenTokenSet").isJsonObject());
        JsonObject writtenTokenSet0 = respObj0.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(2, tokens0.size()); // The written token set should maintain the original number of tokens

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(3, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd2.get("tokenSet").isJsonObject());
        JsonObject tokenSet1 = respObjAdd2.get("tokenSet").getAsJsonObject();

        JsonArray tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd3.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd3.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

//        /* Add the 5th stroke: 1 */
//        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
//                "{\"numPoints\":5,\"x\":[22, 22.1, 22.2, 22.3, 22.4],\"y\":[0, 5, 10, 15, 20]}");
//
//        assertEquals(respObjAdd4.get("errors").getAsJsonArray().size(), 0);
//
//        writtenTokenSet = respObjAdd4.get("writtenTokenSet").getAsJsonObject();
//        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
//        assertEquals(5, tokens.size());
//        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
//        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
//        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
//        assertEquals("1", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());
//        assertEquals("1", tokens.get(4).getAsJsonObject().get("recogWinner").getAsString());
//
//        assertTrue(respObjAdd4.get("tokenSet").isJsonObject());
//        tokenSet1 = respObjAdd4.get("tokenSet").getAsJsonObject();
//
//        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
//        assertEquals(4, tokens1.size());
//
//        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
//        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());
//
//        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
//        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());
//
//        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
//        assertEquals("1", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());
//
//        assertFalse(tokens1.get(3).getAsJsonObject().has("node"));
//        assertEquals("1", tokens1.get(3).getAsJsonObject().get("recogWinner").getAsString());

        /* Force set the last token to "3" */
        // Note: Token ID is for the abstract token set. It'll be translated into the written token set internally by the
        // implementation of the HandwritingEngine
        JsonObject forceSetRespObj = helper.forceSetTokenName(engineUuid, 2, "3");

        assertEquals(forceSetRespObj.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = forceSetRespObj.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("3", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(forceSetRespObj.get("tokenSet").isJsonObject());
        tokenSet1 = forceSetRespObj.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("3", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        /* Second parsing: Parse only the two tokens that make up the denominator */
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj1);
        assertEquals(0, respObj1.get("errors").getAsJsonArray().size());

        assertTrue(respObj1.get("parseResult").isJsonObject());

        parseResult = respObj1.get("parseResult").getAsJsonObject();
        assertEquals("(3 / 72)", parseResult.get("stringizerOutput").getAsString());

    }

    // Force merging of strokes after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByForceStrokeMerging() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        JsonObject writtenTokenSet = respObjAdd1.get("writtenTokenSet").getAsJsonObject();
        JsonArray tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(2, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj0);
        assertEquals(0, respObj0.get("errors").getAsJsonArray().size());

        assertTrue(respObj0.get("parseResult").isJsonObject());

        JsonObject parseResult = respObj0.get("parseResult").getAsJsonObject();
        assertEquals("72", parseResult.get("stringizerOutput").getAsString());
        assertEquals("72", parseResult.get("evaluatorOutput").getAsString());

        assertTrue(respObj0.get("tokenSet").isJsonObject());
        JsonObject tokenSet0 = respObj0.get("tokenSet").getAsJsonObject();

        JsonArray tokens0 = tokenSet0.get("tokens").getAsJsonArray();
        assertEquals(1, tokens0.size());

        // The node token should have been put at the head of the list
        assertTrue(tokens0.get(0).getAsJsonObject().get("node").isJsonObject());

        assertTrue(respObj0.get("writtenTokenSet").isJsonObject());
        JsonObject writtenTokenSet0 = respObj0.get("writtenTokenSet").getAsJsonObject();

        tokens0 = writtenTokenSet0.get("tokens").getAsJsonArray();

        assertEquals(2, tokens0.size()); // The written token set should maintain the original number of tokens

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd2.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(3, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd2.get("tokenSet").isJsonObject());
        JsonObject tokenSet1 = respObjAdd2.get("tokenSet").getAsJsonObject();

        JsonArray tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(2, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 4th stroke: - (1st stroke of pi) */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[10, 11, 12, 13, 14, 15],\"y\":[3, 3, 3, 3, 3, 3]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd3.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd3.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd3.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 5th stroke: - (2nd stroke of pi) */
        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[11, 11, 11, 11, 11, 11],\"y\":[5, 7, 9, 11, 13, 15]}");

        assertEquals(respObjAdd4.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd4.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(5, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(4).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd4.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd4.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(4, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(3).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(3).getAsJsonObject().get("recogWinner").getAsString());

        /* Add the 6th stroke: - (2nd stroke of pi) */
        JsonObject respObjAdd5 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[14, 14.1, 14.2, 14.3, 14.4, 14.5],\"y\":[5, 7, 9, 11, 13, 15]}");

        assertEquals(respObjAdd5.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = respObjAdd5.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(6, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(4).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("1", tokens.get(5).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(respObjAdd5.get("tokenSet").isJsonObject());
        tokenSet1 = respObjAdd5.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(5, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(3).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(4).getAsJsonObject().has("node"));
        assertEquals("1", tokens1.get(4).getAsJsonObject().get("recogWinner").getAsString());

        /* Merge the last three strokes to get gr_Pi */
        // The stroke indices are for strokes in stroke curator, not for the abstract token set
        JsonObject mergeRespObj = helper.mergeStrokesAsToken(engineUuid, new int[] {3, 4, 5});

        assertEquals(mergeRespObj.get("errors").getAsJsonArray().size(), 0);

        writtenTokenSet = mergeRespObj.get("writtenTokenSet").getAsJsonObject();
        tokens = writtenTokenSet.get("tokens").getAsJsonArray();
        assertEquals(4, tokens.size());
        assertEquals("7", tokens.get(0).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("2", tokens.get(1).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("-", tokens.get(2).getAsJsonObject().get("recogWinner").getAsString());
        assertEquals("gr_Pi", tokens.get(3).getAsJsonObject().get("recogWinner").getAsString());

        assertTrue(mergeRespObj.get("tokenSet").isJsonObject());
        tokenSet1 = mergeRespObj.get("tokenSet").getAsJsonObject();

        tokens1 = tokenSet1.get("tokens").getAsJsonArray();
        assertEquals(3, tokens1.size());

        assertTrue(tokens1.get(0).getAsJsonObject().has("node"));
        assertEquals("72", tokens1.get(0).getAsJsonObject().get("parsingResult").getAsString());

        assertFalse(tokens1.get(1).getAsJsonObject().has("node"));
        assertEquals("-", tokens1.get(1).getAsJsonObject().get("recogWinner").getAsString());

        assertFalse(tokens1.get(2).getAsJsonObject().has("node"));
        assertEquals("gr_Pi", tokens1.get(2).getAsJsonObject().get("recogWinner").getAsString());

        /* 2nd parsing */
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        assertNotNull(respObj1);
        assertEquals(0, respObj1.get("errors").getAsJsonArray().size());

        assertTrue(respObj1.get("parseResult").isJsonObject());

        parseResult = respObj1.get("parseResult").getAsJsonObject();
        assertEquals("(gr_Pi / 72)", parseResult.get("stringizerOutput").getAsString());

    }

    // TODO: Test Multi-token Deletion after subset parsing
    // TODO: Test Force stroke merging with stroke merging after subset parsing
    // TODO: Test Token moving after subset parsing
    // TODO: Test Undo/Redo after subset parsing
}
