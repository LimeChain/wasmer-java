package org.wasmer;

import java.util.List;

public class Imports {

    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }
    private static native long nativeImportsInstantiate(List<ImportObject> imports, long modulePointer) throws RuntimeException;
    private static native long nativeImportsChain(long back, long front) throws RuntimeException;
    private static native long nativeImportsWasi(long modulePointer) throws RuntimeException;
    private static native void nativeDrop(long nativePointer);

    final long importsPointer;

    private Imports(long importsPointer) {
        this.importsPointer = importsPointer;
    }

    public static Imports from(List<ImportObject> imports, Module module) throws RuntimeException {
        return new Imports(nativeImportsInstantiate(imports, module.modulePointer));
    }

    public static Imports chain(Imports back, Imports front) {
       return new Imports(nativeImportsChain(back.importsPointer, front.importsPointer));
    }

    public static Imports wasi(Module module) {
        return new Imports(nativeImportsWasi(module.modulePointer));
    }

    protected void finalize() {
        nativeDrop(importsPointer);
        // TODO allow memory-safe user invocation
    }
}
