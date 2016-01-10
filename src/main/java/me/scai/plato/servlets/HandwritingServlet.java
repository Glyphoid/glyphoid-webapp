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
import me.scai.handwriting.*;
import me.scai.plato.engine.HandwritingEngine;
import me.scai.plato.engine.HandwritingEngineImpl;
import me.scai.parsetree.HandwritingEngineException;
import me.scai.parsetree.TokenSetParserOutput;
import me.scai.parsetree.evaluation.PlatoVarMap;
import me.scai.parsetree.evaluation.ValueUnion;
import me.scai.plato.helpers.CStrokeJsonHelper;
import me.scai.plato.helpers.CWrittenTokenSetJsonHelper;

import me.scai.plato.helpers.HandwritingEnginePool;
import me.scai.plato.helpers.PlatoVarMapHelper;
import me.scai.plato.publishers.PlatoRequestPublisher;
import me.scai.plato.publishers.PlatoRequestPublisherS3;
import me.scai.plato.security.BypassSecurity;
import me.scai.plato.serverutils.PropertiesHelper;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.clienttypes.ClientTypeMajor;
import me.scai.utilities.clienttypes.ClientTypeMinor;

public class HandwritingServlet extends HttpServlet {
    /* Constants */
    public static final String TOKEN_SET_PARSING_FAILURE_ERROR_MESSAGE = "Failed to parse token set";
    public static final String TOKEN_SET_PARSING_FAILUE_TIMEOUT        = "Parsing cancelled due to timeout";
    public static final String TOKEN_SET_PARSING_FAILUE_INTERRUPTED    = "Parsing was interrupted";

    private static final Logger logger = Logger.getLogger(HandwritingServlet.class.getName());
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    /* Member variables */
    private static PlatoRequestPublisher s3RequestPublisher;

    HandwritingEnginePool hwEngPool;

    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(4); // TODO: Do not hard code

    private long parsingTimeoutMillis;


    @Override
    public void init() throws ServletException {
        parsingTimeoutMillis =
                Long.parseLong(PropertiesHelper.getPropertyByName("handwritingEngineTimeoutMillis"));

        logger.info("Parsing timeout = " + parsingTimeoutMillis + " ms");

        try {
            hwEngPool = new HandwritingEnginePool();
        } catch (IOException exc) {
            logger.severe("Encountered IOException during the construction of HandwritingEnginePool: " +
                          exc.getMessage());
        }

        logger.info("HandwritingServlet has created an instance of HandwritingEnginePool successfully");

        /* Obtain S3 request publisher instance */
//        s3BucketNameForRequestLogging = getS3BucketNameForRequestLogging();
        try {
            s3RequestPublisher = PlatoRequestPublisherS3.createOrGetInstance(hwEngPool.getWorkersClientInfo());
        } catch (Exception e) {
            logger.warning("Failed to create S3 request publisher, due to " + e.getMessage());
        }

    }

