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
     * 是否更新本地class文件，不存在则创建
     * 项目每次启动会加载类路径下的class文件
     */
    private boolean freshClassFile;

    public boolean isFreshClassFile() {
        return freshClassFile;
    }

    public void setFreshClassFile(boolean freshClassFile) {
        this.freshClassFile = freshClassFile;
    }

    public JavaClassHotswapDto(String javaFilePath, boolean freshClassFile) {
        this.javaFilePath = javaFilePath;
        this.freshClassFile = freshClassFile;
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
