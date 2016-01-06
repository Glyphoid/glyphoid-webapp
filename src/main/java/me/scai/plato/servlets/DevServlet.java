package me.scai.plato.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.scai.plato.serverutils.PropertiesHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

public class DevServlet extends HttpServlet {
    private static final JsonParser jsonParser = new JsonParser();
    private static final Gson gson = new Gson();

    private static final Logger logger = Logger.getLogger(HttpServlet.class.getName());

    private static boolean enabled = false;

    @Override
    public void init(ServletConfig servletConfig) {
        try {
            logger.info("DevServlet: getProperty enableDevServlet = " +
                    PropertiesHelper.getPropertyByName("enableDevServlet"));
            enabled = Boolean.parseBoolean(PropertiesHelper.getPropertyByName("enableDevServlet"));
            logger.info("DevServlet: enabled = " + enabled);
        } catch (Exception e) {
            logger.severe("Failed to read property enableDevServlet");
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JsonObject outObj = new JsonObject();

        if (enabled) {
            BufferedReader rdr = request.getReader();
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line);
            }

            rdr.close();

            String reqBody = sb.toString();

            JsonObject reqObj = jsonParser.parse(reqBody).getAsJsonObject();

            String filePath = reqObj.get("filePath").getAsString();
            String fileContent = reqObj.get("fileContent").getAsString();

        /* Check if file already exists */
            File checkedFile = new File(filePath);


            String resultString = null;
            boolean isError = false;
            if (checkedFile.exists() && checkedFile.isFile()) {
                resultString = "ERROR: File already exists";
                isError = true;
            } else {
                PrintWriter pw = null;

                try {
                    pw = new PrintWriter(filePath);
                    pw.write(fileContent);


                    resultString = "success";
                    isError = false;
                } catch (Exception e) {
                    resultString = "failure";
                    isError = true;
                } finally {
                    if (pw != null) {
                        pw.close();
                    }
                }
            }

            outObj.add("result", new JsonPrimitive(resultString));
            outObj.add("isError", new JsonPrimitive(isError));
        } else {
            outObj.add("result", new JsonPrimitive("ERROR: not enabled"));
            outObj.add("isError", new JsonPrimitive(true));
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(gson.toJson(outObj));

        out.close();
    }
}
