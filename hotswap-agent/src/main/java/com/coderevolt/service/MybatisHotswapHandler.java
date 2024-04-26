package com.coderevolt.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.coderevolt.AgentCommand;
import com.coderevolt.HotswapException;
import com.coderevolt.dto.MapperHotswapDto;
import com.coderevolt.plugin.MapperHotswapPlugin;
import com.coderevolt.util.AgentUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/4/20 18:33
 * @description
 */
public class MybatisHotswapHandler implements HotswapHandler{

    private boolean isSwapStrictMap = false;

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
        if (!validateMapperHotSwapEvn()) {
            throw new HotswapException("mapper环境验证失败");
        }
        try {
            Class<?> type = Class.forName(mapperHotswapDto.getMapperClass());
            SqlSessionFactory sqlSessionFactory = SpringUtil.getBean(SqlSessionFactory.class);
            Configuration configuration = sqlSessionFactory.getConfiguration();
            if (!isSwapStrictMap) {
                MapperHotswapPlugin.swapStrictMap(configuration);
                isSwapStrictMap = true;
            }
            // 拷贝到编译目录
            File xmlFile = new File(mapperHotswapDto.getMapperXmlPath());
            String absClassPath = AgentUtil.getAbsClassPath(type);
            File xmlResourceFile = AgentUtil.searchFile(absClassPath, xmlFile.getName());
            IoUtil.copy(new FileInputStream(xmlFile), new FileOutputStream(xmlResourceFile));

            // 执行mapper热更新
            String xmlResource = xmlResourceFile.getAbsolutePath().substring(absClassPath.length());
            MapperHotswapPlugin.reloadMapperXml(configuration, xmlResource, type);
        } catch (Exception e) {
            throw new HotswapException("mapper热更新失败: " + e.getMessage(), e);
        }
    }


}
