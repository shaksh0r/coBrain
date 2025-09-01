package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.CodeExecution;
import com.sandox.sandbox_service.service.ContainerCreation;
import com.sandox.sandbox_service.service.ContainerManagement;
import com.sandox.sandbox_service.service.GdbDebugger;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
public class ContainerController {
    private ContainerCreation containerCreation;
    private ContainerManagement containerManagement;
    private CodeExecution codeExecution;
    private GdbDebugger gdbDebugger;
    private String userID;

    public ContainerController(ContainerCreation containerCreation, ContainerManagement containerManagement,CodeExecution codeExecution,GdbDebugger gdbDebugger) throws IOException, InterruptedException {
        this.containerCreation = containerCreation;
        this.containerCreation.setLanguage("cpp");
        this.containerCreation.buildContainers();
        this.containerCreation.runContainers();



        this.containerManagement = containerManagement;
        this.containerManagement.setContainerStatus(this.containerCreation.getContainerStatus());

        this.codeExecution = codeExecution;
        this.codeExecution.setContainerAssignment(this.containerManagement.getContainerAssignment());

        this.gdbDebugger = gdbDebugger;
        this.gdbDebugger.setContainerAssignment(this.containerManagement.getContainerAssignment());

    }

    @GetMapping("/getContainer/{userID}")
    public String getContainer(@PathVariable String userID){
        System.out.println("UserID: " + userID);
        String containerName =  this.containerManagement.getContainer(userID);
        return "Container Name: " + containerName;
    }

    @PostMapping("/run")
    public void RUN(@RequestBody Map<String,String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");

        execute(userID);
        System.out.println("execute done");
    }

    @PostMapping("/copy")
    public void copyContainer(@RequestBody Map<String,String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String directoryPath = request.get("directoryPath");
        String containerName =  this.containerManagement.getContainerName(userID);
        System.out.println("Copying Container Name: " + containerName);
        //String source = "sandbox-service/src/main/java/com/sandox/sandbox_service/test.cpp";

        gdbDebugger.copyDirectory(directoryPath,containerName);
    }


    @PostMapping("/compile")
    public void compile(@RequestBody Map<String,String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String sourcePath = request.get("sourcePath");
        String language = request.get("language");
        System.out.println("Compiling Source Path: " + sourcePath);
        this.gdbDebugger.compile(userID,sourcePath,language);
    };


    public String execute(String userID) throws IOException, InterruptedException {
        gdbDebugger.debug(userID);
        return "Execution started for user: " + userID;
    }

}
