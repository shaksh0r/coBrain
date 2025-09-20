package com.sandox.sandbox_service.controller;

import com.sandox.sandbox_service.service.ContainerCreation;
import com.sandox.sandbox_service.service.cpp.CppContainerManagement;
import com.sandox.sandbox_service.service.cpp.CppCodeExecution;
import com.sandox.sandbox_service.service.cpp.GdbDebugger;
import com.sandox.sandbox_service.service.java.JavaContainerManagement;
import com.sandox.sandbox_service.service.java.JavaCodeExecution;
import com.sandox.sandbox_service.service.java.JdbDebugger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*; // For temp dir and extraction
import java.util.Comparator;
import java.util.zip.*; // For ZipInputStream
import java.nio.file.StandardCopyOption;

@RestController
public class ContainerController {
    private final ContainerCreation containerCreation;
    private final CppContainerManagement cppContainerManagement;
    private final JavaContainerManagement javaContainerManagement;
    private final CppCodeExecution cppCodeExecution;
    private final JavaCodeExecution javaCodeExecution;
    private final GdbDebugger gdbDebugger;
    private final JdbDebugger jdbDebugger;
    // Note: JavaDebugger could be added here if Java debugging is implemented
    private static final String CPP_SOURCE_PATH_PREFIX = "cpp/";
    private static final String JAVA_SOURCE_PATH_PREFIX = "java/";

    public ContainerController(ContainerCreation containerCreation,
                               CppContainerManagement cppContainerManagement,
                               JavaContainerManagement javaContainerManagement,
                               CppCodeExecution cppCodeExecution,
                               JavaCodeExecution javaCodeExecution, GdbDebugger gdbDebugger,JdbDebugger jdbDebugger) throws IOException, InterruptedException {
        this.containerCreation = containerCreation;
        this.containerCreation.buildContainers();
        this.containerCreation.runContainers();

        this.cppContainerManagement = cppContainerManagement;
        this.javaContainerManagement = javaContainerManagement;
        this.cppContainerManagement.setContainerStatus(this.containerCreation.getContainerStatus("cpp"));
        this.javaContainerManagement.setContainerStatus(this.containerCreation.getContainerStatus("java"));

        this.cppCodeExecution = cppCodeExecution;
        this.javaCodeExecution = javaCodeExecution;
        this.cppCodeExecution.setContainerAssignment(this.cppContainerManagement.getContainerAssignment());
        this.javaCodeExecution.setContainerAssignment(this.javaContainerManagement.getContainerAssignment());

        this.gdbDebugger = gdbDebugger;
        this.gdbDebugger.setContainerAssignment(this.cppContainerManagement.getContainerAssignment());

        this.jdbDebugger = jdbDebugger;
        this.jdbDebugger.setContainerAssignment(this.javaContainerManagement.getContainerAssignment());
        // Note: If JavaDebugger is added, set its container assignment here
    }

    @GetMapping("/getContainer/{userID}/{language}")
    public ResponseEntity<?> getContainer(@PathVariable String userID, @PathVariable String language) {
        System.out.println("[TROUBLESHOOT] UserID: " + userID + ", Language: " + language);

        String containerName;
        if ("cpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainer(userID);
        } else if ("java".equalsIgnoreCase(language)) {
            containerName = this.javaContainerManagement.getContainer(userID);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported language: " + language));
        }

        return ResponseEntity.ok(Map.of(
                "userID", userID,
                "language", language,
                "containerName", containerName
        ));
    }


    @PostMapping("/run")
    public void run(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String language = request.get("language");
        String className = request.get("className");
        System.out.println("[TROUBLESHOOT] Running for userID: " + userID + ", Language: " + language);
        execute(userID,language,className);
        System.out.println("[TROUBLESHOOT] Execute done for userID: " + userID);
    }

    @PostMapping(value = "/copy")
    public void copyContainer(
            @RequestParam("userID") String userID,
            @RequestParam("language") String language,
            @RequestParam("directoryContent") MultipartFile zipFile  // The uploaded zip
    ) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Copying for userID: " + userID + ", Language: " + language + ", Zip size: " + zipFile.getSize());

        // Optional: Validate zip
        if (zipFile.isEmpty()) {
            throw new IllegalArgumentException("Zip file is empty");
        }
        if (!"application/zip".equals(zipFile.getContentType()) && !"application/x-zip-compressed".equals(zipFile.getContentType())) {
            throw new IllegalArgumentException("Invalid file type: expected ZIP");
        }

        String containerName;
        String adjustedPath;
        if ("cpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainerName(userID);
            adjustedPath = extractZipToTempDir(zipFile);
            // Copy contents of the temp directory, not the directory itself
            this.cppCodeExecution.copyDirectory(adjustedPath, containerName);
        } else if ("java".equalsIgnoreCase(language)) {
            containerName = this.javaContainerManagement.getContainerName(userID);
            adjustedPath = extractZipToTempDir(zipFile);
            this.javaCodeExecution.copyDirectory(adjustedPath, containerName);
        } else if ("debugCpp".equalsIgnoreCase(language)) {
            containerName = this.cppContainerManagement.getContainerName(userID);
            adjustedPath = extractZipToTempDir(zipFile);
            this.gdbDebugger.copyDirectory(adjustedPath, containerName);
        } else if ("debugJava".equalsIgnoreCase(language)) {
            containerName = this.javaContainerManagement.getContainerName(userID);
            adjustedPath = extractZipToTempDir(zipFile);
            this.jdbDebugger.copyDirectory(adjustedPath, containerName);
        } else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        // Cleanup temp dir after copy
        cleanupTempDir(adjustedPath);

