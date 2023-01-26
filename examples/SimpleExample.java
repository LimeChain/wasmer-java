import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Memory;
import org.wasmer.Module;
import org.wasmer.Type;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

class SimpleExample {
    public static void main(String[] args) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("runtime.wasm"));
        Module module = new Module(bytes);
        AtomicReference<Instance> arInstance = new AtomicReference<>();
        Imports imports = Imports.from(Arrays.asList(
                new Imports.Spec("env", "ext_storage_set_version_1", argv -> {
                    Memory memory = arInstance.get().exports.getMemory("memory");
                    int msgKeyPtr = argv.get(0).intValue();
                    ByteBuffer mbf = memory.buffer();
                    String msgKey = getString(msgKeyPtr, mbf);
                    argv.set(0, msgKey.length());
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Collections.emptyList()),
                new Imports.Spec("env", "ext_storage_get_version_1", argv -> {
                    return argv;
                }, Collections.singletonList(Type.I64), Collections.singletonList(Type.I64))), module);
        Instance instance = module.instantiate(imports);
        System.out.println("here");

        System.out.println(instance.exports.toString());
        Object result = instance.exports.getFunction("Core_version").apply(1, 1)[0];

//        assert result == 3;

        System.out.println(result);

        instance.close();
    }

    private static String getString(Integer ptr, ByteBuffer mbf) {
        StringBuilder sb = new StringBuilder();
        for(int i = ptr, max = mbf.limit(); i < max; i++) {
            mbf.position(i);
            byte b = mbf.get();
            if (b == 0) {
                break;
            }
            sb.appendCodePoint(b);
        }
        return sb.toString();
    }
}
