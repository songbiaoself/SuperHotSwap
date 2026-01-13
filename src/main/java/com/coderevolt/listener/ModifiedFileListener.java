package com.coderevolt.listener;

import com.intellij.util.messages.Topic;

public interface ModifiedFileListener {

    Topic<ModifiedFileListener> TOPIC = Topic.create("SuperHotSwap.ModifiedFileListener", ModifiedFileListener.class);

    void filesChanged();
}
