package io.github.nickid2018.koishibot.message;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.FormatTransformer;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class AudioSender {

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

    public static void sendAudio(Future<File[]> filesToSend, MessageContext context, Environment environment) {
        if (context.group() != null || environment.audioToFriendSupported())
            EXECUTOR.execute(() -> {
                try {
                    File[] audios = filesToSend.get();
                    for (File file : audios) {
                        Thread.sleep((FormatTransformer.QQ_VOICE_TRANSFORM_MAX_LENGTH + 10) * 1000);
                        environment.getMessageSender().sendMessage(
                                context, environment.newAudio(context.group(), new FileInputStream(file)));
                    }
                    Stream.of(audios).forEach(TempFileSystem::unlockFile);
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "audio", context, false);
                }
            });
    }
}
