package io.github.nickid2018.koishibot.module.wiki;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// CAN'T BE TESTED IN PRODUCTION ENVIRONMENT(aka IDE)!
// PROCESS CAN'T RUN WITHOUT AN ACTUAL CONSOLE, EVEN THOUGH IDE CONSOLES!
public class FormatTransformer {

    public static final Logger TRANSFORMER_LOGGER = LoggerFactory.getLogger("Format Transformer");

    public static final int VOICE_TRANSFORM_MAX_LENGTH = 110;

    public static String FFMPEG_LOCATION;
    public static String ENCODER_LOCATION;

    public static void loadFFmpeg(JsonObject settingsRoot) {
        JsonUtil.getData(settingsRoot, "audio", JsonObject.class).ifPresent(audio -> {
            FFMPEG_LOCATION = JsonUtil.getStringOrNull(audio, "ffmpeg");
            ENCODER_LOCATION = JsonUtil.getStringOrNull(audio, "encoder");
        });
    }

    public static File[] transformWebAudioToSilks(String suffix, URL source) throws Exception {
        if (FFMPEG_LOCATION == null)
            return null;
        File sourceFile = TempFileSystem.createTmpFileAndCreate("as", suffix);
        IOUtils.copy(source, sourceFile);
        return transformAsSilk(sourceFile);
    }

    public static int getAudioLength(File source) throws Exception {
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        executeCommand(dataStream, FFMPEG_LOCATION, "-i", source.getAbsolutePath());
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
        TRANSFORMER_LOGGER.info("Start transforming {} to silk files, length = {}s.", sourceFile, length);
        while (length > VOICE_TRANSFORM_MAX_LENGTH) {
            File pcm = TempFileSystem.createTmpFile("tmp", "pcm");
            executeCommand(null, FFMPEG_LOCATION, "-i",
                    sourceFile.getAbsolutePath(), "-ss", offset + "", "-t", VOICE_TRANSFORM_MAX_LENGTH + "",
                    "-f", "s16le", "-ar", "24000", "-ac", "1",
                    "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
            silks.add(transformPCMtoSILK(pcm));
            offset += VOICE_TRANSFORM_MAX_LENGTH;
            length -= VOICE_TRANSFORM_MAX_LENGTH;
        }
        File pcm = TempFileSystem.createTmpFile("tmp", "pcm");
        executeCommand(null, FFMPEG_LOCATION, "-i",
                sourceFile.getAbsolutePath(), "-ss", offset + "", "-f", "s16le", "-ar", "24000", "-ac", "1",
                "-acodec", "pcm_s16le", "-y", pcm.getAbsolutePath());
        silks.add(transformPCMtoSILK(pcm));
        TempFileSystem.unlockFileAndDelete(sourceFile);
        TRANSFORMER_LOGGER.info("Transformed {} to silk files.", sourceFile);
        return silks.toArray(new File[0]);
    }

    public static File transformPCMtoSILK(File sourceFile) throws Exception {
        File silk = TempFileSystem.createTmpFile("slk", "silk");
        executeCommand(null, ENCODER_LOCATION,
                sourceFile.getAbsolutePath(), silk.getAbsolutePath(),
                "-Fs_API", "24000", "-tencent");
        TempFileSystem.unlockFileAndDelete(sourceFile);
        return silk;
    }

    public static InputStream transformImageToPNG(InputStream input, String format) throws Exception {
        if (FFMPEG_LOCATION == null)
            return null;
        File inputImage = TempFileSystem.createTmpFile("image", format);
        try (FileOutputStream fos = new FileOutputStream(inputImage)) {
            IOUtils.copy(input, fos);
        }
        File output = TempFileSystem.createTmpFile("imageO", "png");
        executeCommand(null, FFMPEG_LOCATION,
                "-i", inputImage.getAbsolutePath(), output.getAbsolutePath());
        TempFileSystem.unlockFileAndDelete(inputImage);
        TRANSFORMER_LOGGER.info("Transformed a {} image to PNG.", format);
        return new DeleteStream(output);
    }

    public static void executeCommand(OutputStream output, String... commandStr) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.command(commandStr);
        TRANSFORMER_LOGGER.debug("Execute Command: {}", String.join(" ", commandStr));
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

    private static class DeleteStream extends FileInputStream {

        private final File source;

        public DeleteStream(@NotNull File file) throws FileNotFoundException {
            super(file);
            source = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            TempFileSystem.unlockFileAndDelete(source);
        }
    }
}
