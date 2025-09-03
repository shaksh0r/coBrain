package com.sandox.sandbox_service;

public class Test2 {
    public static void main(String[] args) {
        Thread t = new Thread();
        t.stop(); // Deprecated in JDK
    }

    @Deprecated
    public void myDeprecatedMethod() {}

    public void callDeprecated() {
        myDeprecatedMethod(); // Local deprecated method
    }
}