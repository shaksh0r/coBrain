package com.sandox.sandbox_service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class DeprecationChecker {
    public static class Warning {
        public String filePath; // Added to store file name
        public int line;
        public String message;

        public Warning(String filePath, int line, String message) {
            this.filePath = filePath;
            this.line = line;
            this.message = message;
        }

        // Override toString for readable output
        @Override
        public String toString() {
            return String.format("[file: %s, line: %d, message: %s]", filePath, line, message);
        }
    }

    public List<Warning> check(String code, String filePath, List<String> classpathJars) {
        List<Warning> warnings = new ArrayList<>();

        // Set up symbol solver with classpath
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver()); // For JDK
        classpathJars.forEach(jar -> {
            try {
                typeSolver.add(new JarTypeSolver(jar)); // For dependencies
            } catch (Exception e) {
                warnings.add(new Warning(filePath, -1, "Failed to load JAR: " + jar));
            }
        });
        typeSolver.add(new JavaParserTypeSolver(new File("src/main/java"))); // Project sources
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        try {
            // Parse code
            CompilationUnit cu = StaticJavaParser.parse(code);

            // Visitor to find deprecated method calls
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodCallExpr methodCall, Void arg) {
                    super.visit(methodCall, arg);
                    try {
                        // Resolve method
                        var decl = methodCall.resolve();
                        String methodName = decl.getName();
                        String className = decl.declaringType().getQualifiedName();

                        // Check for @Deprecated via reflection for JDK/external classes
                        try {
                            Class<?> clazz = Class.forName(className);
                            for (Method method : clazz.getMethods()) {
                                if (method.getName().equals(methodName) && method.isAnnotationPresent(Deprecated.class)) {
                                    int line = methodCall.getBegin().map(p -> p.line).orElse(-1);
                                    warnings.add(new Warning(filePath, line, "Method '" + methodName + "' in " + className + " is deprecated"));
                                    break;
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // Skip if class not found (e.g., library not in classpath)
                        }

                        // Check for @Deprecated in local source (if method is defined in project)
                        methodCall.findAncestor(com.github.javaparser.ast.body.MethodDeclaration.class)
                                .ifPresent(methodDecl -> {
                                    if (methodDecl.getAnnotationByName("Deprecated").isPresent()) {
                                        int line = methodCall.getBegin().map(p -> p.line).orElse(-1);
                                        warnings.add(new Warning(filePath, line, "Local method '" + methodName + "' is deprecated"));
                                    }
                                });
                    } catch (Exception e) {
                        // Skip unresolved methods
                    }
                }
            }, null);
        } catch (Exception e) {
            warnings.add(new Warning(filePath, -1, "Error parsing code: " + e.getMessage()));
        }

        return warnings;
    }
}