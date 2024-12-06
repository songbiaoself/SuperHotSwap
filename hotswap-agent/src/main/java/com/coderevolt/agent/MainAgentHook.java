package com.coderevolt.agent;

import com.coderevolt.Constant;
import com.coderevolt.server.RPCServer;
import com.coderevolt.util.OsUtil;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:17
 * @description JVM attach回调类
 */
public class MainAgentHook {

    /**
     * javaAgent回调
     *
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    /**
     * attach回调方法
     *
     * @param agentArgs
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            int port = Integer.parseInt(agentArgs);
            RPCServer.start(port);
            AgentContextHolder.init(port, inst);
            String version = null;
            try (
                    InputStream resourceAsStream = MainAgentHook.class.getResourceAsStream("/version.txt");
                    InputStreamReader streamReader = new InputStreamReader(resourceAsStream);
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
            ) {
                version = bufferedReader.readLine().trim();
            }
            resolveJarManifest();
            System.out.println("   _____                            _    _         _     _____                       \n" +
                    "  / ____|                          | |  | |       | |   / ____|                      \n" +
                    " | (___   _   _  _ __    ___  _ __ | |__| |  ___  | |_ | (___ __      __ __ _  _ __  \n" +
                    "  \\___ \\ | | | || '_ \\  / _ \\| '__||  __  | / _ \\ | __| \\___ \\\\ \\ /\\ / // _` || '_ \\ \n" +
                    "  ____) || |_| || |_) ||  __/| |   | |  | || (_) || |_  ____) |\\ V  V /| (_| || |_) |\n" +
                    " |_____/  \\__,_|| .__/  \\___||_|   |_|  |_| \\___/  \\__||_____/  \\_/\\_/  \\__,_|| .__/ \n" +
                    "                | |                                                           | |    \n" +
                    "                |_|                                                           |_|    ");
            System.out.println("SuperHotSwap启动成功，监听端口: " + agentArgs + "，版本: " + version + "，link: https://mp.weixin.qq.com/s/QPviEak1uvmJlDcB4I-3ZQ");
            String logPath = Constant.homePath + File.separator + "log";;
            System.out.println("SuperHotSwap日志路径：file:///" + logPath.replace("\\", "/"));
        } catch (IOException e) {
            System.err.println("rpc服务端启动失败");
            e.printStackTrace();
        }
    }

    /**
     * 将jar包中的class路径添加到属性中，否则影响动态编译
     */
    private static void resolveJarManifest() {
        StringBuilder property = new StringBuilder(System.getProperty("java.class.path"));
        List<String> paths = Arrays.asList(property.toString().split(File.pathSeparator));
        for (String path : paths) {
            File file = new File(path);
            if (file.getName().startsWith("classpath")) {
                try (
                        InputStream ips = new FileInputStream(file);
                        JarInputStream jarStream = new JarInputStream(ips);
                ) {
                    Manifest manifest = jarStream.getManifest();
                    String attributes = manifest.getMainAttributes().getValue("Class-Path");
                    if (attributes != null) {
                        attributes = attributes
                                .replace("file:/", "")
                                .replace(" ", File.pathSeparator);
                        if (OsUtil.isWindows()) {
                            attributes = attributes.replace("/", "\\");
                        }
                        if (!property.toString().endsWith(File.pathSeparator)) {
                            property.append(File.pathSeparator);
                        }
                        property.append(attributes);
                    }
                } catch (Exception e) {
                    System.err.println("解析JarManifest失败");
                    e.printStackTrace();
                }
            }
        }
        System.setProperty("java.class.path", property.toString());
    }


}
