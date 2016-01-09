package me.scai.plato.helpers;

import me.scai.handwriting.StrokeCurator;
import me.scai.handwriting.StrokeCuratorConfigurable;
import me.scai.handwriting.TokenRecogEngine;
import me.scai.handwriting.TokenRecogEngineSDV;
import me.scai.parsetree.*;

import me.scai.plato.engine.HandwritingEngineImpl;
import me.scai.plato.serverutils.PropertiesHelper;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.WorkerPool;
import me.scai.utilities.WorkerPoolImpl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Date;
import java.util.logging.Logger;

/* Factory for "handwriting engines", each of which consists of an instance of:
 *     1) token engine,
 *     2) token-set parser,
 *     3) stroke-curator
 */
public class HandwritingEnginePool {
    /* Members */
    private static final Logger logger = Logger.getLogger(HandwritingEnginePool.class.getName());

    private static int maxNumHandwritingEngines;
    private static long handwritingEngineTimeoutMillis;

    private static WorkerPool workerPool;

    private TerminalSet terminalSet;
    private GraphicalProductionSet gpSet;

    private float recursionGeomScoreRatioThresh = 0.90f;

//    private ConcurrentHashMap<String, HandwritingEngine> hwEngines = new ConcurrentHashMap<String, HandwritingEngine>();

    /* Methods */
    /* Constructor */
    public HandwritingEnginePool() throws IOException {
        /* Get the value of recursionGeomScoreRatioThresh */
        recursionGeomScoreRatioThresh = Float.parseFloat(PropertiesHelper.getPropertyByName("recursionGeomScoreRatioThresh"));

        /* Get the value of maxNumHandwritingEngines */
        maxNumHandwritingEngines = Integer.parseInt(PropertiesHelper.getPropertyByName("maxNumHandwritingEngines"));
        if (maxNumHandwritingEngines <= 0) {
            logger.warning("Detected unreasonable value in maxNumHandwritingEngines: " +
                           maxNumHandwritingEngines);
        }

        handwritingEngineTimeoutMillis = Long.parseLong(PropertiesHelper.getPropertyByName("handwritingEngineTimeoutMillis"));
        if (handwritingEngineTimeoutMillis <= 0) {
            logger.warning("Detected unreasonable value in handwritingEngineTimeoutMillis: " +
                    handwritingEngineTimeoutMillis);
        }
        logger.info("handwritingEngineTimeoutMillis = " + handwritingEngineTimeoutMillis);

        /* Create WorkerPool instance */
        this.workerPool = createOrGetWorkerPool();

        /* Get terminal set, this is shared among all instances of the token-set parser */
        String termSetFN = PropertiesHelper.getPropertyByName("terminalsDefinitionFile");
        URL termSetUrl = new File(termSetFN).toURI().toURL();
        logger.info("Properties file indicates that terminal set file is at: \"" + termSetUrl + "\"");

        try {
//            terminalSet = TerminalSet.createFromUrl(termSetUrl);
            terminalSet = TerminalSet.createFromJsonAtUrl(termSetUrl);
        }
        catch (Exception exc) {
            logger.severe("Failed to create terminal set from URL: \"" + termSetUrl + "\", due to " + exc.getMessage());
        }

        /* Get the graphical production set */
        String gpSetFN = PropertiesHelper.getPropertyByName("productionsDefinitionFile");
        URL gpSetUrl = new File(gpSetFN).toURI().toURL();
        logger.info("Properties file indicates that graphical production file is at: \"" + gpSetUrl + "\"");

        try {
            gpSet = GraphicalProductionSet.createFromUrl(gpSetUrl, terminalSet);
        }
        catch (Exception exc) {
            logger.severe("Failed to create terminal set from URL: \"" + gpSetUrl + "\", due to " + exc.getMessage());
        }
    }

    /* Create a stroke curator */
    private StrokeCurator genStrokeCurator() {
        String strokeCuratorConfigFN = PropertiesHelper.getPropertyByName("strokeCuratorConfigFile");

        URL strokeCuratorConfigFileURL = null;
        try {
            strokeCuratorConfigFileURL = new File(strokeCuratorConfigFN).toURI().toURL();
        }
        catch (MalformedURLException exc) {
            logger.severe("Failed to convert the file name of stroke curator configuration file path to URL, due to " + exc.getMessage());
        }

        StrokeCurator strokeCurator = null;
        try {
            strokeCurator = new StrokeCuratorConfigurable(strokeCuratorConfigFileURL, genTokenRecogEngine());
        }
        catch (Exception exc) {
            logger.severe("Failed to create new instance of stroke curator, due to " + exc.getMessage());
        }

        return strokeCurator;
    }

