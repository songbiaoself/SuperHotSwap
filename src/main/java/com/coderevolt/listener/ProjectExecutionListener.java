package com.coderevolt.listener;

import com.coderevolt.HotswapException;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.util.ProjectUtil;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 程序启动关闭监听
 * @author 公众号:codeRevolt
 */
public class ProjectExecutionListener implements ExecutionListener {

    private static String agentJarPath;

    static {
        try {
            agentJarPath = ProjectUtil.copyToLocal(ProjectExecutionListener.class.getResourceAsStream("/hotswap-agent.jar"), "hotswap-agent.jar");
        } catch (FileNotFoundException e) {
            System.err.println("agentJar初始化到本地失败");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        ExecutionListener.super.processStarted(executorId, env, handler);
        try {
            RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
            String runProfileName = runProfile.getName();
            String pid = ProjectUtil.getPid(runProfileName);
            int port = ProjectUtil.findAvailablePort();
            VirtualMachine virtualMachine = VirtualMachine.attach(pid);

            MachineBeanInfo machineBeanInfo = new MachineBeanInfo();
            machineBeanInfo.setProcessName(runProfileName);
            machineBeanInfo.setVirtualMachine(virtualMachine);
            machineBeanInfo.setIp("127.0.0.1");
            machineBeanInfo.setPort(port);
            machineBeanInfo.setPid(pid);
            VirtualMachineContext.put(runProfileName, machineBeanInfo);
            virtualMachine.loadAgent(agentJarPath, port + "");
        } catch (AttachNotSupportedException | IOException | HotswapException e) {
            System.err.println("attach异常");
            e.printStackTrace();
        } catch (AgentLoadException | AgentInitializationException e) {
            System.err.println("agent挂载异常");
            e.printStackTrace();
        }

    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        ExecutionListener.super.processTerminated(executorId, env, handler, exitCode);
        RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
        String runProfileName = runProfile.getName();
        MachineBeanInfo machineBeanInfo = VirtualMachineContext.get(runProfileName);
        if (machineBeanInfo != null) {
            try {
                machineBeanInfo.getVirtualMachine().detach();
            } catch (IOException e) {
                System.err.println("detach异常");
                e.printStackTrace();
            } finally {
                VirtualMachineContext.remove(runProfileName);
            }
        }
    }

}
