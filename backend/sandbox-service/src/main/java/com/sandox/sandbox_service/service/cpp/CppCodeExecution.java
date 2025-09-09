git package com.sandox.sandbox_service.service.cpp;

import com.sandox.sandbox_service.service.ContainerIO;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CppCodeExecution extends TextWebSocketHandler {

    private  Map<String, ContainerIO> executionContainerIOMap = new HashMap<>();
    private Map<String, ContainerIO> compileContainerIOMap = new HashMap<>();
    private Map<String, String> containerAssignment; // <userID,containerID>
    private  Map<WebSocketSession, String> sessionMap = new ConcurrentHashMap<>(); // session to userID
    private  Map<String, WebSocketSession> userToSession = new ConcurrentHashMap<>(); // userID to session

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("[TROUBLESHOOT] New session created: " + session.getId());
        System.out.println("[TROUBLESHOOT] Full URI: " + session.getUri()); // Log full URI to verify query params
        System.out.println("[TROUBLESHOOT] Query string: " + session.getUri().getQuery()); // Log query for debugging

        session.sendMessage(new TextMessage("Connected to server! Session ID: " + session.getId()));

        // Parse query parameters from URI to get userId (e.g., ws://localhost:8080/ws?userID=123)
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                System.out.println("[TROUBLESHOOT] Parsed param: " + param); // Log each param for verification
                if (keyValue.length == 2 && keyValue[0].equals("userId")) {
                    String userId = keyValue[1];
                    sessionMap.put(session, userId);
                    userToSession.put(userId, session);
                    System.out.println("[TROUBLESHOOT] User ID extracted and mapped: " + userId);
                    System.out.println("[TROUBLESHOOT] userToSession map after put: " + userToSession); // Log map state
                    System.out.println("[TROUBLESHOOT] Session ID for user: " + userToSession.get(userId));
                    session.sendMessage(new TextMessage("User ID registered: " + userId));
                }
            }
        } else {
            System.out.println("[TROUBLESHOOT] No query parameters found in URI.");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("[TROUBLESHOOT] Received message: " + payload);

        String userId = sessionMap.get(session);
        if (userId == null) {
            System.out.println("[TROUBLESHOOT] No user ID for session: " + session.getId() + ". sessionMap: " + sessionMap);
            session.sendMessage(new TextMessage("Error: No user ID associated with this session."));
            return;
        }

        if (payload.startsWith("input:")) {
            String input = payload.substring(6);
            ContainerIO io = executionContainerIOMap.get(userId);
            if (io != null) {
                io.getStdin().write(input + "\n");
                io.getStdin().flush();
                System.out.println("[TROUBLESHOOT] Input sent to program for user " + userId + ": " + input);
            } else {
                System.out.println("[TROUBLESHOOT] No ContainerIO for user " + userId + ". containerIOMap: " + executionContainerIOMap.keySet());
                session.sendMessage(new TextMessage("Error: No running program to accept input."));
            }
        } else {
            // Handle other messages or echo
            session.sendMessage(new TextMessage("Sending message: " + payload));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Called when session is closed
        System.out.println("[TROUBLESHOOT] Session closed: " + session.getId());
        String userId = sessionMap.remove(session);
        if (userId != null) {
            userToSession.remove(userId);
            executionContainerIOMap.remove(userId);
            System.out.println("[TROUBLESHOOT] Removed mappings for user " + userId + ". userToSession now: " + userToSession);
        } else {
            System.out.println("[TROUBLESHOOT] No user ID to remove for session " + session.getId() + ". sessionMap: " + sessionMap);
        }
    }

    public void copyDirectory(String source, String containerName) throws IOException, InterruptedException {
        String destinationDirectory = containerName + ":/code";
        new ProcessBuilder("docker", "cp", source, destinationDirectory).start().waitFor();
    }

    public void setContainerAssignment(Map<String, String> containerAssignment) {
        this.containerAssignment = containerAssignment;
    }

    public void compile(String userID, String className, String language) throws IOException, InterruptedException {

        System.out.println("[TROUBLESHOOT] Starting execute for userID: " + userID);
        System.out.println("[TROUBLESHOOT] Current userToSession map: " + userToSession);

        new Thread(() -> {
            try {
                WebSocketSession session = userToSession.get(userID);
                if (session == null) {
                    System.out.println("[TROUBLESHOOT] Session is null for userID: " + userID + ". Check if map was populated.");
                } else {
                    System.out.println("[TROUBLESHOOT] Session found for userID " + userID + ": " + session.getId() + ", isOpen: " + session.isOpen());
                }

                if (session == null || !session.isOpen()) {
                    System.out.println("[TROUBLESHOOT] No open session for user " + userID + ". userToSession keys: " + userToSession.keySet());
                    return;
                }

                String containerName = containerAssignment.get(userID);
                if (containerName == null) {
                    session.sendMessage(new TextMessage("Error: No container assigned for user " + userID));
                    System.out.println("[TROUBLESHOOT] No container for user " + userID + ". containerAssignment: " + containerAssignment);
                    return;
                }
                System.out.println("[TROUBLESHOOT] Container found for user " + userID + ": " + containerName);

                // Start the execution process
                Process process = new ProcessBuilder(
                        "docker", "exec", "-i", containerName,
                        "g++", className, "-o", "main").start();

                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                ContainerIO io = new ContainerIO(stdout, stderr, stdin);
                compileContainerIOMap.put(userID, io);
                System.out.println("[TROUBLESHOOT] ContainerIO created and mapped for user " + userID);

                // Thread to stream stdout to WebSocket
                Thread outputThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stdout.readLine()) != null) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(line));
                                System.out.println("[TROUBLESHOOT] Sent stdout line to session: " + line);
                            }
                        }
                    } catch (Exception e) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage("Error: Output stream error: " + e.getMessage()));
                            } catch (IOException ioEx) {
                                System.out.println("[TROUBLESHOOT] Failed to send output error: " + ioEx.getMessage());
                            }
                        }
                        e.printStackTrace();
                    }
                });

                // Thread to stream stderr to WebSocket
                Thread errorThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stderr.readLine()) != null) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage("Error: " + line));
                                System.out.println("[TROUBLESHOOT] Sent stderr line to session: " + line);
                            }
                        }
                        // [FIX] Check if process terminated abnormally after stderr ends
                        if (!process.isAlive() && process.exitValue() != 0) {
                            if (session.isOpen()) {
                                try {
                                    session.sendMessage(new TextMessage("Runtime Error: Process exited with code " + process.exitValue()));
                                    System.out.println("[TROUBLESHOOT] Sent exit code error: " + process.exitValue());
                                } catch (IOException e) {
                                    System.out.println("[TROUBLESHOOT] Failed to send exit code error: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage("Error: Stderr stream error: " + e.getMessage()));
                            } catch (IOException ioEx) {
                                System.out.println("[TROUBLESHOOT] Failed to send stderr error: " + ioEx.getMessage());
                            }
                        }
                        e.printStackTrace();
                    }
                });

                outputThread.start();
                errorThread.start();
                System.out.println("[TROUBLESHOOT] Output and error threads started for user " + userID);

                // Wait for process to finish
                int exitCode = process.waitFor();
                System.out.println("[TROUBLESHOOT] Process finished with exit code: " + exitCode);

                // [FIX] Check exit code and notify client of runtime error
                if (exitCode != 0 && session.isOpen()) {
                    session.sendMessage(new TextMessage("Runtime Error: Program terminated with exit code " + exitCode + " (possible segmentation fault)"));
                    System.out.println("[TROUBLESHOOT] Notified client of runtime error with exit code: " + exitCode);
                }

                // Join threads
                outputThread.join();
                errorThread.join();

                // Cleanup
                stdin.close();
                compileContainerIOMap.remove(userID);
                System.out.println("[TROUBLESHOOT] Cleaned up for user " + userID + ". containerIOMap now: " + compileContainerIOMap.keySet());

                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("Program finished."));
                }
            } catch (Exception e) {
                System.out.println("[TROUBLESHOOT] Exception in execute: " + e.getMessage());
                if (userToSession.get(userID) != null && userToSession.get(userID).isOpen()) {
                    try {
                        userToSession.get(userID).sendMessage(new TextMessage("Error: Execution failed: " + e.getMessage()));
                    } catch (IOException ioEx) {
                        System.out.println("[TROUBLESHOOT] Failed to send execution error: " + ioEx.getMessage());
                    }
                }
                e.printStackTrace();
            }
        }).start();



    }

    public void check(){
        System.out.println("[TROUBLESHOOT] sessionMap: " + sessionMap);
        System.out.println("[TROUBLESHOOT] userToSession: " + userToSession);
    }

    public void execute(String userID) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Starting execute for userID: " + userID);
        System.out.println("[TROUBLESHOOT] Current userToSession map: " + userToSession);

        new Thread(() -> {
            try {
                WebSocketSession session = userToSession.get(userID);
                if (session == null) {
                    System.out.println("[TROUBLESHOOT] Session is null for userID: " + userID + ". Check if map was populated.");
                } else {
                    System.out.println("[TROUBLESHOOT] Session found for userID " + userID + ": " + session.getId() + ", isOpen: " + session.isOpen());
                }

                if (session == null || !session.isOpen()) {
                    System.out.println("[TROUBLESHOOT] No open session for user " + userID + ". userToSession keys: " + userToSession.keySet());
                    return;
                }

                String containerName = containerAssignment.get(userID);
                if (containerName == null) {
                    session.sendMessage(new TextMessage("Error: No container assigned for user " + userID));
                    System.out.println("[TROUBLESHOOT] No container for user " + userID + ". containerAssignment: " + containerAssignment);
                    return;
                }
                System.out.println("[TROUBLESHOOT] Container found for user " + userID + ": " + containerName);

                // Start the execution process
                Process process = new ProcessBuilder("docker", "exec", "-i", containerName, "./main").start();

                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                ContainerIO io = new ContainerIO(stdout, stderr, stdin);
                executionContainerIOMap.put(userID, io);
                System.out.println("[TROUBLESHOOT] ContainerIO created and mapped for user " + userID);

                // Thread to stream stdout to WebSocket
                Thread outputThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stdout.readLine()) != null) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(line));
                                System.out.println("[TROUBLESHOOT] Sent stdout line to session: " + line);
                            }
                        }
                    } catch (Exception e) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage("Error: Output stream error: " + e.getMessage()));
                            } catch (IOException ioEx) {
                                System.out.println("[TROUBLESHOOT] Failed to send output error: " + ioEx.getMessage());
                            }
                        }
                        e.printStackTrace();
                    }
                });

                // Thread to stream stderr to WebSocket
                Thread errorThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stderr.readLine()) != null) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage("Error: " + line));
                                System.out.println("[TROUBLESHOOT] Sent stderr line to session: " + line);
                            }
                        }
                        // [FIX] Check if process terminated abnormally after stderr ends
                        if (!process.isAlive() && process.exitValue() != 0) {
                            if (session.isOpen()) {
                                try {
                                    session.sendMessage(new TextMessage("Runtime Error: Process exited with code " + process.exitValue()));
                                    System.out.println("[TROUBLESHOOT] Sent exit code error: " + process.exitValue());
                                } catch (IOException e) {
                                    System.out.println("[TROUBLESHOOT] Failed to send exit code error: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage("Error: Stderr stream error: " + e.getMessage()));
                            } catch (IOException ioEx) {
                                System.out.println("[TROUBLESHOOT] Failed to send stderr error: " + ioEx.getMessage());
                            }
                        }
                        e.printStackTrace();
                    }
                });

                outputThread.start();
                errorThread.start();
                System.out.println("[TROUBLESHOOT] Output and error threads started for user " + userID);

                // Wait for process to finish
                int exitCode = process.waitFor();
                System.out.println("[TROUBLESHOOT] Process finished with exit code: " + exitCode);

                // [FIX] Check exit code and notify client of runtime error
                if (exitCode != 0 && session.isOpen()) {
                    session.sendMessage(new TextMessage("Runtime Error: Program terminated with exit code " + exitCode + " (possible segmentation fault)"));
                    System.out.println("[TROUBLESHOOT] Notified client of runtime error with exit code: " + exitCode);
                }

                // Join threads
                outputThread.join();
                errorThread.join();

                // Cleanup
                stdin.close();
                executionContainerIOMap.remove(userID);
                System.out.println("[TROUBLESHOOT] Cleaned up for user " + userID + ". containerIOMap now: " + executionContainerIOMap.keySet());

                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("Program finished."));
                }
            } catch (Exception e) {
                System.out.println("[TROUBLESHOOT] Exception in execute: " + e.getMessage());
                if (userToSession.get(userID) != null && userToSession.get(userID).isOpen()) {
                    try {
                        userToSession.get(userID).sendMessage(new TextMessage("Error: Execution failed: " + e.getMessage()));
                    } catch (IOException ioEx) {
                        System.out.println("[TROUBLESHOOT] Failed to send execution error: " + ioEx.getMessage());
                    }
                }
                e.printStackTrace();
            }
        }).start();
    }

}