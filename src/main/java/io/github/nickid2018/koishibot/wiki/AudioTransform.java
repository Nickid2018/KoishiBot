package io.github.nickid2018.koishibot.wiki;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.Settings;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// CAN'T BE TESTED IN PRODUCTION ENVIRONMENT(aka IDE)!
// PROCESS CAN'T RUN WITHOUT AN ACTUAL CONSOLE, EVEN THOUGH IDE CONSOLES!
public class AudioTransform {

    public static final Random NAME_RANDOM = new Random();
    public static final File DATA_FOLDER = KoishiBotMain.INSTANCE.getDataFolder();

    public static File[] transform(String suffix, URL source) throws Exception {
        File sourceFile = new File(DATA_FOLDER, "as" + NAME_RANDOM.nextLong() + "." + suffix);
        sourceFile.createNewFile();
        IOUtils.copy(source, sourceFile);
        return transformAsSilk(sourceFile);
    }

    public static int getAudioLength(File source) throws Exception {
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        executeCommand(dataStream, Settings.FFMPEG_LOCATION, "-i", source.getAbsolutePath());
        String buffer = IOUtils.toString(dataStream.toByteArray(), "UTF-8");
        int offset = buffer.indexOf("Duration:") + 10;
        String str = buffer.substring(offset).split(",")[0];
        String[] times = str.split("[.:]");
        int hours = Integer.parseInt(times[0]);
        int minutes = Integer.parseInt(times[1]);
        int secs = Integer.parseInt(times[2]);
        return hours * 3600 + minutes * 60 + secs;
    }

    public static File[] transformAsSilk(File sourceFile) throws Exception {
        List<File> silks = new ArrayList<>();
        int length = getAudioLength(sourceFile);
        int offset = 0;
        while (length > 50) {
            File pcm = new File(DATA_FOLDER, "tmp" + NAME_RANDOM.nextLong() + ".pcm");
            executeCommand(null, Settings.FFMPEG_LOCATION, "-i",
                    sourceFile.getAbsolutePath(), "-ss", offset + "", "-t", "50", "-f", "s16le", "-ar", "24000", "-ac", "1",
                    "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
            silks.add(transformPCMtoSILK(pcm));
            offset += 50;
            length -= 50;
        }
        File pcm = new File(DATA_FOLDER, "tmp" + NAME_RANDOM.nextLong() + ".pcm");
        executeCommand(null, Settings.FFMPEG_LOCATION, "-i",
                sourceFile.getAbsolutePath(), "-ss", offset + "", "-f", "s16le", "-ar", "24000", "-ac", "1",
                "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
        silks.add(transformPCMtoSILK(pcm));
        sourceFile.delete();

        return silks.toArray(new File[0]);
    }

    public static File transformPCMtoSILK(File sourceFile) throws Exception {
        File silk = new File(DATA_FOLDER, "slk" + NAME_RANDOM.nextLong() + ".silk");
        executeCommand(null, Settings.ENCODER_LOCATION,
                sourceFile.getAbsolutePath(), silk.getAbsolutePath(),
                "-Fs_API", "24000", "-tencent");
        sourceFile.delete();

        return silk;
    }

    public static void executeCommand(OutputStream output, String... commandStr) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.command(commandStr);
        Process process = builder.start();
        InputStream stream = process.getInputStream();
        if (stream != null)
            consume(stream, output);
        process.waitFor();
    }

    public static void consume(InputStream in, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer, 0, 1024)) >= 0)
            if (output != null)
                output.write(buffer, 0, length);
    }
}
