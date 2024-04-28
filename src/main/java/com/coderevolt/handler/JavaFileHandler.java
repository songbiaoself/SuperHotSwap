package com.coderevolt.handler;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.connect.AgentConnector;
import com.coderevolt.connect.ConnectorApi;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:40
 * @description
 */
public class JavaFileHandler implements Handler{

    private ConnectorApi<JavaClassHotswapDto, Object> connectorApi = GeneratorProxy.getProxy(new AgentConnector<>());

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
            VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
//            获取命名空间
//            PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
//            Class aClass = psiFile.getClass().getSuperclass();
//            Field packageNameField = aClass.getDeclaredField("myPackageName");
//            String packageName = (String) packageNameField.get(psiFile);

            AgentCommand<JavaClassHotswapDto> command = new AgentCommand<>();
            command.setCommandEnum(AgentCommandEnum.JAVA_CLASS_HOTSWAP);
            command.setData(new JavaClassHotswapDto(file.getPath()));

            connectorApi.sendToAllProcess(vm -> {
                AgentResponse<Object> agentResponse = connectorApi.execute(command);
                IdeaNotifyUtil.notify("[" + vm.getProcessName() + "]:" + agentResponse.getMsg(), agentResponse.isOk() ? NotificationType.INFORMATION : NotificationType.ERROR);
            });
        } catch (Exception exception) {
            throw new HotswapException(exception.getMessage(), exception);
        }
    }
}
