package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.ContainerCreation;
import com.sandox.sandbox_service.service.cpp.CppContainerManagement;
import com.sandox.sandbox_service.service.cpp.CppCodeExecution;
import com.sandox.sandbox_service.service.cpp.GdbDebugger;
import com.sandox.sandbox_service.service.java.JavaContainerManagement;
import com.sandox.sandbox_service.service.java.JavaCodeExecution;
import com.sandox.sandbox_service.service.java.JdbDebugger;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
public class ContainerController {
    private final ContainerCreation containerCreation;
    private final CppContainerManagement cppContainerManagement;
    private final JavaContainerManagement javaContainerManagement;
    private final CppCodeExecution cppCodeExecution;
    private final JavaCodeExecution javaCodeExecution;
    private final GdbDebugger gdbDebugger;
    private final JdbDebugger jdbDebugger;
    // Note: JavaDebugger could be added here if Java debugging is implemented
    private static final String CPP_SOURCE_PATH_PREFIX = "cpp/";
    private static final String JAVA_SOURCE_PATH_PREFIX = "java/";

    public ContainerController(ContainerCreation containerCreation,
                               CppContainerManagement cppContainerManagement,
                               JavaContainerManagement javaContainerManagement,
                               CppCodeExecution cppCodeExecution,
                               JavaCodeExecution javaCodeExecution, GdbDebugger gdbDebugger,JdbDebugger jdbDebugger) throws IOException, InterruptedException {
        this.containerCreation = containerCreation;
        this.containerCreation.buildContainers();
        this.containerCreation.runContainers();

        this.cppContainerManagement = cppContainerManagement;
        this.javaContainerManagement = javaContainerManagement;
        this.cppContainerManagement.setContainerStatus(this.containerCreation.getContainerStatus("cpp"));
        this.javaContainerManagement.setContainerStatus(this.containerCreation.getContainerStatus("java"));

        this.cppCodeExecution = cppCodeExecution;
        this.javaCodeExecution = javaCodeExecution;
        this.cppCodeExecution.setContainerAssignment(this.cppContainerManagement.getContainerAssignment());
        this.javaCodeExecution.setContainerAssignment(this.javaContainerManagement.getContainerAssignment());

        this.gdbDebugger = gdbDebugger;
        this.gdbDebugger.setContainerAssignment(this.cppContainerManagement.getContainerAssignment());

        this.jdbDebugger = jdbDebugger;
        this.jdbDebugger.setContainerAssignment(this.javaContainerManagement.getContainerAssignment());
        // Note: If JavaDebugger is added, set its container assignment here
    }

    @GetMapping("/getContainer/{userID}/{language}")
    public String getContainer(@PathVariable String userID, @PathVariable String language) {
        System.out.println("[TROUBLESHOOT] UserID: " + userID + ", Language: " + language);
        String containerName;
        if ("cpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainer(userID);
        } else if ("java".equalsIgnoreCase(language)) {
            containerName = this.javaContainerManagement.getContainer(userID);
        } else {
            return "Error: Unsupported language: " + language;
        }
        return "Container Name: " + containerName;
    }

    @PostMapping("/run")
    public void run(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String language = request.get("language");
        String className = request.get("className");
        System.out.println("[TROUBLESHOOT] Running for userID: " + userID + ", Language: " + language);
        execute(userID,language,className);
        System.out.println("[TROUBLESHOOT] Execute done for userID: " + userID);
    }

    @PostMapping("/copy")
    public void copyContainer(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String directoryPath = request.get("directoryPath");
        String language = request.get("language");
        System.out.println("[TROUBLESHOOT] Copying for userID: " + userID + ", Language: " + language);

        String containerName;
        String adjustedPath;
        if ("cpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainerName(userID);
            adjustedPath =  directoryPath;
            this.cppCodeExecution.copyDirectory(adjustedPath, containerName);
        } else if ("java".equalsIgnoreCase(language)) {
            containerName = this.javaContainerManagement.getContainerName(userID);
            adjustedPath =  directoryPath;
            this.javaCodeExecution.copyDirectory(adjustedPath, containerName);
        } else if("debugCpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainerName(userID);
            adjustedPath =  directoryPath;
            this.gdbDebugger.copyDirectory(adjustedPath, containerName);
        }else if("debugJava".equalsIgnoreCase(language)) {
            containerName  = this.javaContainerManagement.getContainerName(userID);
            adjustedPath =  directoryPath;
            this.jdbDebugger.copyDirectory(adjustedPath, containerName);
        }
        else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        System.out.println("[TROUBLESHOOT] Copying Container Name: " + containerName + ", Path: " + adjustedPath);
    }

    @PostMapping("/compile")
    public void compile(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String sourcePath = request.get("sourcePath");
        String language = request.get("language");
        System.out.println("[TROUBLESHOOT] Compiling for userID: " + userID + ", Source Path: " + sourcePath + ", Language: " + language);

        String adjustedPath="";
        if ("cpp".equalsIgnoreCase(language)) {
            adjustedPath = sourcePath;
            this.cppCodeExecution.compile(userID, adjustedPath, language);
        } else if ("java".equalsIgnoreCase(language)) {
            adjustedPath =  sourcePath;
            this.javaCodeExecution.compile(userID, adjustedPath, language);
        } else if("debugCpp".equalsIgnoreCase(language)) {
            this.gdbDebugger.compile(userID,sourcePath,"cpp");
        }else if("debugJava".equalsIgnoreCase(language)) {
            this.jdbDebugger.compile(userID,sourcePath,"java");
        }
        else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        System.out.println("[TROUBLESHOOT] Compiling Source Path: " + adjustedPath);
    }

    public String execute(String userID, String language,String className) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Executing for userID: " + userID + ", Language: " + language);
        if ("cpp".equalsIgnoreCase(language)) {
            // For C++, use GdbDebugger for debugging
            this.cppCodeExecution.execute(userID);
        } else if ("java".equalsIgnoreCase(language)) {
            // For Java, use JavaCodeExecution for execution
            // Note: If Java debugging is implemented, a JavaDebugger could be used here
            this.javaCodeExecution.execute(userID,className);
        }else if("debugCpp".equalsIgnoreCase(language)) {
            this.gdbDebugger.debug(userID);
        }else if("debugJava".equalsIgnoreCase(language)) {
            this.jdbDebugger.debug(userID,className);
        }
        else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return "Execution started for user: " + userID + ", Language: " + language;
    }
}