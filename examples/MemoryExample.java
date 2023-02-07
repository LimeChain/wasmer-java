import org.wasmer.Instance;
import org.wasmer.Memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

class MemoryExample {
    public static void main(String[] args) throws IOException {
        System.out.println("\nTEST ACCESSING AND MODIFYING WASM MEMORY");
        System.out.println("Reading wasm bytes");
        Instance instance = new Instance(Files.readAllBytes(Paths.get("hello_world.wasm")));

        System.out.println("Getting memory export");
        Memory memory = instance.exports.getMemory("memory");
        ByteBuffer memoryBuffer = memory.buffer();

        System.out.println("Calling exported function 'string' which returns a pointer");
        int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
        byte[] data = new byte[13];
        memoryBuffer.position(pointer);
        memoryBuffer.get(data);

        System.out.println("Current value of data starting from the `string` pointer: " + new String(data));

        memoryBuffer.position(pointer);
        System.out.println("Changing first byte at pointer address");
        memoryBuffer.put(new byte[]{'A'});

        memory = instance.exports.getMemory("memory");
        memoryBuffer = memory.buffer();

        System.out.println("Calling the exported 'string' function a second time");
        pointer = (Integer) instance.exports.getFunction("string").apply()[0];
        data = new byte[13];
        memoryBuffer.position(pointer);
        memoryBuffer.get(data);

        System.out.println("Current value of data starting from the `string` pointer: " + new String(data));

        instance.close();
    }
}
