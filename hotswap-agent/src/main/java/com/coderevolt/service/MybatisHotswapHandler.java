package com.coderevolt.service;

import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;
import com.coderevolt.dto.MapperHotswapDto;
import com.coderevolt.plugin.MapperHotswapPlugin;
import com.coderevolt.util.AgentUtil;
import com.coderevolt.util.OsUtil;
import com.coderevolt.util.SpringUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:33
 * @description
 */
public class MybatisHotswapHandler implements HotswapHandler {

    private static volatile boolean isSwapStrictMap = false;

    @Override
    public boolean validateEnv() throws HotswapException {
        try {
            // 后续可通过 && 运算符添加相应环境校验
            return validateMapperHotSwapEvn();
        } catch (Exception e) {
            throw new HotswapException("mybatis环境验证失败", e);
        }
    }

    private boolean validateMapperHotSwapEvn() {
        SqlSessionFactory sqlSessionFactory = SpringUtil.getBean(SqlSessionFactory.class);
        return sqlSessionFactory != null;
    }

    @Override
    public void dispatch(AgentCommand command) throws HotswapException {
        switch (command.getCommandEnum()) {
            case MYBATIS_MAPPER_HOTSWAP:
                mapperHotswap((MapperHotswapDto) command.getData());
                break;
            default:
                throw new HotswapException("mybatis不支持该命令: " + command.getCommandEnum());
        }
    }

    private void mapperHotswap(MapperHotswapDto mapperHotswapDto) throws HotswapException {
        try {
            Class<?> type = Class.forName(mapperHotswapDto.getMapperClass());
            SqlSessionFactory sqlSessionFactory = SpringUtil.getBean(SqlSessionFactory.class);
            Configuration configuration = sqlSessionFactory.getConfiguration();
            if (!isSwapStrictMap) {
                synchronized (MybatisHotswapHandler.class) {
                    if (!isSwapStrictMap) {
                        MapperHotswapPlugin.swapStrictMap(configuration);
                        isSwapStrictMap = true;
                    }
                }
            }
            // 从所有模块的类路径下查找mapper文件
            File xmlFile = new File(mapperHotswapDto.getMapperXmlPath());
            Set<UrlResource> allModuleClassPath = AgentUtil.getAllModuleClassPath();
            String resourceClassPath = null;
            File xmlResourceFile = null;
            for (Resource resource : allModuleClassPath) {
                URL url = resource.getURL();
                if ("file".equals(url.getProtocol())) {
                    try {
                        xmlResourceFile = AgentUtil.searchFile(url.getPath(), xmlFile.getName());
                        resourceClassPath = OsUtil.isWindows() && url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
                        Files.copy(new FileInputStream(xmlFile), xmlResourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        break;
                    } catch (FileNotFoundException ignored) {

                    }
                }
            }
            if (xmlResourceFile == null) {
                throw new HotswapException("查找路径:"+ allModuleClassPath+", 找不到mapper文件:" + xmlFile.getName());
            }
            // 执行mapper热更新
            String xmlResource = xmlResourceFile.getAbsolutePath().substring(resourceClassPath.length());
            MapperHotswapPlugin.reloadMapperXml(configuration, xmlResource, type);
        } catch (Exception e) {
            throw new HotswapException("mapper热更新失败: " + e.getMessage(), e);
        }
    }


}
