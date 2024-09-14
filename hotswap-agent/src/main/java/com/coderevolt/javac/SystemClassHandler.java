package com.coderevolt.javac;

import com.coderevolt.HotswapException;
import com.coderevolt.util.AgentUtil;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import javax.annotation.processing.Processor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

/**
 * class处理器
 *
 * @author 公众号: CodeRevolt
 */
public class SystemClassHandler {

    private static final JavaStringCompiler compiler = new JavaStringCompiler();

    private static final Pattern LOMBOK_PATTERN = Pattern.compile("import(\\s)+lombok.*\\.(.+);");

    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    /**
     * 使用系统类加载器加载类，只有相同的类加载器加载的类才能相互调用
     * @param name 全限定类名，例如: com.coderevolt.javac.xxx
     * @param b 字节码
     * @param off
     * @param len
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Class<?> defineClass(String name, byte[] b, int off, int len) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method defineClassMth = ClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, byte[].class, int.class, int.class
        );
        defineClassMth.setAccessible(true);
        return (Class<?>) defineClassMth.invoke(systemClassLoader, name, b, off, len);
    }

    /**
     * 编译java文件
     *
     * @param fileAbsPath 文件绝对路径
     * @return class字节码
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Map<String, byte[]> compileJava(Path fileAbsPath) throws IOException {
        if (useLombokCompile(fileAbsPath)) {
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
                JCTree.JCCompilationUnit unit = javaCompiler.parse(fileAbsPath.toString());
                com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> trees = javaCompiler.enterTrees(toJavacList(Arrays.asList(unit)));
                javaCompilerClass.getMethod("processAnnotations", com.sun.tools.javac.util.List.class).invoke(javaCompiler, trees);

                // 编译后的源码
                String source = trees.get(0).toString();
                return compiler.compile(fileAbsPath.getFileName().toString(), source);
            } catch (Throwable e) {
                System.err.println("[SuperHotSwap]尝试默认编译，lombok编译失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return compiler.compile(fileAbsPath.getFileName().toString(), new String(Files.readAllBytes(fileAbsPath), StandardCharsets.UTF_8));
    }

    private static <T> com.sun.tools.javac.util.List<T> toJavacList(List<T> list) {
        com.sun.tools.javac.util.List<T> out = com.sun.tools.javac.util.List.nil();
        ListIterator<T> li = list.listIterator(list.size());
        while (li.hasPrevious()) out = out.prepend(li.previous());
        return out;
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
            File classFile = new File(AgentUtil.getAbsClassPath(loadClass), name);
            if (!classFile.getParentFile().exists()) {
                classFile.getParentFile().mkdirs();
            }
            Files.write(classFile.toPath(), classByte, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new HotswapException("更新class文件失败", e);
        }
    }

}
