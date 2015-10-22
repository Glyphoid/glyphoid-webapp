package me.scai.plato.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import me.scai.plato.helpers.HandwritingEnginePool;
import me.scai.utilities.WorkerPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class HandwritingEnginePoolConfigServlet  extends HttpServlet {
    private static final JsonParser jsonParser = new JsonParser();
    private static final Gson gson = new Gson();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String reqData = buffer.toString();

        JsonObject reqObj = jsonParser.parse(reqData).getAsJsonObject();

        /* Security */
        boolean authorized = checkAuthorization(reqObj);
        JsonObject respObj = new JsonObject();

        if (authorized) {
            /* Get the instance of worker pool */
            WorkerPool workerPool = HandwritingEnginePool.createOrGetWorkerPool();

            JsonArray updated = new JsonArray();
            if (reqObj.has("maxNumWorkers")) {
                int maxNumWorkers = reqObj.get("maxNumWorkers").getAsJsonPrimitive().getAsInt();
                workerPool.setMaxNumWorkers(maxNumWorkers);

                updated.add(new JsonPrimitive("maxNumWorkers"));
            }

            if (reqObj.has("workerTimeoutMillis")) {
                long workerTimeoutMillis = reqObj.get("workerTimeoutMillis").getAsJsonPrimitive().getAsLong();
                workerPool.setWorkerTimeout(workerTimeoutMillis);

                updated.add(new JsonPrimitive("workerTimeoutMillis"));
            }

            respObj.add("updated", updated);
        }
        else {
            respObj.add("error", new JsonPrimitive("Authorization failed"));
        }

        /* Write the response */
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        out.write(gson.toJson(respObj));
    }

    private boolean checkAuthorization(final JsonObject reqObj) {
        final String trueUsername = "plato-admin";
        final String truePassword = "Ydm2tovw4HjCwzOReeQU";

        if (!reqObj.has("username") || !reqObj.has("password")) {
            return false;
        }

        String username = reqObj.get("username").getAsJsonPrimitive().getAsString();
        String password = reqObj.get("password").getAsJsonPrimitive().getAsString();

        if (username.equals(trueUsername) && password.equals(truePassword)) {
            return true;
        }
        else {
            return false;
        }
    }
}
