package com.coderevolt.handler;

import com.coderevolt.HotswapException;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:32
 * @description
 */
public interface Handler {

    /**
     * 是否支持
     * @return
     */
    boolean isSupport(Object obj);

    /**
     * 执行
     * @param obj
     */
    void execute(Object obj) throws HotswapException;

}
