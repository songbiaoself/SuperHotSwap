package com.coderevolt.util;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/5/18 13:17
 * @description
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Spring(Spring boot)工具封装，包括：
 *
 * <ol>
 *     <li>Spring IOC容器中的bean对象获取</li>
 *     <li>注册和注销Bean</li>
 * </ol>
 *
 * @author loolly
 * @since 5.1.0
 */
@Component
public class SpringUtil implements BeanFactoryPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringUtil.class);

    /**
     * "@PostConstruct"注解标记的类中，由于ApplicationContext还未加载，导致空指针<br>
     * 因此实现BeanFactoryPostProcessor注入ConfigurableListableBeanFactory实现bean的操作
     */
    private static ConfigurableListableBeanFactory beanFactory;
    /**
     * Spring应用上下文环境
     */
    private static ApplicationContext applicationContext;
    public SpringUtil() {
        logger.info("[SuperHotSwap]初始化Spring上下文容器");
    }

    /**
     * 获取{@link ApplicationContext}
     *
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtil.applicationContext = applicationContext;
    }

    /**
     * 是否是spring环境
     * @return
     */
    public static boolean isSpringProfile() {
        return applicationContext != null || beanFactory != null;
    }

    /**
     * 获取{@link ListableBeanFactory}，可能为{@link ConfigurableListableBeanFactory} 或 {@link ApplicationContextAware}
     *
     * @return {@link ListableBeanFactory}
     * @since 5.7.0
     */
    public static ListableBeanFactory getBeanFactory() {
        final ListableBeanFactory factory = null == beanFactory ? applicationContext : beanFactory;
        if (null == factory) {
            throw new RuntimeException("No ConfigurableListableBeanFactory or ApplicationContext injected, maybe not in the Spring environment?");
        }
        return factory;
    }

    /**
     * 获取{@link ConfigurableListableBeanFactory}
     *
     * @return {@link ConfigurableListableBeanFactory}
     * @throws RuntimeException 当上下文非ConfigurableListableBeanFactory抛出异常
     * @since 5.7.7
     */
    public static ConfigurableListableBeanFactory getConfigurableBeanFactory() throws RuntimeException {
        final ConfigurableListableBeanFactory factory;
        if (null != beanFactory) {
            factory = beanFactory;
        } else if (applicationContext instanceof ConfigurableApplicationContext) {
            factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        } else {
            throw new RuntimeException("No ConfigurableListableBeanFactory from context!");
        }
        return factory;
    }

    /**
     * 注册bean
     * @param beanName
     * @param clz
     */
    public static void registerBean(String beanName, Class<?> clz)  {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) getConfigurableBeanFactory();
        BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(clz);
        factory.registerBeanDefinition(beanName, beanDefinition.getRawBeanDefinition());
    }

    /**
     * 销毁bean
     * @param beanName
     */
    public static void destroyBean(String beanName) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) getConfigurableBeanFactory();
        factory.removeBeanDefinition(beanName);
        factory.destroySingleton(beanName);
    }

    /**
     * 通过name获取 Bean
     *
     * @param <T>  Bean类型
     * @param name Bean名称
     * @return Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) getBeanFactory().getBean(name);
    }

    //通过name获取 Bean.

    /**
     * 通过class获取Bean
     *
     * @param <T>   Bean类型
     * @param clazz Bean类
     * @return Bean对象
     */
    public static <T> T getBean(Class<T> clazz) {
        return getBeanFactory().getBean(clazz);
    }

    /**
     * 是否包含bean
     * @param clazz
     * @return
     */
    public static boolean hasBean(Class<?> clazz) {
        try {
            getBeanFactory().getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
        return true;
    }

    /**
     * 获取配置文件配置项的值
     *
     * @param key 配置项key
     * @return 属性值
     * @since 5.3.3
     */
    public static String getProperty(String key) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用程序名称
     * @since 5.7.12
     */
    public static String getApplicationName() {
        return getProperty("spring.application.name");
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringUtil.beanFactory = beanFactory;
    }

    /**
     * 添加mapping url映射
     * @param clazz
     */
    public static void addMapping(Class<?> clazz) {
        RequestMappingHandlerMapping rmm = getBean("requestMappingHandlerMapping");
        Method method= null;
        try {
            method = rmm.getClass().getSuperclass().getSuperclass().getDeclaredMethod("detectHandlerMethods",Object.class);
            method.setAccessible(true);
            method.invoke(rmm, getBean(clazz));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除映射
     * @param targetClass
     */
    public static void removeMapping(Class<?> targetClass) {
        RequestMappingHandlerMapping rmm = getBean("requestMappingHandlerMapping");
        if (!hasBean(targetClass)) {
            throw new RuntimeException("No bean with name '" + targetClass.getName() + "' found!");
        }
        ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) {
                Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                try {
                    Class<?> aClass = rmm.getClass();
                    Method createMappingMethod = aClass.getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
                    createMappingMethod.setAccessible(true);
                    RequestMappingInfo requestMappingInfo = (RequestMappingInfo) createMappingMethod.invoke(rmm,specificMethod,targetClass);
                    if(requestMappingInfo != null) {
                        rmm.unregisterMapping(requestMappingInfo);
                    }
                }catch (Exception e){
                   throw new RuntimeException(e);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
    }


    public static boolean isControllerBean(Class<?> clz) {
        return AgentUtil.existAnnotation(clz, Controller.class);
    }

    public static boolean isServiceBean(Class<?> clz) {
        return AgentUtil.existAnnotation(clz, Service.class);
    }

    public static boolean isRepositoryBean(Class<?> clz) {
        return AgentUtil.existAnnotation(clz, Repository.class);
    }

    public static boolean isSpringBean(Class<?> clz) {
        return AgentUtil.existAnnotation(clz, Component.class);
    }

    public static AnnotatedElement getBeanType(Class<?> clz) {
        if (isControllerBean(clz)) return Controller.class;
        else if (isServiceBean(clz)) return Service.class;
        else if (isRepositoryBean(clz)) return Repository.class;
        else if (isSpringBean(clz)) return Component.class;
        else return null;
    }

}




