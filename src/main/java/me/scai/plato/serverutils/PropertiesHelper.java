package me.scai.plato.serverutils;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public final class PropertiesHelper {
    /* Constants */
    private static final String propFileName = "plato.properties";

    private static final String ASSET_ROOT_PLACE_HOLDER = "${assetRoot}";

    private static final Logger logger = Logger.getLogger(PropertiesHelper.class.getName());

    /* Methods */
    private static final Properties getProperties() {
        Properties props = new Properties();
        InputStream inStream = PropertiesHelper.class.getClassLoader().getResourceAsStream(propFileName);

        try {
            if (inStream != null) {
                props.load(inStream);
            } else {
                logger.severe("InputStream for properties file \"" + propFileName + "\" is null");
            }
        } catch (IOException exc) {
            logger.severe("IOException occurred during loading of properties file \"" + propFileName + "\"");
        }

        return props;
    }

    public static final String getPropertyByName(String propName) {
        Properties props = getProperties();
        String propVal = props.getProperty(propName);

        if (propVal.indexOf(ASSET_ROOT_PLACE_HOLDER) != -1) {
            String sysAssetRoot = System.getProperty("assetRoot");

            if (sysAssetRoot == null) {
                throw new IllegalStateException("Property " + propName + " requires system property assetRoot, but assetRoot is not set");
            }

            propVal = propVal.replace(ASSET_ROOT_PLACE_HOLDER, sysAssetRoot);
        }

        return propVal;
    }



    public static final Properties getNestedProperties(String nestedPropFileNamePrefix) {
        Properties props = getProperties();

        final String propName = nestedPropFileNamePrefix;

        String platoAwsPropertiesFileName = getPropertyByName(propName);

        Properties nestedProps = new Properties();

        InputStream inStrem = null;
        try {
            inStrem = new BufferedInputStream(new FileInputStream(new File(platoAwsPropertiesFileName)));
        } catch (FileNotFoundException exc) {
            logger.severe("Failed to find nested properties file: " + platoAwsPropertiesFileName);
        }

        if (inStrem != null) {
            try {
                nestedProps.load(inStrem);
            } catch (IOException exc) {
                logger.severe("Failed to read nested properties file: \"" + platoAwsPropertiesFileName + "\". All attempts to create S3 client have failed.");
                return null;
            } finally {
                try {
                    inStrem.close();
                } catch (IOException exc) {}
            }
        } else {
            logger.severe("InputStream for nested properties file \"" + platoAwsPropertiesFileName + "\" is null. All attempts to create S3 client have failed.");
            return null;
        }

        return nestedProps;
    }
}
