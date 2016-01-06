package me.scai.plato.security;

import com.google.gson.JsonObject;
import me.scai.plato.serverutils.PropertiesHelper;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class BypassSecurity {
    /* Constants */
    private static final String propFileName = "plato.properties";
    private static final String bypassServletPathSuffix = "-bypass";
    private static final Logger logger = Logger.getLogger(BypassSecurity.class.getName());

    /* Member variables */
    private static String bypassSecurityToken;
    private static AtomicBoolean initialized = new AtomicBoolean(false);

    public static boolean isBypassSecurity(String servletPath) {
        String [] reqPathParts = servletPath.split("/");

        return (reqPathParts.length > 0 &&
                reqPathParts[reqPathParts.length - 1].endsWith(bypassServletPathSuffix));
    }

    public static synchronized void initialize() {
        if (initialized.get()) {
            return;
        }

        Properties platoSecretsProps = PropertiesHelper.getNestedProperties("secretsPropertiesFile");

        bypassSecurityToken = platoSecretsProps.getProperty("bypassSecurityToken");
        initialized.set(true);
    }

    public static boolean isAuthorized(JsonObject reqObj) {
        if ( !initialized.get() ) {
            initialize();
        }

        if (reqObj.get("securityToken") == null ||
            reqObj.get("securityToken").getAsJsonPrimitive() == null ||
            reqObj.get("securityToken").getAsJsonPrimitive().getAsString() == null ||
            !reqObj.get("securityToken").getAsJsonPrimitive().getAsString().equals(bypassSecurityToken)) {
            return false;
        } else {
            return true;
        }
    }
}
