package com.coderevolt.javac;

import com.coderevolt.Constant;
import com.coderevolt.HotswapException;
import com.coderevolt.util.AgentUtil;

import javax.annotation.processing.Processor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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

    private static JavaStringCompiler compiler = new JavaStringCompiler();;

    private static final Pattern LOMBOK_PATTERN = Pattern.compile("import(\\s)+lombok.*\\.(.+);");

    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    private static volatile Boolean isLombokProfile = null;

    public static final String myClassPathName = "root";

    static {
        // 添加自定义类路径
        try {
            Class<? extends URLClassLoader> aClass = URLClassLoader.class;
            Method addURL = aClass.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(systemClassLoader, new URL("file:" + getMyClassPath()));
        } catch (Throwable e) {
            System.err.println("初始化自定义类路径失败");
            e.printStackTrace();
        }
    }

    private static String getMyClassPath() {
        return Constant.homePath + File.separator + myClassPathName;
    }

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
     * @param filePaths 文件绝对路径
     * @return class字节码
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Map<String, byte[]> compileJava(List<Path> filePaths) throws IOException, HotswapException {
        if (filePaths == null || filePaths.isEmpty()) {
            throw new HotswapException("路径不能为空");
        }

        List<Processor> processors = new ArrayList<>();

        if (isLombokProfile()) {
            try {
                Class<?> aClass = Class.forName("lombok.launch.ShadowClassLoader");
                Constructor<?> constructor = aClass.getDeclaredConstructor(ClassLoader.class, String.class, String.class, List.class, List.class);
                constructor.setAccessible(true);
                ClassLoader classLoader = (ClassLoader) constructor.newInstance(AgentUtil.class.getClassLoader(), "lombok", null, Arrays.asList(), Arrays.asList("lombok.patcher.Symbols"));

                // 反射得到lombok注解处理器，然后设置到编译器里
                Class<?> processorClz = classLoader.loadClass("lombok.javac.apt.LombokProcessor");
                processors.add((Processor) processorClz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<JavaStringCompiler.CompileArg> compileList = new ArrayList<>();
        for (Path path : filePaths) {
//            String pathString = path.toString();
//            int indexOf = pathString.indexOf(":");
//            pathString = indexOf > 0 ? pathString.substring(indexOf + 1) : pathString;
            JavaStringCompiler.CompileArg arg = new JavaStringCompiler.CompileArg(path.getFileName().toString(), new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            compileList.add(arg);
        }
        return compiler.compile(compileList, processors);
    }

    private static <T> com.sun.tools.javac.util.List<T> toJavacList(List<T> list) {
        com.sun.tools.javac.util.List<T> out = com.sun.tools.javac.util.List.nil();
        ListIterator<T> li = list.listIterator(list.size());
        while (li.hasPrevious()) out = out.prepend(li.previous());
        return out;
    }

    private static boolean isLombokProfile() {
        try {
            if (isLombokProfile == null) {
                Class.forName("lombok.launch.ShadowClassLoader");
                isLombokProfile = true;
            }
        } catch (Exception ignored) {
            isLombokProfile = false;
        }
        return isLombokProfile;
    }

    private static boolean useLombokCompile(Path path) {
        if (!isLombokProfile()) return false;
        BufferedReader bufferedReader = null;
        try {
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
            e.printStackTrace();
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
            File classFile = new File(getMyClassPath(), name);
            if (!classFile.getParentFile().exists()) {
                classFile.getParentFile().mkdirs();
            }
            Files.write(classFile.toPath(), classByte, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new HotswapException("更新class文件失败", e);
        }
    }

}
