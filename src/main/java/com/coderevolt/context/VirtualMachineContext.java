package com.coderevolt.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 21:41
 * @description
 */
public class VirtualMachineContext {

    private static final Map<String, MachineBeanInfo> MACHINE_MAP = new ConcurrentHashMap<>();

    private static final Set<String> PID_SET = new HashSet<>();

    public static void put(String key, MachineBeanInfo virtualMachine) {
        MACHINE_MAP.put(key, virtualMachine);
        PID_SET.add(virtualMachine.getPid());
    }

    public static MachineBeanInfo get(String key) {
        return MACHINE_MAP.get(key);
    }

    public static int size() {
        return MACHINE_MAP.size();
    }

    public static void remove(String key) {
        PID_SET.remove(MACHINE_MAP.remove(key).getPid());
    }

    public static Collection<MachineBeanInfo> values() {
        return Collections.unmodifiableCollection(MACHINE_MAP.values());
    }

    public static boolean existPid(String pid) {
        return PID_SET.contains(pid);
    }
}
