package me.scai.plato.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import com.google.gson.*;
import me.scai.handwriting.CStroke;
import me.scai.handwriting.CWrittenTokenSet;
import me.scai.handwriting.StrokeCuratorUserAction;
import me.scai.parsetree.HandwritingEngine;
import me.scai.parsetree.HandwritingEngineException;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.plato.helpers.CStrokeJsonHelper;
import me.scai.plato.helpers.CWrittenTokenSetJsonHelper;
import me.scai.plato.helpers.HandwritingEngineImpl;
import me.scai.plato.helpers.HandwritingEnginePool;
import me.scai.plato.publishers.PlatoRequestPublisher;
import me.scai.plato.publishers.PlatoRequestPublisherS3;
import me.scai.plato.security.BypassSecurity;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.clienttypes.ClientTypeMajor;
import me.scai.utilities.clienttypes.ClientTypeMinor;

public class HandwritingServlet extends HttpServlet {
    /* Constants */
    public static final String TOKEN_SET_PARSING_FAILURE_ERROR_MESSAGE = "Failed to parse token set";
    public static final String TOKEN_SET_PARSING_FAILUE_TIMEOUT = "Parsing cancelled due to timeout";

    private static final Logger logger = Logger.getLogger(HandwritingServlet.class.getName());
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    /* Member variables */
    private static PlatoRequestPublisher s3RequestPublisher;

    HandwritingEnginePool hwEngPool;

    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(4); // TODO: Do not hard code

    private long parsingTimeoutMillis = 30000L;

    @Override
    public void init() throws ServletException {
        try {
            hwEngPool = new HandwritingEnginePool();
        } catch (IOException exc) {
            logger.severe("Encountered IOException during the construction of HandwritingEnginePool: " +
                          exc.getMessage());
        }

        logger.info("HandwritingServlet has created an instance of HandwritingEnginePool successfully");

        /* Obtain S3 request publisher instance */
//        s3BucketNameForRequestLogging = getS3BucketNameForRequestLogging();
        s3RequestPublisher = PlatoRequestPublisherS3.createOrGetInstance(hwEngPool.getWorkersClientInfo());

    }

