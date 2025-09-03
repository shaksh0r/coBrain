package com.sandox.sandbox_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DeprecationService {
    private final DeprecationChecker checker = new DeprecationChecker();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String checkDeprecations(String sessionId, String projectDir, List<String> classpathJars) throws IOException {
        List<DeprecationChecker.Warning> allWarnings = new ArrayList<>();
        Path projectPath = Paths.get("/home/shakshor/coBrain/coBrain/backend/code-snippet");

        // Recursively find .java files
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        String relativePath = null;
                        try {
                            // Read file content
                            String code = Files.readString(path);
                            // Convert absolute path to relative path (e.g., src/Main.java)
                            relativePath = projectPath.relativize(path).toString();
                            // Call DeprecationChecker
                            List<DeprecationChecker.Warning> warnings = checker.check(code, relativePath, classpathJars);
                            allWarnings.addAll(warnings);
                        } catch (IOException e) {
                            allWarnings.add(new DeprecationChecker.Warning(relativePath, -1, "Failed to read file: " + e.getMessage()));
                        }
                    });
        } catch (IOException e) {
            allWarnings.add(new DeprecationChecker.Warning("", -1, "Failed to traverse directory: " + e.getMessage()));
        }


        return objectMapper.writeValueAsString(allWarnings);

    }
}
