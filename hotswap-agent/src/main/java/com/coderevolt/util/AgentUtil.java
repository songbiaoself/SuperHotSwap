package com.coderevolt.util;

import com.coderevolt.javac.JavaStringCompiler;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import javax.annotation.processing.Processor;
import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/22 13:32
 * @description
 */
public class AgentUtil {

    private static final JavaStringCompiler compiler = new JavaStringCompiler();

    private static final Pattern LOMBOK_PATTERN = Pattern.compile("import(\\s)+lombok.*\\.(.+);");

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
        if (supportLombok(path)) {
            try {
                Context context = new Context();
                //仅处理注解，不生成字节码
                Options.instance(context).put("compilePolicy", "attr");
                JavaCompiler javaCompiler = new JavaCompiler(context);

                Class<?> aClass = Class.forName("lombok.launch.ShadowClassLoader");
                Constructor<?> constructor = aClass.getDeclaredConstructor(ClassLoader.class, String.class, String.class, List.class, List.class);
                constructor.setAccessible(true);
                ClassLoader classLoader = (ClassLoader) constructor.newInstance(AgentUtil.class.getClassLoader(), "lombok", null, Arrays.asList(), Arrays.asList("lombok.patcher.Symbols"));

                // 反射得到lombok注解处理器，然后设置到编译器里
                Class<?> processorClz = classLoader.loadClass("lombok.javac.apt.LombokProcessor");
                List<Processor> iterable = new ArrayList<>();
                iterable.add((Processor) processorClz.newInstance());

                Class<? extends JavaCompiler> javaCompilerClass = javaCompiler.getClass();
                // jdk8与jdk17initProcessAnnotations入参不同，gradle8.7需指定jdk11及以上，项目使用jdk8，兼容处理，否则gradle编译失败
                javaCompilerClass.getMethod("initProcessAnnotations", Iterable.class).invoke(javaCompiler, iterable);
                JCTree.JCCompilationUnit unit = javaCompiler.parse(filePath);
                com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> trees = javaCompiler.enterTrees(toJavacList(Arrays.asList(unit)));
                javaCompilerClass.getMethod("processAnnotations", com.sun.tools.javac.util.List.class).invoke(javaCompiler, trees);

                // 编译后的源码
                String source = trees.get(0).toString();
                return compiler.compile(path.getFileName().toString(), source);
            } catch (Throwable e) {
                System.err.println("[SuperHotSwap]尝试默认编译，lombok编译失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return compiler.compile(path.getFileName().toString(), new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static boolean supportLombok(Path path) {
        BufferedReader bufferedReader = null;
        try {
            Class.forName("lombok.launch.ShadowClassLoader");
            // 判断是否时使用了lombok注解
            bufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(path.toFile().toPath()), StandardCharsets.UTF_8));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (LOMBOK_PATTERN.matcher(line).matches()) {
                    return true;
                } else if (line.contains(" class ")) {
                    // class类定义标志直接返回
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("[SuperHotSwap]不支持lombok，走默认编译");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
    }

    private static <T> com.sun.tools.javac.util.List<T> toJavacList(List<T> list) {
        com.sun.tools.javac.util.List<T> out = com.sun.tools.javac.util.List.nil();
        ListIterator<T> li = list.listIterator(list.size());
        while (li.hasPrevious()) out = out.prepend(li.previous());
        return out;
    }

}
