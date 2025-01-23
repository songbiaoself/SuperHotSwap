package com.coderevolt.action;

import com.coderevolt.handler.HandlerStrategyFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 文件热更新action
 *
 * @author 公众号:codeRevolt
 */
public class ProjectAction extends AnAction {

    private final HandlerStrategyFactory handlerStrategyFactory = new HandlerStrategyFactory();

    public ProjectAction(@Nullable @NlsActions.ActionText String text) {
        super(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        handlerStrategyFactory.doAction(e);
    }

}
