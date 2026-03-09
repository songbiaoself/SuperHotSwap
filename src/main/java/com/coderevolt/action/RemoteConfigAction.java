package com.coderevolt.action;

import com.coderevolt.ui.RemoteConfigDialog;
import com.coderevolt.ui.RemoteConfigState;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 远程热更新配置action
 *
 * @author 公众号:codeRevolt
 */
public class RemoteConfigAction extends AnAction {

    public RemoteConfigAction() {
        super("Super HotSwap Remote Config");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        RemoteConfigDialog dialog = new RemoteConfigDialog(RemoteConfigState.getInstance());
        dialog.setVisible(true);
    }
}
