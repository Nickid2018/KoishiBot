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
    public static final File DATA_FOLDER = KoishiBotMain.INSTANCE.getDataFolder();
    public static final long ONE_MB = 1_000_000;
    public static final String[] AMR_BITRATE = new String[] {"23.05k", "19.85k", "18.25k", "15.85k", "14.25k", "12.65k",
            "8.85k", "6.6k"};

    public static File transform(String suffix, URL source) throws Exception {
        File sourceFile = new File(DATA_FOLDER, "as" + NAME_RANDOM.nextLong() + "." + suffix);
        sourceFile.createNewFile();
        IOUtils.copy(source, sourceFile);
        if (sourceFile.length() < 5_800_000)
            return transformAsSilk(sourceFile);
        else
            return transformAsAMR(sourceFile);
    }

    public static File transformAsSilk(File sourceFile) throws Exception {
        File pcm = new File(DATA_FOLDER, "tmp" + NAME_RANDOM.nextLong() + ".pcm");
        executeCommand(Settings.FFMPEG_LOCATION, "-i",
                sourceFile.getAbsolutePath(), "-f", "s16le", "-ar", "24000", "-ac", "1",
                "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
        sourceFile.delete();

        File silk = new File(DATA_FOLDER, "slk" + NAME_RANDOM.nextLong() + ".silk");
        executeCommand(Settings.ENCODER_LOCATION, pcm.getAbsolutePath(), silk.getAbsolutePath(),
                "-Fs_API", "24000", "-tencent");
        pcm.delete();

        return silk;
    }

    public static File transformAsAMR(File sourceFile) throws Exception {
        File amr = new File(DATA_FOLDER, "am" + NAME_RANDOM.nextLong() + ".amr");
        executeCommand(Settings.FFMPEG_LOCATION, "-i", sourceFile.getAbsolutePath(),
                "-ab", AMR_BITRATE[0], "-ar", "16000", "-ac", "1",
                "-acodec", "amr_wb", "-y", amr.getAbsolutePath());
        int index = 1;
        while (amr.length() > ONE_MB && index < AMR_BITRATE.length) {
            executeCommand(Settings.FFMPEG_LOCATION, "-i", sourceFile.getAbsolutePath(),
                    "-ab", AMR_BITRATE[index++], "-ar", "16000", "-ac", "1",
                    "-acodec", "amr_wb", "-y", amr.getAbsolutePath());
        }
        sourceFile.delete();

        return amr.length() > ONE_MB ? null : amr;
    }

    public static void executeCommand(String... commandStr) throws Exception {
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