    /* Create a token engine */
    private TokenRecogEngine genTokenRecogEngine() {
        TokenRecogEngine tokEngine = null;

        String tokenEngineFN = PropertiesHelper.getPropertyByName("tokenEngineSerFile");
        logger.info("Properties file indicates that token engine serialization file is at: \"" + tokenEngineFN + "\"");

        ObjectInputStream objInStream = null;
        try {
            objInStream = new ObjectInputStream(new FileInputStream(tokenEngineFN));
            logger.info("objInStream = " + objInStream);

            tokEngine = (TokenRecogEngineSDV) objInStream.readObject();
            logger.info("After readObject() call, tokEngine = " + tokEngine);
        } catch (IOException e) {
            logger.severe("Failed to read token engine from file: \"" + tokenEngineFN + "\" due to " +
                    e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.severe("Failed to read token engine from file: \"" + tokenEngineFN + "\" due to " +
                    e.getMessage());
        } finally {
            try {
                objInStream.close();
            } catch (IOException e) {
                logger.severe("Failed to close token engine object input stream");
            }
        }

        logger.info("De-serialization of the token engine is complete.");

        return tokEngine;
    }

    /* Create a new instance of token-set parser */
    private TokenSetParser genTokenSetParser() {
        TokenSetParser parser = null;

        if (terminalSet == null || gpSet == null) {
            logger.severe("Attempt to create new instance of token set parser failed because " +
                          "of null value(s) in terminal set and/or graphical production set");
            return null;
        }

        return new TokenSetParser(terminalSet, gpSet, recursionGeomScoreRatioThresh);
    }

    /* Get a new instance of handwriting engine */
    public synchronized String genNewHandwritingEngine(WorkerClientInfo wkrClientInfo) {
        HandwritingEngineImpl hwEng = new HandwritingEngineImpl(genStrokeCurator(),
                                                        genTokenSetParser(),
                                                        gpSet,
                                                        terminalSet);

        String newWkrId = workerPool.registerWorker(hwEng, wkrClientInfo);
        int numCurrWorkers = workerPool.getCurrNumWorkers(); //DEBUG
        logger.info("numCurrWorkers = " + numCurrWorkers);   //DEBUG

        if (newWkrId == null) {
            logger.severe("Attempting to create new handwriting engine while limit on the number of such engines (" +
                          maxNumHandwritingEngines + ") is already reached. Returning null.");
        }
        return newWkrId;

//        if (numEngines() >= maxNumHandwritingEngines) {
//            logger.severe("Attempting to create new handwriting engine while limit on the number of such engines (" +
//                          maxNumHandwritingEngines + ") is already reached. Returning null.");
//            return null;
//        }
//        else {
//            hwEngines.put(tUuid.toString(), hwEng);
//            return tUuid.toString();
//        }
    }

    /* Get the number of currently registered engines */
    public synchronized int numEngines() {
        return workerPool.getCurrNumWorkers();
//        return hwEngines.size();
    }

    /* Empty the pool of handwriting engines */
    public synchronized void clear() {
        workerPool.clearWorkers();
//        hwEngines.clear();
    }

    /* Get the reference to a handwriting engine */
    public synchronized HandwritingEngineImpl getHandwritingEngine(String uuid) {
        return (HandwritingEngineImpl) workerPool.getWorker(uuid);
//        return hwEngines.get(uuid);
    }

    /* Test whether an engine exists */
    public synchronized boolean engineExists(String uuid) {
        boolean exists = (workerPool.getWorker(uuid) != null);

//        boolean exists = hwEngines.containsKey(uuid);
        return exists;
    }

    /* Remove a given engine */
    public void remove(String uuid) {
        workerPool.removeWorker(uuid);
//        hwEngines.remove(uuid);
    }

    /* Update timestamp */
    public void updateWorkerTimestamp(String uuid) {
        workerPool.updateWorkerTimestamp(uuid, new Date());
        workerPool.incrementMessageCount(uuid);
    }

    /* Create or get the worker pool */
    public static WorkerPool createOrGetWorkerPool() {
        if (workerPool == null) {
            workerPool = new WorkerPoolImpl(maxNumHandwritingEngines, handwritingEngineTimeoutMillis);
            logger.info("Created worker pool instance: " + workerPool);
        }

        return workerPool;
    }

    public int getMessageCount(String uuid) {
        return workerPool.getMessageCount(uuid);
    }

    public float getCurrentAverageMessageRate(String uuid) {
        return workerPool.getCurrentAverageMessageRate(uuid);
    }

    public Map<String, WorkerClientInfo> getWorkersClientInfo() {
        return workerPool.getWorkersClientInfo();
    }
}
