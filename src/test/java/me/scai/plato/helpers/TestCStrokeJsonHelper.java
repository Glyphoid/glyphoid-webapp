/**
 * Created by scai on 3/25/2015.
 */

package me.scai.plato.helpers;

import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import me.scai.handwriting.CStroke;

public class TestCStrokeJsonHelper {
    /* Valid JSON for CStroke */
    @Test
    public void testJson2CStroke_valid0() {
        final String json = "{\"numPoints\" : 3, \"x\" : [1, 2, 3], \"y\": [4, 5, 6]}";

        CStroke s0 = null;
        try {
            s0 = CStrokeJsonHelper.json2CStroke(json);
        }
        catch (CStrokeJsonHelper.CStrokeJsonConversionException exc) {
            fail();
        }

        assertEquals(s0.nPoints(), 3);

        float [] xs = s0.getXs();
        float [] ys = s0.getYs();

        assertEquals(xs.length, s0.nPoints());
        assertEquals(ys.length, s0.nPoints());
    }

    /* Invalid JSON for CStroke */
    @Test
    public void testJson2CStroke_invalid0() {
        final String json = "{\"numPoints\" : 4, \"x\" : [1, 2, 3], \"y\": [4, 5, 6]}";

        CStroke s0 = null;
        boolean invalid = false;
        try {
            s0 = CStrokeJsonHelper.json2CStroke(json);
        }
        catch (CStrokeJsonHelper.CStrokeJsonConversionException exc) {
            invalid = true;
        }

        assertTrue(invalid);
    }
}
