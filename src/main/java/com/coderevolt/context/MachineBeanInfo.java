package com.coderevolt.context;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/21 8:28
 * @description
 */
public class MachineBeanInfo {

    private String ip;

    private int port;

    private String processName;

    private String projectLocationHash;

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

    public String getUniqueId() {
        return ip + ":" + port;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProjectLocationHash() {
        return projectLocationHash;
    }

    public void setProjectLocationHash(String projectLocationHash) {
        this.projectLocationHash = projectLocationHash;
    }

    @Override
    public String toString() {
        return "MachineBeanInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", processName='" + processName + '\'' +
                ", projectLocationHash='" + projectLocationHash + '\'' +
                '}';
    }
}
