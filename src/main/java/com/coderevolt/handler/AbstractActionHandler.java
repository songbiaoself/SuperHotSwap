package com.coderevolt.handler;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractActionHandler implements Handler {

    private AnActionEvent actionEvent;
    private Project project;
    private String processName;

    public AnActionEvent getActionEvent() {
        return actionEvent;
    }

    public void setActionEvent(AnActionEvent actionEvent) {
        this.actionEvent = actionEvent;
    }

    public void setActionContext(@Nullable Project project, @Nullable String processName) {
        this.project = project;
        this.processName = processName;
    }

    public @Nullable Project getProject() {
        if (project != null) {
            return project;
        }
        return actionEvent == null ? null : actionEvent.getProject();
    }

    public @Nullable String getProcessName() {
        if (processName != null) {
            return processName;
        }
        return actionEvent == null ? null : actionEvent.getPresentation().getText();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
