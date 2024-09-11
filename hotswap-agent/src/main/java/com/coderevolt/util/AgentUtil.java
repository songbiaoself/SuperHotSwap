package com.coderevolt.util;

import com.coderevolt.HotswapException;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.javac.JavaStringCompiler;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import javax.annotation.processing.Processor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/22 13:32
 * @description
 */
public class AgentUtil {

    private static final JavaStringCompiler compiler = new JavaStringCompiler();

    public static final String homePath = System.getProperty("user.home") + File.separator + "SuperHotSwap";

    private static final Pattern LOMBOK_PATTERN = Pattern.compile("import(\\s)+lombok.*\\.(.+);");

    /**
     * bfs搜索目录下的文件
     *
     * @param dir
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public static File searchFile(String dir, String fileName) throws FileNotFoundException {
        LinkedList<File> fileQueue = new LinkedList<>();
        fileQueue.offerFirst(new File(dir));
        while (!fileQueue.isEmpty()) {
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
     *
     * @return
     */
    public static String getAbsClassPath(Class clz) {
        String path = clz.getResource("/").getPath();
        path = path.replace("\\", File.separator).replace("/", File.separator);
        return (OsUtil.isWindows() && path.startsWith(File.separator)) ? path.substring(1) : path;
    }

    /**
     * 编译java文件
     *
     * @param javaClassHotswapDto
     * @return class字节码
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Map<String, byte[]> compileJava(JavaClassHotswapDto javaClassHotswapDto) throws IOException {
        Path path = Paths.get(javaClassHotswapDto.getJavaFilePath());
        if (useLombokCompile(path)) {
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
                JCTree.JCCompilationUnit unit = javaCompiler.parse(javaClassHotswapDto.getJavaFilePath());
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

    /**
     * 加载class
     *
     * @param className  全类名
     * @param classBytes 字节码
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String className, byte[] classBytes) throws IOException, ClassNotFoundException {
        return compiler.loadClass(className, classBytes);
    }

    private static boolean useLombokCompile(Path path) {
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

    /**
     * 更新本地class字节码文件
     *
     * @param loadClass
     * @param classByte
     * @throws IOException
     */
    public static void freshClassFile(Class<?> loadClass, byte[] classByte) throws HotswapException {
        try {
            String name = loadClass.getName().replace(".", "/") + ".class";
            name = name.replace("\\", File.separator).replace("/", File.separator);
            File classFile = new File(getAbsClassPath(loadClass), name);
            if (!classFile.getParentFile().exists()) {
                classFile.getParentFile().mkdirs();
            }
            Files.write(classFile.toPath(), classByte, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new HotswapException("更新class文件失败", e);
        }
    }

    /**
     * 遍历注解链，是否存在注解
     * @param object
     * @param annotationClz
     * @return
     */
    public static boolean existAnnotation(Object object, Class<? extends Annotation> annotationClz) {
        // 手动遍历继承链检查注解
        Queue<Annotation> queue = new LinkedList<>();
        Set<Class<?>> cache = new HashSet<>();
        List<Annotation> annotations = null;
        if (object instanceof AnnotatedElement) {
            annotations = Arrays.asList(((AnnotatedElement) object).getAnnotations());
        } else {
            annotations = Arrays.asList(object.getClass().getAnnotations());
        }
        for (Annotation annotation : annotations) {
            if (!cache.contains(annotation.getClass())) {
                queue.add(annotation);
                cache.add(annotation.getClass());
            }
        }
        while (!queue.isEmpty()) {
            Class<? extends Annotation> anType = queue.poll().annotationType();
            if (annotationClz.isAssignableFrom(anType)) {
                return true;
            }
            if (!cache.contains(anType)) {
                Annotation[] t = anType.getAnnotations();
                for (Annotation annotation : t) {
                    if (!cache.contains(annotation.getClass())) {
                        queue.add(annotation);
                        cache.add(annotation.getClass());
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSpringProfile() {
        try {
            return SpringUtil.isSpringProfile();
        } catch (Throwable e) {
            return false;
        }
    }
}
