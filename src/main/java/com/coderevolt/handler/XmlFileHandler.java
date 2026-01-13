package com.coderevolt.handler;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.connect.Connector;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.dto.MapperHotswapDto;
import com.coderevolt.enums.AgentCommandEnum;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:32
 * @description
 */
public class XmlFileHandler extends AbstractActionHandler {

    private static final String mapperClassRegex = "<mapper\\s+namespace\\s*=\\s*\"(.+)\">";

    @Override
    public boolean isSupport(Object obj) {
        if (!(obj instanceof List)) {
            return false;
        }
        for (Object o : (List) obj) {
            if (!(o instanceof VirtualFile)) return false;
            VirtualFile vf = (VirtualFile) o;
            String name = vf.getName().toLowerCase();
            if (!name.endsWith(".xml")) return false;
        }
        return true;
    }

    @Override
    public AgentResponse<Object> execute(Object obj) throws HotswapException {

        List<VirtualFile> vfList = (List<VirtualFile>) obj;
        List<MapperHotswapDto> mapperHotswapDtoList = new ArrayList<>();
        Project project = getProject();
        if (project == null) {
            throw new HotswapException("Project不能为空");
        }

        for (VirtualFile virtualFile : vfList) {
            String text = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null) return null;
                return psiFile.getText();
            });
            if (text == null) {
                System.err.println("获取文件内容失败：" + virtualFile.getPath());
                continue;
            }
            Matcher matcher = Pattern.compile(mapperClassRegex).matcher(text);

            if (matcher.find()) {
                String mapperClass = matcher.group(1).replace("\\s", "");
                MapperHotswapDto mapperHotswapDto = new MapperHotswapDto();
                mapperHotswapDto.setMapperClass(mapperClass);
                mapperHotswapDto.setMapperXmlPath(virtualFile.getPath());
                mapperHotswapDtoList.add(mapperHotswapDto);

            } else {
                System.err.println("namespace解析失败: " + virtualFile.getPath());
            }
        }

        AgentCommand<List<MapperHotswapDto>> command = new AgentCommand<>();
        command.setCommandEnum(AgentCommandEnum.MYBATIS_MAPPER_HOTSWAP);
        command.setData(mapperHotswapDtoList);
        String processName = getProcessName();
        if (processName == null) {
            throw new HotswapException("ProcessName不能为空");
        }

        AgentResponse<Object> agentResponse = Connector.sendToProcess(command, VirtualMachineContext.get(processName));
        System.out.println("XmlFileHandler执行结果: " + agentResponse);
        return agentResponse;
    }
}
