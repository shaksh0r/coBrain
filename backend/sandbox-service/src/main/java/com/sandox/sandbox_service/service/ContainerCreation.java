package com.sandox.sandbox_service.service;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ContainerCreation {

    private int cppContainerCount,javaContainerCount;

    public ContainerCreation() {
        this.cppContainerCount = 3;
        this.javaContainerCount = 3;
    }

    public void runContainers() throws IOException, InterruptedException {
        String cppImageBase = "cpp_";
        String javaImageBase = "java_";

        for(int i = 0; i<this.cppContainerCount; i++){
            String containerName = cppImageBase + (i+1);
            new ProcessBuilder("docker","run","-d","--name",containerName,"cpp:latest","tail","-f","/dev/null").start();
        }

        for(int i = 0; i<this.javaContainerCount; i++){
            String containerName = javaImageBase + (i+1);
            new ProcessBuilder("docker","run","-d","--name",containerName,"java:latest","tail","-f","/dev/null").start();
        }
    }

    public void cleanContainers() throws IOException, InterruptedException {
        new ProcessBuilder("bash", "-c", "docker rm -f $(docker ps -aq)").start().waitFor();
    }

    public void buildContainers() throws IOException, InterruptedException {
        String cppImageName = "cpp:latest";
        String javaImageName = "java:latest";
        String dockerDirectory = "sandbox-service/src/main/java/com/sandox/sandbox_service/Dockerfiles";

        System.out.println("Here");

        new ProcessBuilder("docker","build","-t",cppImageName,dockerDirectory+"/cpp").start().waitFor();

        new ProcessBuilder("docker","build","-t",javaImageName,dockerDirectory+"/java").start().waitFor();
    }

}
