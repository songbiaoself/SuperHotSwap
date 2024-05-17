package com.coderevolt.utils;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/5/17 22:10
 * @description 注解的替代方式
 */
public class RpcInfo {

    private String ip;

    private int port;

    private String classSimpleName;

    public RpcInfo(String ip, int port, String classSimpleName) {
        this.ip = ip;
        this.port = port;
        this.classSimpleName = classSimpleName;
    }

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

    public String getClassSimpleName() {
        return classSimpleName;
    }

    public void setClassSimpleName(String classSimpleName) {
        this.classSimpleName = classSimpleName;
    }
}
