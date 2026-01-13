package com.coderevolt.context;

import com.coderevolt.handler.HandlerStrategyFactory;
import com.coderevolt.listener.ModifiedFileListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service(Service.Level.PROJECT)
public final class ModifiedFileTracker {

    public static final String DEFAULT_EXCLUDE_REGEX = "(^|[\\\\/])(\\.idea|\\.gradle|\\.mvn|build|out|target)([\\\\/]|$)";

    private final Project project;
    private final ConcurrentMap<String, VirtualFile> modifiedFiles = new ConcurrentHashMap<>();
    private final List<Predicate<VirtualFile>> predicates;
    private volatile Pattern directoryExcludePattern;
    private volatile String directoryExcludeRegex;

    public ModifiedFileTracker(Project project) {
        this.project = project;
        this.predicates = new HandlerStrategyFactory().listPredicates();
        this.directoryExcludeRegex = DEFAULT_EXCLUDE_REGEX;
        this.directoryExcludePattern = Pattern.compile(DEFAULT_EXCLUDE_REGEX);
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(project);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                handleEvents(events);
            }
        });
    }

    public List<VirtualFile> listModifiedFiles() {
        return new ArrayList<>(modifiedFiles.values());
    }

    public void removeFiles(List<VirtualFile> files) {
        boolean changed = false;
        for (VirtualFile file : files) {
            if (file == null) {
                continue;
            }
            if (modifiedFiles.remove(file.getPath()) != null) {
                changed = true;
            }
        }
        if (changed) {
            fireChanged();
        }
    }

    public void clear() {
        if (!modifiedFiles.isEmpty()) {
            modifiedFiles.clear();
            fireChanged();
        }
    }

    public String getDirectoryExcludeRegex() {
        return directoryExcludeRegex;
    }

    public boolean updateDirectoryExcludeRegex(String regex) {
        String normalized = regex == null ? "" : regex.trim();
        Pattern compiled = null;
        try {
            if (!normalized.isEmpty()) {
                compiled = Pattern.compile(normalized);
            }
        } catch (PatternSyntaxException ex) {
            return false;
        }
        directoryExcludeRegex = normalized;
        directoryExcludePattern = compiled;
        if (compiled != null) {
            boolean changed = false;
            for (String path : new ArrayList<>(modifiedFiles.keySet())) {
                if (compiled.matcher(path).find()) {
                    if (modifiedFiles.remove(path) != null) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                fireChanged();
            }
        }
        return true;
    }

    private void handleEvents(List<? extends VFileEvent> events) {
        boolean changed = false;
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent) {
                if (modifiedFiles.remove(event.getPath()) != null) {
                    changed = true;
                }
                continue;
            }
            if (!(event instanceof VFileContentChangeEvent)) {
                continue;
            }
            VirtualFile file = event.getFile();
            if (file == null || !file.isValid()) {
                continue;
            }
            if (!projectFileIndex.isInContent(file)) {
                continue;
            }
            if (isExcludedByDir(file.getPath())) {
                continue;
            }
            if (!isSupported(file)) {
                continue;
            }
            if (modifiedFiles.put(file.getPath(), file) == null) {
                changed = true;
            }
        }
        if (changed) {
            fireChanged();
        }
    }

    private boolean isSupported(VirtualFile file) {
        for (Predicate<VirtualFile> predicate : predicates) {
            if (predicate.test(file)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcludedByDir(String path) {
        Pattern pattern = directoryExcludePattern;
        return pattern != null && pattern.matcher(path).find();
    }

    private void fireChanged() {
        project.getMessageBus().syncPublisher(ModifiedFileListener.TOPIC).filesChanged();
    }
}
