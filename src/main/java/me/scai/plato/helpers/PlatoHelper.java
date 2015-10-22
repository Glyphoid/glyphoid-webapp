package me.scai.plato.helpers;

/**
 * Created by scai on 3/25/2015.
 */
public class PlatoHelper {
    /* OS detection helper function */
    public static final String determineOS() {
        String osName = System.getProperty("os.name");

        if (osName.indexOf("Windows") >= 0) {
            return "win";
        }
        else if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") >= 0) {
            return "glnx";
        }
        else {
            return null;
        }
    }

    public static final String getOSSuffix() {
        return "_" + PlatoHelper.determineOS();
    }
}
