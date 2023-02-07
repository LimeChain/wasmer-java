import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.MemoryType;
import org.wasmer.Module;
import org.wasmer.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

class ImportExample {
    public static void main(String[] args) throws IOException {
        System.out.println("TEST IMPORTING FUNCTIONS AND MEMORY + INVOCATION OF BOTH IMPORTED FUNCTIONS");
        System.out.println("Reading wasm bytes");
        byte[] bytes = Files.readAllBytes(Paths.get("runtime.wasm"));
        Module module = new Module(bytes);
        System.out.println("Creating import object");
        Imports imports = Imports.from(Arrays.asList(
                        new Imports.Spec("env", "ext_storage_set_version_1", argv -> {
                            System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                            return argv;
                        }, Arrays.asList(Type.I64, Type.I64), Collections.emptyList()),
                        new Imports.Spec("env", "ext_storage_get_version_1", argv -> {
                            System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                            return argv;
                        },
                                Collections.singletonList(Type.I64), Collections.singletonList(Type.I64))),
                new MemoryType("env", "memory", false, 45), module);
        System.out.println("Instantiating module");
        Instance instance = module.instantiate(imports);

        System.out.println("Calling exported function 'Core_initialize_block' as it calls both of the imported functions");
        instance.exports.getFunction("Core_initialize_block").apply(1,2);

        instance.close();
    }
}
