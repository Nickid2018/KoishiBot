package io.github.nickid2018.koishibot.message.api;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.util.value.Either;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Future;

public interface Environment {

    AtMessage at();

    ChainMessage chain();

    TextMessage text();

    AudioMessage audio();

    ImageMessage image();

    ForwardMessage forwards();

    MessageEntry messageEntry();

    QuoteMessage quote();

    ServiceMessage service();

    UserInfo getUser(String id, boolean isStranger);

    GroupInfo getGroup(String id);

    String getBotId();

    MessageEventPublisher getEvents();

    MessageSender getMessageSender();

    MessageManager getManager();

    boolean forwardMessageSupported();

    boolean audioSupported();

    boolean audioToFriendSupported();

    boolean quoteSupported();

    String getEnvironmentName();

    String getEnvironmentUserPrefix();

    Future<File[]> parseAudioFile(String suffix, URL url) throws IOException;

    void close();

    default AtMessage newAt(GroupInfo group, UserInfo user) {
        return at().fillAt(group, user);
    }

    default ChainMessage newChain(AbstractMessage... messages) {
        return chain().fillChain(messages);
    }

    default TextMessage newText(String text) {
        return text().fillText(text);
    }

    default AudioMessage newAudio(GroupInfo group, InputStream source) throws IOException {
        return audio().fillAudio(group, source);
    }

    default ImageMessage newImage(InputStream source) throws IOException {
        return image().fillImage(source);
    }

    default ForwardMessage newForwards(ContactInfo group, MessageEntry... entries) {
        return forwards().fillForwards(group, entries);
    }

    default MessageEntry newMessageEntry(String id, String name, AbstractMessage message, int time) {
        return messageEntry().fillMessageEntry(id, name, message, time);
    }

    default QuoteMessage newQuote(ChainMessage message) {
        return quote().fill(message);
    }

    default ServiceMessage newService(String name, Either<JsonObject, String> data) {
        return service().fillService(name, data);
    }
}
