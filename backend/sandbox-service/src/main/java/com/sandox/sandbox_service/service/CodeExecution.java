package com.sandox.sandbox_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CodeExecution extends TextWebSocketHandler {

    private Map<String,ContainerIO> containerIOMap = new HashMap<>();
    private Map<String,String> containerAssignment;
    private Map<WebSocketSession,String> sessionMap = new HashMap<>();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New session created: " + session.getId());
        session.sendMessage(new TextMessage("Connected to server! Session ID: " + session.getId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message: " + payload);

        session.sendMessage(new TextMessage("Sending message: " + payload));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Called when session is closed
        System.out.println("Session closed: " + session.getId());
    }



    public void copyDirectory(String source, String containerName) throws IOException, InterruptedException {
        String destinationDirectory = containerName+":/code";
        new ProcessBuilder("docker","cp",source,destinationDirectory).start().waitFor();
    }

    public void setContainerAssignment(Map<String,String> containerAssignment) {
        this.containerAssignment = containerAssignment;
    }

    public String compile(String userID,String className, String language) throws IOException, InterruptedException {

        String containerName = containerAssignment.get(userID);

        Process process = new ProcessBuilder(
                "docker","exec","-i",containerName,
                "g++",className,"-o","main").start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));


        ContainerIO io = new ContainerIO(reader,errorReader,writer);
        containerIOMap.put(userID,io);

        String finalOutput = "";

        AtomicReference<String> output = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        Thread outputThread = new Thread(()->{
            try{
                String line;
                String total="";

                while((line = io.getStdout().readLine()) != null){
                    total+=line+"\n";
                }
                output.set(total);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        outputThread.start();
        outputThread.join();

        finalOutput += output.get();

        Thread errorThread = new Thread(()->{
            try{
                String line;
                String total="";

                while((line = io.getStderr().readLine()) != null){
                    total+=line+"\n";
                }
                error.set(total);
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        errorThread.start();
        errorThread.join();

        finalOutput += error.get();
        process.waitFor();

        return finalOutput;
    }



}
