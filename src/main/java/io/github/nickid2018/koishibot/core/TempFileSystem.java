package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.KoishiBotMain;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class TempFileSystem {

    private static final Set<File> FILES_NOT_DELETE = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Map<String, File>> BUFFERED = Collections.synchronizedMap(new HashMap<>());
    private static final Random NAME_RANDOM = new Random();

    static {
        Thread cleaner = new Thread(() -> {
            if (!KoishiBotMain.INSTANCE.tmpDir.isDirectory())
                KoishiBotMain.INSTANCE.tmpDir.mkdir();
            while (true) {
                try {
                    Thread.sleep(7200_000);
                    Stream.of(Objects.requireNonNull(KoishiBotMain.INSTANCE.tmpDir.listFiles())).filter(
                            file -> !FILES_NOT_DELETE.contains(file)
                    ).forEach(File::delete);
                } catch (InterruptedException ignored) {
                }
            }
        });
        cleaner.setDaemon(true);
        cleaner.start();
    }

    public static File createTmpFile(String prefix, String suffix) {
        File file = new File(KoishiBotMain.INSTANCE.tmpDir, name(prefix, suffix));
        FILES_NOT_DELETE.add(file);
        return file;
    }

    public static File createTmpFileAndCreate(String prefix, String suffix) throws IOException {
        File file = new File(KoishiBotMain.INSTANCE.tmpDir, name(prefix, suffix));
        file.createNewFile();
        FILES_NOT_DELETE.add(file);
        return file;
    }

    public static File createTmpFileBuffered(
            String module, String name, String prefix, String suffix, boolean create) throws IOException {
        File file = new File(KoishiBotMain.INSTANCE.tmpDir, name(prefix, suffix));
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

    public static void onDisable() {
        Stream.of(Objects.requireNonNull(KoishiBotMain.INSTANCE.tmpDir.listFiles())).forEach(File::delete);
    }
}
