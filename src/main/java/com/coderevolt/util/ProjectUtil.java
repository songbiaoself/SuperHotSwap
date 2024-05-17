package com.coderevolt.util;


import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import com.coderevolt.HotswapException;
import com.coderevolt.proxy.TargetProxy;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
public class ProjectUtil {

    private static final String homePath = System.getProperty("user.home");

    private static final OsInfo os = new OsInfo();

    private static final int retry = 5;

    public static String getPid(String javaBinDir, String projectName) throws HotswapException {
        Process process = null;
        BufferedReader reader = null;
        try {
            // 重试
            for (int i = 0; i < retry; i++) {
                process = Runtime.getRuntime().exec(javaBinDir + "jps");
                reader = new BufferedReader(new InputStreamReader(process.getInputStream(), os.isWindows() ? "GBK" : "UTF-8"));
                String str;
                while ((str = reader.readLine()) != null) {
                    String[] lineArr = str.split(" ");
                    if (lineArr.length > 1 && projectName.equals(lineArr[1].trim())) {
                        return lineArr[0];
                    }
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        throw new HotswapException("项目名称: " + projectName + ", pid查找失败");
    }

    public static String copyToLocal(InputStream inputStream, String fileName) throws FileNotFoundException {
        File file = new File(homePath, "hotswap-libs");
        if (!file.exists()) {
            file.mkdir();
        }
        File targetFile = new File(file, fileName);
        IoUtil.copy(inputStream, new FileOutputStream(targetFile));
        return targetFile.getAbsolutePath();
    }

    public static int findAvailablePort() throws IOException {
        Random random = new Random();
        while (true) {
            int p = 20000 + random.nextInt(45535);
            Process process;
            if (os.isWindows()) {
                process = Runtime.getRuntime().exec("netstat -ano | findstr " + p);
            } else if (os.isLinux()) {
                process = Runtime.getRuntime().exec("netstat -tunlp | grep " + p);
            } else {
                return p;
            }
            try (
                    InputStream inputStream = process.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                String line = bufferedReader.readLine();
                if (StrUtil.isEmpty(line)) {
                    return p;
                }
            }
        }
    }

    /**
     * 获取jdk目标代理对象
     * @param obj 代理对象
     * @param <T>
     * @return 目标代理对象
     * @throws HotswapException
     */
    public static <T> T getTargetProxyObject(Object obj) throws HotswapException {
        try {
            Class aClass = obj.getClass();
            Field hField = aClass.getSuperclass().getDeclaredField("h");
            hField.setAccessible(true);
            TargetProxy h = (TargetProxy) hField.get(obj);
            Field tField = h.getClass().getDeclaredField("target");
            tField.setAccessible(true);
            return (T) tField.get(h);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new HotswapException("没找到目标代理对象", e);
        }
    }

    public static Map getAnnotationAttr(Object obj) throws HotswapException {
        try {
            Field hField = obj.getClass().getSuperclass().getDeclaredField("h");
            hField.setAccessible(true);
            Object handler = hField.get(obj);
            Field memberValuesField = handler.getClass().getDeclaredField("memberValues");
            memberValuesField.setAccessible(true);
            return (Map) memberValuesField.get(handler);
        } catch (Exception e) {
            throw new HotswapException("获取注解属性失败", e);
        }
    }

}
