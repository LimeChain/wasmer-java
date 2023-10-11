package org.wasmer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class Main {
    public static void main(String[] args) throws IOException {
        Native.nativePanic("panicccccc");
        System.out.println("Reading wasm bytes");
        byte[] bytes = Files.readAllBytes(Paths.get("../../examples/runtime.wasm"));
        Module module = new Module(bytes);
        System.out.println("Creating import object");
        Imports imports = Imports.from(Arrays.asList(
                new ImportObject.FuncImport("env", "ext_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Collections.emptyList()),
                new ImportObject.FuncImport("env", "ext_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                    return argv;
                }, Collections.singletonList(Type.I64), Collections.singletonList(Type.I64)),
                new ImportObject.MemoryImport("env", 20, false)), module);
        System.out.println("Instantiating module");
        Instance instance = module.instantiate(imports);

        System.out.println("Calling exported function 'Core_initialize_block' as it calls both of the imported functions");
        instance.exports.getFunction("Core_initialize_block").apply(1,2);

        Global heapBase = instance.exports.getGlobal("__heap_base");
        System.out.println(heapBase.getIntValue());

        Memory memory = instance.exports.getMemory("memory");
        System.out.println(memory.buffer());
        instance.close();
    }
}
