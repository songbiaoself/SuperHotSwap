package com.coderevolt.connect;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.api.AgentApi;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.util.CacheMap;
import com.coderevolt.utils.RpcInfo;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:32
 * @description
 */
public class Connector {

    private static final ThreadPoolExecutor COMMAND_THREAD_POOL = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            Integer.MAX_VALUE,
            30,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "Command线程"));

    private static final CacheMap<String, AgentApi> cache = new CacheMap<>();

    static {
        COMMAND_THREAD_POOL.allowCoreThreadTimeOut(true);
    }

    /**
     * 发送命令给所有进程
     * @param command 指令
     * @param vmList 接收进程
     * @param consumer
     * @throws HotswapException
     */
    public static void sendToProcess(AgentCommand command, Collection<MachineBeanInfo> vmList, Consumer<AgentResponse<Object>> consumer) throws HotswapException{
        if (vmList != null && !vmList.isEmpty()) {
            for (MachineBeanInfo vm : vmList) {
                COMMAND_THREAD_POOL.execute(() -> {
                    try {
                        // 获取rpc连接
                        Class<AgentApi> agentApiClass = AgentApi.class;
                        AgentApi rpcProxy = cache.get(vm.getPid());
                        if (rpcProxy == null) {
                            rpcProxy = (AgentApi) GeneratorProxy.getRPCProxy(agentApiClass, new RpcInfo(vm.getIp(), vm.getPort(), agentApiClass.getSimpleName() + "Impl"));
                            cache.put(vm.getPid(), rpcProxy, 1, TimeUnit.HOURS);
                        }
                        consumer.accept(rpcProxy.execute(command));
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("vm指令发送失败，进程名: " + vm.getProcessName() + "，pid: " + vm.getPid() + "，异常: " + e.getMessage());
                    }
                });
            }
        }
    }




}
