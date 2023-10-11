/**
 * Code reduced and simplified from zmq integration in Java. See
 * https://github.com/zeromq/jzmq/blob/3384ea1c04876426215fe76b5d1aabc58c099ca0/jzmq-jni/src/main/java/org/zeromq/EmbeddedLibraryTools.java.
 */

package org.wasmer;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Native {
    public static final boolean LOADED_EMBEDDED_LIBRARY;
    private static final Logger logger = Logger.getLogger(Native.class.getName());

    static {
        LOADED_EMBEDDED_LIBRARY = loadEmbeddedLibrary();
    }

    private Native() {}

    public static String getCurrentPlatformIdentifier() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            osName = "windows";
        } else if (osName.contains("mac os x")) {
            osName = "darwin";
            String[] args = new String[] {"/bin/bash", "-c", "uname", "-p"};
            try {
                Process proc = new ProcessBuilder(args).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                if (reader.readLine().equals("Darwin")) {
                    return osName + "-arm64";
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            osName = osName.replaceAll("\\s+", "_");
        }
        return osName + "-" + System.getProperty("os.arch");
    }

    private static boolean loadEmbeddedLibrary() {
        boolean usingEmbedded = false;

        // attempt to locate embedded native library within JAR at following location:
        // /NATIVE/${os.arch}/${os.name}/[libwasmer.so|libwasmer.dylib|wasmer.dll]
        String[] libs;
        final String libsFromProps = System.getProperty("wasmer-native");

        if (libsFromProps == null) {
            libs = new String[]{"libwasmer_jni.so", "libwasmer_jni.dylib", "wasmer_jni.dll"};
        } else {
            libs = libsFromProps.split(",");
        }

        StringBuilder url = new StringBuilder();
        url.append("/org/wasmer/native/");
        url.append(getCurrentPlatformIdentifier()).append("/");

        URL nativeLibraryUrl = null;
        // loop through extensions, stopping after finding first one
        for (String lib: libs) {
            nativeLibraryUrl = Module.class.getResource(url + lib);

            if (nativeLibraryUrl != null) {
                break;
            }
        }
        
        if (nativeLibraryUrl != null) {
            // native library found within JAR, extract and load
            try {
                final File libfile = File.createTempFile("wasmer_jni", ".lib");
                libfile.deleteOnExit(); // just in case

                final InputStream in = nativeLibraryUrl.openStream();
                final OutputStream out = new BufferedOutputStream(Files.newOutputStream(libfile.toPath()));

                int len;
                byte[] buffer = new byte[8192];

                while ((len = in.read(buffer)) > -1) {
                    out.write(buffer, 0, len);
                }

                out.close();
                in.close();
                System.load(libfile.getAbsolutePath());

                usingEmbedded = true;
            } catch (IOException x) {
                logger.log(Level.SEVERE, "Failed to load native library", x);
            }

        }

        return usingEmbedded;
    }

    public static native void nativePanic(String message);
}
