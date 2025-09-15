package com.sandox.sandbox_service.service.java;

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
public class JdbDebugger extends TextWebSocketHandler {
    private Map<String, ContainerIO> executionContainerIOMap = new HashMap<>();
    private Map<String, ContainerIO> compileContainerIOMap = new HashMap<>();
    private Map<String, String> containerAssignment;
    // private Map<WebSocketSession, String> sessionMap = new ConcurrentHashMap<>(); // session to userID
    private Map<WebSocketSession, String> sessionMap = new ConcurrentHashMap<>();
    private Map<String, WebSocketSession> userToSession = new ConcurrentHashMap<>(); // userID to session
    private Map<String, String> userExpectedOutput = new ConcurrentHashMap<>(); // Track expected output type per user (e.g., "locals")

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("[TROUBLESHOOT] New session created: " + session.getId());
        System.out.println("[TROUBLESHOOT] Full URI: " + session.getUri());
        // Log full URI to verify query params
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
        String jdbCommand = parseJdbCommand(payload);
        System.out.println("[TROUBLESHOOT] JDB Command: " + jdbCommand);
        ContainerIO io = executionContainerIOMap.get(userId);
        if (io == null) {
            System.out.println("[TROUBLESHOOT] No ContainerIO for user " + userId + ". containerIOMap: " + executionContainerIOMap.keySet());
            session.sendMessage(new TextMessage("Error: No running JDB process for user."));
            return;
        }
        // Parse and handle the JDB command
        if (jdbCommand != null) {
            try {
                // Set expected output type based on command
                if (jdbCommand.equals("locals")) {
                    userExpectedOutput.put(userId, "locals");
                } else if (jdbCommand.startsWith("stop at")) {
                    userExpectedOutput.put(userId, "breakpoint");
                } else {
                    userExpectedOutput.remove(userId);
                }
                System.out.println("[TROUBLESHOOT] Expected output for user " + userId + ": " + userExpectedOutput.get(userId));
                io.getStdin().write(jdbCommand + "\n");
                io.getStdin().flush();
                System.out.println("[TROUBLESHOOT] JDB command sent for user " + userId + ": " + jdbCommand);
            } catch (IOException e) {
                System.out.println("[TROUBLESHOOT] Failed to send JDB command for user " + userId + ": " + e.getMessage());
                session.sendMessage(new TextMessage("Error: Failed to send JDB command: " + e.getMessage()));
            }
        } else {
            System.out.println("[TROUBLESHOOT] Invalid JDB command from user " + userId + ": " + payload);
            session.sendMessage(new TextMessage("Error: Invalid JDB command: " + payload));
        }
    }

    // Helper: Parse and validate JDB command from client message
    private String parseJdbCommand(String payload) {
        payload = payload.trim();
        // Supported JDB commands
        if (payload.equals("run") || payload.equals("cont") || payload.equals("locals")) {
            return payload;
        }
        // Breakpoint command: e.g., "stop at test:4"
        Pattern breakpointPattern = Pattern.compile("^stop at\\s+(\\S+):(\\d+)$");
        Matcher breakpointMatcher = breakpointPattern.matcher(payload);
        if (breakpointMatcher.matches()) {
            return payload; // Return as-is
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

    public void compile(String userID, String className, String language) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Starting compile for userID: " + userID);
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
                // Start the compilation process
                Process process = new ProcessBuilder(
                        "docker", "exec", "-i", containerName, "javac", "-g", className + ".java").start();
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
                        // Check if process terminated abnormally after stderr ends
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
                // Check exit code and notify client of runtime error
                if (exitCode != 0 && session.isOpen()) {
                    session.sendMessage(new TextMessage("Compilation Error: Failed with exit code " + exitCode));
                    System.out.println("[TROUBLESHOOT] Notified client of compilation error with exit code: " + exitCode);
                }
                // Join threads
                outputThread.join();
                errorThread.join();
                // Cleanup
                stdin.close();
                compileContainerIOMap.remove(userID);
                System.out.println("[TROUBLESHOOT] Cleaned up for user " + userID + ". compileContainerIOMap now: " + compileContainerIOMap.keySet());
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("Compilation finished."));
                }
            } catch (Exception e) {
                System.out.println("[TROUBLESHOOT] Exception in compile: " + e.getMessage());
                if (userToSession.get(userID) != null && userToSession.get(userID).isOpen()) {
                    try {
                        userToSession.get(userID).sendMessage(new TextMessage("Error: Compilation failed: " + e.getMessage()));
                    } catch (IOException ioEx) {
                        System.out.println("[TROUBLESHOOT] Failed to send compilation error: " + ioEx.getMessage());
                    }
                }
                e.printStackTrace();
            }
        }).start();
    }

    public void debug(String userID, String className) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Starting debug for userID: " + userID);
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
                // Start the JDB process
                Process process = new ProcessBuilder("docker", "exec", "-i", containerName, "jdb", className).start();
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
                                Map<String, Object> jdbOutput = parseJdbOutput(userID, line);
                                if (!((List<Map<String, Object>>) jdbOutput.get("events")).isEmpty()) {
                                    // Send only non-empty
                                    ObjectMapper mapper = new ObjectMapper();
                                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                                    String json = mapper.writeValueAsString(jdbOutput);
                                    session.sendMessage(new TextMessage(json));
                                    System.out.println("[TROUBLESHOOT] Sent parsed JDB output: " + json);
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
                        // Check if process terminated abnormally after stderr ends
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
                // Check exit code and notify client of runtime error
                if (exitCode != 0 && session.isOpen()) {
                    session.sendMessage(new TextMessage("Runtime Error: Program terminated with exit code " + exitCode));
                    System.out.println("[TROUBLESHOOT] Notified client of runtime error with exit code: " + exitCode);
                }
                // Join threads
                outputThread.join();
                errorThread.join();
                // Cleanup
                stdin.close();
                executionContainerIOMap.remove(userID);
                System.out.println("[TROUBLESHOOT] Cleaned up for user " + userID + ". executionContainerIOMap now: " + executionContainerIOMap.keySet());
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("Program finished."));
                }
            } catch (Exception e) {
                System.out.println("[TROUBLESHOOT] Exception in debug: " + e.getMessage());
                if (userToSession.get(userID) != null && userToSession.get(userID).isOpen()) {
                    try {
                        userToSession.get(userID).sendMessage(new TextMessage("Error: Debug failed: " + e.getMessage()));
                    } catch (IOException ioEx) {
                        System.out.println("[TROUBLESHOOT] Failed to send debug error: " + ioEx.getMessage());
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
                .replace("\\t", "\t"); // Add more escapes if necessary
    }

    public Map<String, Object> parseJdbOutput(String userId, String output) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> events = new ArrayList<>();
        result.put("events", events);
        ObjectMapper mapper = new ObjectMapper();
        String line = output.trim();
        if (line.isEmpty()) {
            return result;
        }
        String expected = userExpectedOutput.get(userId);
        // Parse locals and method args if expected
        if ("locals".equals(expected)) {
            // Parse method arguments
            if (line.contains("Method arguments:")) {
                String after = line.substring(line.indexOf(":") + 1).trim();
                Pattern argPattern = Pattern.compile("(\\w+)\\s*=\\s*(.+?)(?=\\s+(?:\\w+=)|$)", Pattern.DOTALL);
                Matcher ma = argPattern.matcher(after);
                while (ma.find()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("type", "method_argument");
                    event.put("name", ma.group(1));
                    event.put("value", unescape(ma.group(2).trim()));
                    events.add(event);
                }
                return result;
            }
            // Parse local variables
            if (line.contains("Local variables:")) {
                String after = line.substring(line.indexOf(":") + 1).trim();
                Pattern varPattern = Pattern.compile("(\\w+)\\s*=\\s*(.+?)(?=\\s+(?:\\w+=)|$)", Pattern.DOTALL);
                Matcher mv = varPattern.matcher(after);
                while (mv.find()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("type", "local_variable");
                    event.put("name", mv.group(1));
                    event.put("value", unescape(mv.group(2).trim()));
                    events.add(event);
                }
                return result;
            }
        }
        // Parse breakpoint set deferred (now using find() to match anywhere in line)
        Pattern breakpointDeferredPattern = Pattern.compile("Deferring breakpoint (\\S+)\\.");
        Matcher breakpointDeferredMatcher = breakpointDeferredPattern.matcher(line);
        if (breakpointDeferredMatcher.find()) {
            String breakpoint = breakpointDeferredMatcher.group(1);
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "breakpoint_set_deferred");
            currentEvent.put("breakpoint", breakpoint);
            events.add(currentEvent);
            return result;
        }
        // Parse VM Started and deferred breakpoints
        if (line.startsWith("VM Started:")) {
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "vm_started");
            events.add(currentEvent);
            // Also parse any "Set deferred breakpoint" in the same line
            Pattern setDeferredPattern = Pattern.compile("Set deferred breakpoint (\\S+)");
            Matcher setDeferredMatcher = setDeferredPattern.matcher(line);
            while (setDeferredMatcher.find()) {
                Map<String, Object> deEvent = new HashMap<>();
                deEvent.put("type", "breakpoint_set");
                deEvent.put("breakpoint", setDeferredMatcher.group(1));
                events.add(deEvent);
            }
            return result;
        }
        // Parse breakpoint hit (extended to capture bci)
        Pattern breakpointHitPattern = Pattern.compile("^Breakpoint hit: \"([^\"]+)\", (\\S+)\\.(\\S+)\\(\\), line=(\\d+) bci=(\\d+)");
        Matcher breakpointHitMatcher = breakpointHitPattern.matcher(line);
        if (breakpointHitMatcher.matches()) {
            String thread = breakpointHitMatcher.group(1);
            String className = breakpointHitMatcher.group(2);
            String method = breakpointHitMatcher.group(3);
            int lineNum = Integer.parseInt(breakpointHitMatcher.group(4));
            int bci = Integer.parseInt(breakpointHitMatcher.group(5));
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "breakpoint_hit");
            currentEvent.put("thread", thread);
            currentEvent.put("class", className);
            currentEvent.put("method", method);
            currentEvent.put("line", lineNum);
            currentEvent.put("bci", bci);
            events.add(currentEvent);
            return result;
        }
        // Parse result or exit
        if (line.startsWith("Result:")) {
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "execution_result");
            currentEvent.put("result", line.substring(7).trim());
            events.add(currentEvent);
            return result;
        } else if (line.contains("The application exited")) {
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "execution_result");
            currentEvent.put("status", "exited");
            events.add(currentEvent);
            return result;
        }
        // Parse prompts (lines starting with "main[")
        if (line.startsWith("main[")) {
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "prompt");
            currentEvent.put("text", line);
            events.add(currentEvent);
            return result;
        }
        // General console output (skip lines with bci= or main[ to avoid duplicates)
        if (!line.contains("bci=") && !line.startsWith("main[") && !line.isEmpty()) {
            Map<String, Object> currentEvent = new HashMap<>();
            currentEvent.put("type", "console");
            currentEvent.put("text", unescape(line));
            events.add(currentEvent);
        }
        return result;
    }
}