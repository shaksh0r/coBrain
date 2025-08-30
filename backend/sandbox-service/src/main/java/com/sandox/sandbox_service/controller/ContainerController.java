package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.CodeExecution;
import com.sandox.sandbox_service.service.ContainerCreation;
import com.sandox.sandbox_service.service.ContainerManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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

    @GetMapping("/copy/{userID}")
    public void copyContainer(@PathVariable String userID) throws IOException, InterruptedException {
        String containerName =  this.containerManagement.getContainerName(userID);
        System.out.println("Copying Container Name: " + containerName);
        String source = "sandbox-service/src/main/java/com/sandox/sandbox_service/test.cpp";

        codeExecution.copyDirectory(source,containerName);
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

    @GetMapping("/compile/{userID}")
    public String compile(@PathVariable String userID) throws IOException, InterruptedException {
        return this.codeExecution.compile(userID,"test.cpp","cpp");
    };

}
