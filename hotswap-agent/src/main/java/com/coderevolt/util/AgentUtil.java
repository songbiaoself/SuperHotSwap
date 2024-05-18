package com.coderevolt.util;

import com.coderevolt.javac.JavaStringCompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/22 13:32
 * @description
 */
public class AgentUtil {

    private static final JavaStringCompiler compiler = new JavaStringCompiler();

    /**
     * bfs搜索目录下的文件
     * @param dir
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public static File searchFile(String dir, String fileName) throws FileNotFoundException {
        LinkedList<File> fileQueue = new LinkedList<>();
        fileQueue.offerFirst(new File(dir));
        while (fileQueue.size() > 0) {
            File f = fileQueue.pollFirst();
            if (f.exists()) {
                if (f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        for (File listFile : files) {
                            fileQueue.offer(listFile);
                        }
                    }
                } else if (f.getName().equals(fileName)) {
                    return f;
                }
            }
        }
        throw new FileNotFoundException("没有找到该文件: " + fileName);
    }

    /**
     * @param fileName
     * @return 文件类路径
     * @throws FileNotFoundException 没找到文件
     */
    public static String searchFileClassPath(String dir, String fileName) throws FileNotFoundException {
        File file = searchFile(dir, fileName);
        return file.getAbsolutePath().substring(dir.length());
    }

    /**
     * 获取当前类的绝对类路径
     * @return
     */
    public static String getAbsClassPath(Class clz) {
        String name = "/" + clz.getName().replace(".", "/") + ".class";
        String path = clz.getResource(name).getPath();
        name = name.replace("\\", File.separator).replace("/", File.separator);
        path = path.replace("\\", File.separator).replace("/", File.separator);
        path = path.substring(0, path.indexOf(name) + 1);
        return (OsUtil.isWindows() && path.startsWith(File.separator)) ? path.substring(1) : path;
    }

    /**
     * 编译java文件
     * @param filePath
     * @return class字节码
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Map<String, byte[]> compileJava(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return compiler.compile(path.getFileName().toString(), new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

}
