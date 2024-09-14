package com.coderevolt.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/22 13:32
 * @description
 */
public class AgentUtil {

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
