package com.coderevolt.action;

import com.coderevolt.HotswapException;
import com.coderevolt.handler.Handler;
import com.coderevolt.handler.HandlerStrategyFactory;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 文件热更新action
 * @author 公众号:codeRevolt
 */
public class ProjectAction extends AnAction {

    public ProjectAction(@Nullable @NlsActions.ActionText String text) {
        super(text);
    }

    /**
     * 只会存在一个非核心线程，60秒回收，
     */
    private static final ExecutorService ACTION_THREAD_POOL = new ThreadPoolExecutor(0,
            1,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "action服务端线程"));

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        String fileName = psiFile.getName();
        for (Handler handler : HandlerStrategyFactory.listFileHandler()) {
            if (handler.isSupport(fileName)) {
                ACTION_THREAD_POOL.execute(() -> {
                    try {
                        handler.execute(e);
                    } catch (HotswapException ex) {
                        IdeaNotifyUtil.notify(ex.getMessage(), NotificationType.ERROR);
                        ex.printStackTrace();
                    }
                });
                break;
            }
        }
    }

}
