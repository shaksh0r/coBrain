package com.therap.coBrain;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DependencyController {

    private final DependencyProcessor dependencyProcessor;

    public DependencyController(DependencyProcessor dependencyProcessor) {
        this.dependencyProcessor = dependencyProcessor;
    }

    @PostMapping("/dependencies")
    public List<String> getDependencies(@RequestBody Map<String, Object> requestBody) {
        Object filesArr = requestBody.get("files");
        List<String> outputMessages = new java.util.ArrayList<>();
        if (filesArr instanceof List) {
            List<?> files = (List<?>) filesArr;
            for (Object obj : files) {
                if (obj instanceof Map) {
                    Map<String, Object> file = (Map<String, Object>) obj;
                    String name = (String) file.get("name");
                    String content = (String) file.get("content");
                    outputMessages.add(dependencyProcessor.processFile(name, content));
                }
            }
        }
        return outputMessages;
    }
}
