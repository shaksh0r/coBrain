
package com.therap.coBrain;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DependencyProcessor {

    private String getTagValue(Element element, String tag) {
        NodeList nl = element.getElementsByTagName(tag);
        if (nl.getLength() > 0 && nl.item(0).getFirstChild() != null) {
            return nl.item(0).getFirstChild().getNodeValue();
        }
        return "";
    }

    public String processMavenDependencies(String content) {
        StringBuilder result = new StringBuilder();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            NodeList dependencies = doc.getElementsByTagName("dependency");
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                String groupId = getTagValue(dep, "groupId");
                String artifactId = getTagValue(dep, "artifactId");
                String version = getTagValue(dep, "version");
                String apiUrl = String.format(
                    "https://search.maven.org/solrsearch/select?q=g:\"%s\"+AND+a:\"%s\"+AND+v:\"%s\"&rows=1&wt=json",
                    groupId, artifactId, version
                );
                boolean exists = false;
                boolean deprecated = false;
                try {
                    String response = restTemplate.getForObject(apiUrl, String.class);
                    JsonNode root = objectMapper.readTree(response);
                    int numFound = root.path("response").path("numFound").asInt();
                    exists = numFound > 0;
                    // For demo, mark as deprecated if version is not the latest
                    if (exists) {
                        JsonNode docs = root.path("response").path("docs");
                        if (docs.isArray() && docs.size() > 0) {
                            String latestVersion = docs.get(0).path("latestVersion").asText("");
                            if (!version.equals(latestVersion)) {
                                deprecated = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    result.append(String.format("Error checking %s:%s:%s: %s\n", groupId, artifactId, version, e.getMessage()));
                }
                result.append(String.format("Dependency: %s:%s:%s\nExists: %s\nDeprecated: %s\n\n",
                    groupId, artifactId, version, exists ? "Yes" : "No", deprecated ? "Yes" : "No"));
            }
        } catch (Exception e) {
            return "Error processing pom.xml: " + e.getMessage();
        }
        return result.toString();
    }

    public String processJavaFile(String content){
        // Simple parser: look for @Deprecated annotations and usage of known deprecated methods
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean inDeprecatedMethod = false;
        String currentMethod = null;
        Set<String> deprecatedUserMethods = new HashSet<>();
        // Step 1: Find user-defined deprecated methods
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("@Deprecated")) {
                inDeprecatedMethod = true;
                continue;
            }
            if (inDeprecatedMethod && (line.startsWith("public") || line.startsWith("private") || line.startsWith("protected"))) {
                // Try to extract method name
                String[] parts = line.split("\\s+|\\(");
                if (parts.length > 2) {
                    currentMethod = parts[2];
                    deprecatedUserMethods.add(currentMethod);
                    result.append("User-defined deprecated method: ").append(currentMethod).append("\n");
                }
                inDeprecatedMethod = false;
            }
        }

        // Step 2: Find imported libraries
        Set<String> importedClasses = new HashSet<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("import ")) {
                String imp = line.replace("import","").replace(";","").trim();
                importedClasses.add(imp);
            }
        }

        // Step 3: Find method calls and check for deprecated usage
        // For demo, use a known list of deprecated library methods
        String[] knownDeprecated = {"Thread.stop", "Date.getYear"};
        for (String dep : knownDeprecated) {
            for (String line : lines) {
                if (line.contains(dep)) {
                    result.append("Usage of deprecated library function: ").append(dep).append("\n");
                }
            }
        }

        // Step 4: Find calls to user-defined deprecated methods
        for (String userMethod : deprecatedUserMethods) {
            for (String line : lines) {
                if (line.contains(userMethod + "(")) {
                    result.append("Call to user-defined deprecated method: ").append(userMethod).append("\n");
                }
            }
        }

        if (result.length() == 0) {
            return "No deprecated functions found.";
        }
        return result.toString();
    }

    public String processFile(String name, String content){
        if (name.equals("pom.xml")){
            return processMavenDependencies(content);
        }
        else if (name.toLowerCase().endsWith(".java")){
            return processJavaFile(content);
        }
        else {
            return "Unsupported file type";
        }
    }
}
