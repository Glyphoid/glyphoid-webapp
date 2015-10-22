package me.scai.plato.helpers;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.scai.handwriting.*;

import me.scai.parsetree.*;
import me.scai.parsetree.evaluation.ParseTreeEvaluator;
import me.scai.parsetree.evaluation.ParseTreeEvaluatorException;

import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.utilities.PooledWorker;

public class HandwritingEngineImpl implements HandwritingEngine, PooledWorker {
    private static final JsonParser jsonParser = new JsonParser();

    public StrokeCurator strokeCurator;
    public TokenSetParser tokenSetParser;
    public ParseTreeStringizer stringizer;
    public ParseTreeMathTexifier mathTexifier;
    public ParseTreeEvaluator evaluator;

    /* Constructor */
    public HandwritingEngineImpl(StrokeCurator tStrokeCurator,
                                 TokenSetParser tTokenSetParser,
                                 GraphicalProductionSet gpSet,
                                 TerminalSet termSet) {
        strokeCurator  = tStrokeCurator;
        tokenSetParser = tTokenSetParser;

        stringizer     = gpSet.genStringizer();
        mathTexifier   = new ParseTreeMathTexifier(gpSet, termSet);
        evaluator      = gpSet.genEvaluator();
    }

    /* Mehtods */
    @Override
    public void addStroke(CStroke stroke) {
        this.strokeCurator.addStroke(stroke);
    }

    @Override
    public void removeLastToken() {
        this.strokeCurator.removeLastToken();
    }

    @Override
    public void removeToken(int idxToken) throws HandwritingEngineException {
        try {
            this.strokeCurator.removeToken(idxToken);
        } catch (IllegalArgumentException exc) {
            throw new HandwritingEngineException("removeToken() failed due to: " + exc.getMessage());
        }
    }

    @Override
    public float[] moveToken(int tokenIdx, float [] newBounds)
        throws HandwritingEngineException {

        try {
            return strokeCurator.moveToken(tokenIdx, newBounds);
        } catch (IllegalArgumentException exc) {
            throw new HandwritingEngineException(exc.getMessage());
        }
    }

    @Override
    public void mergeStrokesAsToken(int [] strokeInds) {
        this.strokeCurator.mergeStrokesAsToken(strokeInds);
    }

    @Override
    public void forceSetRecogWinner(int tokenIdx, String tokenName) {
        this.strokeCurator.forceSetRecogWinner(tokenIdx, tokenName);
    }

    @Override
    public void clearStrokes() {
        this.strokeCurator.clear();
    }

    @Override
    public CWrittenTokenSet getTokenSet() {
        return this.strokeCurator.getWrittenTokenSet();
    }

    @Override
    public List<int []> getTokenConstStrokeIndices() {
        return strokeCurator.getWrittenTokenConstStrokeIndices();
    }

    /* Perform token set parsing */
    @Override
    public TokenSetParserOutput parseTokenSet() throws HandwritingEngineException {
//        JsonObject outObj = new JsonObject();

        /* Invoke parser */
        CWrittenTokenSetNoStroke wtSetNoStroke = new CWrittenTokenSetNoStroke(strokeCurator.getWrittenTokenSet());

        Node parseOutRoot = null;
        try {
            parseOutRoot = tokenSetParser.parse(wtSetNoStroke);
        } catch (TokenSetParserException exc) {
            String msg = "Failed to parse token set";
            if (exc.getMessage() != null && !exc.getMessage().isEmpty()) {
                msg += " due to: " + exc.getMessage();
            }

            throw new HandwritingEngineException(msg);
        }

        /* Invoke stringizer */
//        outObj.add("stringizerOutput", new JsonPrimitive(stringizer.stringize(parseOutRoot)));

        /* Invoke Math TeXifier */
//        outObj.add("mathTex", new JsonPrimitive(mathTexifier.texify(parseOutRoot)));

        /* Invoke evaluator */
        String evalRes = null;
        try {
            evalRes = evaluator.eval2String(parseOutRoot);

//            outObj.add("evaluatorOutput", new JsonPrimitive(evalRes));
        }
        catch (ParseTreeEvaluatorException exc) {
            evalRes = "Exception occurred during evaluation: " + exc.getMessage();
        }


        TokenSetParserOutput output = new TokenSetParserOutput(
                stringizer.stringize(parseOutRoot),
                evalRes,
                mathTexifier.texify(parseOutRoot)
        );


        return output;
    }

    @Override
    public float[] getTokenBounds(int tokenIdx)
            throws HandwritingEngineException {
        CWrittenTokenSet wtSet = strokeCurator.getTokenSet();

        if (tokenIdx < 0 || tokenIdx >= wtSet.nTokens()) {
            throw new HandwritingEngineException("Invalid token index " + tokenIdx);
        } else {
            return wtSet.getTokenBounds(tokenIdx);
        }
    }

    @Override
    public PlatoVarMap getVarMap()
        throws HandwritingEngineException {
        return evaluator.getVarMap();
    }

    @Override
    public ValueUnion getFromVarMap(String varName)
            throws HandwritingEngineException {
        ValueUnion vu = evaluator.getFromVarMap(varName);

        if (vu == null) {
            throw new HandwritingEngineException("There is no variable with name \"" + varName + "\" currently defined");
        } else {
            return vu;
        }
    }

    @Override
    public void removeEngine() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void injectState(JsonObject stateData) {
        strokeCurator.injectSerializedState(stateData);

    }

    @Override
    public StrokeCuratorUserAction getLastStrokeCuratorUserAction() {
        return strokeCurator.getLastUserAction();
    }

    @Override
    public void undoStrokeCuratorUserAction() {
        strokeCurator.undoUserAction();
    }

    @Override
    public void redoStrokeCuratorUserAction() {
        strokeCurator.redoUserAction();
    }

    @Override
    public boolean canUndoStrokeCuratorUserAction() {
        return strokeCurator.canUndoUserAction();
    }

    @Override
    public boolean canRedoStrokeCuratorUserAction() {
        return strokeCurator.canRedoUserAction();
    }

    @Override
    public List<String> getAllTokenNames() {
        return strokeCurator.getAllTokenNames();
    }

}
