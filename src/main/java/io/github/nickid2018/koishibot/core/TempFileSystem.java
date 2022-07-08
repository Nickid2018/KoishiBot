package io.github.nickid2018.koishibot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class TempFileSystem {

    public static final Logger TEMP_LOGGER = LoggerFactory.getLogger("Temp File System");

    private static final Set<File> FILES_NOT_DELETE = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Map<String, File>> BUFFERED = Collections.synchronizedMap(new HashMap<>());
    private static final Random NAME_RANDOM = new Random();

    public static final File TEMP_DIR = new File("temp");

    static {
        Thread cleaner = new Thread(() -> {
            if (!TEMP_DIR.isDirectory())
                TEMP_DIR.mkdir();
            while (true) {
                try {
                    Thread.sleep(7200_000);
                    Stream.of(Objects.requireNonNull(TEMP_DIR.listFiles())).filter(
                            file -> !FILES_NOT_DELETE.contains(file)
                    ).forEach(File::delete);
                    TEMP_LOGGER.info("Ran scheduled temporary files deleting.");
                } catch (InterruptedException ignored) {
                }
            }
        }, "Temp File Cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    public static File createTmpFile(String prefix, String suffix) {
        File file = new File(TEMP_DIR, name(prefix, suffix));
        FILES_NOT_DELETE.add(file);
        return file;
    }

    public static File createTmpFileAndCreate(String prefix, String suffix) throws IOException {
        File file = new File(TEMP_DIR, name(prefix, suffix));
        file.createNewFile();
        FILES_NOT_DELETE.add(file);
        return file;
    }

    public static File createTmpFileBuffered(
            String module, String name, String prefix, String suffix, boolean create) throws IOException {
        File file = new File(TEMP_DIR, name(prefix, suffix));
        if (create)
            file.createNewFile();
        FILES_NOT_DELETE.add(file);
        BUFFERED.computeIfAbsent(module, s -> new HashMap<>()).put(name, file);
        return file;
    }

    public static File getTmpFileBuffered(String module, String name) {
        File file = BUFFERED.containsKey(module) ? BUFFERED.get(module).get(name) : null;
        if (file == null)
            return null;
        if (file.exists()) {
            FILES_NOT_DELETE.add(file);
            return file;
        } else {
            BUFFERED.get(module).remove(name);
            return null;
        }
    }

    private static String name(String prefix, String suffix) {
        return prefix + NAME_RANDOM.nextLong() + "." + suffix;
    }

    public static void unlockFileAndDelete(File file) {
        FILES_NOT_DELETE.remove(file);
        file.delete();
    }

    public static void unlockFile(File file) {
        FILES_NOT_DELETE.remove(file);
    }

    public static void close() {
        Stream.of(Objects.requireNonNull(TEMP_DIR.listFiles())).forEach(File::delete);
    }
}
