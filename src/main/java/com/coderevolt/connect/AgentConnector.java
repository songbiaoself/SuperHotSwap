package com.coderevolt.connect;

import com.coderevolt.AgentCommand;
import com.coderevolt.AgentResponse;
import com.coderevolt.api.AgentApi;
import com.coderevolt.utils.RPC;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 20:31
 * @description
 */
public class AgentConnector<T, R> implements ConnectorApi<T, R> {

    @RPC(value = "AgentApiImpl")
    private AgentApi<T, R> agentApi;

    @Override
    public AgentResponse<R> execute(AgentCommand<T> command) {
        return agentApi.execute(command);
    }
}
