package org.wasmer;

public class Util {
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }

    public static native void nativePanic(String message);
}
