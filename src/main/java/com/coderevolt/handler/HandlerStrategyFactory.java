package com.coderevolt.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/25 16:38
 * @description 处理器策略工厂
 */
public class HandlerStrategyFactory {

    /**
     * 文件处理器集合
     */
    private static final List<Handler> fileHandlerList = new ArrayList<>();

    static {
        fileHandlerList.add(new XmlFileHandler());
        fileHandlerList.add(new JavaFileHandler());
    }

    public static List<Handler> listFileHandler() {
        return Collections.unmodifiableList(fileHandlerList);
    }

}
