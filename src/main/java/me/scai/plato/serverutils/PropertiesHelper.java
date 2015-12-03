package me.scai.plato.serverutils;

import me.scai.plato.helpers.PlatoHelper;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public final class PropertiesHelper {
    private static final String propFileName = "plato.properties";

    private static final Logger logger = Logger.getLogger(PropertiesHelper.class.getName());

    public static final Properties getProperties() {
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



    public static final Properties getNestedProperties(String nestedPropFileNamePrefix) {
        Properties props = getProperties();

        String osSuffix = PlatoHelper.getOSSuffix();

        final String propName = nestedPropFileNamePrefix + osSuffix;

        String platoAwsPropertiesFileName = props.getProperty(propName);

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
