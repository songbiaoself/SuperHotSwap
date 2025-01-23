package com.coderevolt.handler;

import com.intellij.openapi.actionSystem.AnActionEvent;

public abstract class AbstractActionHandler implements Handler {

    private AnActionEvent actionEvent;

    public AnActionEvent getActionEvent() {
        return actionEvent;
    }

    public void setActionEvent(AnActionEvent actionEvent) {
        this.actionEvent = actionEvent;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
