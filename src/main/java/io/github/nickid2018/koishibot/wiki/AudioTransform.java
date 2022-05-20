package io.github.nickid2018.koishibot.wiki;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.Settings;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.Random;

// CAN'T BE TESTED IN PRODUCTION ENVIRONMENT(aka IDE)!
// PROCESS CAN'T RUN WITHOUT AN ACTUAL CONSOLE, EVEN THOUGH IDE CONSOLES!
public class AudioTransform {

    public static final Random NAME_RANDOM = new Random();

    public static File transform(String suffix, URL source) throws Exception {
        File dataFolder = KoishiBotMain.INSTANCE.getDataFolder();
        File sourceFile = new File(dataFolder, "as" + NAME_RANDOM.nextLong() + "." + suffix);
        sourceFile.createNewFile();
        File pcm = new File(dataFolder, "tmp" + NAME_RANDOM.nextLong() + ".pcm");
        IOUtils.copy(source, sourceFile);
        exeCmd(Settings.FFMPEG_LOCATION, "-i",
                sourceFile.getAbsolutePath(), "-f", "s16le", "-ar", "24000", "-ac", "1",
                "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
        sourceFile.delete();

        File silk = new File(dataFolder, "slk" + NAME_RANDOM.nextLong() + ".silk");
        exeCmd(Settings.ENCODER_LOCATION, pcm.getAbsolutePath(), silk.getAbsolutePath(),
                "-Fs_API", "24000", "-tencent");
        pcm.delete();

        return silk;
    }

    public static void exeCmd(String... commandStr) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.command(commandStr);
        Process process = builder.start();
        InputStream stream = process.getInputStream();
        if (stream != null)
            consume(stream);
        process.waitFor();
    }

    public static void consume(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        while (in.read(buffer, 0, 1024) >= 0)
            ;
    }
}
