package me.scai.plato.helpers;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.scai.handwriting.*;

import me.scai.parsetree.*;
import me.scai.parsetree.evaluation.ParseTreeEvaluator;
import me.scai.parsetree.evaluation.ParseTreeEvaluatorException;

import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.utilities.PooledWorker;

public class HandwritingEngineImpl implements HandwritingEngine, PooledWorker {
    private static final Gson gson = new Gson();

    public StrokeCurator strokeCurator;
    public TokenSetParser tokenSetParser;
    public TokenSet2NodeTokenParser tokenSet2NodeTokenParser;
    public ParseTreeStringizer stringizer;
    public ParseTreeMathTexifier mathTexifier;
    public ParseTreeEvaluator evaluator;

    // Keeps the latest CWrittenTokenSetNoStroke for the parser. Could contain NodeTokens if subset parsing
    // has occurred before.
    private CWrittenTokenSetNoStroke currentTokenSet;


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

        tokenSet2NodeTokenParser = new TokenSet2NodeTokenParser(tokenSetParser, stringizer);

        currentTokenSet = new CWrittenTokenSetNoStroke();
    }

    /* Mehtods */
    @Override
    public void addStroke(CStroke stroke) {
        this.strokeCurator.addStroke(stroke);

        updateCurrentTokenSet();
    }

    @Override
    public void removeLastToken() {
        this.strokeCurator.removeLastToken();

        updateCurrentTokenSet();
    }

    @Override
    public void removeToken(int idxToken) throws HandwritingEngineException {
        try {
            this.strokeCurator.removeToken(idxToken);
        } catch (IllegalArgumentException exc) {
            throw new HandwritingEngineException("removeToken() failed due to: " + exc.getMessage());
        }

        updateCurrentTokenSet();
    }

    @Override
    public float[] moveToken(int tokenIdx, float [] newBounds)
        throws HandwritingEngineException {

        float[] bounds = null;
        try {
            bounds = strokeCurator.moveToken(tokenIdx, newBounds);
        } catch (IllegalArgumentException exc) {
            throw new HandwritingEngineException(exc.getMessage());
        }

        updateCurrentTokenSet();

        return bounds;
    }

    @Override
    public void mergeStrokesAsToken(int [] strokeInds) {
        this.strokeCurator.mergeStrokesAsToken(strokeInds);

        updateCurrentTokenSet();
    }

    @Override
    public void forceSetRecogWinner(int tokenIdx, String tokenName) {
        this.strokeCurator.forceSetRecogWinner(tokenIdx, tokenName);

        updateCurrentTokenSet();
    }

    @Override
    public void clearStrokes() {
        this.strokeCurator.clear();

        updateCurrentTokenSet();
    }

    /**
     * Could contain node tokens
     * @return
     */
    @Override
    public CAbstractWrittenTokenSet getTokenSet() {
        return currentTokenSet;
    }

    /**
     * Contains only basic written tokens: Never contains node tokens
     * @return
     */
    @Override
    public CWrittenTokenSet getWrittenTokenSet() {
        return strokeCurator.getWrittenTokenSet();
    }

    @Override
    public List<int []> getTokenConstStrokeIndices() {
        return strokeCurator.getWrittenTokenConstStrokeIndices();
    }

    /* Perform token set parsing */
    @Override
    public TokenSetParserOutput parseTokenSet() throws HandwritingEngineException {
        return parseTokenSet(false, null);
    }

    @Override
    public TokenSetParserOutput parseTokenSet(int[] tokenIndices) throws HandwritingEngineException {
        return parseTokenSet(true, tokenIndices);
    }

    private TokenSetParserOutput parseTokenSet(boolean isSubsetParsing, int[] tokenIndices) throws HandwritingEngineException {
        // Input sanity check
        if ( isSubsetParsing ) {
            assert(tokenIndices != null);
        } else{
            assert(tokenIndices == null);
        }

        Node parseOutRoot = null;
        try {
            if (isSubsetParsing) {
                currentTokenSet = tokenSet2NodeTokenParser.parseAsNodeToken(currentTokenSet, tokenIndices);

                // Assume the the result from parsing the subset is stored in the first abstract token, i.e., the
                // first abstract token is expected to be a NodeToken
                NodeToken nodeToken = (NodeToken) currentTokenSet.tokens.get(0);
                parseOutRoot = nodeToken.getNode();
            } else {
                parseOutRoot = tokenSetParser.parse(currentTokenSet);
            }
        } catch (TokenSetParserException exc) {
            String msg = "Failed to parse token set";
            if (exc.getMessage() != null && !exc.getMessage().isEmpty()) {
                msg += " due to: " + exc.getMessage();
            }

            throw new HandwritingEngineException(msg);
        } catch (InterruptedException exc) {
            throw new HandwritingEngineException("Parser interrupted");
        }

        /* Invoke evaluator */
        String evalRes = null;
        try {
            evalRes = evaluator.eval2String(parseOutRoot);
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
    public JsonArray getGraphicalProductions() {
        return gson.toJsonTree(tokenSetParser.getGraphicalProductionSet().prods).getAsJsonArray();
    }

    /**
     * Get the bounds of a specified abstract token (Could contain node tokens)
     * @param tokenIdx
     * @return
     * @throws HandwritingEngineException
     */
    @Override
    public float[] getTokenBounds(int tokenIdx)
            throws HandwritingEngineException {
        if (tokenIdx < 0 || tokenIdx >= currentTokenSet.nTokens()) {
            throw new HandwritingEngineException("Invalid abstract token index " + tokenIdx);
        } else {
            return currentTokenSet.getTokenBounds(tokenIdx);
        }
    }

    /**
     * Get bounds of a specified basic written token (Never concerned with node tokens)
     * @param tokenIdx
     * @return
     * @throws HandwritingEngineException
     */
    @Override
    public float[] getWrittenTokenBounds(int tokenIdx)
            throws HandwritingEngineException {
        CWrittenTokenSet wtSet = strokeCurator.getTokenSet();

        if (tokenIdx < 0 || tokenIdx >= wtSet.nTokens()) {
            throw new HandwritingEngineException("Invalid written token index " + tokenIdx);
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
        strokeCurator.injectSerializedState(stateData); //TODO: Check validity in the face of subset parsing

        updateCurrentTokenSet();
    }

    @Override
    public StrokeCuratorUserAction getLastStrokeCuratorUserAction() {
        return strokeCurator.getLastUserAction();
    }

    @Override
    public void undoStrokeCuratorUserAction() {
        strokeCurator.undoUserAction();

        updateCurrentTokenSet();
    }

    @Override
    public void redoStrokeCuratorUserAction() {
        strokeCurator.redoUserAction();

        updateCurrentTokenSet();
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

    private void updateCurrentTokenSet() {
        this.currentTokenSet = new CWrittenTokenSetNoStroke(strokeCurator.getTokenSet());
    }

}
