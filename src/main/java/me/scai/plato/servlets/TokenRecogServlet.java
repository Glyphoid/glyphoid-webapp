package me.scai.plato.servlets;

import com.google.gson.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.scai.handwriting.CWrittenToken;
import me.scai.handwriting.TokenRecogEngine;
import me.scai.handwriting.TokenRecogEngineSDV;
import me.scai.plato.security.BypassSecurity;
import me.scai.plato.serverutils.PropertiesHelper;
import org.apache.commons.io.IOUtils;

class TokenPValPair implements Comparable<TokenPValPair> {
    /* Member variables */
    private String tokenName;
    private double p;

    /* Constructor */
    public TokenPValPair(String tokenName, double p) {
        this.tokenName = tokenName;
        this.p = p;
    }

    /* Getters */
    public String getTokenName() {
        return tokenName;
    }

    public double getP() {
        return p;
    }

    @Override
    public int compareTo(TokenPValPair that) {
        return Double.compare(this.p, that.p);
    }

}


public class TokenRecogServlet extends HttpServlet {
    /* Constants */
    private static final int NUM_CANDIDATES = 10;

    private static final Logger logger = Logger.getLogger(TokenRecogServlet.class.getName());

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    /* Member variables */
    private TokenRecogEngine tokEngine;
    private ArrayList<String> tokNames;


    @Override
    public void init() throws ServletException {
        String tokEngSerFN = PropertiesHelper.getPropertyByName("tokenEngineSerFile");
        URL tokenEngineSerURL = null;
        try {
            tokenEngineSerURL = new File(tokEngSerFN).toURI().toURL();
        } catch (MalformedURLException exc) {
            logger.severe("Encountered MalformedURLException while trying to get the URL of " +
                          "token engine serialization file \"" + tokEngSerFN + "\"");
        }

        logger.info("tokenEngineSerURL = " + tokenEngineSerURL);

        ObjectInputStream objInStream = null;
        try {
            objInStream = new ObjectInputStream(tokenEngineSerURL.openStream());
            logger.info("objInStream = " + objInStream);

            tokEngine = (TokenRecogEngineSDV) objInStream.readObject();
            logger.info("After readObject() call, tokEngine = " + tokEngine);
        } catch (IOException e) {
            logger.severe("Failed to read token engine from URL: \"" + tokenEngineSerURL + "\" due to " +
                          e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.severe("Failed to read token engine from URL: \"" + tokenEngineSerURL + "\" due to " +
                          e.getMessage());
        } finally {
            try {
                objInStream.close();
            } catch (IOException e) {
                logger.severe("Failed to close token engine object input stream");
            }
        }

        tokNames = tokEngine.tokenNames; /* TODO: Thread safety */
//        tokNamesArray = getTokenNamesJsonArray();

        logger.info("De-serialization of the token engine is complete.");
    }

    /* Construct JsonArray for the token names */
    private JsonArray getTokenNamesJsonArray() {
        if (tokNames == null) {
            return null;
        }

        JsonArray tokNamesArray = new JsonArray();
        for (String tokName : tokNames) {
            tokNamesArray.add(new JsonPrimitive(tokName));
        }

        return tokNamesArray;
    }

    private JsonArray getPValuesJsonArray(double [] pVals) {
        if (pVals == null) {
            return null;
        }

        JsonArray pValsArray = new JsonArray();
        for (int i = 0; i < pVals.length; ++i) {
            pValsArray.add(new JsonPrimitive(pVals[i]));
        }

        return pValsArray;
    }

    private JsonArray getRecogPValuesArray(double [] pVals) {
        if (tokNames == null || pVals == null || tokNames.size() != pVals.length) {
            return null;
        }

        JsonArray ps = new JsonArray();
        for (int i = 0; i < tokNames.size(); ++i) {
            JsonArray po = new JsonArray();
            po.add(new JsonPrimitive(tokNames.get(i)));
            po.add(new JsonPrimitive(pVals[i]));

            ps.add(po);
        }

        return ps;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean bypassSecurity = BypassSecurity.isBypassSecurity(request.getServletPath());

        /* Read the body of the request */
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String reqData = buffer.toString();

//        logger.info("reqData = \"" + reqData + "\""); //DEBUG

        /* XHR for debugging */ // TODO: Remove
        response.addHeader("Access-Control-Allow-Origin", "null");
        response.addHeader("Access-Control-Allow-Methods", "POST");

        /* Prepare the response */
        response.setContentType("application/json");
        JsonObject outObj = new JsonObject();
        JsonArray errors = new JsonArray();
        PrintWriter out = response.getWriter();

        /* Parse the request */
        JsonObject reqObj = null;
        try {
            reqObj = jsonParser.parse(reqData).getAsJsonObject();
        } catch (Exception exc) {
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

        /* Construct written token object */
        CWrittenToken wt = new CWrittenToken(reqData);

        /* Perform recognition */
        /* TODO: Check to see if the token engine is null */
        double[] recogPVals = new double[tokNames.size()];
        int recogWinnerIdx = tokEngine.recognize(wt, recogPVals);
        String recogWinnerTokenName = tokNames.get(recogWinnerIdx);

        /* Sort all tokens by descending order of p-value */
        TokenPValPair[] pairs = new TokenPValPair[recogPVals.length];

        for (int i = 0; i < pairs.length; ++i) {
            pairs[i] = new TokenPValPair(tokNames.get(i), recogPVals[i]);
        }

        Arrays.sort(pairs);

        JsonArray recogPValsJson = new JsonArray();
        int candidateCount = 0;
        for (int i = pairs.length - 1;
             i >= 0 && candidateCount++ < NUM_CANDIDATES;
             --i) {
            JsonArray el = new JsonArray();

            el.add(new JsonPrimitive(pairs[i].getTokenName()));
            el.add(new JsonPrimitive(pairs[i].getP()));

            recogPValsJson.add(el);
        }

//        PrintWriter out = response.getWriter();
//        JsonObject outObj = new JsonObject();


        outObj.add("agent", new JsonPrimitive("TokenRecogServlet"));
//        outObj.add("requestData", new JsonPrimitive(reqData));
        outObj.add("winnerTokenName", new JsonPrimitive(recogWinnerTokenName));
//        outObj.add("recogPVals", getRecogPValuesArray(recogPVals));
        outObj.add("recogPVals", recogPValsJson);

        try {
            out.println(gson.toJson(outObj));
        } catch (Exception e) {

        } finally {
            IOUtils.closeQuietly(out);
        }

    }
}
