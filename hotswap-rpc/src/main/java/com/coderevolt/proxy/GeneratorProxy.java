package com.coderevolt.proxy;

import com.coderevolt.utils.RpcInfo;

import java.lang.reflect.Proxy;

/**
 * @Description: 生成代理类
 * @Author: 公众号: CodeRevolt
 */
public class GeneratorProxy {

    public static Object getRPCProxy(Class object, RpcInfo rpcInfo){
        //idea debug 模式会有意想不到的bug
        Object proxy = Proxy.newProxyInstance(
                  object.getClassLoader()//指定类的加载器
                , new Class[]{object} // 代理需要实现的接口，可指定多个，这是一个数组
                , new RPCProxy(rpcInfo, object)); // 代理对象处理器
        return proxy;
    }

    public static <T> T getProxy(Object object){
        Object proxy = Proxy.newProxyInstance(
                 object.getClass().getClassLoader()//指定类的加载器
                ,object.getClass().getInterfaces() // 代理需要实现的接口，可指定多个，这是一个数组
                , new TargetProxy(object)); // 代理对象处理器
        return (T) proxy;
    }

}
