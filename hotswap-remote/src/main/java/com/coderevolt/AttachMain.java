package com.coderevolt;

import com.coderevolt.api.AgentApi;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.util.OsUtil;
import com.coderevolt.util.ProjectUtil;
import com.coderevolt.utils.RpcInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class AttachMain {

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("type error!\nformat: java -jar hotswap-remote.jar [attach|detach]");
            return;
        }
        String arg = args[0];
        switch (arg) {
            case "attach": attach();
                break;
            case "detach": detach();
                break;
            default:
                System.err.println("type error!\nformat: java -jar hotswap-remote.jar [attach|detach]");
                break;
        }
    }

    private static void detach() {
        System.out.print("type listen port: ");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        Class<AgentApi> agentApiClass = AgentApi.class;
        AgentApi rpcProxy = (AgentApi) GeneratorProxy.getRPCProxy(agentApiClass, new RpcInfo("127.0.0.1", port, agentApiClass.getSimpleName() + "Impl"));
        AgentCommand command = new AgentCommand();
        command.setCommandEnum(AgentCommandEnum.DETACH);
        AgentResponse response = rpcProxy.execute(command);
        if (response.isOk()) {
            System.out.println("detach success");
        } else {
            System.err.println("detach failed");
        }
    }

    private static void attach() {
        int availablePort;
        try {
            availablePort = ProjectUtil.findAvailablePort();
        } catch (IOException e) {
            System.err.println("获取端口异常");
            e.printStackTrace();
            return;
        }

        Map<String, String> pidMap = new HashMap<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("jps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            System.out.println("Pid\tProcessName");
            while ((line = reader.readLine()) != null) {
                String[] pidAndName = line.split(" ");
                if (pidAndName.length > 1) {
                    pidMap.put(pidAndName[0], pidAndName[1]);
                    System.out.println(pidAndName[0] + "\t" + pidAndName[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("jps指令异常");
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("type attach pid: ");
        String pid = scanner.next();

        Class vmClz = null;
        Method attachMth = null;
        try (
            InputStream stream = AttachMain.class.getResourceAsStream("/hotswap-agent.jar");
        ){
            Path tempFile = Files.createTempFile("hotswap-agent", ".jar");
            tempFile.toFile().deleteOnExit();
            try {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("copy agent jar异常");
                e.printStackTrace();
                return;
            }

            String path = tempFile.toString();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{new URL("file:/" + path)});
            vmClz = urlClassLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            attachMth = vmClz.getMethod("attach", String.class);
            Object vmInstance = attachMth.invoke(null, pid);
            Method loadAgentMth = vmClz.getMethod("loadAgent", String.class, String.class);
            path = (OsUtil.isWindows() && path.startsWith("/")) ? path.substring(1) : path;
            loadAgentMth.invoke(vmInstance, path, availablePort + "");

            Method detachMth = vmClz.getMethod("detach");
            detachMth.invoke(vmInstance);
        } catch (Exception e) {
            System.err.println("attach异常");
            e.printStackTrace();
            return;
        }

        System.out.println("attach success!");
        List<NetworkInterface> networkInterfaces = listAddresses();
        System.out.println("ProcessName\tIpv4\tPort\tNetworkInterface");
        String processName = pidMap.getOrDefault(pid, "remoteApplication");
        networkInterfaces.forEach(networkInterface -> {

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    System.out.println(processName + "\t" + address.getHostAddress() + "\t" + availablePort + "\t" + networkInterface.getDisplayName());
                }
            }

        });
    }

    private static List<NetworkInterface> listAddresses() {
        List<NetworkInterface> result = new ArrayList<>();
        try {
            // 获取所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // 跳过回环接口、未启用的接口以及不是“up”状态的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

                result.add(networkInterface);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

}