    @Override
    public void destroy() {
        if (s3RequestPublisher != null) {
            logger.info("Calling destroy() of s3RequestPublisher");

            try {
                s3RequestPublisher.destroy();
            } catch (Exception e) {
                logger.severe("Failed to destroy S3 request publisher, due to " + e.getMessage());
            }

            logger.info("Done calling destroy() of s3RequestPublisher");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        /* Examine the path to determine if the bypass security check is required */
        boolean bypassSecurity = BypassSecurity.isBypassSecurity(request.getServletPath());

        /* Read the body of the request */
        BufferedReader rdr = request.getReader();
        StringBuilder sb = new StringBuilder();

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

        // Actions that affect the state of the stroke curator and parser
        final String[] configActions = {"get-all-token-names",
                                        "get-graphical-productions"}; // TODO: Replace with enums

        final String[] dataActions = {"add-stroke",
                                      "remove-token",
                                      "remove-last-token",
                                      "remove-tokens",
                                      "move-token",
                                      "move-multiple-tokens",
                                      "get-token-bounds",
                                      "get-written-token-bounds", //TODO: Implement
                                      "merge-strokes-as-token",
                                      "force-set-token-name",
                                      "clear",
                                      "parse-token-set",
                                      "parse-token-subset",
                                      "inject-state",
                                      "get-last-stroke-curator-user-action",
                                      "undo-stroke-curator-user-action",
                                      "redo-stroke-curator-user-action",
    //                                 "get-all-token-names"      // Get all possible token names from the hw engine
                                        }; // TODO: Replace with enums

        boolean isConfigAction = Arrays.asList(configActions).contains(action);
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

                // Initial varMap
                HandwritingEngine hwEng = hwEngPool.getHandwritingEngine(engUuid);
                try {
                    outObj.add(PlatoVarMapHelper.VAR_MAP_KEY, PlatoVarMapHelper.getVarMapAsJson(hwEng.getVarMap(),
                            ((HandwritingEngineImpl) hwEng).stringizer));
                } catch (HandwritingEngineException e) {
                    errors.add(new JsonPrimitive("Failed to obtain initial variable map after engine creation " +
                                                 "(engine UUID = " + engUuid));
                }

            } else if (action.equals("remove-engine")) {
                if (toPublish(reqObj) && s3RequestPublisher != null) {
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
            } else if (action.equals("get-var-map")) {
                HandwritingEngine hwEng = getHandwritingEngineFromRequest(reqObj, errors);

                PlatoVarMap varMap = null;
                try {
                    varMap = hwEng.getVarMap();
                } catch (HandwritingEngineException exc) {
                    errors.add(new JsonPrimitive("Encountered exception during getVarMap() call: " + exc.getMessage()));
                }

                JsonObject varMapObj = new JsonObject();
                for (final String varName : varMap.getVarNamesSorted()) {
                    final ValueUnion vu = varMap.getVarValue(varName);

                    JsonObject vuObj = serializeValueUnion(varName, vu);
                    varMapObj.add(varName, vuObj);
                }

                outObj.add(PlatoVarMapHelper.VAR_MAP_KEY, varMapObj);

            } else if (isConfigAction) {
                HandwritingEngine hwEng = getHandwritingEngineFromRequest(reqObj, errors);

                if (hwEng != null) {
                    try {
                        if (action.equals("get-graphical-productions")) {
                            outObj.add("graphicalProductions", hwEng.getGraphicalProductions());

                        } else if (action.equals("get-all-token-names")) {
                            List<String> allTokenNames = hwEng.getAllTokenNames();

                            JsonElement allTokenNamesJson = gson.toJsonTree(allTokenNames);
                            outObj.add("allTokenNames", allTokenNamesJson);

                        }
                    } catch (HandwritingEngineException e) {
                        errors.add(new JsonPrimitive("Handwriting engine exception occurred: " + e.getMessage()));
                    }

                }

            } else if (isDataAction) {
                HandwritingEngine hwEng = getHandwritingEngineFromRequest(reqObj, errors);

                if (hwEng != null) {
                    try {
                        synchronized (hwEng) {

                            /* First, obtain a snapshot of the var map, which will be compared with the var map
                             * after the operation. The result of the comparison will be used to determine if
                             * varMap will appear in the response JSON object. */
                            JsonObject varMapObj0 = PlatoVarMapHelper.getVarMapAsJson(hwEng.getVarMap(),
                                    ((HandwritingEngineImpl) hwEng).stringizer);


                            if (action.equals("add-stroke")) { /* Add stroke */
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

                            } else if (action.equals("remove-tokens")) { /* Remove a number of tokens */
                                JsonArray tokenIndicesArray = reqObj.get("tokenIndices").getAsJsonArray();

                                int[] tokenIndices = new int[tokenIndicesArray.size()];
                                for (int i = 0; i < tokenIndicesArray.size(); ++i) {
                                    tokenIndices[i] = tokenIndicesArray.get(i).getAsInt();
                                }

                                try {
                                    hwEng.removeTokens(tokenIndices);
                                } catch (HandwritingEngineException exc) {
                                    errors.add(new JsonPrimitive(exc.getMessage()));
                                }

                            } else if (action.equals("move-token")) {        /* Move a single token */

                                final int tokenIdx = reqObj.get("tokenIdx").getAsInt();
                                JsonArray newBoundsJson = reqObj.get("newBounds").getAsJsonArray();

                                final float[] newBounds = new float[newBoundsJson.size()];
                                for (int i = 0; i < newBoundsJson.size(); ++i) {
                                    newBounds[i] = newBoundsJson.get(i).getAsFloat();
                                }

                                logger.info("Handling move-single-token request: strokeIdx = " + tokenIdx +
                                        ", newBounds.length = " + newBounds.length);


                                try {
                                    hwEng.moveToken(tokenIdx, newBounds);
                                } catch (HandwritingEngineException exc) {
                                    errors.add(new JsonPrimitive("Failed to move token due to: " + exc.getMessage()));
                                }
                            } else if (action.equals("move-multiple-tokens")) {        /* Move multiple tokens */

                                JsonArray tokenIndicesArray = reqObj.get("tokenIndices").getAsJsonArray();
                                JsonArray newBoundsArrayJson = reqObj.get("newBoundsArray").getAsJsonArray();

                                // Marshal token indices
                                int[] tokenIndices = new int[tokenIndicesArray.size()];
                                for (int j = 0; j < tokenIndicesArray.size(); ++j) {
                                    tokenIndices[j] = tokenIndicesArray.get(j).getAsInt();
                                }

                                // Marshal new bounds array
                                final float[][] newBoundsArray = new float[newBoundsArrayJson.size()][];
                                for (int j = 0; j < newBoundsArrayJson.size(); ++j) {
                                    JsonArray newBounds = newBoundsArrayJson.get(j).getAsJsonArray();

                                    newBoundsArray[j] = new float[newBounds.size()];
                                    for (int i = 0; i < newBounds.size(); ++i) {
                                        newBoundsArray[j][i] = newBounds.get(i).getAsFloat();
                                    }
                                }

                                logger.info("Handling move-multiple-token request: strokeIndices length = " + tokenIndices.length);

                                // call moveTokens
                                try {
                                    hwEng.moveTokens(tokenIndices, newBoundsArray);
                                } catch (HandwritingEngineException exc) {
                                    errors.add(new JsonPrimitive("Failed to move " + tokenIndices.length + " token(s) at once"));
                                }

//                                for (int i = 0; i < tokenIndices.size(); ++i) {
//                                    int tokenIndex = tokenIndices.get(i).getAsInt();
//
//                                    try {
//                                        hwEng.moveToken(tokenIndex, newBoundsArray[i]);
//                                    } catch (HandwritingEngineException exc) {
//                                        errors.add(new JsonPrimitive("Failed to move token " + i + " of " + tokenIndices.size() +
//                                                " due to: " + exc.getMessage()));
//                                    }
//                                }

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

                            } else if (action.equals("get-written-token-bounds")) {
                                throw new IllegalStateException("get-written-token-bounds not implemented yet");

                            } else if (action.equals("force-set-token-name")) {  /* Force set token name */
                                int setTokenIdx = reqObj.get("tokenIdx").getAsInt();
                                String setTokenRecogWinner = reqObj.get("tokenRecogWinner").getAsString();

                                hwEng.forceSetRecogWinner(setTokenIdx, setTokenRecogWinner);
//                                hwEng.strokeCurator.forceSetRecogWinner(setTokenIdx, setTokenRecogWinner);
                            } else if (action.equals("clear")) {                        /* Clear */
                                hwEng.clearStrokes();
//                                hwEng.strokeCurator.clear();
                            } else if (action.equals("parse-token-set") ||
                                       action.equals("parse-token-subset")) {      /* Parse token set */

                                final HandwritingEngine hwEng0 = hwEng;
                                final long parsingTimeoutMillis0 = parsingTimeoutMillis;
                                final boolean isSubsetParsing = action.equals("parse-token-subset");

                                // Obtain the subset token indices
                                final int[] tokenIndices;
                                if (isSubsetParsing) {
                                    JsonArray tokenIndicesArray = reqObj.get("tokenIndices").getAsJsonArray();

                                    tokenIndices = new int[tokenIndicesArray.size()];
                                    for (int k = 0; k < tokenIndicesArray.size(); ++k) {
                                        tokenIndices[k] = tokenIndicesArray.get(k).getAsInt();
                                    }
                                } else {
                                    tokenIndices = null;
                                }

                                final Future<TokenSetParserOutput> future = execService.submit(
                                        new Callable<TokenSetParserOutput>() {
                                             @Override
                                             public TokenSetParserOutput call() {
                                                 TokenSetParserOutput parserResult = null;
                                                 try {
                                                     if (isSubsetParsing) {
                                                         parserResult = hwEng0.parseTokenSubset(tokenIndices);
                                                     } else {
                                                         parserResult = hwEng0.parseTokenSet();
                                                     }
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
                                    errors.add(new JsonPrimitive(TOKEN_SET_PARSING_FAILUE_INTERRUPTED));
                                } catch (ExecutionException execExc) {
                                    errors.add(new JsonPrimitive("Exception occurred during parsing"));
                                }

                                if (parserResult != null) {
                                    outObj.add("parseResult", gson.toJsonTree(parserResult));
                                } else {
                                    outObj.add("parseResult", new JsonNull());
                                }

                            } else if (action.equals("get-from-var-map")) {           /* Get from var map */ //TODO Move out
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
                                HandwritingEngineUserAction userAction = hwEng.getLastUserAction();

                                if (userAction != null) {
                                    outObj.add("lastStrokeCuratorUserAction", new JsonPrimitive(userAction.toString()));
                                } else {
                                    outObj.add("lastStrokeCuratorUserAction", null);
                                }
                            } else if (action.equals("undo-stroke-curator-user-action")) {
                                try {
                                    hwEng.undoUserAction();
                                } catch (IllegalStateException exc) {
                                    errors.add(new JsonPrimitive("Cannot undo stroke curator user action"));
                                }
                            } else if (action.equals("redo-stroke-curator-user-action")) {
                                try {
                                    hwEng.redoUserAction();
                                } catch (IllegalStateException exc) {
                                    errors.add(new JsonPrimitive("Cannot redo stroke curator user action"));
                                }
                            }

                            /* TODO: The following steps may not be necessary for actions such as "parse-token-set" */
                            /* Get written token set */
                            CAbstractWrittenTokenSet tokenSet = hwEng.getTokenSet();
                            CWrittenTokenSet writtenTokenSet = hwEng.getWrittenTokenSet();

                            JsonObject tokenSetJsonObj =
                                    CWrittenTokenSetJsonHelper.CAbstractWrittenTokenSet2JsonObj(tokenSet);
                            JsonObject writtenTokenSetJsonObj =
                                    CWrittenTokenSetJsonHelper.CAbstractWrittenTokenSet2JsonObj(writtenTokenSet);

                            JsonArray constituentWrittenTokenUuids = gson.toJsonTree(hwEng.getConstituentWrittenTokenUUIDs()).getAsJsonArray();
                            JsonArray writtenTokenUuids = gson.toJsonTree(hwEng.getWrittenTokenUUIDs()).getAsJsonArray();

                            // Remove the unused "recogPs" fields in writtenTokenSetJsonObj and tokenSetJsonObj
                            removeRecogPs(writtenTokenSetJsonObj);
                            removeRecogPs(tokenSetJsonObj);

                            outObj.add("writtenTokenSet", writtenTokenSetJsonObj);  // Written token set
                            outObj.add("tokenSet", tokenSetJsonObj);                // Abstract token set

                            outObj.add("writtenTokenUuids", writtenTokenUuids);     // UUIDs of the written tokens
                            outObj.add("constituentWrittenTokenUuids", constituentWrittenTokenUuids);
                                                                                    // UUIDs of the constituent written tokens of the abstract tokens

                            // TODO: Better way to get the stringizer
                            if ( !outObj.has(PlatoVarMapHelper.VAR_MAP_KEY) ) {
                                JsonObject varMapObj1 = PlatoVarMapHelper.getVarMapAsJson(hwEng.getVarMap(),
                                        ((HandwritingEngineImpl) hwEng).stringizer);

                                // Compare the new var map JSON object with the original one obtained before the data
                                // operation. If difference(s) are found, include the new one in the response object.
                                varMapObj1 = PlatoVarMapHelper.compare(varMapObj1, varMapObj0);
                                if (varMapObj1 != null) {
                                    outObj.add(PlatoVarMapHelper.VAR_MAP_KEY, varMapObj1);
                                }
                            }

                            /* Get last stroke-curator user action */
                            HandwritingEngineUserAction lastUserAction =  hwEng.getLastUserAction();
                            if (lastUserAction != null) {
                                outObj.add("lastStrokeCuratorUserAction", new JsonPrimitive(lastUserAction.toString()));
                            } else {
                                outObj.add("lastStrokeCuratorUserAction", new JsonNull());
                            }

                            /* Undo/redo flags */
                            outObj.add("canUndoStrokeCuratorUserAction", new JsonPrimitive(hwEng.canUndoUserAction()));
                            outObj.add("canRedoStrokeCuratorUserAction", new JsonPrimitive(hwEng.canRedoUserAction()));

                            /* Get the constituent strokes of the written token set */
                            List<int[]> wtConstStrokeIndices = hwEng.getTokenConstStrokeIndices();
//                            List<int []> wtConstStrokeIndices = hwEng.strokeCurator.getWrittenTokenConstStrokeIndices();
                            JsonArray constituentStrokes = CWrittenTokenSetJsonHelper.listOfInt2JsonArray(wtConstStrokeIndices);
                            outObj.add("constituentStrokes", constituentStrokes);

                        }
                    } catch (HandwritingEngineException e) {
                        errors.add(new JsonPrimitive("Handwriting engine exception occurred: " + e.getMessage()));
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
        if (toPublish(reqObj) && s3RequestPublisher != null) {
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

    private HandwritingEngine getHandwritingEngineFromRequest(JsonObject reqObj, JsonArray errors) {
        HandwritingEngineImpl hwEng = null;
        JsonElement engineUuidElem = reqObj.get("engineUuid");

        if (engineUuidElem == null) {
            errors.add(new JsonPrimitive("Engine UUID is not specified"));
        } else {
            String engUuid = engineUuidElem.getAsString();

            try {
                if (hwEngPool.engineExists(engUuid)) {
                    hwEng = hwEngPool.getHandwritingEngine(engUuid);
                    hwEngPool.updateWorkerTimestamp(engUuid); /* TODO: Refactor in */
                } else {
                    errors.add(new JsonPrimitive("The specified engine UUID does not exist in the handwriting engine pool"));
                }
//                else {
//                    errors.add(new JsonPrimitive("Request did not specify engine UUID, (engUuid), " +
//                            "which is required for the action \"" + action + "\""));
//                }
            } catch (IllegalArgumentException exc) {
                errors.add(new JsonPrimitive("Engine UUID is invalid: " + engUuid));
            }
        }

        return hwEng;
    }

    private void removeRecogPs(JsonObject tokenSetJsonObj) {
        JsonArray tokensArray = tokenSetJsonObj.get("tokens").getAsJsonArray();
        for (int k = 0; k < tokensArray.size(); ++k) {
            if (tokensArray.get(k).getAsJsonObject().has("recogPs")) {
                tokensArray.get(k).getAsJsonObject().remove("recogPs");
            }
        }
    }
}

