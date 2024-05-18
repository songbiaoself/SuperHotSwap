package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;
import com.coderevolt.context.AgentContextHolder;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.util.AgentUtil;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/27 9:15
 * @description
 */
public class JavaClassHotswapHandler implements HotswapHandler{

    private static final AgentContextHolder agentContext = AgentContextHolder.getInstance();

    @Override
    public boolean validateEnv() throws HotswapException {
        return true;
    }

    @Override
    public void dispatch(AgentCommand command) throws HotswapException {
        switch (command.getCommandEnum()) {
            case JAVA_CLASS_HOTSWAP:
                classHotSwapDo(command);
                break;
            default:
                throw new HotswapException("Class不支持该命令: " + command.getCommandEnum());
        }
    }

    private void classHotSwapDo(AgentCommand command) throws HotswapException {
        JavaClassHotswapDto javaClassHotswapDto = (JavaClassHotswapDto) command.getData();
        Instrumentation inst = agentContext.getInst();
        try {
            Map<String, byte[]> classMap = AgentUtil.compileJava(javaClassHotswapDto.getJavaFilePath());
            if (!CollectionUtils.isEmpty(classMap)) {
                List<ClassDefinition> definitions = new ArrayList<>();
                classMap.forEach((k, v) -> {
                    try {
                        definitions.add(new ClassDefinition(Class.forName(k), v));
                    } catch (ClassNotFoundException e) {
                        System.err.println("class加载失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            } else {
                throw new HotswapException("编译失败: " + javaClassHotswapDto.getJavaFilePath());
            }
        } catch (IOException | UnmodifiableClassException | ClassNotFoundException e) {
            throw new HotswapException("class热更新失败: " + javaClassHotswapDto.getJavaFilePath(), e);
        }
    }
}
