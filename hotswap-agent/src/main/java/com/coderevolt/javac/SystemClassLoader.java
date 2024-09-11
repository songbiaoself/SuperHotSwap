package com.coderevolt.javac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemClassLoader {

    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    public static Class<?> defineClass(String name, byte[] b, int off, int len) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method defineClassMth = ClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, byte[].class, int.class, int.class
        );
        defineClassMth.setAccessible(true);
        return (Class<?>) defineClassMth.invoke(systemClassLoader, name, b, off, len);
    }

}
