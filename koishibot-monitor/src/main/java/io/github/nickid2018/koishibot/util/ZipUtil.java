package io.github.nickid2018.koishibot.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipUtil {

    public static void unzipTo(File zipPath, File destPath) throws Exception {
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            zipFile.stream().forEach(entry -> {
                File file = new File(destPath, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                    return;
                }
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    FileUtils.copyInputStreamToFile(zipFile.getInputStream(entry), file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
