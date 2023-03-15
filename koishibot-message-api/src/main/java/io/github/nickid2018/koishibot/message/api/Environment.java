package io.github.nickid2018.koishibot.message.api;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Environment implements SerializableData {

    private final Connection connection;
    private String botID;
    private String environmentName;
    private String environmentUserPrefix;
    private boolean forwardMessageSupported;
    private boolean audioSupported;
    private boolean audioToFriendSupported;
    private boolean quoteSupported;

    public Environment(Connection connection) {
        this.connection = connection;
    }

    public UserInfo getUser(String id, boolean isStranger) {
        UserInfoQuery query = new UserInfoQuery(this);
        query.id = id;
        query.isStranger = isStranger;
        Future<byte[]> future = getListener().queryData(query.queryId);
        connection.sendPacket(query);
        try {
            return UserInfoQuery.fromBytes(connection.getRegistry(), future.get());
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public GroupInfo getGroup(String id) {
        GroupInfoQuery query = new GroupInfoQuery(this);
        query.id = id;
        Future<byte[]> future = getListener().queryData(query.queryId);
        connection.sendPacket(query);
        try {
            return GroupInfoQuery.fromBytes(connection.getRegistry(), future.get());
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public String getBotId() {
        return botID;
    }

    public boolean forwardMessageSupported() {
        return forwardMessageSupported;
    }

    public boolean audioSupported() {
        return audioSupported;
    }

    public boolean audioToFriendSupported() {
        return audioToFriendSupported;
    }

    public boolean quoteSupported() {
        return quoteSupported;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getEnvironmentUserPrefix() {
        return environmentUserPrefix;
    }

//    Future<File[]> parseAudioFile(String suffix, URL url) throws IOException;

    public void close() {

    }

    public Connection getConnection() {
        return connection;
    }

    public DataPacketListener getListener() {
        return (DataPacketListener) connection.getListener();
    }

    public AtMessage newAt(UserInfo user) {
        return new AtMessage(this).fillAt(user);
    }

    public ChainMessage newChain(AbstractMessage... messages) {
        return new ChainMessage(this).fillChain(messages);
    }

    public TextMessage newText(String text) {
        return new TextMessage(this).fillText(text);
    }

    public AudioMessage newAudio(GroupInfo group, URL source) {
        return new AudioMessage(this).fillAudio(group, source);
    }

    public ImageMessage newImage(URL source) {
        return new ImageMessage(this).fillImage(source);
    }

    public ForwardMessage newForwards(ContactInfo group, MessageEntry... entries) {
        return new ForwardMessage(this).fillForwards(group, entries);
    }

    public MessageEntry newMessageEntry(String id, String name, AbstractMessage message, int time) {
        return new MessageEntry(this).fillMessageEntry(id, name, message, time);
    }

    public QuoteMessage newQuote(ChainMessage message) {
        return new QuoteMessage(this).fill(message);
    }

    public ServiceMessage newService(String name, Either<JsonObject, String> data) {
        return new ServiceMessage(this).fillService(name, data);
    }

    @Override
    public void read(ByteData buf) {
        botID = buf.readString();
        environmentName = buf.readString();
        environmentUserPrefix = buf.readString();
        forwardMessageSupported = buf.readBoolean();
        audioSupported = buf.readBoolean();
        audioToFriendSupported = buf.readBoolean();
        quoteSupported = buf.readBoolean();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(botID);
        buf.writeString(environmentName);
        buf.writeString(environmentUserPrefix);
        buf.writeBoolean(forwardMessageSupported);
        buf.writeBoolean(audioSupported);
        buf.writeBoolean(audioToFriendSupported);
        buf.writeBoolean(quoteSupported);
    }
}
