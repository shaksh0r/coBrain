package com.sandox.sandbox_service;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PomParser {
    public List<String> getClasspath(String pomPath) {
        try {
            // Parse pom.xml
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomPath));

            // Initialize Maven Resolver
            RepositorySystem repoSystem = newRepositorySystem();
            RepositorySystemSession session = newRepositorySystemSession(repoSystem);

            // Create a list of dependencies from the POM model
            List<Dependency> dependencies = model.getDependencies().stream()
                    .filter(dep -> dep.getVersion() != null && !dep.getVersion().isEmpty()) // Skip dependencies without explicit versions
                    .map(dep -> new Dependency(
                            new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), "jar", dep.getVersion()),
                            dep.getScope()
                    ))
                    .collect(Collectors.toList());

            // Add some common dependencies with explicit versions for testing
            if (dependencies.isEmpty()) {
                dependencies.add(new Dependency(
                        new DefaultArtifact("org.apache.commons", "commons-lang3", "jar", "3.12.0"),
                        "compile"
                ));
            }

            // Build a collect request to resolve dependencies
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setDependencies(dependencies);
            collectRequest.addRepository(new RemoteRepository.Builder(
                    "central", "default", "https://repo.maven.apache.org/maven2").build());

            // Resolve dependencies
            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
            DependencyResult dependencyResult = repoSystem.resolveDependencies(session, dependencyRequest);

            // Collect JAR file paths
            List<String> resolvedJars = dependencyResult.getArtifactResults().stream()
                    .map(artifactResult -> artifactResult.getArtifact().getFile().getAbsolutePath())
                    .filter(path -> path.endsWith(".jar"))
                    .collect(Collectors.toList());

            return resolvedJars;
        } catch (Exception e) {
            // Handle errors (e.g., invalid POM, missing files, resolution failures)
            System.err.println("Error resolving dependencies: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private RepositorySystem newRepositorySystem() {
        return new RepositorySystemSupplier().get();
    }

    private RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        String localRepoPath = System.getProperty("user.home") + "/.m2/repository";
        LocalRepository localRepo = new LocalRepository(localRepoPath);
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }
}