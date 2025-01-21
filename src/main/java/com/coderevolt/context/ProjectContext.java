package com.coderevolt.context;

import com.coderevolt.util.CacheMap;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ProjectContext {

    private static final Map<Project, CacheMap<Object, Object>> projectCache = new ConcurrentHashMap<>();

    public static <K,V> void put(Project project, K key, V value) {
        projectCache.computeIfAbsent(project, k -> new CacheMap<>()).put(key, value);
    }

    public static <K,V> V get(Project project, K key) {
        CacheMap<Object, Object> cacheMap = getCacheMap(project);
        return (V) cacheMap.get(key);
    }

    public static <K,V> V load(Project project, K key, Supplier<V> supplier) {
        CacheMap<Object, Object> cacheMap = getCacheMap(project);
        return (V) cacheMap.load(key, (Supplier<Object>) supplier);
    }

    private static @Nullable CacheMap<Object, Object> getCacheMap(Project project) {
        projectCache.computeIfAbsent(project, k -> new CacheMap<>());
        return projectCache.get(project);
    }

    public static void clear(Project project) {
        projectCache.remove(project);
    }

}
