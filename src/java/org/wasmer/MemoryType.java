package org.wasmer;

public class MemoryType {
    private String namespace;
    private String name;
    private boolean shared;
    private int minPages;

    public MemoryType(String namespace, String name, boolean shared, int minPages) {
        this.namespace = namespace;
        this.name = name;
        this.shared = shared;
        this.minPages = minPages;
    }
}
