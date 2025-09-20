package com.sandox.sandbox_service.service.cpp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sandox.sandbox_service.service.ContainerIO;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GdbDebugger extends TextWebSocketHandler {
    private Map<String, ContainerIO> executionContainerIOMap = new HashMap<>();
    private Map<String, ContainerIO> compileContainerIOMap = new HashMap<>();
    private Map<String, String> containerAssignment; // <userID,containerID>
    private Map<WebSocketSession, String> sessionMap = new ConcurrentHashMap<>(); // session to userID
    private Map<String, WebSocketSession> userToSession = new ConcurrentHashMap<>(); // userID to session
    private Map<String, String> userExpectedOutput = new ConcurrentHashMap<>(); // Track expected output type per user (e.g., "locals")

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
        String gdbCommand = parseGdbCommand(payload);
        System.out.println("[TROUBLESHOOT] GDB Command: " + gdbCommand);
        ContainerIO io = executionContainerIOMap.get(userId);
        if (io == null) {
            System.out.println("[TROUBLESHOOT] No ContainerIO for user " + userId + ". containerIOMap: " + executionContainerIOMap.keySet());
            session.sendMessage(new TextMessage("Error: No running GDB process for user."));
            return;
        }

        // Parse and handle the GDB command
        if (gdbCommand != null) {
            try {
                // Set expected output type based on command
                if (gdbCommand.equals("-stack-list-locals 1")) {
                    userExpectedOutput.put(userId, "locals");
                } else if (gdbCommand.startsWith("-break-insert")) {
                    userExpectedOutput.put(userId, "breakpoint");
                } else {
                    userExpectedOutput.remove(userId);
                }
                System.out.println("[TROUBLESHOOT] Expected output for user " + userId + ": " + userExpectedOutput.get(userId));

                io.getStdin().write(gdbCommand + "\n");
                io.getStdin().flush();
                System.out.println("[TROUBLESHOOT] GDB command sent for user " + userId + ": " + gdbCommand);
            } catch (IOException e) {
                System.out.println("[TROUBLESHOOT] Failed to send GDB command for user " + userId + ": " + e.getMessage());
                session.sendMessage(new TextMessage("Error: Failed to send GDB command: " + e.getMessage()));
            }
        } else {
            System.out.println("[TROUBLESHOOT] Invalid GDB command from user " + userId + ": " + payload);
            session.sendMessage(new TextMessage("Error:cpp Invalid GDB command: " + payload));
        }
    }

    // Helper: Parse and validate GDB command from client message
    private String parseGdbCommand(String payload) {
        payload = payload.trim();
        // Supported MI2 commands
        if (payload.equals("-exec-run") ||
                payload.equals("-exec-continue") ||
                payload.equals("-stack-list-locals 1")) {
            return payload;
        }
        // Breakpoint command: e.g., "-break-insert 9" or "-break-insert file:line"
        Pattern breakpointPattern = Pattern.compile("^-break-insert\\s+(\\S+)$");
        Matcher breakpointMatcher = breakpointPattern.matcher(payload);
        if (breakpointMatcher.matches()) {
            return payload; // Return as-is since it's already in MI2 format
        }
        // Return null for invalid commands
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Called when session is closed
        System.out.println("[TROUBLESHOOT] Session closed: " + session.getId());
        String userId = sessionMap.remove(session);
        if (userId != null) {
            userToSession.remove(userId);
            executionContainerIOMap.remove(userId);
            userExpectedOutput.remove(userId);
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

    public void compile(String userID, String className, String language,String Directory) throws IOException, InterruptedException {
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

                String compilationPath = Directory + "/" + className;
                System.out.println("[TROUBLESHOOT] compilationPath: " + compilationPath);
                // Start the execution process
                Process process = new ProcessBuilder(
                        "docker", "exec", "-i", containerName,
                        "g++","-g", compilationPath, "-o", "main").start();

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

    public void debug(String userID) throws IOException, InterruptedException {
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
                // First, make sure the file is executable
                new ProcessBuilder("docker", "exec", containerName, "chmod", "+x", "./main").start().waitFor();

                // Then run GDB in MI2 mode
                Process process = new ProcessBuilder("docker","exec","-i",containerName,"gdb","--interpreter=mi2","./main").start();

                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                ContainerIO io = new ContainerIO(stdout, stderr, stdin);
                System.out.println("UserID: " + userID);
                System.out.println("io"+io);
                executionContainerIOMap.put(userID, io);
                System.out.println("[TROUBLESHOOT] ContainerIO created and mapped for user " + userID);

                // Thread to stream stdout to WebSocket
                Thread outputThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stdout.readLine()) != null) {
                            if (session.isOpen()) {
                                Map<String, Object> gdbOutput = parseGdbOutput(userID, line);
                                if (!((List<?>) gdbOutput.get("events")).isEmpty()) { // Send only non-empty
                                    ObjectMapper mapper = new ObjectMapper();
                                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                                    String json = mapper.writeValueAsString(gdbOutput);
                                    session.sendMessage(new TextMessage(json));
                                    System.out.println("[TROUBLESHOOT] Sent parsed GDB output: " + json);
                                }
                                System.out.println("[TROUBLESHOOT] Raw stdout line: " + line);
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
                //executionContainerIOMap.remove(userID);
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

    private String unescape(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t");
        // Add more escapes if necessary
    }

    public Map<String, Object> parseGdbOutput(String userId, String output) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> events = new ArrayList<>();
        result.put("events", events);

        ObjectMapper mapper = new ObjectMapper();

        String line = output.trim();
        if (line.isEmpty()) {
            return result;
        }

        char first = line.charAt(0);
        if (first == '^' || first == '*' || first == '=') {
            int commaIdx = line.indexOf(',');
            String clazz;
            String resultsStr;
            if (commaIdx == -1) {
                clazz = line.substring(1);
                resultsStr = "";
            } else {
                clazz = line.substring(1, commaIdx);
                resultsStr = line.substring(commaIdx + 1);
            }

            Map<String, Object> parsedResults = new HashMap<>();
            if (!resultsStr.isEmpty()) {
                String jsonStr = "{" + resultsStr.replaceAll("([a-zA-Z0-9_-]+)=", "\"$1\":") + "}";
                try {
                    parsedResults = mapper.readValue(jsonStr, Map.class);
                } catch (Exception e) {
                    System.out.println("[TROUBLESHOOT] Failed to parse MI results: " + jsonStr + " Error: " + e.getMessage());
                    return result;
                }
            }

            if (first == '^' && "done".equals(clazz)) {
                if (parsedResults.containsKey("bkpt")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bkpt = (Map<String, Object>) parsedResults.get("bkpt");
                    Map<String, Object> currentEvent = new HashMap<>();
                    currentEvent.put("type", "breakpoint_set");
                    currentEvent.put("number", Integer.parseInt((String) bkpt.get("number")));
                    currentEvent.put("address", (String) bkpt.get("addr"));
                    currentEvent.put("file", (String) bkpt.get("file"));
                    currentEvent.put("line", Integer.parseInt((String) bkpt.get("line")));
                    events.add(currentEvent);
                } else if (parsedResults.containsKey("locals")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> localsList = (List<Map<String, String>>) parsedResults.get("locals");
                    Map<String, String> variables = new HashMap<>();
                    for (Map<String, String> var : localsList) {
                        variables.put(var.get("name"), var.get("value"));
                    }
                    Map<String, Object> currentEvent = new HashMap<>();
                    currentEvent.put("type", "locals");
                    currentEvent.put("variables", variables);
                    events.add(currentEvent);
                }
            } else if (first == '*' && "stopped".equals(clazz)) {
                String reason = (String) parsedResults.get("reason");
                if ("breakpoint-hit".equals(reason)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> frame = (Map<String, Object>) parsedResults.get("frame");
                    Map<String, Object> currentEvent = new HashMap<>();
                    currentEvent.put("type", "breakpoint_hit");
                    currentEvent.put("bkptno", Integer.parseInt((String) parsedResults.get("bkptno")));
                    currentEvent.put("file", (String) frame.get("file"));
                    currentEvent.put("line", Integer.parseInt((String) frame.get("line")));
                    events.add(currentEvent);
                }
            }
            // Add more parsing for other MI records as needed
        } else if (first == '~') {
            // Parse console output
            if (line.length() > 2) {
                String text = line.substring(2, line.length() - 1);
                text = unescape(text);
                Map<String, Object> currentEvent = new HashMap<>();
                currentEvent.put("type", "console");
                currentEvent.put("text", text);
                events.add(currentEvent);
            }
        } else if (first == '&') {
            // Parse log output
            if (line.length() > 2) {
                String text = line.substring(2, line.length() - 1);
                text = unescape(text);
                Map<String, Object> currentEvent = new HashMap<>();
                currentEvent.put("type", "log");
                currentEvent.put("text", text);
                events.add(currentEvent);
            }
        }

        return result;
    }
}