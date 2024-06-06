package com.coderevolt.connect;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.api.AgentApi;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.proxy.GeneratorProxy;
import com.coderevolt.utils.RpcInfo;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:32
 * @description
 */
public class Connector {

    /**
     * 发送命令给所有进程
     * @param command 指令
     * @param vmList 接收进程
     * @param consumer
     * @throws HotswapException
     */
    public static void sendToProcess(AgentCommand command, Collection<MachineBeanInfo> vmList, Consumer<AgentResponse<Object>> consumer) throws HotswapException{
        if (vmList != null && !vmList.isEmpty()) {
            vmList.forEach(vm -> {
                try {
                    // 获取rpc连接
                    Class<AgentApi> agentApiClass = AgentApi.class;
                    AgentApi rpcProxy = (AgentApi) GeneratorProxy.getRPCProxy(agentApiClass, new RpcInfo(vm.getIp(), vm.getPort(), agentApiClass.getSimpleName() + "Impl"));
                    consumer.accept(rpcProxy.execute(command));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("vm指令发送失败，进程名: " + vm.getProcessName() + "，pid: " + vm.getPid() + "，异常: " + e.getMessage());
                }
            });
        }
    }

}
