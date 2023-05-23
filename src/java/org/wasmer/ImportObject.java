package org.wasmer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImportObject {
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }

    private final String namespace;
    private final String name;

    public ImportObject(String namespace, String name) {
        this.name = name;
        this.namespace = namespace;
    }

    public static class FuncImport extends ImportObject {
        private Function<long[], long[]> function;
        private final List<Type> argTypes;
        private final List<Type> retTypes;
        private final int[] argTypesInt;
        private final int[] retTypesInt;

        public FuncImport(String namespace, String name, Function<List<Number>, List<Number>> function, List<Type> argTypes, List<Type> retTypes) {
            super(namespace, name);
            this.function = (long[] argv) -> {
                List<Number> lret = function.apply(IntStream.range(0, argTypes.size()).mapToObj((int i) -> {
                    switch (argTypes.get(i)) {
                        case I32:
                            return (int) argv[i];
                        case I64:
                            return argv[i];
                        case F32:
                            return Float.intBitsToFloat((int) argv[i]);
                        case F64:
                            return Double.longBitsToDouble(argv[i]);
                        default:
                            throw new RuntimeException("Unreachable (argument type)");
                    }
                }).collect(Collectors.toList()));
                long[] ret = argv.length >= retTypes.size() ? argv : new long[retTypes.size()];
                for (int i = 0; i < retTypes.size(); i++)
                    switch (retTypes.get(i)) {
                        case I32:
                        case I64:
                            ret[i] = lret.get(i).longValue();
                            break;
                        case F32:
                            ret[i] = Float.floatToRawIntBits(lret.get(i).floatValue());
                            break;
                        case F64:
                            ret[i] = Double.doubleToRawLongBits(lret.get(i).doubleValue());
                            break;
                        default:
                            throw new RuntimeException("Unreachable (return type)");
                    }
                return ret;
            };
            this.argTypesInt = argTypes.stream().mapToInt(t -> t.i).toArray();
            this.retTypesInt = retTypes.stream().mapToInt(t -> t.i).toArray();
            this.argTypes = Collections.unmodifiableList(argTypes);
            this.retTypes = Collections.unmodifiableList(retTypes);
        }
    }

    public static class MemoryImport extends ImportObject {
        private int minPages;
        private Integer maxPages;
        private boolean shared;

        public MemoryImport(String namespace, int minPages, Integer maxPages, boolean shared) {
            super(namespace, "memory");
            this.minPages = minPages;
            this.maxPages = maxPages;
            this.shared = shared;
        }

        public MemoryImport(String namespace, int minPages, boolean shared) {
            super(namespace, "memory");
            this.minPages = minPages;
            this.maxPages = null;
            this.shared = shared;
        }
    }
}
