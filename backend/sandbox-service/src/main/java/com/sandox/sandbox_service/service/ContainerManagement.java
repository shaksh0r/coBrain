package com.sandox.sandbox_service.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContainerManagement {
    private Map<String, String> containerAssignment; // < userID, containerID >
    private Map<String, String> containerStatus; // < containerID, status >

    public ContainerManagement() {
        this.containerAssignment = new HashMap<String, String>();
    }

    public void setContainerStatus(Map<String,String> containerStatus) throws IOException, InterruptedException {
        this.containerStatus = containerStatus;
    }

    public Map<String,String> getContainerAssignment(){
        return this.containerAssignment;
    }

    public String getContainer(String userID){
        for(String containerName : containerStatus.keySet()){
            String containerState = containerStatus.get(containerName);

            if(containerState.equals("INACTIVE")){
                containerAssignment.put(userID, containerName);
                containerStatus.put(containerName,"ACTIVE");
                return containerName;
            }
        }

        return null;
    }

    public String getContainerName(String userID){
        return containerAssignment.get(userID);
    }


    public void deallocateContainer(String userID){
        for(String key : containerAssignment.keySet()){
            if(key.equals(userID)){
                String containerName = containerAssignment.get(key); // containerID
                containerAssignment.remove(key);

                containerStatus.put(containerName,"INACTIVE");
            }
        }

        System.out.println("No container allocated for user:"+userID);
    }


    public void pauseContainer(String userID) throws IOException, InterruptedException {
        for(String key : containerAssignment.keySet()){
            if(key.equals(userID)){
                String containerName = containerAssignment.get(key);
                containerStatus.put(containerName,"PAUSED");

                new ProcessBuilder("docker","stop",containerName).start().waitFor();
            }
        }
        System.out.println("No container for user:"+userID);
    }


    public void resumeContainer(String userID) throws IOException, InterruptedException {
        for(String key : containerAssignment.keySet()){
            if(key.equals(userID)){
                String containerName = containerAssignment.get(key);
                containerStatus.put(containerName,"RESUMED");
                new ProcessBuilder("docker","start",containerName).start().waitFor();
            }
        }
        System.out.println("No container for user:"+userID);
    }
}
