package org.wasmer;

import org.wasmer.exports.Export;

@SuppressWarnings("unused")
public class Global implements Export {
    private String value;
    //TODO: maybe find a way to use the Type enum for this type as well
    private String type;

    private Global() {
        // This object is instantiated by Rust.
    }

    public String getType() {
        return this.type;
    }

    public int getIntValue() {
        if (!type.equals("i32")) {
            throw new RuntimeException("Type mismatch, wanted type is i32 but global type is " + this.type);
        }
        return Integer.parseInt(this.value);
    }

    public long getLongValue() {
        if (!type.equals("i64")) {
            throw new RuntimeException("Type mismatch, wanted type is i64 but global type is " + this.type);
        }
        return Long.parseLong(this.value);
    }

    public float getFloatValue() {
        if (!type.equals("f32")) {
            throw new RuntimeException("Type mismatch, wanted type is f32 but global type is " + this.type);
        }
        return Float.parseFloat(this.value);
    }

    public double getDoubleValue() {
        if (!type.equals("f64")) {
            throw new RuntimeException("Type mismatch, wanted type is f64 but global type is " + this.type);
        }
        return Double.parseDouble(this.value);
    }
}
