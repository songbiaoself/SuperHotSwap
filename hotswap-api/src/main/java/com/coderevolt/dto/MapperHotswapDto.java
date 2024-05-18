package com.coderevolt.dto;

import java.io.Serializable;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 19:02
 * @description
 */
public class MapperHotswapDto implements Serializable {

    private static final long serialVersionUID = 3L;
    /**
     * 类全名称
     */
    private String mapperClass;

    /**
     * xml文件路径
     */
    private String mapperXmlPath;

    public String getMapperXmlPath() {
        return mapperXmlPath;
    }

    public void setMapperXmlPath(String mapperXmlPath) {
        this.mapperXmlPath = mapperXmlPath;
    }

    public String getMapperClass() {
        return mapperClass;
    }

    public void setMapperClass(String mapperClass) {
        this.mapperClass = mapperClass;
    }

    @Override
    public String toString() {
        return "MapperHotswapDto{" +
                "mapperClass='" + mapperClass + '\'' +
                ", mapperXmlPath='" + mapperXmlPath + '\'' +
                '}';
    }
}
