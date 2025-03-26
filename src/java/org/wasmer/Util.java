package org.wasmer;

public class Util {
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary(Native.DYNAMIC_LIBRARY_NAME_SHORT);
        }
    }

    public static native void nativePanic(String message);
}
