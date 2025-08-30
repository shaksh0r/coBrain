package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.CodeExecution;
import com.sandox.sandbox_service.service.ContainerCreation;
import com.sandox.sandbox_service.service.ContainerManagement;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
public class ContainerController {
    private ContainerCreation containerCreation;
    private ContainerManagement containerManagement;
    private CodeExecution codeExecution;
    private String userID;

    public ContainerController(ContainerCreation containerCreation, ContainerManagement containerManagement,CodeExecution codeExecution) throws IOException, InterruptedException {
        this.containerCreation = containerCreation;
        this.containerCreation.setLanguage("cpp");
        this.containerCreation.buildContainers();
        this.containerCreation.runContainers();



        this.containerManagement = containerManagement;
        this.containerManagement.setContainerStatus(this.containerCreation.getContainerStatus());

        this.codeExecution = codeExecution;
        this.codeExecution.setContainerAssignment(this.containerManagement.getContainerAssignment());
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
        String directoryPath = request.get("directoryPath");
        String sourcePath =  request.get("sourcePath");
        String language =  request.get("language");

        copyContainer(userID,directoryPath);
        System.out.println("copy done");
        compile(userID,sourcePath,language);
        System.out.println("compilation done");
        execute(userID);
        System.out.println("execute done");
    }

    public void copyContainer(String userID,String directoryPath) throws IOException, InterruptedException {
        String containerName =  this.containerManagement.getContainerName(userID);
        System.out.println("Copying Container Name: " + containerName);
        //String source = "sandbox-service/src/main/java/com/sandox/sandbox_service/test.cpp";

        codeExecution.copyDirectory(directoryPath,containerName);
    }

    @GetMapping("/check")
    public void checkMaps() throws IOException, InterruptedException {
        codeExecution.check();
    }

    @GetMapping("/deallocate")
    public void deallocateContainer(){
        this.containerManagement.deallocateContainer(this.userID);
    }

    @GetMapping("/pause")
    public void pauseContainer() throws IOException, InterruptedException {
        this.containerManagement.pauseContainer(this.userID);
    }

    @GetMapping("/resume")
    public void resumeContainer() throws IOException, InterruptedException {
        this.containerManagement.resumeContainer(this.userID);
    }

    @GetMapping("/getID")
    public String getID(){
        return this.userID;
    }


    @GetMapping("/clean")
    public void cleanContainers() throws IOException, InterruptedException {
        containerCreation.cleanContainers();
    }

    public void compile(String userID,String sourcePath,String language) throws IOException, InterruptedException {
         this.codeExecution.compile(userID,sourcePath,language);
    };


    public String execute(String userID) throws IOException, InterruptedException {
        codeExecution.execute(userID);
        return "Execution started for user: " + userID;
    }

}
