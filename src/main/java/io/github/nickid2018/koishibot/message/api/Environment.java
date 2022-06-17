package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.MessageSender;

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

    UserInfo getUser(String id, boolean isStranger);

    GroupInfo getGroup(String id);

    String getBotId();

    MessageEventPublisher getEvents();

    default AtMessage newAt(UserInfo user) {
        return newAt().fill(user);
    }

    default ChainMessage newChain(AbstractMessage... messages) {
        return newChain().fill(messages);
    }

    default TextMessage newText(String text) {
        return newText().fillText(text);
    }

    default AudioMessage newAudio(InputStream source) throws IOException {
        return newAudio().fillAudio(source);
    }

    default ImageMessage newImage(InputStream source) throws IOException {
        return newImage().fillImage(source);
    }

    default ForwardMessage newForwards(ContactInfo group, MessageEntry... entries) {
        return newForwards().fill(group, entries);
    }

    default MessageEntry newMessageEntry(String id, String name, AbstractMessage message, int time) {
        return newMessageEntry().fill(id, name, message, time);
    }

    default QuoteMessage newQuote(AbstractMessage message) {
        return newQuote().fill(message);
    }

    MessageSender getMessageSender();
}
