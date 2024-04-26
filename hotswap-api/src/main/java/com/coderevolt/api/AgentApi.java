package com.coderevolt.api;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:13
 * @description agent暴露api接口
 */
public interface AgentApi<T, R> {

    /**
     * 执行指令
     * @param agentCommand
     * @return
     */
    AgentResponse<R> execute(AgentCommand<T> agentCommand);

}
