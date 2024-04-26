package com.coderevolt;

import com.coderevolt.enums.AgentCommandEnum;

import java.io.Serializable;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:17
 * @description
 */
public class AgentCommand<T> implements Serializable {

    private AgentCommandEnum commandEnum;

    private T data;

    public AgentCommandEnum getCommandEnum() {
        return commandEnum;
    }

    public void setCommandEnum(AgentCommandEnum commandEnum) {
        this.commandEnum = commandEnum;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
