package com.coderevolt.agent;

import com.coderevolt.context.AgentContextHolder;
import com.coderevolt.server.RPCServer;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:17
 * @description JVM attach回调类
 */
public class MainAgentHook {

    /**
     * attach回调方法
     * @param agentArgs
     * @param inst
     */
    public static void agentmain (String agentArgs, Instrumentation inst) {
        try {
            int port = Integer.parseInt(agentArgs);
            RPCServer.start(port);
            AgentContextHolder.init(port, inst);
            System.out.println("   _____                            _    _         _     _____                       \n" +
                    "  / ____|                          | |  | |       | |   / ____|                      \n" +
                    " | (___   _   _  _ __    ___  _ __ | |__| |  ___  | |_ | (___ __      __ __ _  _ __  \n" +
                    "  \\___ \\ | | | || '_ \\  / _ \\| '__||  __  | / _ \\ | __| \\___ \\\\ \\ /\\ / // _` || '_ \\ \n" +
                    "  ____) || |_| || |_) ||  __/| |   | |  | || (_) || |_  ____) |\\ V  V /| (_| || |_) |\n" +
                    " |_____/  \\__,_|| .__/  \\___||_|   |_|  |_| \\___/  \\__||_____/  \\_/\\_/  \\__,_|| .__/ \n" +
                    "                | |                                                           | |    \n" +
                    "                |_|                                                           |_|    ");
            System.out.println("SuperHotSwap启动成功，监听端口: " + agentArgs + "，版本: 1.5.2");
        } catch (IOException e) {
            System.err.println("rpc服务端启动失败");
            e.printStackTrace();
        }
    }


}
