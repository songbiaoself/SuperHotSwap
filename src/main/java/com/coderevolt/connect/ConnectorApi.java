package com.coderevolt.connect;

import cn.hutool.core.collection.CollectionUtil;
import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.HotswapException;
import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.util.ProjectUtil;
import com.coderevolt.utils.RPC;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:32
 * @description
 */
public interface ConnectorApi<T, R> {

    AgentResponse<R> execute(AgentCommand<T> command);

    /**
     * 发送命令给所有进程
     * @param consumer
     * @throws HotswapException
     */
    default void sendToAllProcess(Consumer<MachineBeanInfo> consumer) throws HotswapException{
        Collection<MachineBeanInfo> vmList = VirtualMachineContext.values();
        if (CollectionUtil.isNotEmpty(vmList)) {
            try {
                Field agentApiField = this.getClass().getDeclaredField("agentApi");
                agentApiField.setAccessible(true);
                RPC rpcAnnotation = agentApiField.getAnnotation(RPC.class);
                Map annotationAttr = ProjectUtil.getAnnotationAttr(rpcAnnotation);
                String originIp = (String) annotationAttr.get("ip");
                int originPort = (int) annotationAttr.get("port");
                vmList.forEach(vm -> {
                    try {
                        annotationAttr.put("ip", vm.getIp());
                        annotationAttr.put("port", vm.getPort());
                        consumer.accept(vm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("vm指令发送失败，进程名: " + vm.getProcessName() + "，pid: " + vm.getPid() + "，异常: " + e.getMessage());
                    }
                });
                annotationAttr.put("ip", originIp);
                annotationAttr.put("port", originPort);
            } catch (Exception e) {
                throw new HotswapException("修改远程调用ip端口失败", e);
            }
        }
    }

}
