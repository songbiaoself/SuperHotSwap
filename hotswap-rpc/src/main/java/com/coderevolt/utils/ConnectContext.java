package com.coderevolt.utils;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:19
 * @description 通信信息上下文
 */
public class ConnectContext {

    private static ConnectContext infoContext = new ConnectContext();

    private String ip;

    private int port;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private ConnectContext() {

    }

    public static void init(String ip, int port) {
        infoContext.ip = ip;
        infoContext.port = port;
    }

    public static ConnectContext create() {
        return infoContext;
    }

}
