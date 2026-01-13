package com.coderevolt.listener;

import com.coderevolt.context.ModifiedFileTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class ModifiedFileStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        project.getService(ModifiedFileTracker.class);
    }
}
