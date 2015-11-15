/**
 * Created by scai on 3/25/2015.
 */

package me.scai.plato.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.scai.handwriting.CWrittenToken;
import me.scai.handwriting.CWrittenTokenSet;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.gson.JsonArray;

import java.util.List;
import java.util.ArrayList;

public class TestCWrittenTokenSetJsonHelper {
    private final static JsonParser jsonParser = new JsonParser();
    private final static float floatEqualTol = 1e-9f;

    @Test
    public void testListOfInts2JsonArray0() {
        List li = new ArrayList();

        int [][] ints = {{10, 20, 30}, {111, 222, 333, 444}};

        for (int i = 0; i < ints.length; ++i) {
            li.add(ints[i]);
        }

        JsonArray jsonArr = CWrittenTokenSetJsonHelper.listOfInt2JsonArray(li);

        assertEquals(jsonArr.size(), ints.length);

        for (int i = 0; i < ints.length; ++i) {
            JsonArray jsonInts = jsonArr.get(i).getAsJsonArray();
            assertEquals(jsonInts.size(), ints[i].length);
            for (int j = 0; j < jsonInts.size(); ++j) {
                assertEquals(jsonInts.get(j).getAsInt(), ints[i][j]);
            }

        }

    }

    @Test
    public void testListOfInts2JsonArray1() {   /* Edge case: Empty list */
        List li = new ArrayList();

        int [][] ints = {};

        for (int i = 0; i < ints.length; ++i) {
            li.add(ints[i]);
        }

        JsonArray jsonArr = CWrittenTokenSetJsonHelper.listOfInt2JsonArray(li);

        assertEquals(jsonArr.size(), ints.length);

        for (int i = 0; i < ints.length; ++i) {
            JsonArray jsonInts = jsonArr.get(i).getAsJsonArray();
            assertEquals(jsonInts.size(), ints[i].length);
            for (int j = 0; j < jsonInts.size(); ++j) {
                assertEquals(jsonInts.get(j).getAsInt(), ints[i][j]);
            }

        }
    }

    @Test
    public void testListOfInts2JsonArray2() {   /* Edge case: Non-empty list with some (but not all) empty arrays */
        List li = new ArrayList();

        int [][] ints = {{1}, {22, 33}, {}};

        for (int i = 0; i < ints.length; ++i) {
            li.add(ints[i]);
        }

        JsonArray jsonArr = CWrittenTokenSetJsonHelper.listOfInt2JsonArray(li);

        assertEquals(jsonArr.size(), ints.length);

        for (int i = 0; i < ints.length; ++i) {
            JsonArray jsonInts = jsonArr.get(i).getAsJsonArray();
            assertEquals(jsonInts.size(), ints[i].length);
            for (int j = 0; j < jsonInts.size(); ++j) {
                assertEquals(jsonInts.get(j).getAsInt(), ints[i][j]);
            }

        }
    }

    @Test
    public void testJsonObjCWrittenTokenSet() {
        final String jsonStr = "{\"tokens\":[{\"bounds\":[0.0,0.0,2.0,1.0],\"width\":2.0,\"height\":1.0,\"recogWinner\":\".\",\"recogPs\":[0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0]}]}";

        JsonObject jsonObj = jsonParser.parse(jsonStr).getAsJsonObject();

        CWrittenTokenSet wtSet = CWrittenTokenSetJsonHelper.jsonObj2CWrittenTokenSet(jsonObj);

        assertEquals(wtSet.getNumTokens(), 1);

        CWrittenToken wt = wtSet.tokens.get(0);
        assertEquals(wt.getRecogResult(), ".");
        final float [] trueBnds = {0.0f, 0.0f, 2.0f, 1.0f};
        final float [] bnds = wt.getBounds();
        assertArrayEquals(bnds, trueBnds, floatEqualTol);
        assertEquals(wt.width, 2.0, floatEqualTol);
        assertEquals(wt.height, 1.0, floatEqualTol);
        assertEquals(wt.getRecogResult(), ".");
    }

    @Test
    public void testJsonArray2ConstituentStrokeIndices() {
        final String jsonArrayStr = "[[0, 4], [1, 2, 3]]";
        JsonArray jsonArray = jsonParser.parse(jsonArrayStr).getAsJsonArray();

        List<int []> constStrokeIndices = CWrittenTokenSetJsonHelper.jsonArray2ConstituentStrokeIndices(jsonArray);

        assertEquals(constStrokeIndices.size(), 2);
        final int [] indices0 = {0, 4};
        assertArrayEquals(constStrokeIndices.get(0), indices0);
        final int [] indices1 = {1, 2, 3};
        assertArrayEquals(constStrokeIndices.get(1), indices1);

    }
}
