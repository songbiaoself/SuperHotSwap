package com.coderevolt.proxy;

import com.coderevolt.utils.RPC;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Description: web代理
 * @Author: 公众号: CodeRevolt
 */
public class TargetProxy implements InvocationHandler {

    private Object target;

    public TargetProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> clazz = target.getClass();
        //获取类中所有声明的字段 包括private
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            //注入private
            field.setAccessible(true);
            RPC rpc = field.getAnnotation(RPC.class);
            if (rpc!=null) {
                //注入带有rpc注解的成员变量中
                Object rpcProxy = GeneratorProxy.getRPCProxy(field.getType(), rpc);
                field.set(target,rpcProxy);
            }
        }
        return method.invoke(target, args);
    }
}
