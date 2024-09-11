package com.coderevolt.listener;

import com.coderevolt.HotswapException;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.log.SystemLogCollect;
import com.coderevolt.util.IdeaNotifyUtil;
import com.coderevolt.util.ProjectUtil;
import com.coderevolt.util.StrUtil;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.notification.NotificationType;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序启动关闭监听
 *
 * @author 公众号:codeRevolt
 */
public class ProjectExecutionListener implements ExecutionListener {

    private static final Pattern javaExeRegex = Pattern.compile("^(.*?)java.exe");
    private static final String[] runTypeList = new String[]{"application", "spring boot", "jar application"};
    private static final ExecutorService EXECUTOR_THREAD_POOL = new ThreadPoolExecutor(2,
            Integer.MAX_VALUE,
            0,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "ExecutionListener线程"));
    private static String agentJarPath;

    static {
        SystemLogCollect.injectStandardStream();
    }

    public static String getAgentJarPath() throws IOException {
        if (agentJarPath == null) {
            synchronized (ProjectExecutionListener.class) {
                if (agentJarPath == null) {
                    agentJarPath = ProjectUtil.copyToLocal(ProjectExecutionListener.class.getResourceAsStream("/hotswap-agent.jar"), "hotswap-agent.jar");
                }
            }
        }
        return agentJarPath;
    }

    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        ExecutionListener.super.processStarted(executorId, env, handler);
        EXECUTOR_THREAD_POOL.execute(() -> {
            String error = null;
            int i = 1;
            // 尝试三次，jvm.dll加载完成后attach
            for (; i <= 3; i++) {
                try {
                    RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
                    if (!StrUtil.equalsAny(runProfile.getType().getDisplayName(), true, runTypeList)) {
                        return;
                    }
                    if (StrUtil.isEmpty(getAgentJarPath())) {
                        System.err.println("获取agent jar路径失败");
                        return;
                    }
                    String pid = getPid(handler);
                    String runProfileName = runProfile.getName() + "("+pid+")";
                    int port = ProjectUtil.findAvailablePort();

                    VirtualMachine virtualMachine = VirtualMachine.attach(pid);
                    MachineBeanInfo machineBeanInfo = new MachineBeanInfo();
                    machineBeanInfo.setProcessName(runProfileName);
                    machineBeanInfo.setVirtualMachine(virtualMachine);
                    machineBeanInfo.setIp("127.0.0.1");
                    machineBeanInfo.setPort(port);
                    machineBeanInfo.setPid(pid);
                    VirtualMachineContext.put(runProfileName, machineBeanInfo);
                    System.out.println("开始attach，agentJar地址：" + agentJarPath + "，端口号：" + port);
                    virtualMachine.loadAgent(agentJarPath, port + "");
                    virtualMachine.detach();
                    error = null;
                    break;
                } catch (AttachNotSupportedException | IOException | HotswapException e) {
                    System.err.println("attach异常");
                    e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                    error = e.getMessage();
                } catch (AgentLoadException | AgentInitializationException e) {
                    if (!e.getMessage().equals("0")) {
                        System.err.println("agent挂载异常");
                        e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                        error = e.getMessage();
                    } else if ("Agent JAR not found".equals(e.getMessage())) {
                        try {
                            getAgentJarPath();
                        } catch (IOException ex) {
                            System.err.println("加载 agent jar 失败");
                            ex.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                        }
                    } else {
                        error = null;
                        break;
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                }
            }
            if (error != null) {
                IdeaNotifyUtil.notify(error, NotificationType.ERROR);
            } else {
                System.out.println("attach成功，尝试次数：" + i);
            }
        });
    }

    private String getStartFileName(ExecutionEnvironment env) {
        RunContentDescriptor contentToReuse = env.getContentToReuse();
        String commandLine = contentToReuse.getProcessHandler().toString();
        String specialClassName = commandLine.substring(commandLine.lastIndexOf(" ")).trim();
        return specialClassName.contains(".") ? specialClassName.substring(specialClassName.lastIndexOf(".") + 1) : specialClassName;
    }

    private String getJavaBinDir(ExecutionEnvironment env) {
        RunContentDescriptor contentToReuse = env.getContentToReuse();
        String commandLine = contentToReuse.getProcessHandler().toString();
        Matcher matcher = javaExeRegex.matcher(commandLine);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String getPid(@NotNull ProcessHandler handler) throws HotswapException {
        try {
            Process process = ((BaseProcessHandler) handler).getProcess();
            Class<? extends Process> proceeClz = process.getClass();
            Method getPidMethod = proceeClz.getMethod("pid");
            getPidMethod.setAccessible(true);
            return String.valueOf(getPidMethod.invoke(process));
        } catch (Exception e) {
            e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
            throw new HotswapException("反射获取pid失败", e);
        }
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        ExecutionListener.super.processTerminated(executorId, env, handler, exitCode);
        EXECUTOR_THREAD_POOL.execute(() -> {
            RunConfigurationBase runProfile = (RunConfigurationBase) env.getRunProfile();
            if (!StrUtil.equalsAny(runProfile.getType().getDisplayName(), true, runTypeList)) {
                return;
            }
            try {
                String pid = getPid(handler);
                String name = runProfile.getName() + "(" + pid + ")";
                System.out.println("进程销毁，回收上下文信息：" + name);
                VirtualMachineContext.remove(name);
            } catch (HotswapException e) {
                e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
            }
        });
    }



}
