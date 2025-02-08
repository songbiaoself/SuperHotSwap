package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;

public class HeartBeatHandler implements HotswapHandler {
    @Override
    public boolean validateEnv() throws HotswapException {
        return true;
    }

    @Override
    public void dispatch(AgentCommand command) throws HotswapException {
        System.out.println("received heart beat!");
    }
}
