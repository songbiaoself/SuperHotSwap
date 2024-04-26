package com.coderevolt.handler;

import com.coderevolt.HotswapException;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.psi.PsiFile;

import java.lang.reflect.Field;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:40
 * @description
 */
public class JavaFileHandler implements Handler{
    @Override
    public boolean isSupport(Object obj) {
        if (!(obj instanceof String)) {
            return false;
        }
        return ((String) obj).toLowerCase().endsWith(".java");
    }

    @Override
    public void execute(Object obj) throws HotswapException {
        try {
            AnActionEvent e = (AnActionEvent) obj;
            PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
            Class aClass = psiFile.getClass().getSuperclass();
            Field packageNameField = aClass.getDeclaredField("myPackageName");
            String packageName = (String) packageNameField.get(psiFile);

            IdeaNotifyUtil.notify("Java热更新待开发", NotificationType.INFORMATION);
        } catch (Exception exception) {
            throw new HotswapException(exception.getMessage(), exception);
        }
    }
}
