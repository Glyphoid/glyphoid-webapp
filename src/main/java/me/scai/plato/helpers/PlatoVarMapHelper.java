package me.scai.plato.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.scai.parsetree.ParseTreeStringizer;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;

import java.util.List;
import java.util.Map;

public class PlatoVarMapHelper {
    /* Constants */
    public static final String VAR_MAP_KEY = "varMap";

    public static final String IS_NEW_KEY = "isNew";
    public static final String TYPE_KEY = "type";
    public static final String VALUE_KEY = "value";


    /* Static methods */
    public static synchronized JsonObject getVarMapAsJson(PlatoVarMap varMap,
                                                          ParseTreeStringizer stringizer) {
        JsonObject r = new JsonObject();

        List<String> varNames = varMap.getVarNamesSorted();

        for (final String varName : varNames) {
            ValueUnion val = varMap.getVarValue(varName);

            JsonObject varObj = new JsonObject();
            varObj.add(TYPE_KEY, new JsonPrimitive(val.getValueType().toString()));
            varObj.add(VALUE_KEY, new JsonPrimitive(val.getValueString(stringizer)));

            r.add(varName, varObj);
        }

        return r;
    }

    /**
     * Compare an var map JSON object with another (original) and determine if there is any difference from the
     * original.
     * @param obj        (New) var map JSON object
     * @param original   Original var map JSON object
     * @return  If there is no difference between obj and original, return null
     *          If the two are different, return a JSON object that has all the members and only the members of obj,
     *              with the different fields marked by the sub-field "isNew: true"
     *              Note: if some keys of original have been deleted in obj, the return value is still non-null, but
     *                    the deletion is not oreflected in the return value.
     */
    public static JsonObject compare(JsonObject obj, JsonObject original) {
        boolean diff = false;

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            final String varName = entry.getKey();
            final JsonObject varObj = entry.getValue().getAsJsonObject();

            if ( !original.has(varName) ) {
                diff = true;
                obj.get(varName).getAsJsonObject().add(IS_NEW_KEY, new JsonPrimitive(true));
            } else {
                final JsonObject origVarObj = original.get(varName).getAsJsonObject();

                if ( !varObj.get(TYPE_KEY).getAsString().equals(origVarObj.get(TYPE_KEY).getAsString()) ||
                     !varObj.get(VALUE_KEY).getAsString().equals(origVarObj.get(VALUE_KEY).getAsString())) {
                    diff = true;

                    obj.get(varName).getAsJsonObject().add(IS_NEW_KEY, new JsonPrimitive(true));
                }
            }
        }

        return diff ? obj : null;
    }

}
