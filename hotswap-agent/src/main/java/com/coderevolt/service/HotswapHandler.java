package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:35
 * @description
 */
public interface HotswapHandler {

    /**
     * 检查当前环境
     * @return  true表示正常，否则异常
     */
    boolean validateEnv() throws HotswapException;

    /**
     * 分派任务
     * @param command
     */
    void dispatch(AgentCommand command) throws HotswapException;

}
