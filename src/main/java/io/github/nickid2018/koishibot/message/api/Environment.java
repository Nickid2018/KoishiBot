package io.github.nickid2018.koishibot.message.api;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.util.value.Either;

import java.io.IOException;
import java.io.InputStream;

public interface Environment {

    AtMessage newAt();

    ChainMessage newChain();

    TextMessage newText();

    AudioMessage newAudio();

    ImageMessage newImage();

    ForwardMessage newForwards();

    MessageEntry newMessageEntry();

    QuoteMessage newQuote();

    ServiceMessage newService();

    UserInfo getUser(String id, boolean isStranger);

    GroupInfo getGroup(String id);

    String getBotId();

    MessageEventPublisher getEvents();

    MessageSender getMessageSender();

    MessageManager getManager();

    boolean forwardMessageSupported();

    boolean audioSupported();

    void close();

    default AtMessage newAt(UserInfo user) {
        return newAt().fillAt(user);
    }

    default ChainMessage newChain(AbstractMessage... messages) {
        return newChain().fillChain(messages);
    }

    default TextMessage newText(String text) {
        return newText().fillText(text);
    }

    default AudioMessage newAudio(GroupInfo group, InputStream source) throws IOException {
        return newAudio().fillAudio(group, source);
    }

    default ImageMessage newImage(InputStream source) throws IOException {
        return newImage().fillImage(source);
    }

    default ForwardMessage newForwards(ContactInfo group, MessageEntry... entries) {
        return newForwards().fillForwards(group, entries);
    }

    default MessageEntry newMessageEntry(String id, String name, AbstractMessage message, int time) {
        return newMessageEntry().fillMessageEntry(id, name, message, time);
    }

    default QuoteMessage newQuote(AbstractMessage message) {
        return newQuote().fill(message);
    }

    default ServiceMessage newService(String name, Either<JsonObject, String> data) {
        return newService().fillService(name, data);
    }
}
