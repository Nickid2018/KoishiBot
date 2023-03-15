package io.github.nickid2018.koishibot.backend;

import io.github.nickid2018.koishibot.message.action.*;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.event.QueryResultEvent;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.message.qq.*;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.function.Function;
import java.util.function.Supplier;

public class BackendDataListener extends DataPacketListener {

    public final DataRegistry registry = new DataRegistry();

    public BackendDataListener(Supplier<Environment> environment) {
        Function<Class<? extends SerializableData>, ? extends SerializableData> dataFactory = c -> {
            try {
                return c.getConstructor(Environment.class).newInstance(environment.get());
            } catch (Exception e) {
                return null;
            }
        };

        registry.registerData(QQEnvironment.class, c -> null);

        registry.registerData(QQAt.class, dataFactory);
        registry.registerData(QQAudio.class, dataFactory);
        registry.registerData(QQChain.class, dataFactory);
        registry.registerData(QQForward.class, dataFactory);
        registry.registerData(QQGroup.class, dataFactory);
        registry.registerData(QQImage.class, dataFactory);
        registry.registerData(QQMessageEntry.class, dataFactory);
        registry.registerData(QQMessageSource.class, dataFactory);
        registry.registerData(QQQuote.class, dataFactory);
        registry.registerData(QQService.class, dataFactory);
        registry.registerData(QQText.class, dataFactory);
        registry.registerData(QQUser.class, dataFactory);

        registry.registerData(QueryResultEvent.class, dataFactory);

        registry.registerData(GroupInfoQuery.class, dataFactory);
        registry.registerData(NameInGroupQuery.class, dataFactory);
        registry.registerData(NudgeAction.class, dataFactory);
        registry.registerData(RecallAction.class, dataFactory);
        registry.registerData(SendMessageAction.class, dataFactory);
        registry.registerData(UserInfoQuery.class, dataFactory);
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {
        super.receivePacket(connection, packet);

    }
}
