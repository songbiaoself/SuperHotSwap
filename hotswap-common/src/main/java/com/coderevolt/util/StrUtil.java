package com.coderevolt.util;

import java.util.Arrays;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/5/18 13:36
 * @description
 */
public class StrUtil {

    public static boolean equalsAny(String origin, boolean ignoreCase, String... dest) {
        if (dest == null || dest.length == 0) {
            return false;
        }
        return Arrays.stream(dest).anyMatch(origin::equalsIgnoreCase);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
