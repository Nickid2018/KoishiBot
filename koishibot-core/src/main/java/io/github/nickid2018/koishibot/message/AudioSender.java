package io.github.nickid2018.koishibot.message;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.FormatTransformer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class AudioSender {

    public static final AtomicBoolean PAUSE_FLAG = new AtomicBoolean(false);

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

    public static void sendAudio(Future<File[]> filesToSend, MessageContext context, DelegateEnvironment environment) {
        if (context.group() != null || environment.audioToFriendSupported())
            EXECUTOR.execute(() -> {
                if (PAUSE_FLAG.get()) {
                    try {
                        Stream.of(filesToSend.get()).forEach(TempFileSystem::unlockFile);
                    } catch (Exception ignored) {
                    }
                    return;
                }
                try {
                    File[] audios = filesToSend.get();
                    for (File file : audios) {
                        Thread.sleep((FormatTransformer.QQ_VOICE_TRANSFORM_MAX_LENGTH + 10) * 1000);
                        if (PAUSE_FLAG.get())
                            break;
                        environment.getMessageSender().sendMessage(
                                context, environment.newAudio(context.group(), file.toURI().toURL()));
                    }
                    Stream.of(audios).forEach(TempFileSystem::unlockFile);
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "audio", context, false);
                }
            });
    }
}
