package com.coderevolt.handler;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.connect.AgentConnector;
import com.coderevolt.connect.ConnectorApi;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.dto.MapperHotswapDto;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:32
 * @description
 */
public class XmlFileHandler implements Handler{

    private ConnectorApi<MapperHotswapDto, Object> connectorApi = GeneratorProxy.getProxy(new AgentConnector<>());

    private static final String mapperClassRegex = "<mapper\\s+namespace\\s*=\\s*\"(.+)\">";

    @Override
    public boolean isSupport(Object obj) {
        if (!(obj instanceof String)) {
            return false;
        }
        return ((String) obj).toLowerCase().endsWith(".xml");
    }

    @Override
    public void execute(Object obj) throws HotswapException {

        AnActionEvent e = (AnActionEvent) obj;
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        Document document = e.getData(PlatformDataKeys.EDITOR).getDocument();
        String documentText = document.getText();
        Matcher matcher = Pattern.compile(mapperClassRegex).matcher(documentText);

        if (matcher.find()) {
            String mapperClass = matcher.group(1).replace("\\s", "");

            AgentCommand<MapperHotswapDto> command = new AgentCommand<>();
            MapperHotswapDto mapperHtosDto = new MapperHotswapDto();
            mapperHtosDto.setMapperClass(mapperClass);
            mapperHtosDto.setMapperXmlPath(psiFile.getVirtualFile().getPath());
            command.setCommandEnum(AgentCommandEnum.MYBATIS_MAPPER_HOTSWAP);
            command.setData(mapperHtosDto);

            String processName = e.getPresentation().getText();
            connectorApi.sendToProcess(Collections.singletonList(VirtualMachineContext.get(processName)),vm -> {
                AgentResponse<Object> agentResponse = connectorApi.execute(command);
                IdeaNotifyUtil.notify("[" + vm.getProcessName() + "]:" + agentResponse.getMsg(), agentResponse.isOk() ? NotificationType.INFORMATION : NotificationType.ERROR);
            });
        } else {
            IdeaNotifyUtil.notify("namespace解析失败", NotificationType.WARNING);
        }
    }
}
