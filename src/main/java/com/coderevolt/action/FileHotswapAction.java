package com.coderevolt.action;

import com.coderevolt.HotswapException;
import com.coderevolt.handler.Handler;
import com.coderevolt.handler.HandlerStrategyFactory;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * 文件热更新action
 * @author 公众号:codeRevolt
 */
public class FileHotswapAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        String fileName = psiFile.getName();
        for (Handler handler : HandlerStrategyFactory.listFileHandler()) {
            if (handler.isSupport(fileName)) {
                try {
                    handler.execute(e);
                } catch (HotswapException ex) {
                    IdeaNotifyUtil.notify(ex.getMessage(), NotificationType.ERROR);
                    ex.printStackTrace();
                }
                break;
            }
        }
    }

}
