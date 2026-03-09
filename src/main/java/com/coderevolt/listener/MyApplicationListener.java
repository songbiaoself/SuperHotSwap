package com.coderevolt.listener;


import com.coderevolt.log.SystemLogCollect;
import com.intellij.ide.AppLifecycleListener;

public class MyApplicationListener implements AppLifecycleListener {

    static {
        SystemLogCollect.injectStandardStream();
    }

    @Override
    public void appStarted() {
        RemoteConnectionListener.startListener();
    }

    @Override
    public void appClosing() {
        RemoteConnectionListener.stopListener();
    }
}
