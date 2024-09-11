package com.coderevolt.javac;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Load class from byte[] which is compiled in memory.
 *
 * @author michael
 */
class MemoryClassLoader extends URLClassLoader {

    private static final Map<String, byte[]> classByteMap = new HashMap<>();

    // class name to class bytes:
    public MemoryClassLoader() {
        super(new URL[0], MemoryClassLoader.class.getClassLoader());
    }

    public void put(String className, byte[] bytes) {
        classByteMap.put(className, bytes);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] buf = classByteMap.get(name);
        if (buf == null) {
            return super.findClass(name);
        }
        classByteMap.remove(name);
        return defineClass(name, buf, 0, buf.length);
    }

}
