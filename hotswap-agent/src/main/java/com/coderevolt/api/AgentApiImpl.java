package com.coderevolt.api;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.enums.AgentCommandEnum;
import com.coderevolt.service.HotswapHandler;
import com.coderevolt.service.MybatisHotswapHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:17
 * @description
 */
public class AgentApiImpl implements AgentApi {

    private static final Map<AgentCommandEnum, HotswapHandler> HANDLER_MAP = new HashMap<>();

    static {
        HANDLER_MAP.put(AgentCommandEnum.MYBATIS_MAPPER_HOTSWAP, new MybatisHotswapHandler());
    }

    @Override
    public AgentResponse execute(AgentCommand agentCommand) {
        HotswapHandler handler = createHandler(agentCommand.getCommandEnum());
        if (handler == null) {
            return AgentResponse.failed("没找到处理器: " + agentCommand.getCommandEnum(), null);
        }
        try {
            handler.dispatch(agentCommand);
            return AgentResponse.success("执行命令成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return AgentResponse.failed("执行命名失败: " + e.getMessage(), null);
        }
    }

    private HotswapHandler createHandler(AgentCommandEnum commandEnum) {
        return HANDLER_MAP.get(commandEnum);
    }

}
