package org.navitproject.glassheadup;

public class ConversionHelper {

    static {
            System.loadLibrary("zonedetect");
    }

    public static native String getTimezone(double lat, double lon);
}
