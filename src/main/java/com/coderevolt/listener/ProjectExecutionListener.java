package com.coderevolt.listener;

import com.coderevolt.HotswapException;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.util.ProjectUtil;
import com.coderevolt.util.StrUtil;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序启动关闭监听
 * @author 公众号:codeRevolt
 */
public class ProjectExecutionListener implements ExecutionListener {

    private static String agentJarPath;

    private static final Pattern javaExeRegex = Pattern.compile("^(.*?)java.exe");

    private static final String[] runTypeList = new String[]{"application", "spring boot", "jar application"};

    /**
     * 只会存在一个非核心线程，60秒回收，
     */
    private static final ExecutorService EXECUTOR_THREAD_POOL = new ThreadPoolExecutor(0,
            1,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "ExecutionListener线程"));


    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        ExecutionListener.super.processStarted(executorId, env, handler);
        EXECUTOR_THREAD_POOL.execute(() -> {
            try {
                if (StrUtil.isEmpty(getAgentJarPath())) {
                    return;
                }
                RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
                if (!StrUtil.equalsAny(runProfile.getType().getDisplayName(), true, runTypeList)) {
                    return;
                }
                String runProfileName = runProfile.getName();
                String javaBinDir = getJavaBinDir(env);
                String startFileName = getStartFileName(env);

                // jps 进程名以文件命名
                String pid = ProjectUtil.getPid(javaBinDir, startFileName);
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
                throw new RuntimeException(e);
            } catch (AgentLoadException | AgentInitializationException e) {
                if (!e.getMessage().equals("0")) {
                    System.err.println("agent挂载异常");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String getStartFileName(ExecutionEnvironment env) {
        RunContentDescriptor contentToReuse = env.getContentToReuse();
        String commandLine = contentToReuse.getProcessHandler().toString();
        String specialClassName = commandLine.substring(commandLine.lastIndexOf(" ")).trim();
        return specialClassName.contains(".") ? specialClassName.substring(specialClassName.lastIndexOf(".") + 1) : specialClassName;
    }

    private static String getAgentJarPath() throws IOException {
        if (agentJarPath == null) {
            synchronized (ProjectExecutionListener.class) {
                if (agentJarPath == null) {
                    agentJarPath = ProjectUtil.copyToLocal(ProjectExecutionListener.class.getResourceAsStream("/hotswap-agent.jar"), "hotswap-agent.jar");
                }
            }
        }
        return agentJarPath;
    }

    private String getJavaBinDir(ExecutionEnvironment env) {
        RunContentDescriptor contentToReuse = env.getContentToReuse();
        String commandLine = contentToReuse.getProcessHandler().toString();
        Matcher matcher = javaExeRegex.matcher(commandLine);
        return matcher.find() ? matcher.group(1) : "";
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        ExecutionListener.super.processTerminated(executorId, env, handler, exitCode);
        EXECUTOR_THREAD_POOL.execute(() -> {
            RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
            if (!StrUtil.equalsAny(runProfile.getType().getDisplayName(), true, runTypeList)) {
                return;
            }
            String runProfileName = runProfile.getName();
            MachineBeanInfo machineBeanInfo = VirtualMachineContext.get(runProfileName);
            if (machineBeanInfo != null) {
                try {
                    machineBeanInfo.getVirtualMachine().detach();
                } catch (IOException e) {
                    System.err.println("detach异常");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    VirtualMachineContext.remove(runProfileName);
                }
            }
        });
    }

}
