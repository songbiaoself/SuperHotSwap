package com.coderevolt.context;

import com.sun.tools.attach.VirtualMachine;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/21 8:28
 * @description
 */
public class MachineBeanInfo {

    private VirtualMachine virtualMachine;

    private String ip;

    private int port;

    private String pid;

    private String processName;

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public void setVirtualMachine(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

}
