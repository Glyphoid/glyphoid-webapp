package me.scai.plato.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.scai.parsetree.ParseTreeStringizer;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;

import java.util.List;

public class PlatoVarMapHelper {
    public static synchronized JsonObject getVarMapAsJson(PlatoVarMap varMap,
                                                          ParseTreeStringizer stringizer) {
        JsonObject r = new JsonObject();

        List<String> varNames = varMap.getVarNamesSorted();

        for (final String varName : varNames) {
            ValueUnion val = varMap.getVarValue(varName);

            JsonObject varObj = new JsonObject();
            varObj.add("type", new JsonPrimitive(val.getValueType().toString()));
            varObj.add("value", new JsonPrimitive(val.getValueString(stringizer)));

            r.add(varName, varObj);
        }

        return r;
    }

}
