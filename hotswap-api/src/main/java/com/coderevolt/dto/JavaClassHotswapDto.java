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
     * 源文件绝对路径
     */
    private String originalFilePath;

    /**
     * 是否更新本地class文件，不存在则创建
     * 项目每次启动会加载类路径下的class文件
     */
    private boolean freshClassFile;

    public JavaClassHotswapDto(String originalFilePath, boolean freshClassFile) {
        this.originalFilePath = originalFilePath;
        this.freshClassFile = freshClassFile;
    }

    public boolean isFreshClassFile() {
        return freshClassFile;
    }

    public void setFreshClassFile(boolean freshClassFile) {
        this.freshClassFile = freshClassFile;
    }

    @Override
    public String toString() {
        return "JavaClassHotswapDto{" +
                ", originalFilePath='" + originalFilePath + '\'' +
                ", freshClassFile=" + freshClassFile +
                '}';
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

}
