package com.coderevolt.context;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 21:41
 * @description
 */
public class VirtualMachineContext {

    private static final Map<String, MachineBeanInfo> MACHINE_MAP = new ConcurrentHashMap<>();

    public static void put(String key, MachineBeanInfo virtualMachine) {
        MACHINE_MAP.put(key, virtualMachine);
    }

    public static MachineBeanInfo get(String key) {
        return MACHINE_MAP.get(key);
    }

    public static int size() {
        return MACHINE_MAP.size();
    }

    public static void remove(String key) {
        MACHINE_MAP.remove(key);
    }

    public static Collection<MachineBeanInfo> values() {
        return Collections.unmodifiableCollection(MACHINE_MAP.values());
    }
}
