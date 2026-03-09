package com.coderevolt.ui;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@State(
        name = "RemoteSettings",
        storages = @Storage("RemoteSettings.xml")
)
@Service
public final class RemoteConfigState implements PersistentStateComponent<RemoteConfigState> {
    public static class Entry {
        public String processName;
        public String ip;
        public String port;
        public String lastHeartBeat;

        public Entry() {
        }

        public Entry(String processName, String ip, String port, String lastHeartBeat) {
            this.processName = processName;
            this.ip = ip;
            this.port = port;
            this.lastHeartBeat = lastHeartBeat;
        }

        public String getProcessName() {
            return processName;
        }

        public String getIp() {
            return ip;
        }

        public String getPort() {
            return port;
        }

        public String getLastHeartBeat() {
            return lastHeartBeat;
        }

        public String getUniqueId() {
            return ip + ":" + port;
        }
    }

    public List<Entry> entries = new CopyOnWriteArrayList<>();

    public static RemoteConfigState getInstance() {
        return ServiceManager.getService(RemoteConfigState.class);
    }

    @Override
    public RemoteConfigState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RemoteConfigState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}