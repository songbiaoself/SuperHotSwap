package com.coderevolt.plugin;

import com.coderevolt.HotswapException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 核心逻辑
 * @author 公众号:codeRevolt
 */
public class MapperHotswapPlugin {

    /**
     * 替换掉mybatis默认的strictMap，把put方法中的containsKey异常校验去掉
     * @see Configuration.StrictMap
     * @throws IllegalStateException 替换strictMap失败
     * @param configuration
     */
    public static void swapStrictMap(Configuration configuration) {
        Class<? extends Configuration> configurationClass = configuration.getClass();
        List<String> targetFields = Arrays.asList("mappedStatements", "caches", "resultMaps", "parameterMaps", "keyGenerators", "sqlFragments");
        Field[] declaredFields = configurationClass.getDeclaredFields();
        Map<String, Field> fieldMap = Arrays.stream(declaredFields).collect(Collectors.toMap(Field::getName, obj -> obj));
        targetFields.stream().filter(fieldMap::containsKey).forEach(f -> {
            Field field = fieldMap.get(f);
            try {
                field.setAccessible(true);
                // map集合对象复制
                Class<?> originStrictMapClass = field.get(configuration).getClass();
                Set<Map.Entry> entrySet = (Set<Map.Entry>) originStrictMapClass.getMethod("entrySet").invoke(field.get(configuration));
                StrictMap<Object> strictMap = new StrictMap<>(f + " collection");
                entrySet.forEach(s -> strictMap.put(String.valueOf(s.getKey()), s.getValue()));
                field.set(configuration, strictMap);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException  e) {
                throw new IllegalStateException("替换strictMap失败:" + e.getMessage(), e);
            }
        });
    }

    /**
     * 重新加载mapper的xml文件，并重新解析
     * @param configuration
     * @param xmlResource 类路径开始：self/sn/main/mapper/UserMapper.xml
     * @param type mapper类
     */
    public static void reloadMapperXml(Configuration configuration, String xmlResource, Class type) throws HotswapException {
        long startTime = System.currentTimeMillis();
        Set<String> loadedResources = getLoadedResources(configuration);
        loadedResources.remove(xmlResource);
        loadedResources.remove(type.toString());
        InputStream resource = configuration.getClass().getResourceAsStream("/" + xmlResource);
        XMLMapperBuilder xmlParser = new XMLMapperBuilder(resource,
                configuration,
                xmlResource,
                configuration.getSqlFragments(),
                type.getName());
        // 源码对应 com.baomidou.mybatisplus.core.MybatisConfiguration
        if ("MybatisConfiguration".equals(configuration.getClass().getSimpleName())) {
            // mybatis-plus加了重复校验， 移除所有select、insert、update、delete语句
            removeStatement(xmlResource, configuration, type);
        }
        xmlParser.parse();
        System.out.println(xmlResource + "文件热更新完成, 耗时ms: " + (System.currentTimeMillis() - startTime));
    }

    private static void removeStatement(String xmlResource, Configuration configuration, Class type) {
        try {
            Field mappedStatementField = configuration.getClass().getDeclaredField("mappedStatements");
            mappedStatementField.setAccessible(true);
            Map<String, MappedStatement> statementMap = (Map<String, MappedStatement>) mappedStatementField.get(configuration);
            XPathParser xPathParser = new XPathParser(configuration.getClass().getResourceAsStream("/" + xmlResource), true, configuration.getVariables(), new XMLMapperEntityResolver());
            XNode xNode = xPathParser.evalNode("/mapper");
            List<XNode> xNodes = xNode.evalNodes("select|insert|update|delete");
            String namespace = type.getName();
            for (XNode node : xNodes) {
                String id = namespace + "." + node.getStringAttribute("id");
                statementMap.remove(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("mybatis statement移除失败");
        }
    }

    public static Set<String> getLoadedResources(Configuration configuration) throws HotswapException {
        Class<? extends Configuration> configurationClass = configuration.getClass();
        while (configurationClass != Configuration.class) {
            // 子类向上转型, 属性是父类protected修饰的属性
            configurationClass = (Class<? extends Configuration>) configurationClass.getSuperclass();
        }
        Field field = null;
        try {
            field = configurationClass.getDeclaredField("loadedResources");
            field.setAccessible(true);
            return (Set<String>) field.get(configuration);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new HotswapException("获取loadedResources集合失败",  e);
        }
    }

    static class StrictMap<V> extends ConcurrentHashMap<String, V> {

        private static final long serialVersionUID = -4950446264854982944L;
        private final String name;
        private BiFunction<V, V, String> conflictMessageProducer;

        public StrictMap(String name, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
            this.name = name;
        }

        public StrictMap(String name, int initialCapacity) {
            super(initialCapacity);
            this.name = name;
        }

        public StrictMap(String name) {
            this.name = name;
        }

        public StrictMap(String name, Map<String, ? extends V> m) {
            super(m);
            this.name = name;
        }

        /**
         * Assign a function for producing a conflict error message when contains value with the same key.
         * <p>
         * function arguments are 1st is saved value and 2nd is target value.
         *
         * @param conflictMessageProducer
         *          A function for producing a conflict error message
         *
         * @return a conflict error message
         *
         * @since 3.5.0
         */
        public StrictMap<V> conflictMessageProducer(BiFunction<V, V, String> conflictMessageProducer) {
            this.conflictMessageProducer = conflictMessageProducer;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V put(String key, V value) {
            if (key.contains(".")) {
                final String shortKey = getShortName(key);
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    super.put(shortKey, (V) new Ambiguity(shortKey));
                }
            }
            return super.put(key, value);
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                return false;
            }

            return super.get(key) != null;
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + " does not contain value for " + key);
            }
            if (value instanceof StrictMap.Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
                        + " (try using the full name including the namespace, or rename one of the entries)");
            }
            return value;
        }

        protected static class Ambiguity {
            private final String subject;

            public Ambiguity(String subject) {
                this.subject = subject;
            }

            public String getSubject() {
                return subject;
            }
        }

        private String getShortName(String key) {
            final String[] keyParts = key.split("\\.");
            return keyParts[keyParts.length - 1];
        }

    }

}
