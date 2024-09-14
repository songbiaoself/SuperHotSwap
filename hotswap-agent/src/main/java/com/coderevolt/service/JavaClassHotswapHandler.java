package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;
import com.coderevolt.context.AgentContextHolder;
import com.coderevolt.dto.JavaClassHotswapDto;
import com.coderevolt.javac.SystemClassHandler;
import com.coderevolt.util.AgentUtil;
import com.coderevolt.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/27 9:15
 * @description
 */
public class JavaClassHotswapHandler implements HotswapHandler {

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
        List<ClassDefinition> definitions = new ArrayList<>();
        Map<Class<?>, AnnotatedElement> oldBeanTypeMap = new HashMap<>();
        try {
            Map<String, byte[]> classMap = SystemClassHandler.compileJava(Paths.get(javaClassHotswapDto.getJavaFilePath()));
            if (classMap != null && !classMap.isEmpty()) {
                boolean springProfile = AgentUtil.isSpringProfile();
                classMap.forEach((k, v) -> {
                    Class<?> clz = null;
                    try {
                        clz = Class.forName(k);
                    } catch (ClassNotFoundException e) {
                        try {
                            clz = SystemClassHandler.defineClass(k, v, 0, v.length);
                            if (springProfile) {
                                oldBeanTypeMap.put(clz, null);
                            }
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    definitions.add(new ClassDefinition(clz, v));

                });
                if (springProfile) {
                    // 在类重定义之前获取bean的类型
                    for (ClassDefinition classDefinition : definitions) {
                        Class<?> clz = classDefinition.getDefinitionClass();
                        if (!oldBeanTypeMap.containsKey(clz)) {
                            oldBeanTypeMap.put(clz, SpringUtil.getBeanType(clz));
                        }
                    }
                }
                for (ClassDefinition classDefinition : definitions) {
                    // 更新本地class文件，动态编译需要动态链接class文件（-classpath）
                    try {
                        SystemClassHandler.freshClassFile(classDefinition.getDefinitionClass(), classDefinition.getDefinitionClassFile());
                    } catch (HotswapException e) {
                        throw new RuntimeException(e);
                    }
                }
                inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            } else {
                throw new HotswapException("编译失败: " + javaClassHotswapDto.getJavaFilePath());
            }
            if (!oldBeanTypeMap.isEmpty()) {
                springHotSwap(definitions, oldBeanTypeMap);
            }
        } catch (IOException | UnmodifiableClassException | ClassNotFoundException e) {
            throw new HotswapException("class热更新失败: " + javaClassHotswapDto.getJavaFilePath(), e);
        }
    }

    /**
     * bean注册和销毁
     * 更新requestmapping
     * @param definitions
     */
    private void springHotSwap(List<ClassDefinition> definitions, Map<Class<?>, AnnotatedElement> oldBbeanTypeMap) {
        final Logger logger = LoggerFactory.getLogger(JavaClassHotswapHandler.class);
        for (ClassDefinition definition : definitions) {
            Class<?> beanClass = definition.getDefinitionClass();
            AnnotatedElement oldAnnotation = oldBbeanTypeMap.get(beanClass);
            if (oldAnnotation == null && SpringUtil.isSpringBean(beanClass)) {
                String beanName = beanClass.getSimpleName().substring(0, 1).toLowerCase() +
                        (beanClass.getSimpleName().length() > 1 ? beanClass.getSimpleName().substring(1) : "");
                logger.info("[SuperHotSwap]注册bean:{}", beanName);
                SpringUtil.registerBean(beanName, beanClass);
                if (SpringUtil.isControllerBean(beanClass)) {
                    refreshMapping(beanClass);
                    logger.info("[SuperHotSwap]更新requestMapping完成");
                }
            }
            if (oldAnnotation != null && !SpringUtil.isSpringBean(beanClass)){
                String beanName = beanClass.getSimpleName().substring(0, 1).toLowerCase() +
                        (beanClass.getSimpleName().length() > 1 ? beanClass.getSimpleName().substring(1) : "");
                logger.info("[SuperHotSwap]销毁bean:{}", beanName);
                if (Controller.class.isAssignableFrom((Class<?>) oldAnnotation)) {
                    SpringUtil.removeMapping(beanClass);
                    ReflectionUtils.clearCache();
                    AnnotationUtils.clearCache();
                }
                try {
                    SpringUtil.destroyBean(beanName);
                } catch (Exception e) {
                    logger.error("[SuperHotSwap]销毁bean失败", e);
                }
            }

            if (oldAnnotation != null && SpringUtil.isControllerBean(beanClass)) {
                refreshMapping(beanClass);
                logger.info("[SuperHotSwap]更新requestMapping完成");
            }
        }
    }

    private void refreshMapping(Class<?> beanClass) {
        SpringUtil.removeMapping(beanClass);
        // 清除方法、字段缓存
        ReflectionUtils.clearCache();
        // 清除注解缓存
        AnnotationUtils.clearCache();
        SpringUtil.addMapping(beanClass);
    }


}