    @Override
    public void destroy() {
        logger.info("Calling destroy() of s3RequestPublisher");

        s3RequestPublisher.destroy();

        logger.info("Done calling destroy() of s3RequestPublisher");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        /* Examine the path to determine if the bypass security check is required */
        boolean bypassSecurity = BypassSecurity.isBypassSecurity(request.getServletPath());

        /* Read the body of the request */
        BufferedReader rdr = request.getReader();
        StringBuilder sb = new StringBuilder();

        logger.info("In doPost"); //DEBUG

        String line;
        while ((line = rdr.readLine()) != null) {
            sb.append(line);
        }

        rdr.close();

        String reqBody = sb.toString();

        logger.info("reqBody = \"" + reqBody + "\""); //DEBUG

        /* XHR for debugging */ // TODO: Remove
        response.addHeader("Access-Control-Allow-Origin", "null");
        response.addHeader("Access-Control-Allow-Methods", "POST");

        /* Prepare the response */
        response.setContentType("application/json");
        JsonObject outObj = new JsonObject();
        final JsonArray errors = new JsonArray();
        PrintWriter out = response.getWriter();

        /* Parse the request */
        JsonObject reqObj = null;
        try {
            reqObj = jsonParser.parse(reqBody).getAsJsonObject();
        }
        catch (Exception exc) {
            errors.add(new JsonPrimitive("Attempt to parse the request body as JSON failed"));
            outObj.add("errors", errors);
            out.print(gson.toJson(outObj));
            return;
        }

        /* If bypass-type security is required, perform it */
        if (bypassSecurity && !BypassSecurity.isAuthorized(reqObj)) {
            errors.add(new JsonPrimitive("Security authorization failure"));
            outObj.add("errors", errors);
            out.print(gson.toJson(outObj));
            response.setStatus(401);
            return;
        }

        String action = reqObj.get("action").getAsString();

        InetAddress clientIPAddress = null;
        String clientHostName = null;

        String ipAddressStr = request.getHeader("X-FORWARDED-FOR");
        if (ipAddressStr == null) {
            ipAddressStr = request.getRemoteAddr();
        }

        if (ipAddressStr != null) {
            clientIPAddress = InetAddress.getByName(ipAddressStr);
            clientHostName  = clientIPAddress.getHostName();
        }
        else {
            if (reqObj.has("clientIPAddress")) {
                clientIPAddress = InetAddress.getByName(reqObj.get("clientIPAddress").getAsString());
            }
            if (reqObj.has("clientHostName")) {
                clientHostName = reqObj.get("clientHostName").getAsString();
            }
        }

        String [] dataActions = {"add-stroke",
                                 "remove-token",
                                 "remove-last-token",
                                 "move-token",
                                 "get-token-bounds",
                                 "merge-strokes-as-token",
                                 "force-set-token-name",
                                 "clear",
                                 "parse-token-set",
                                 "get-var-map",
                                 "inject-state",
                                 "get-last-stroke-curator-user-action",
                                 "undo-stroke-curator-user-action",
                                 "redo-stroke-curator-user-action",
                                 "get-all-token-names"      // Get all possible token names from the hw engine
                                }; // TODO: Replace with enums
        boolean isDataAction = Arrays.asList(dataActions).contains(action);

        /* Write to the response */
        String engUuid = null;
        boolean isCreateEngine = false;
        boolean isRemoveEngine = false;

        ClientTypeMajor clientTypeMajor = null;
        if (action != null) {
            if (action.equals("create-engine")) {
                isCreateEngine = true;

                Date now = new Date();

                try {
                    clientTypeMajor = ClientTypeMajor.valueOf(reqObj.get("ClientTypeMajor").getAsString());
                } catch (Exception exc) {
                    clientTypeMajor = null;
                }

                ClientTypeMinor clientTypeMinor = ClientTypeMinor.None;
                if (reqObj.has("ClientTypeMinor")) {
                    try {
                        clientTypeMinor = clientTypeMinor.valueOf(reqObj.get("ClientTypeMinor").getAsString());
                    } catch (Exception exc) {}
                }

                String clientPlatformVersion = "n/a";
                if (reqObj.has("ClientPlatformVersion")) {
                    clientPlatformVersion = reqObj.get("ClientPlatformVersion").getAsString();
                }

                String clientAppVersion = "n/a";
                if (reqObj.has("ClientAppVersion")) {
                    clientAppVersion = reqObj.get("ClientAppVersion").getAsString();
                }

                JsonObject customClientData = null;
                if (reqObj.has("CustomClientData")) {
                    customClientData = reqObj.get("CustomClientData").getAsJsonObject();
                }

                WorkerClientInfo wkrClientInfo = new WorkerClientInfo(clientIPAddress,
                                                                      clientHostName,
                                                                      clientTypeMajor,
                                                                      clientTypeMinor,
                                                                      clientPlatformVersion,
                                                                      clientAppVersion,
                                                                      customClientData);

                synchronized (hwEngPool) {
                    engUuid = hwEngPool.genNewHandwritingEngine(wkrClientInfo);
                }

                if (engUuid != null) {
                    outObj.add("engineUuid", new JsonPrimitive(engUuid));
                } else {
                    errors.add(new JsonPrimitive("Failed to create new instance of handwriting engine"));
                    outObj.add("engineUuid", new JsonNull());
                }
            } else if (action.equals("remove-engine")) {
                if (toPublish(reqObj)) {
                    s3RequestPublisher.publishRemoveEngineRequest(reqObj);
                }

                isRemoveEngine = true;

                String engineUuid = reqObj.get("engineUuid").getAsString();

                JsonElement removedEngineUuid;
                if (hwEngPool.engineExists(engineUuid)) {
                    synchronized (hwEngPool) {
                        hwEngPool.remove(engineUuid);
                    }
                    removedEngineUuid = new JsonPrimitive(engineUuid);
                } else {
                    errors.add(new JsonPrimitive("Engine with this ID does not exist: " + engineUuid));
                    removedEngineUuid = new JsonNull();
                }

                outObj.add("removedEngineUuid", removedEngineUuid);

            } else if (action.equals("clear-engines")) { // Privileged action!!
                hwEngPool.clear();

                outObj.add("actionTaken", new JsonPrimitive("clear-engines"));
            } else if (isDataAction) {
                JsonElement engineUuidElem = reqObj.get("engineUuid");

                if (engineUuidElem == null) {
                    errors.add(new JsonPrimitive("Engine UUID is not specified"));
                } else {
                    engUuid = engineUuidElem.getAsString();

                    HandwritingEngineImpl hwEng = null; // TODO: Replace with HandwritingEngine and catch exceptions
                    try {
                        hwEng = hwEngPool.getHandwritingEngine(engUuid);
                    } catch (IllegalArgumentException exc) {
                        errors.add(new JsonPrimitive("Engine UUID is invalid: " + engUuid));
                    }

                    if (hwEng != null) {
                        synchronized (hwEng) {

                            if (engUuid != null) {
                                if (hwEngPool.engineExists(engUuid)) {
                                    hwEngPool.updateWorkerTimestamp(engUuid); /* TODO: Refactor in */

                                    if (action.equals("get-all-token-names")) {
                                        List<String> allTokenNames = hwEng.getAllTokenNames();

                                        JsonElement allTokenNamesJson = gson.toJsonTree(allTokenNames);
                                        outObj.add("allTokenNames", allTokenNamesJson);

                                    } else if (action.equals("add-stroke")) { /* Add stroke */
                                        String strokeJson = gson.toJson(reqObj.get("stroke"));
                                        logger.info("strokeJson = \"" + strokeJson + "\""); //DEBUG

                                        try {
                                            CStroke stroke = CStrokeJsonHelper.json2CStroke(strokeJson);

                                            hwEng.addStroke(stroke);
//                                    hwEng.strokeCurator.addStroke(stroke);
                                            logger.info("Done calling addStroke()"); //DEBUG
                                        } catch (CStrokeJsonHelper.CStrokeJsonConversionException exc) {
                                            errors.add(new JsonPrimitive("Failed to parse stroke data in request for action: \"" +
                                                    action + "\", due to: " + exc.getMessage()));
                                        }

                                    } else if (action.equals("remove-token")) {
                                        int idxToken = reqObj.get("idxToken").getAsInt();
                                        try {
                                            hwEng.removeToken(idxToken);
                                        } catch (HandwritingEngineException exc) {
                                            errors.add(new JsonPrimitive(exc.getMessage()));
                                        }

                                    } else if (action.equals("remove-last-token")) { /* Remove last token */
                                        hwEng.removeLastToken();
//                                hwEng.strokeCurator.removeLastToken();

                                    } else if (action.equals("move-token")) {        /* Move token */
                                        final int tokenIdx = reqObj.get("tokenIdx").getAsInt();
                                        JsonArray newBoundsJson = reqObj.get("newBounds").getAsJsonArray();

                                        final float[] newBounds = new float[newBoundsJson.size()];
                                        for (int i = 0; i < newBoundsJson.size(); ++i) {
                                            newBounds[i] = newBoundsJson.get(i).getAsFloat();
                                        }

                                        logger.info("Handling move-token request: strokeIdx = " + tokenIdx +
                                                ", newBounds.length = " + newBounds.length);

                                        try {
                                            hwEng.moveToken(tokenIdx, newBounds);
                                        } catch (HandwritingEngineException exc) {
                                            errors.add(new JsonPrimitive("Failed to move token due to: " + exc.getMessage()));
                                        }
                                    } else if (action.equals("merge-strokes-as-token")) {  /* Merge strokes as token */
                                        JsonArray mergeIndicesJson = null;
                                        if (reqObj.get("strokeIndices").isJsonArray()) {
                                            mergeIndicesJson = reqObj.get("strokeIndices").getAsJsonArray();
                                        } else if (reqObj.get("strokeIndices").isJsonPrimitive()) {
                                            mergeIndicesJson = new JsonArray();
                                            mergeIndicesJson.add(reqObj.get("strokeIndices"));
                                        }

                                        /* Convert the JSON array to int array */
                                        int[] mergeIndices = new int[mergeIndicesJson.size()];
                                        logger.info("Merging " + mergeIndices.length + " stroke(s)");
                                        for (int j = 0; j < mergeIndicesJson.size(); ++j) {
                                            mergeIndices[j] = mergeIndicesJson.get(j).getAsInt();
                                        }

                                        hwEng.mergeStrokesAsToken(mergeIndices);
//                                hwEng.strokeCurator.mergeStrokesAsToken(mergeIndices);
                                    } else if (action.equals("get-token-bounds")) {  /* Get token bounds */
                                        final int tokenIdx = reqObj.get("tokenIdx").getAsInt();

                                        float[] bounds = null;
                                        try {
                                            bounds = hwEng.getTokenBounds(tokenIdx);
                                        } catch (HandwritingEngineException exc) {
                                            errors.add(new JsonPrimitive("Failed to move token due to: " + exc.getMessage()));
                                        }

                                        JsonArray boundsJson = new JsonArray();
                                        for (int i = 0; i < bounds.length; ++i) {
                                            boundsJson.add(new JsonPrimitive(bounds[i]));
                                        }
                                        outObj.add("tokenBounds", boundsJson);

                                    } else if (action.equals("force-set-token-name")) {  /* Force set token name */
                                        int setTokenIdx = reqObj.get("tokenIdx").getAsInt();
                                        String setTokenRecogWinner = reqObj.get("tokenRecogWinner").getAsString();

                                        hwEng.forceSetRecogWinner(setTokenIdx, setTokenRecogWinner);
//                                hwEng.strokeCurator.forceSetRecogWinner(setTokenIdx, setTokenRecogWinner);
                                    } else if (action.equals("clear")) {                        /* Clear */
                                        hwEng.clearStrokes();
//                                hwEng.strokeCurator.clear();
                                    } else if (action.equals("parse-token-set")) {      /* Parse token set */
//                                        final TokenSetParserOutput[] parserResults = new TokenSetParserOutput[1];

                                        final HandwritingEngine hwEng0 = hwEng;
                                        final long parsingTimeoutMillis0 = parsingTimeoutMillis;

                                        final Future<TokenSetParserOutput> future = execService.submit(
                                                new Callable<TokenSetParserOutput>() {
                                                     @Override
                                                     public TokenSetParserOutput call() {
                                                         TokenSetParserOutput parserResult = null;
                                                         try {
                                                             parserResult = hwEng0.parseTokenSet();
                                                         } catch (HandwritingEngineException exc) {
                                                             errors.add(new JsonPrimitive(TOKEN_SET_PARSING_FAILURE_ERROR_MESSAGE + ": " + exc.getMessage()));
                                                         }

                                                         return parserResult;
                                                     }
                                                });

                                        execService.schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                future.cancel(true);
                                            }
                                        }, parsingTimeoutMillis0, TimeUnit.MILLISECONDS);

                                        TokenSetParserOutput parserResult = null;
                                        try {
                                            parserResult = future.get();
                                        } catch (CancellationException cancelExc) {
                                            errors.add(new JsonPrimitive(TOKEN_SET_PARSING_FAILUE_TIMEOUT));
                                        } catch (InterruptedException intExc) {
                                            errors.add(new JsonPrimitive("Parsing interrupted"));
                                        } catch (ExecutionException execExc) {
                                            errors.add(new JsonPrimitive("Exception occurred during parsing"));
                                        }

                                        if (parserResult != null) {
                                            outObj.add("parseResult", gson.toJsonTree(parserResult));
                                        } else {
                                            outObj.add("parseResult", new JsonNull());
                                        }
                                    } else if (action.equals("get-var-map")) {
                                        PlatoVarMap varMap = null;
                                        try {
                                            varMap = hwEng.getVarMap();
                                        } catch (HandwritingEngineException exc) {
                                            errors.add(new JsonPrimitive("Encountered exception during getVarMap() call: " +
                                                    exc.getMessage()));
                                        }

                                        JsonObject varMapObj = new JsonObject();
                                        for (final String varName : varMap.getVarNamesSorted()) {
                                            final ValueUnion vu = varMap.getVarValue(varName);

                                            JsonObject vuObj = serializeValueUnion(varName, vu);
                                            varMapObj.add(varName, vuObj);
                                        }

                                        outObj.add("varMap", varMapObj);

                                    } else if (action.equals("get-from-var-map")) {           /* Get from var map */
                                        final String varName = reqObj.get("varName").getAsString();

                                        ValueUnion vu = null;
                                        try {
                                            vu = hwEng.getFromVarMap(varName);
                                        } catch (HandwritingEngineException exc) {
                                            errors.add(new JsonPrimitive("Encountered exception during getFromVarMap() call: " +
                                                    exc.getMessage()));
                                        }

                                        JsonObject vuObj = serializeValueUnion(varName, vu);
                                        outObj.add("var", vuObj);
                                    } else if (action.equals("inject-state")) {
                                        JsonObject stateData = reqObj.get("stateData").getAsJsonObject();

                                        hwEng.injectState(stateData);

                                    } else if (action.equals("get-last-stroke-curator-user-action")) {
                                        StrokeCuratorUserAction strokeCuratorUserAction = hwEng.getLastStrokeCuratorUserAction();

                                        if (strokeCuratorUserAction != null) {
                                            outObj.add("lastStrokeCuratorUserAction", new JsonPrimitive(strokeCuratorUserAction.toString()));
                                        } else {
                                            outObj.add("lastStrokeCuratorUserAction", null);
                                        }
                                    } else if (action.equals("undo-stroke-curator-user-action")) {
                                        try {
                                            hwEng.undoStrokeCuratorUserAction();
                                        } catch (IllegalStateException exc) {
                                            errors.add(new JsonPrimitive("Cannot undo stroke curator user action"));
                                        }
                                    } else if (action.equals("redo-stroke-curator-user-action")) {
                                        try {
                                            hwEng.redoStrokeCuratorUserAction();
                                        } catch (IllegalStateException exc) {
                                            errors.add(new JsonPrimitive("Cannot redo stroke curator user action"));
                                        }
                                    }

                        /* TODO: The following steps may not be necessary for actions such as "parse-token-set" */
                                    /* Get written token set */
                                    CWrittenTokenSet wtSet = hwEng.getTokenSet();
//                            CWrittenTokenSet wtSet = hwEng.strokeCurator.getWrittenTokenSet();
                                    logger.info("Done calling getWrittenTokenSet()"); //DEBUG
                                    JsonObject wtSetJsonObj = CWrittenTokenSetJsonHelper.CWrittenTokenSet2JsonObj(wtSet);
                                    outObj.add("writtenTokenSet", wtSetJsonObj);

                                    /* Get last stroke-curator user action */
                                    StrokeCuratorUserAction lastStrokeCuratorUserAction =  hwEng.getLastStrokeCuratorUserAction();
                                    if (lastStrokeCuratorUserAction != null) {
                                        outObj.add("lastStrokeCuratorUserAction", new JsonPrimitive(lastStrokeCuratorUserAction.toString()));
                                    } else {
                                        outObj.add("lastStrokeCuratorUserAction", new JsonNull());
                                    }

                                    /* Undo/redo flags */
                                    outObj.add("canUndoStrokeCuratorUserAction", new JsonPrimitive(hwEng.canUndoStrokeCuratorUserAction()));
                                    outObj.add("canRedoStrokeCuratorUserAction", new JsonPrimitive(hwEng.canRedoStrokeCuratorUserAction()));

                        /* Get the constituent strokes of the written token set */
                                    List<int[]> wtConstStrokeIndices = hwEng.getTokenConstStrokeIndices();
//                            List<int []> wtConstStrokeIndices = hwEng.strokeCurator.getWrittenTokenConstStrokeIndices();
                                    JsonArray constituentStrokes = CWrittenTokenSetJsonHelper.listOfInt2JsonArray(wtConstStrokeIndices);
                                    outObj.add("constituentStrokes", constituentStrokes);
                                } else {
                                    errors.add(new JsonPrimitive("The specified engine UUID does not exist in the handwriting engine pool"));
                                }
                            } else {
                                errors.add(new JsonPrimitive("Request did not specify engine UUID, (engUuid), " +
                                        "which is required for the action \"" + action + "\""));
                            }
                        }
                    }
                }

            }
            else {
                errors.add(new JsonPrimitive("Request contains unrecognized action \"" + action + "\""));
            }
        }
        else {
            errors.add(new JsonPrimitive("Request does not contain action"));
        }

        /* UUID instanceUuid = UUID.randomUUID(); */
        outObj.add("errors", errors);
        out.print(gson.toJson(outObj));

        out.close();

        /* Log request to S3 */
        if (toPublish(reqObj)) {
            String objKey = null;
            if (isCreateEngine) { /* TODO: Do not use two booleans */
                objKey = s3RequestPublisher.publishCreateEngineRequest(reqObj, clientIPAddress, clientHostName, engUuid);
            } else if (isRemoveEngine) {

            } else {
                objKey = s3RequestPublisher.publishGeneralRequest(reqObj);
            }
            logger.info("Published object key = \"" + objKey + "\"");
        }
    }

    private boolean toPublish(JsonObject reqObj) {
        boolean publish = true;

        String engineUuid = null;
        try {
            engineUuid = reqObj.get("engineUuid").getAsString();

            Map<String, WorkerClientInfo> workersClientInfo = hwEngPool.getWorkersClientInfo();
            if (workersClientInfo.containsKey(engineUuid)) {
                WorkerClientInfo clientInfo = workersClientInfo.get(engineUuid);

                ClientTypeMajor clientTypeMajor = clientInfo.getClientTypeMajor();
                if (clientTypeMajor != null && clientTypeMajor == ClientTypeMajor.API) {
                    publish = false;
                }
            }

        } catch (Exception exc) {

        }

        // Do not publish requests from API clients (e.g., unit tests)
        return publish;
    }

    private JsonObject serializeValueUnion(final String varName, final ValueUnion vu) {
        JsonObject vuObj = new JsonObject();

        ValueUnion.ValueType valueType = vu.getValueType();

        vuObj.add("name", new JsonPrimitive(varName));
        vuObj.add("valueType", new JsonPrimitive(valueType.toString()));
        vuObj.add("description", new JsonPrimitive(vu.getDescription()));

        if (valueType == ValueUnion.ValueType.Double) {
            vuObj.add("value", new JsonPrimitive(vu.getDouble()));
        } else if (valueType == ValueUnion.ValueType.PhysicalQuantity) {
            vuObj.add("value", new JsonPrimitive(vu.getDouble()));
        } else { // TODO: Implement JSON representation for matrix values
            vuObj.add("value", new JsonNull());
        }

        return vuObj;
    }

    long getParsingTimeoutMillis() {
        return parsingTimeoutMillis;
    }

    void setParsingTimeoutMillis(long parsingTimeoutMillis) {
        this.parsingTimeoutMillis = parsingTimeoutMillis;
    }
}
