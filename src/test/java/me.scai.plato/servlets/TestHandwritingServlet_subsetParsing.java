package me.scai.plato.servlets;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
        JsonObject respObjAdd2 = addSeven(engineUuid);

        /* Add 4th stroke: 2 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd3, new String[] {"1", "-", "7", "2"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{2, 3}, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj0, "72");

        verifyAbstractTokenSet(respObj0, new boolean[] {true, false, false}, new String[] {"72", "1", "-"});
        verifyWrittenTokenSet(respObj0, new String[] {"1", "-", "7", "2"});

        // Second parsing: Parse the entire token set, on top of the first subset parsing
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(1 / 72)");

        verifyAbstractTokenSet(respObj1, new boolean[] {true, false, false}, new String[] {"72", "1", "-"});
        verifyWrittenTokenSet(respObj1, new String[] {"1", "-", "7", "2"});    }



    // Add more strokes after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByAddStroke() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        assertEquals(respObjAdd1.get("errors").getAsJsonArray().size(), 0);

        assertEquals(2, respObjAdd1.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd1.get("constituentWrittenTokenUuids").getAsJsonArray().size());

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});

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

        verifyParserResult(respObj0, "72");

        verifyAbstractTokenSet(respObj0, new boolean[] {true}, new String[] {"72"});
        verifyWrittenTokenSet(respObj0, new String[] {"7", "2"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        assertEquals(respObjAdd2.get("errors").getAsJsonArray().size(), 0);

        // TODO: Refactor and make more succinct
        assertEquals(3, respObjAdd2.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {true, false}, new String[] {"72", "-"});

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

        // Second parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(1 / 72)");

    }

    // Remove a token after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByRemoveToken() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

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

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj0, "72");

        // TODO: Refactor
        assertEquals(2, respObj0.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(1, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        verifyAbstractTokenSet(respObj0, new boolean[] {true}, new String[] {"72"});
        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {true, false}, new String[] {"72", "-"});

        // TODO: Refactor
        assertEquals(3, respObjAdd2.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

        /* Add the 5th stroke: 1 */
        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[22, 22.1, 22.2, 22.3, 22.4],\"y\":[0, 5, 10, 15, 20]}");

        assertEquals(respObjAdd4.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd4, new String[] {"7", "2", "-", "1", "1"});
        verifyAbstractTokenSet(respObjAdd4, new boolean[] {true, false, false, false}, new String[] {"72", "-", "1", "1"});

        // TODO: Refactor
        assertEquals(5, respObjAdd4.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(4, respObjAdd4.get("constituentWrittenTokenUuids").getAsJsonArray().size());
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
        assertEquals(3, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        verifyWrittenTokenSet(removeTokenRespObj, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(removeTokenRespObj, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

        // Second parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(1 / 72)");

    }

    // Remove a token and then add two new strokes (tokens) after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByRemoveTokenThenAddStrokes() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

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

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {false, false, false}, new String[] {"7", "2", "-"});

        // TODO: Refactor
        assertEquals(3, respObjAdd2.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(3, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().size());
//        assertEquals(2, respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
//        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
//                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
//        assertEquals(respObjAdd2.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
//                     respObjAdd2.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {false, false, false, false}, new String[] {"7", "2", "-", "1"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        // TODO: Refactor
        assertEquals(4, respObj0.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(3, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     respObj0.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        verifyParserResult(respObj0, "72");

        verifyWrittenTokenSet(respObj0, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObj0, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

        /* Remove token "1" */
        // The index is for the abstract token
        JsonObject removeTokenRespObj = helper.removeToken(engineUuid, 2);

        verifyWrittenTokenSet(removeTokenRespObj, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(removeTokenRespObj, new boolean[] {true, false}, new String[] {"72", "-"});

        // TODO: Refactor
        assertEquals(3, removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().size());
        assertEquals(2, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().size());
        assertEquals(2, removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().size());
        assertEquals(removeTokenRespObj.get("writtenTokenUuids").getAsJsonArray().get(0).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(respObj0.get("writtenTokenUuids").getAsJsonArray().get(1).getAsString(),
                     removeTokenRespObj.get("constituentWrittenTokenUuids").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());

        /* Add two more "1"s */
        helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");
        JsonObject respAdd11 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[22, 22.1, 22.2, 22.3, 22.4],\"y\":[0, 5, 10, 15, 20]}");

        verifyWrittenTokenSet(respAdd11, new String[] {"7", "2", "-", "1", "1"});
        verifyAbstractTokenSet(respAdd11, new boolean[] {true, false, false, false}, new String[] {"72", "-", "1", "1"});

        // Second parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(11 / 72)");

    }

    // Force set the name of a written token after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByForceSetWrittenTokenName() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObjAdd1, new boolean[] {false, false}, new String[] {"7", "2"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj0, "72");

        verifyWrittenTokenSet(respObj0, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObj0, new boolean[] {true}, new String[] {"72"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {true, false}, new String[] {"72", "-"});

        /* Add the 4th stroke: 1 */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[20, 20.1, 20.2, 20.3, 20.4],\"y\":[0, 5, 10, 15, 20]}");

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

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

        verifyWrittenTokenSet(forceSetRespObj, new String[] {"7", "2", "-", "3"});
        verifyAbstractTokenSet(forceSetRespObj, new boolean[] {true, false, false}, new String[] {"72", "-", "3"});

        /* Second parsing: Parse only the two tokens that make up the denominator */
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(3 / 72)");

    }

    // Force merging of strokes after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedByForceStrokeMerging() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObjAdd1, new boolean[] {false, false}, new String[] {"7", "2"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj0, "72");

        verifyWrittenTokenSet(respObj0, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObj0, new boolean[] {true}, new String[] {"72"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {true, false}, new String[] {"72", "-"});

        /* Add the 4th stroke: - (1st stroke of pi) */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[10, 11, 12, 13, 14, 15],\"y\":[3, 3, 3, 3, 3, 3]}");

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "-"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {true, false, false}, new String[] {"72", "-", "-"});

        /* Add the 5th stroke: - (2nd stroke of pi) */
        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[11, 11, 11, 11, 11, 11],\"y\":[5, 7, 9, 11, 13, 15]}");

        assertEquals(respObjAdd4.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd4, new String[] {"7", "2", "-", "-", "1"});
        verifyAbstractTokenSet(respObjAdd4, new boolean[] {true, false, false, false}, new String[] {"72", "-", "-", "1"});

        /* Add the 6th stroke: - (3rd stroke of pi) */
        JsonObject respObjAdd5 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[14, 14.1, 14.2, 14.3, 14.4, 14.5],\"y\":[5, 7, 9, 11, 13, 15]}");

        verifyWrittenTokenSet(respObjAdd5, new String[] {"7", "2", "-", "-", "1", "1"});
        verifyAbstractTokenSet(respObjAdd5, new boolean[] {true, false, false, false, false}, new String[] {"72", "-", "-", "1", "1"});

        /* Merge the last three strokes to get gr_pi */
        // The stroke indices are for strokes in stroke curator, not for the abstract token set
        JsonObject mergeRespObj = helper.mergeStrokesAsToken(engineUuid, new int[] {3, 4, 5});

        verifyWrittenTokenSet(mergeRespObj, new String[] {"7", "2", "-", "gr_pi"});
        verifyAbstractTokenSet(mergeRespObj, new boolean[] {true, false, false}, new String[] {"72", "-", "gr_pi"});

        /* 2nd parsing */
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj1, "(gr_pi / 72)");

    }

    // Moving of tokens after subset parsing, then parse again
    @Test
    public void testTokenSubsetParsingFollowedMoveTokens() {

        /* Add 1st stroke: 7 */
        JsonObject respObjAdd0 = addSeven(engineUuid);

        /* Add 2nd stroke: 2 */
        JsonObject respObjAdd1 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[15, 30, 30, 15, 15, 30],\"y\":[30, 30, 40, 40, 50, 50]}");

        verifyWrittenTokenSet(respObjAdd1, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObjAdd1, new boolean[] {false, false}, new String[] {"7", "2"});

        // First parsing: Parse only the two tokens that make up the denominator
        JsonObject respObj0 = helper.parseTokenSubset(engineUuid, new int[]{0, 1}, helper.DEFAULT_PARSING_TIMEOUT);

        verifyParserResult(respObj0, "72");

        verifyWrittenTokenSet(respObj0, new String[] {"7", "2"});
        verifyAbstractTokenSet(respObj0, new boolean[] {true}, new String[] {"72"});

        /* After the subset parsing, add the 3rd stroke: - */
        JsonObject respObjAdd2 = helper.addStroke(engineUuid,
                "{\"numPoints\":5,\"x\":[0, 10, 20, 30, 40],\"y\":[25, 25, 25, 25, 25]}");

        verifyWrittenTokenSet(respObjAdd2, new String[] {"7", "2", "-"});
        verifyAbstractTokenSet(respObjAdd2, new boolean[] {true, false}, new String[] {"72", "-"});

        /* Add the 4th stroke: - "1"  */
        JsonObject respObjAdd3 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[11, 11, 11, 11, 11, 11],\"y\":[5, 7, 9, 11, 13, 15]}");

        assertEquals(respObjAdd3.get("errors").getAsJsonArray().size(), 0);

        verifyWrittenTokenSet(respObjAdd3, new String[] {"7", "2", "-", "1"});
        verifyAbstractTokenSet(respObjAdd3, new boolean[] {true, false, false}, new String[] {"72", "-", "1"});

        /* Add the 5th stroke: "7" */
        JsonObject respObjAdd4 = helper.addStroke(engineUuid,
                "{\"numPoints\":6,\"x\":[14, 16, 18, 18, 17, 16],\"y\":[5, 5, 5, 8, 12, 15]}");

        verifyWrittenTokenSet(respObjAdd4, new String[] {"7", "2", "-", "1", "7"});
        verifyAbstractTokenSet(respObjAdd4, new boolean[] {true, false, false, false}, new String[] {"72", "-", "1", "7"});

        /* Parse before token moving */
        JsonObject respObj1 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);
        verifyParserResult(respObj1, "(17 / 72)");

        verifyWrittenTokenSet(respObj1, new String[] {"7", "2", "-", "1", "7"});
        verifyAbstractTokenSet(respObj1, new boolean[] {true, false, false, false}, new String[] {"72", "-", "1", "7"});

        /* Verify the bounds of the tokens */
        JsonObject getTokenBoundsRespObj0 = helper.getTokenBounds(engineUuid, 2); // Index is to abstract token
        verifyTokenBoundsResponse(getTokenBoundsRespObj0, new float[] {11f, 5f, 11f, 15f});

        JsonObject getTokenBoundsRespObj1 = helper.getTokenBounds(engineUuid, 3); // Index is to abstract token
        verifyTokenBoundsResponse(getTokenBoundsRespObj1, new float[] {14f, 5f, 18f, 15f});

        /* Move tokens */
        final float[] newBounds0 = new float[] {26f, 5f, 26f, 15f}; // New position for "1"
        final float[] newBounds1 = new float[] {11f, 5f, 15f, 15f}; // New position for "7"

        // Set new bounds for "1"
        helper.moveToken(engineUuid, 2, newBounds0);

        // Set new bounds for "7"
        helper.moveToken(engineUuid, 2, newBounds1); //TODO: This may be very confusing. This is due to the fact that
        // abstract token indices may have shifted after move token.
        // TODO: Implement the automatic determination of the token index after abstract indices shift following moveToken

        /* Verify the token bounds after the moves */
        getTokenBoundsRespObj0 = helper.getTokenBounds(engineUuid, 2); // Index is to abstract token
        verifyTokenBoundsResponse(getTokenBoundsRespObj0, newBounds0);

        getTokenBoundsRespObj1 = helper.getTokenBounds(engineUuid, 3); // Index is to abstract token
        verifyTokenBoundsResponse(getTokenBoundsRespObj1, newBounds1);

        verifyWrittenTokenSet(getTokenBoundsRespObj1, new String[] {"7", "2", "-", "1", "7"});
        verifyAbstractTokenSet(getTokenBoundsRespObj1, new boolean[] {true, false, false, false}, new String[] {"72", "-", "1", "7"});

        /* Now that the position of 7 and 1 have been changed, parse again and verify the new parsing result */
        JsonObject respObj2 = helper.parseTokenSet(engineUuid, helper.DEFAULT_PARSING_TIMEOUT);
        verifyParserResult(respObj2, "(71 / 72)");

    }

    private JsonObject addSeven(String engineUuid) {
        //        JsonObject respObjAdd0 = helper.addStroke(engineUuid,
//                "{\"numPoints\":5,\"x\":[5, 7, 9, 8, 7],\"y\":[30, 30, 30, 40, 50]}");
        return helper.addStroke(engineUuid,
                "{\"numPoints\":17,\"x\":[5.000, 5.973, 7.189, 8.527, 10.959, 12.784, 14.000, 13.635, 12.297, 10.959, 10.230, 9.500, 9.014, 8.770, 8.649, 8.405, 8.284],\"y\":[30.899, 30.449, 30.225, 30.000, 30.000, 30.000, 30.449, 32.247, 35.169, 38.090, 39.888, 41.910, 43.933, 45.730, 47.753, 48.876, 50.000]}");
    }


    // TODO: Test Clearing after subset parsing
    // TODO: Test Multi-token Deletion after subset parsing
    // TODO: Test Force stroke merging with stroke merging after subset parsing
    // TODO: Test Token moving after subset parsing
    // TODO: Test Undo/Redo after subset parsing

    /* Helper methods for testing */

    /**
     * Verifies parsing result
     * @param responseObj
     * @param trueParseResult
     */
    private void verifyParserResult(JsonObject responseObj, String trueParseResult) {
        assertNotNull(responseObj);
        assertEquals(0, responseObj.get("errors").getAsJsonArray().size());

        assertTrue(responseObj.get("parseResult").isJsonObject());

        JsonObject parseResult = responseObj.get("parseResult").getAsJsonObject();
        assertEquals(trueParseResult, parseResult.get("stringizerOutput").getAsString());
//        assertEquals(trueParseResult, parseResult.get("evaluatorOutput").getAsString());
    }

    /**
     * Verifies written token set
     * @param responseObj
     * @param trueTokenNames
     */
    private void verifyWrittenTokenSet(JsonObject responseObj, String[] trueTokenNames) {
        assertEquals(0, responseObj.get("errors").getAsJsonArray().size());

        JsonObject writtenTokenSet = responseObj.get("writtenTokenSet").getAsJsonObject();
        JsonArray writtenTokens = writtenTokenSet.get("tokens").getAsJsonArray();

        assertEquals(trueTokenNames.length, writtenTokens.size());

        for (int i = 0; i < trueTokenNames.length; ++i) {
            assertEquals(trueTokenNames[i], writtenTokens.get(i).getAsJsonObject().get("recogWinner").getAsString());
        }
    }

    /**
     * Verifies abstract token set
     */
    private void verifyAbstractTokenSet(JsonObject respObj, boolean[] isNodeToken, String[] trueTokenNames) {
        assert(isNodeToken.length == trueTokenNames.length);

        final int nt = isNodeToken.length;

        assertEquals(0, respObj.get("errors").getAsJsonArray().size());

        assertTrue(respObj.get("tokenSet").isJsonObject());
        JsonObject tokenSet = respObj.get("tokenSet").getAsJsonObject();

        JsonArray tokens = tokenSet.get("tokens").getAsJsonArray();
        assertEquals(nt, tokens.size());

        for (int i = 0; i < nt; ++i) {
            if (isNodeToken[i]) {
                assertTrue(tokens.get(i).getAsJsonObject().get("node").isJsonObject());
                assertEquals(trueTokenNames[i], tokens.get(i).getAsJsonObject().get("parsingResult").getAsString());
            } else {
                assertFalse(tokens.get(i).getAsJsonObject().has("node"));
                assertEquals(trueTokenNames[i], tokens.get(i).getAsJsonObject().get("recogWinner").getAsString());
            }

        }

    }

    private void verifyTokenBoundsResponse(JsonObject getTokenBoundsRespObj, float[] trueBounds) {
        final float tol = 1e-6f;

        assert(trueBounds.length == 4);

        assertEquals(0, getTokenBoundsRespObj.get("errors").getAsJsonArray().size());

        assertTrue(getTokenBoundsRespObj.get("tokenBounds").isJsonArray());
        JsonArray tokenBounds = getTokenBoundsRespObj.get("tokenBounds").getAsJsonArray();

        assertEquals(trueBounds.length, tokenBounds.size());

        for (int i = 0; i < trueBounds.length; ++i) {
            assertEquals(trueBounds[i], tokenBounds.get(i).getAsFloat(), tol);
        }

    }
}
