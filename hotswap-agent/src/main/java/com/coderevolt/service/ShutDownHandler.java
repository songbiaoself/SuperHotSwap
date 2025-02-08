package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;

public class ShutDownHandler implements HotswapHandler {
    @Override
    public boolean validateEnv() throws HotswapException {
        return true;
    }

    @Override
    public void dispatch(AgentCommand command) throws HotswapException {
        Thread.currentThread().interrupt();
        System.out.println("thread shutdown!");
    }
}
