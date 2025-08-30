package com.sandox.sandbox_service.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public class ContainerIO {
    private BufferedReader stdout;
    private BufferedReader stderr;
    private BufferedWriter stdin;

    public ContainerIO(BufferedReader stdout, BufferedReader stderr, BufferedWriter stdin) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.stdin = stdin;
    }

    public BufferedReader getStdout() {
        return stdout;
    }
    public BufferedReader getStderr() {
        return stderr;
    }
    public BufferedWriter getStdin() {
        return stdin;
    }
}
