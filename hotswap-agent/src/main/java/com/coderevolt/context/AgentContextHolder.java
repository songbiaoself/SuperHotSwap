package com.coderevolt.context;

import java.lang.instrument.Instrumentation;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/27 9:12
 * @description
 */
public class AgentContextHolder {

    private static final AgentContextHolder agentContext = new AgentContextHolder();

    private int port;

    private Instrumentation inst;

    private AgentContextHolder() {

    }

    public static void init(int port, Instrumentation inst) {
        agentContext.port = port;
        agentContext.inst = inst;
    }

    public static AgentContextHolder getInstance() {
        return agentContext;
    }

    public int getPort() {
        return port;
    }

    public Instrumentation getInst() {
        return inst;
    }
}
