package com.coderevolt.dto;

import java.io.Serializable;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/27 9:19
 * @description
 */
public class JavaClassHotswapDto implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * java文件绝对路径
     */
    private String javaFilePath;

    public JavaClassHotswapDto(String javaFilePath) {
        this.javaFilePath = javaFilePath;
    }

    public String getJavaFilePath() {
        return javaFilePath;
    }

    public void setJavaFilePath(String javaFilePath) {
        this.javaFilePath = javaFilePath;
    }

    @Override
    public String toString() {
        return "JavaClassHotswapDto{" +
                "javaFilePath='" + javaFilePath + '\'' +
                '}';
    }
}