        System.out.println("[TROUBLESHOOT] Copying Container Name: " + containerName + ", Source Path: " + adjustedPath);
    }

    private String extractZipToTempDir(MultipartFile zipFile) throws IOException {
        // Create unique temp dir per request (e.g., /tmp/code_abc123/)
        Path tempDir = Files.createTempDirectory("code_");
        System.out.println("[TROUBLESHOOT] Created temp dir: " + tempDir);

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Security: Normalize path to prevent traversal
                Path filePath = tempDir.resolve(entry.getName()).normalize();
                if (!filePath.startsWith(tempDir)) {
                    throw new IOException("Invalid zip entry: potential path traversal - " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    // Ensure parent dirs exist
                    Files.createDirectories(filePath.getParent());
                    // Copy entry to file, overwriting if needed
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            // Cleanup on error
            cleanupTempDir(tempDir.toString());
            throw e;
        }

        return tempDir.toString();  // Pass this path to copyDirectoryContents
    }

    private void cleanupTempDir(String tempDirPath) {
        try {
            Path tempDir = Paths.get(tempDirPath);
            if (Files.exists(tempDir)) {
                // Recursively delete files/dirs in reverse order (dirs last)
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("[TROUBLESHOOT] Cleaned up temp dir: " + tempDirPath);
            }
        } catch (IOException e) {
            System.out.println("[TROUBLESHOOT] Failed to cleanup temp dir " + tempDirPath + ": " + e.getMessage());
            // Log but don't throw—cleanup is best-effort
        }
    }

    // Copy directory contents to container using docker cp commands
    private void copyDirectoryContentsToContainer(String sourceDirPath, String containerName, String serviceType) throws IOException, InterruptedException {
        Path sourceDir = Paths.get(sourceDirPath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IOException("Source directory does not exist: " + sourceDirPath);
        }

        // Copy each file/subdirectory individually to the container's root working directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    // For subdirectories, you might want to copy recursively
                    copyDirectoryRecursively(entry.toString(), containerName, serviceType);
                } else {
                    // Copy individual file to container root
                    copySingleFileToContainer(entry.toString(), containerName, fileName, serviceType);
                }
            }
        }

        System.out.println("[TROUBLESHOOT] Successfully copied contents of " + sourceDirPath + " to container " + containerName);
    }

    private void copySingleFileToContainer(String filePath, String containerName, String fileName, String serviceType) throws IOException, InterruptedException {
        // Use docker cp command to copy individual file
        String[] command = {
                "docker", "cp",
                filePath,  // Source file path on host
                containerName + ":/" + fileName  // Destination in container root
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to copy file " + fileName + " to container " + containerName + ". Exit code: " + exitCode);
        }

        System.out.println("[TROUBLESHOOT] Copied file: " + fileName + " to container: " + containerName);
    }

    private void copyDirectoryRecursively(String dirPath, String containerName, String serviceType) throws IOException, InterruptedException {
        // This would handle subdirectories if needed
        // Implementation depends on your specific requirements
    }

    @PostMapping("/compile")
    public void compile(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String userID = request.get("userID");
        String sourcePath = request.get("sourcePath");
        String language = request.get("language");
        System.out.println("[TROUBLESHOOT] Compiling for userID: " + userID + ", Source Path: " + sourcePath + ", Language: " + language);

        String adjustedPath="";
        if ("cpp".equalsIgnoreCase(language)) {
            adjustedPath = sourcePath;
            this.cppCodeExecution.compile(userID, adjustedPath, language);
        } else if ("java".equalsIgnoreCase(language)) {
            adjustedPath =  sourcePath;
            this.javaCodeExecution.compile(userID, adjustedPath, language);
        } else if("debugCpp".equalsIgnoreCase(language)) {
            this.gdbDebugger.compile(userID,sourcePath,"cpp");
        }else if("debugJava".equalsIgnoreCase(language)) {
            this.jdbDebugger.compile(userID,sourcePath,"java");
        }
        else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        System.out.println("[TROUBLESHOOT] Compiling Source Path: " + adjustedPath);
    }

    public String execute(String userID, String language,String className) throws IOException, InterruptedException {
        System.out.println("[TROUBLESHOOT] Executing for userID: " + userID + ", Language: " + language);
        if ("cpp".equalsIgnoreCase(language)) {
            // For C++, use GdbDebugger for debugging
            this.cppCodeExecution.execute(userID);
        } else if ("java".equalsIgnoreCase(language)) {
            // For Java, use JavaCodeExecution for execution
            // Note: If Java debugging is implemented, a JavaDebugger could be used here
            this.javaCodeExecution.execute(userID,className);
        }else if("debugCpp".equalsIgnoreCase(language)) {
            this.gdbDebugger.debug(userID);
        }else if("debugJava".equalsIgnoreCase(language)) {
            this.jdbDebugger.debug(userID,className);
        }
        else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return "Execution started for user: " + userID + ", Language: " + language;
    }
}