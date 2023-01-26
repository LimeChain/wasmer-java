import org.wasmer.Instance;
import org.wasmer.Memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

class MemoryExample {
    public static void main(String[] args) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("memory.wasm"));
        Instance instance = new Instance(bytes);
        Integer pointer = (Integer) instance.exports.getFunction("return_hello").apply()[0];

        Memory memory = instance.exports.getMemory("memory");

        ByteBuffer memoryBuffer = memory.buffer();

        byte[] data = new byte[13];
        memoryBuffer.position(pointer);
        memoryBuffer.get(data);

        String result = new String(data);

        assert result.equals("Hello, World!");
        System.out.println("here");

        instance.close();
    }
}
