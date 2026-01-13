package com.coderevolt.handler;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.connect.Connector;
import com.coderevolt.context.ProjectContext;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.log.SystemLogCollect;
import com.coderevolt.util.CacheMap;
import com.coderevolt.util.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:40
 * @description
 */
public class JavaFileHandler extends AbstractActionHandler {

    @Override
    public boolean isSupport(Object obj) {
        if (!(obj instanceof List)) {
            return false;
        }
        for (Object o : (List) obj) {
            if (!(o instanceof VirtualFile)) return false;
            VirtualFile vf = (VirtualFile) o;
            String name = vf.getName().toLowerCase();
            if (!name.endsWith(".java") && !name.endsWith(".class")) return false;
        }
        return true;
    }

    @Override
    public AgentResponse<Object> execute(Object obj) throws HotswapException {
        Project project = getProject();
        if (project == null) {
            throw new HotswapException("Project不能为空");
        }

        List<VirtualFile> virtualFiles = (List<VirtualFile>) obj;
        List<JavaClassHotswapDto> classHotswapDtoList = new ArrayList<>();

        Map<String, String> fileCacheMap = new HashMap<>();
        Set<String> byteClassCacheMap = new HashSet<>();

        for (VirtualFile virtualFile : virtualFiles) {
            String path = virtualFile.getPath();
            String text = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null) return null;
                return psiFile.getText();
            });
            if (text == null) {
                System.err.println("获取文件内容失败：" + virtualFile.getPath());
                continue;
            }
            boolean isBinary = "class".equalsIgnoreCase(virtualFile.getExtension()) || path.contains(".jar!/");
            if (isBinary) {
                try {
                    path = ProjectUtil.copyToLocal(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "tmp/"+ virtualFile.getName().replace(".class", ".java"));
                    byteClassCacheMap.add(path);
                } catch (IOException ex) {
                    deleteTmpFile(byteClassCacheMap);
                    throw new HotswapException("写入临时文件失败: " + path, ex);
                }
            }
            classHotswapDtoList.add(new JavaClassHotswapDto(path, !isBinary));
            fileCacheMap.put(virtualFile.getPath(), text);
        }

        AgentCommand<List<JavaClassHotswapDto>> command = new AgentCommand<>();
        command.setCommandEnum(AgentCommandEnum.JAVA_CLASS_HOTSWAP);
        command.setData(classHotswapDtoList);

        String processName = getProcessName();
        if (processName == null) {
            throw new HotswapException("ProcessName不能为空");
        }
        CacheMap<Object, Object> cacheMap = ProjectContext.getCacheMap(project);
        AgentResponse<Object> agentResponse = Connector.sendToProcess(command, VirtualMachineContext.get(processName));
        System.out.println("JavaFileHandler执行结果: " + agentResponse);
        deleteTmpFile(byteClassCacheMap);
        if (agentResponse.isOk()) {
            fileCacheMap.forEach(cacheMap::put);
        }
        return agentResponse;

    }

    private static void deleteTmpFile(Set<String> byteClassCacheMap) {
        if (byteClassCacheMap == null || byteClassCacheMap.isEmpty()) return;
        byteClassCacheMap.forEach((key) -> {
            try {
                Files.deleteIfExists(Paths.get(key));
            } catch (IOException exc) {
                exc.printStackTrace(SystemLogCollect.getErrStreamWrapper());
            }
        });
    }

}
