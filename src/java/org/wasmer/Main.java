package org.wasmer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        //Purely for demonstration purposes, delete all of it when writing actual code
        byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "../../examples/runtime.wasm"));
        Module module = new Module(bytes);
        System.out.println("Success");
    }
}
