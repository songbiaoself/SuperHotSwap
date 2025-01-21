package com.coderevolt.handler;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;
import com.coderevolt.connect.Connector;
import com.coderevolt.context.ProjectContext;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.util.IdeaNotifyUtil;
import com.coderevolt.util.ProjectUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:40
 * @description
 */
public class JavaFileHandler implements Handler {

    @Override
    public boolean isSupport(Object obj) {
        if (!(obj instanceof String)) {
            return false;
        }
        String suffix = ((String) obj).toLowerCase();
        return suffix.endsWith(".java") || suffix.endsWith(".class");
    }

    @Override
    public void execute(Object obj) throws HotswapException {
        try {
            AnActionEvent e = (AnActionEvent) obj;
            Project project = e.getData(PlatformDataKeys.PROJECT);
            VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
            String path = virtualFile.getPath();

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            String text = psiFile.getText();

            boolean isBinary = "class".equalsIgnoreCase(virtualFile.getExtension()) || path.contains(".jar!/");

            final String javaTmpFileAbsPath = ProjectUtil.copyToLocal(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "tmp/" + virtualFile.getName().replace(".class", ".java"));
            AgentCommand<JavaClassHotswapDto> command = new AgentCommand<>();
            command.setCommandEnum(AgentCommandEnum.JAVA_CLASS_HOTSWAP);
            command.setData(new JavaClassHotswapDto(javaTmpFileAbsPath, !isBinary));

            String processName = e.getPresentation().getText();
            Connector.sendToProcess(command, Collections.singletonList(VirtualMachineContext.get(processName)), agentResponse -> {
                IdeaNotifyUtil.notify("[" + processName + "]" + agentResponse.getMsg(), agentResponse.isOk() ? NotificationType.INFORMATION : NotificationType.ERROR);
                new File(javaTmpFileAbsPath).delete();
                if (agentResponse.isOk() && isBinary) {
                    System.out.println("缓存字节码文件: " + path);
                    ProjectContext.put(project, path, text);
                }
            });
        } catch (Exception exception) {
            throw new HotswapException(exception.getMessage(), exception);
        }
    }
}
