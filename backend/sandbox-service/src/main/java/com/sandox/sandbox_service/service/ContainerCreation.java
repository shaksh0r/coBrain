package com.sandox.sandbox_service.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContainerCreation {

    private int containerCount;
    private String language;

    public ContainerCreation() {
        this.containerCount = 2;
    }

    public Map<String,String> getContainerStatus(String language){
        Map<String,String> containerStatus = new HashMap<String,String>();

        if(language.equals("cpp")){
            for(int i=0; i<containerCount; i++){
                String containerName = "cpp_"+(i+1);
                containerStatus.put(containerName, "INACTIVE");
            }
        }else if(language.equals("java")){
            for(int i=0; i<containerCount; i++){
                String containerName = "java_"+(i+1);
                containerStatus.put(containerName, "INACTIVE");
            }
        }

        return containerStatus;
    }

    public void runContainers() throws IOException, InterruptedException {
        String cppImageBase = "cpp_";
        String javaImageBase = "java_";

        for(int i = 0; i<this.containerCount; i++){
            String containerName = javaImageBase + (i+1);
            new ProcessBuilder("docker","run","-d","--name",containerName,"java:latest","tail","-f","/dev/null").start();
        }
        for(int i = 0; i<this.containerCount; i++){
            String containerName = cppImageBase + (i+1);
            new ProcessBuilder("docker","run","-d","--name",containerName,"cpp:latest","tail","-f","/dev/null").start();
        }
    }

    public void cleanContainers() throws IOException, InterruptedException {
        new ProcessBuilder("bash", "-c", "docker rm -f $(docker ps -aq)").start().waitFor();
    }

    public void buildContainers() throws IOException, InterruptedException {

        String cppImageName = "cpp:latest";
        String javaImageName = "java:latest";
        String dockerDirectory = "sandbox-service/src/main/java/com/sandox/sandbox_service/Dockerfiles";

        new ProcessBuilder("docker","build","-t",cppImageName,dockerDirectory+"/cpp").start().waitFor();

        new ProcessBuilder("docker","build","-t",javaImageName,dockerDirectory+"/java").start().waitFor();
    }

}
