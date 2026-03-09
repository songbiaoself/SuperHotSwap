package com.coderevolt.listener;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.api.AgentApi;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.log.SystemLogCollect;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.ui.RemoteConfigState;
import com.coderevolt.util.CacheMap;
import com.coderevolt.utils.RpcInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RemoteConnectionListener {

    public static final SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final RemoteConfigState remoteConfigState = RemoteConfigState.getInstance();

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private static final int interval = 3;

    private static final CacheMap<String, AgentApi> cache = new CacheMap<>();

    public static void startListener()  {
        new Thread(() -> {
            while (!executorService.isShutdown()) {
                List<RemoteConfigState.Entry> entries = remoteConfigState.entries;
                for (RemoteConfigState.Entry entry : entries) {
                    executorService.execute(() -> {
                        String key = String.format("%s(%s)", entry.getProcessName(), entry.getPort());
                        AgentApi rpcProxy = cache.get(entry.getUniqueId());
                        if (rpcProxy == null) {
                            rpcProxy = (AgentApi) GeneratorProxy.getRPCProxy(AgentApi.class, new RpcInfo(entry.ip, Integer.parseInt(entry.port), AgentApi.class.getSimpleName() + "Impl"));
                            cache.put(entry.getUniqueId(), rpcProxy, 1, TimeUnit.HOURS);
                        }
                        try {
                            AgentCommand agentCommand = new AgentCommand();
                            agentCommand.setCommandEnum(AgentCommandEnum.HEART_BEAT);
                            AgentResponse response = rpcProxy.execute(agentCommand);
                            if (response.isOk()) {
                                entry.lastHeartBeat = yyyyMMddHHmmss.format(new Date(response.getTs()));
                                VirtualMachineContext.put(key, new MachineBeanInfo(entry.getIp(), Integer.parseInt(entry.getPort()), key));
                            } else {
                                VirtualMachineContext.remove(key);
                            }
                        } catch (Exception e) {
                            VirtualMachineContext.remove(key);
                            e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
                        }
                    });
                }
                try {
                    TimeUnit.SECONDS.sleep(interval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void stopListener() {
        executorService.shutdown();
    }

}
