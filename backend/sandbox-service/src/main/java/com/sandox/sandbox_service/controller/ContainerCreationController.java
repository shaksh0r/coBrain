package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.ContainerCreation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ContainerCreationController {
    private ContainerCreation containerCreation;

    public ContainerCreationController(ContainerCreation containerCreation) {
        this.containerCreation = containerCreation;
    }

    @GetMapping("/build")
    public String buildContainers() throws IOException, InterruptedException {
        containerCreation.buildContainers();
        return "Containers build!";
    }

    @GetMapping("/run")
    public void runContainers() throws IOException, InterruptedException {
        containerCreation.runContainers();
    }

    @GetMapping("/clean")
    public void cleanContainers() throws IOException, InterruptedException {
        containerCreation.cleanContainers();
    }

}
