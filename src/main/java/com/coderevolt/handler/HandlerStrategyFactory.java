package com.coderevolt.handler;

import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.action.ExecuteDetailAction;
import com.coderevolt.log.SystemLogCollect;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:38
 * @description 处理器策略工厂
 */
public class HandlerStrategyFactory {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, CPU_COUNT);
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final int QUEUE_CAPACITY = 256;
    private static final AtomicInteger COMMAND_THREAD_ID = new AtomicInteger(1);
    private static final ThreadPoolExecutor COMMAND_THREAD_POOL = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread thread = new Thread(r, "Command线程-" + COMMAND_THREAD_ID.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    static {
        COMMAND_THREAD_POOL.allowCoreThreadTimeOut(true);
    }

    private final AnAction executeInfoAction = new ExecuteDetailAction("info.log");

    private final AnAction executeErrorAction = new ExecuteDetailAction("err.log");


    /**
     * 文件处理器集合
     */
    private final List<Handler> fileHandlerList = new ArrayList<>();

    private final List<Predicate<VirtualFile>> predicateList = new ArrayList<>();

    public HandlerStrategyFactory() {
        fileHandlerList.add(new XmlFileHandler());
        fileHandlerList.add(new JavaFileHandler());
        predicateList.add(file -> file.getName().toLowerCase().endsWith(".class") || file.getName().toLowerCase().endsWith(".java"));
        predicateList.add(file -> file.getName().toLowerCase().endsWith(".xml"));
    }

    public List<Predicate<VirtualFile>> listPredicates() {
        return predicateList;
    }

    public List<Handler> listFileHandler() {
        return fileHandlerList;
    }

    public void doAction(AnActionEvent action) {
        String processName = action.getPresentation().getText();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        COMMAND_THREAD_POOL.execute(() -> {
            try {
                Notification startNotify = IdeaNotifyUtil.notify("[" + processName + "]执行命令", NotificationType.INFORMATION, null, action.getProject());
                // 启动带有进度指示的任务
                ProgressManager.getInstance().run(new Task.Backgroundable(action.getProject(), "热更新执行") {

                    @Override
                    public void onCancel() {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            // 设置进度指示器为不确定模式
                            indicator.setIndeterminate(true);
                            countDownLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                            IdeaNotifyUtil.notify("热更新执行中断", NotificationType.WARNING, null, action.getProject());
                        }
                    }
                });
                AgentResponse<?> response = execute(action);
                startNotify.expire();
                if (response.isOk()) {
                    IdeaNotifyUtil.notify("[" + processName + "]执行成功", NotificationType.INFORMATION, executeInfoAction, action.getProject());
                } else {
                    IdeaNotifyUtil.notify("[" + processName + "]执行失败: " + response.getMsg(), NotificationType.ERROR, executeErrorAction, action.getProject());
                }
            } catch (Throwable ex) {
                ex.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                IdeaNotifyUtil.notify("[" + processName + "]执行异常: " + ex.getMessage(), NotificationType.ERROR, executeErrorAction, action.getProject());
            } finally {
                countDownLatch.countDown();
            }
        });
    }

    /**
     * 执行所有策略，部分异常不影响后续执行策略
     * @param action
     * @return
     */
    private AgentResponse<?> execute(AnActionEvent action) {
        VirtualFile[] files = getFiles(action);
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("VirtualFile不能为空");
        }
        StringBuilder err = new StringBuilder();
        predicateList.parallelStream().forEach(p -> {
            try {
                List<VirtualFile> vfList = Arrays.stream(files).filter(p).collect(Collectors.toList());
                if (!vfList.isEmpty()) {
                    Handler handler = findHandler(action, vfList);
                    if (handler != null) {
                        save2Disk(vfList, action.getProject());
                        AgentResponse<Object> response = handler.execute(vfList);
                        if (!response.isOk()) {
                            err.append(response.getMsg()).append(",");
                        }
                    } else {
                        System.err.println("没有找到合适的文件处理器: " + vfList);
                    }
                }
            } catch (HotswapException e) {
                System.err.println("HotswapException: " + e.getMessage());
                e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
            }
        });
        return (err.length() == 0) ? AgentResponse.success(null, null) : AgentResponse.failed(err.substring(0, err.length() - 1), null);
    }

    /**
     * 同步到磁盘
     * @param files
     * @param project
     */
    private void save2Disk(List<VirtualFile> files, Project project) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // 确保所有 PSI 更改都同步到 Document
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            for (VirtualFile file : files) {
                // 获取与 PsiFile 关联的 Document
                FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
                fileDocumentManager.saveDocument(fileDocumentManager.getDocument(file));
            }
        });
    }

    private static VirtualFile @Nullable [] getFiles(AnActionEvent action) {
        List<VirtualFile> result = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            VirtualFile[] files = action.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
            if (files == null || files.length == 0) return;

            Queue<VirtualFile> queue = new LinkedList<>(Arrays.asList(files));

            while(!queue.isEmpty()) {
                VirtualFile file = queue.poll();
                if (file.isDirectory()) {
                    for (VirtualFile child : file.getChildren()) {
                        queue.offer(child);
                    }
                } else {
                    result.add(file);
                }
            }
        });

        return result.toArray(new VirtualFile[0]);
    }

    private @Nullable Handler findHandler(AnActionEvent action, Object object) {
        for (Handler handler : listFileHandler()) {
            if (handler.isSupport(object)) {
                if (handler instanceof AbstractActionHandler) {
                    ((AbstractActionHandler) handler).setActionEvent(action);
                }
                return handler;
            }
        }
        return null;
    }

}
