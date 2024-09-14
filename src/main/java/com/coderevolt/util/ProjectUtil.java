package com.coderevolt.util;


import com.coderevolt.Constant;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Random;

/**
 * @author Administrator
 */
public class ProjectUtil {

    public static String copyToLocal(InputStream inputStream, String fileName) throws IOException {
        File file = new File(Constant.homePath, "libs");
        if (!file.exists()) {
            file.mkdirs();
        }
        File targetFile = new File(file, fileName);
        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return targetFile.getAbsolutePath();
    }

    public static int findAvailablePort() throws IOException {
        Random random = new Random();
        while (true) {
            int p = 20000 + random.nextInt(45535);
            Process process;
            if (OsUtil.isWindows()) {
                process = Runtime.getRuntime().exec("netstat -ano | findstr " + p);
            } else if (OsUtil.isLinux()) {
                process = Runtime.getRuntime().exec("netstat -tunlp | grep " + p);
            } else if (OsUtil.isMacOs()) {
                process = Runtime.getRuntime().exec("sudo lsof -i tcp:" + p);
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

    //    private static final int retry = 5;

    //    private static final String tmp = System.getenv("TMP");
//    private static final String username = System.getenv("USERNAME");

//    /**
//     * 有时获取不到pid
//     * @see com.coderevolt.util.ProjectUtil#getLatestPidFromDir()
//     * @param javaBinDir
//     * @param projectName
//     * @return
//     * @throws HotswapException
//     */
//    @Deprecated
//    public static String getPid(String javaBinDir, String projectName) throws HotswapException {
//        Process process = null;
//        BufferedReader reader = null;
//        try {
//            // 重试
//            for (int i = 0; i < retry; i++) {
//                process = Runtime.getRuntime().exec(javaBinDir + "jps");
//                reader = new BufferedReader(new InputStreamReader(process.getInputStream(), OsUtil.isWindows() ? "GBK" : "UTF-8"));
//                String str;
//                while ((str = reader.readLine()) != null) {
//                    String[] lineArr = str.split(" ");
//                    if (lineArr.length > 1
//                            && projectName.equals(lineArr[1].trim())
//                            && !VirtualMachineContext.existPid(lineArr[0])) {
//                        return lineArr[0];
//                    }
//                }
//                TimeUnit.SECONDS.sleep(1);
//            }
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (process != null) {
//                process.destroy();
//            }
//        }
//        throw new HotswapException("项目名称: " + projectName + ", pid查找失败");
//    }
//
//    static class PidFileStruct {
//
//        long createTimeMillis;
//
//        String pid;
//
//        public PidFileStruct(long createTimeMillis, String pid) {
//            this.createTimeMillis = createTimeMillis;
//            this.pid = pid;
//        }
//
//    }
//
//    /**
//     * 从pid存放目录获取最新的pid，则是刚启动的java进程
//     * @return
//     * @throws HotswapException
//     */
//    public static String getLatestPidFromDir() throws HotswapException {
//        String pidDir = tmp + "\\" + "hsperfdata_" + username;
//        PidFileStruct pidFileStruct = Arrays.stream(Objects.requireNonNull(new File(pidDir).listFiles()))
//                .filter(p ->  !VirtualMachineContext.existPid(p.getName()))
//                .map(f -> {
//                    try {
//                        return new PidFileStruct(Files.readAttributes(f.toPath(), BasicFileAttributes.class).creationTime().toMillis(), f.getName());
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).max((o1, o2) -> {
//                    if (o1.createTimeMillis == o2.createTimeMillis) return 0;
//                    return o2.createTimeMillis > o1.createTimeMillis ? -1 : 1;
//                }).orElse(null);
//        if (pidFileStruct != null) {
//            return pidFileStruct.pid;
//        }
//        throw new HotswapException("pid查找失败");
//    }

//    /**
//     * 获取jdk目标代理对象
//     * @param obj 代理对象
//     * @param <T>
//     * @return 目标代理对象
//     * @throws HotswapException
//     */
//    public static <T> T getTargetProxyObject(Object obj) throws HotswapException {
//        try {
//            Class aClass = obj.getClass();
//            Field hField = aClass.getSuperclass().getDeclaredField("h");
//            hField.setAccessible(true);
//            TargetProxy h = (TargetProxy) hField.get(obj);
//            Field tField = h.getClass().getDeclaredField("target");
//            tField.setAccessible(true);
//            return (T) tField.get(h);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new HotswapException("没找到目标代理对象", e);
//        }
//    }
//
//    public static Map getAnnotationAttr(Object obj) throws HotswapException {
//        try {
//            Field hField = obj.getClass().getSuperclass().getDeclaredField("h");
//            hField.setAccessible(true);
//            Object handler = hField.get(obj);
//            Field memberValuesField = handler.getClass().getDeclaredField("memberValues");
//            memberValuesField.setAccessible(true);
//            return (Map) memberValuesField.get(handler);
//        } catch (Exception e) {
//            throw new HotswapException("获取注解属性失败", e);
//        }
//    }

}
