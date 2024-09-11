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

    /**
     * 全类名，例如: com.coderevolt.dto.JavaClassHotswapDto
     */
    private String fullClassName;

    public JavaClassHotswapDto(String javaFilePath, String fullClassName) {
        this.javaFilePath = javaFilePath;
        this.fullClassName = fullClassName;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
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
                ", fullClassName='" + fullClassName + '\'' +
                '}';
    }
}
