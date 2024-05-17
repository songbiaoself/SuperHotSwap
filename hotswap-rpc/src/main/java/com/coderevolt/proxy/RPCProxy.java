package com.coderevolt.proxy;

import com.alibaba.fastjson.JSON;
import com.coderevolt.bean.Data;
import com.coderevolt.utils.RpcInfo;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * @Description: rpc代理类
 * @Author: 公众号: CodeRevolt
 */
public class RPCProxy implements InvocationHandler {

    private RpcInfo rpc;
    private Class type;

    public RPCProxy(RpcInfo rpc, Class type) {
        this.rpc = rpc;
        this.type = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        System.out.println("正在连接服务端:[" + rpc.getIp() + ":" + rpc.getPort() + "]");
        //自动释放资源
        try (
                Socket socket = new Socket(rpc.getIp(), rpc.getPort());
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)
        ) {
            //===============发送====================
            Data data = new Data(type, rpc.getClassSimpleName(), method.getReturnType(), method.getName(), method.getParameterTypes(), args);
            objectOutputStream.writeObject(data);
            //==================结束=======================
            //阻塞等待响应
            System.out.println("等待服务器响应....");
            result = objectInputStream.readObject();
        }
        System.out.println("接受服务端[" + rpc.getIp() + ":" + rpc.getPort() + "]的消息:" + JSON.toJSONString(result));
        return result;
    }

    public Class[] covertToClass(Object[] objects) {
        if (objects == null || objects.length==0) {
            return null;
        }
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length ; i++) {
            classes[i] = objects[i].getClass();
        }
        return classes;
    }
}
