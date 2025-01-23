package com.coderevolt.connect;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.api.AgentApi;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.log.SystemLogCollect;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.util.CacheMap;
import com.coderevolt.utils.RpcInfo;

import java.util.concurrent.TimeUnit;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:32
 * @description
 */
public class Connector {


    private static final CacheMap<String, AgentApi> cache = new CacheMap<>();

    /**
     * 发送命令给所有进程
     *
     * @param command  指令
     * @param vm   接收进程
     * @throws HotswapException
     */
    public static AgentResponse<Object> sendToProcess(AgentCommand command, MachineBeanInfo vm) throws HotswapException {
        try {
            // 获取rpc连接
            Class<AgentApi> agentApiClass = AgentApi.class;
            AgentApi rpcProxy = cache.get(vm.getUniqueId());
            if (rpcProxy == null) {
                rpcProxy = (AgentApi) GeneratorProxy.getRPCProxy(agentApiClass, new RpcInfo(vm.getIp(), vm.getPort(), agentApiClass.getSimpleName() + "Impl"));
                cache.put(vm.getUniqueId(), rpcProxy, 1, TimeUnit.HOURS);
            }
            System.out.println("发送指令: " + command);
            return rpcProxy.execute(command);
        } catch (Exception e) {
            e.printStackTrace(SystemLogCollect.getErrStreamWrapper());
            System.err.println("vm指令发送失败，进程名: " + vm.getProcessName() + "，uid: " + vm.getUniqueId() + "，异常: " + e.getMessage());
            throw new HotswapException("vm指令发送失败", e);
        }
    }


}
