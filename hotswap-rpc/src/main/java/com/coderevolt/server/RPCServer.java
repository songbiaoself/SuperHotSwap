package com.coderevolt.server;

import com.alibaba.fastjson.JSON;
import com.coderevolt.bean.Data;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * @Description: 服务器socket
 * @Author: 公众号: CodeRevolt
 */
public class RPCServer {

    /**
     * 只会存在一个非核心线程，60秒回收，
     */
    private static final ExecutorService SERVER_THREAD_POOL = new ThreadPoolExecutor(0,
            1,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "RPC服务端线程"));

    public static void start(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port,5);
        SERVER_THREAD_POOL.execute(() -> {
            try {
                Socket socket = serverSocket.accept();
                try (
                        OutputStream outputStream = socket.getOutputStream();
                        InputStream inputStream = socket.getInputStream();
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)
                ) {
                    //接受客户端数据
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    Data receiveData = (Data) objectInputStream.readObject();
                    System.out.println("接收到[" + socket.getRemoteSocketAddress() + "]客户端数据:" + JSON.toJSONString(receiveData));
                    //反射调用
                    Object result = methodInvoke(receiveData);
                    //响应
                    objectOutputStream.writeObject(result);
//                    System.out.println("服务端响应成功.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 反射调用实现方法
     * @param data
     * @return
     */
    public static Object methodInvoke(Data data){
        //返回类型加参数类型
        MethodType methodType = MethodType.methodType(data.getReturnType(), data.getParameterTypes());
        try {
            //除了static方法 每个方法都有一个隐式参数this
            MethodHandle methodHandle = lookup().findVirtual(data.getType(), data.getMethodName(), methodType).bindTo(getRPCImpl(data));
            return methodHandle.invokeWithArguments(data.getArgs());
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    /**
     * 获取rpc实现类
     * 通过SPI机制找出所有实现类，实际RPC中可以通过其他更复杂的方式减少操作步骤
     * @param data
     * @return
     */
    public static Object getRPCImpl(Data data) {
        ServiceLoader serviceLoader = ServiceLoader.load(data.getType());
        Iterator iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (data.getName().equals(obj.getClass().getSimpleName())) {
                return obj;
            }
        }
        throw new IllegalArgumentException(data.getType().getName() + "没找到对应实现类: " + data.getName());
    }
}
