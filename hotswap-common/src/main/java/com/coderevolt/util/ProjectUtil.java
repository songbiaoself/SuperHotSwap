package com.coderevolt.util;


import com.coderevolt.Constant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Administrator
 */
public class ProjectUtil {

    public static String copyToLocal(InputStream inputStream, String fileName) throws IOException {
        File targetFile = new File(Constant.homePath, fileName);
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return targetFile.getAbsolutePath();
    }

    public static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        }
    }
}
