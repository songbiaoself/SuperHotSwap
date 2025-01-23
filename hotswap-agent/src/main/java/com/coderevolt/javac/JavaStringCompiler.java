package com.coderevolt.javac;

import org.springframework.lang.Nullable;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory compile Java source code as String.
 *
 * @author michael
 */
public class JavaStringCompiler {

    JavaCompiler compiler;
    StandardJavaFileManager stdManager;
    MemoryClassLoader classLoader;

    public JavaStringCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.stdManager = compiler.getStandardFileManager(null, null, null);
        this.classLoader = new MemoryClassLoader();
    }

    /**
     * Compile a Java source file in memory.
     *
     * @param fileName Java file name, e.g. "Test.java"
     * @param source   The source code as String.
     * @param processors java annotation processor
     * @return The compiled results as Map that contains class name as key,
     * class binary as value.
     * @throws IOException If compile error.
     */
    public Map<String, byte[]> compile(List<CompileArg> compilerArgs, @Nullable List<Processor> processors) throws IOException {
        try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
            List<JavaFileObject> javaFileObjectList = compilerArgs.stream().map(c -> manager.makeStringSource(c.getFileName(), c.getSource())).collect(Collectors.toList());
            CompilationTask task = compiler.getTask(null, manager, null, null, null, javaFileObjectList);
            if (processors != null && !processors.isEmpty()) {
                task.setProcessors(processors);
            }
            Boolean result = task.call();
            if (result == null || !result) {
                throw new RuntimeException("Compilation failed.");
            }
            return manager.getClassBytes();
        }
    }

    /**
     * Load class from compiled classes.
     *
     * @param name       Full class name.
     * @param classBytes Compiled results
     * @return The Class instance.
     * @throws ClassNotFoundException If class not found.
     * @throws IOException            If load error.
     */
    public Class<?> loadClass(String name, byte[] classBytes) throws ClassNotFoundException, IOException {
        classLoader.put(name, classBytes);
        return classLoader.loadClass(name);
    }


    public static class CompileArg {

        private String fileName;

        private String source;

        public CompileArg(String fileName, String source) {
            this.fileName = fileName;
            this.source = source;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        @Override
        public String toString() {
            return "CompileArg{" +
                    "fileName='" + fileName + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
}
