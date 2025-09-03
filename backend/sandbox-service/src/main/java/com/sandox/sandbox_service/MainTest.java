package com.sandox.sandbox_service;

import java.io.IOException;
import java.util.List;

public class MainTest {

    public static void main(String[] args) throws IOException {
        PomParser pomParser = new PomParser();
        List<String> resolvedJars = pomParser.getClasspath("/home/shakshor/coBrain/coBrain/backend/sandbox-service/pom.xml");

        DeprecationService  deprecationService = new DeprecationService();
        String warnings =  deprecationService.checkDeprecations("1","/home/shakshor/coBrain/coBrain/backend/code-snippet",resolvedJars);


        System.out.println("Warnings: " + warnings);


    }
}
