package org.wasmer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// The fields producing these warnings are accessed in Rust and would
// break Rust code if changed
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ImportObject {
    private static final Logger logger = Logger.getLogger(ImportObject.class.getName());
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary(Native.DYNAMIC_LIBRARY_NAME_SHORT);
        }
    }

    private final String namespace;
    private final String name;

    public ImportObject(String namespace, String name) {
        logger.finer(String.format("Initializing import %s with namespace %s", name, namespace));
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
            logger.fine(String.format("Initialized function import %s with namespace %s", name, namespace));
            this.function = (long[] argv) -> {
                List<Number> lret = function.apply(IntStream.range(0, argTypes.size()).mapToObj((int i) -> switch (argTypes.get(i)) {
                    case I32 -> (int) argv[i];
                    case I64 -> argv[i];
                    case F32 -> Float.intBitsToFloat((int) argv[i]);
                    case F64 -> Double.longBitsToDouble(argv[i]);
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
            logger.fine(String.format("Initialized memory import with namespace %s", namespace));
            this.minPages = minPages;
            this.maxPages = maxPages;
            this.shared = shared;
        }

        public MemoryImport(String namespace, int minPages, boolean shared) {
            super(namespace, "memory");
            logger.fine(String.format("Initialized memory import with namespace %s", namespace));
            this.minPages = minPages;
            this.maxPages = null;
            this.shared = shared;
        }
    }
}
