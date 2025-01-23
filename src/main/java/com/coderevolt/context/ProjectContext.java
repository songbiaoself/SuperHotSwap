package com.coderevolt.context;

import com.coderevolt.util.CacheMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectContext {

    private static final Map<Project, CacheMap<Object, Object>> projectCache = new ConcurrentHashMap<>();

    private static final Map<Project, ConcurrentHashMap<String, VFileEvent>> projectFileEventMap = new ConcurrentHashMap<>();

    public static CacheMap<Object, Object> getCacheMap(Project project) {
        projectCache.computeIfAbsent(project, k -> new CacheMap<>());
        return projectCache.get(project);
    }

    public static void clearCache(Project project) {
        projectCache.remove(project);
    }

    public static Map<String, VFileEvent> getFileEventMap(Project project) {
        projectFileEventMap.computeIfAbsent(project, k -> new ConcurrentHashMap<>());
        return projectFileEventMap.get(project);
    }

    public static void clearFileEventMap(Project project) {
        projectFileEventMap.remove(project);
    }

    public static void clear(Project project) {
        clearFileEventMap(project);
        clearCache(project);
    }

}
