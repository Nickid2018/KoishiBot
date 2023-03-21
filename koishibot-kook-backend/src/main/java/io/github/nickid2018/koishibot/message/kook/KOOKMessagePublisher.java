package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.events.EventHandler;
import io.github.kookybot.events.Listener;
import io.github.kookybot.events.MessageEvent;
import io.github.kookybot.events.channel.ChannelMessageEvent;
import io.github.kookybot.events.guild.GuildUserJoinEvent;
import io.github.nickid2018.koishibot.backend.Main;
import io.github.nickid2018.koishibot.message.event.OnGroupMessageEvent;
import io.github.nickid2018.koishibot.message.event.OnMemberAddEvent;

public class KOOKMessagePublisher {

    private final KOOKEnvironment environment;

    public KOOKMessagePublisher(KOOKEnvironment environment) {
        this.environment = environment;
        subscribeGroupMessage();
        subscribeNewMemberAdd();
    }

    public void subscribeGroupMessage() {
        environment.getKookClient().getEventManager().addClassListener(new ChannelMessageListener());
    }

    public class ChannelMessageListener implements Listener {

        @EventHandler
        public void onChannelMessage(ChannelMessageEvent event) {
            KOOKTextChannel groupInfo = new KOOKTextChannel(environment, event.getChannel());
            KOOKUser userInfo = new KOOKUser(environment, event.getSender());

            if (event.getEventType() == MessageEvent.EventType.PLAIN_TEXT ||
                    event.getEventType() == MessageEvent.EventType.MARKDOWN) {
                KOOKChain chain = new KOOKChain(environment, KOOKMessageData.fromChannelMessage(event));
                OnGroupMessageEvent groupMessageEvent = new OnGroupMessageEvent(environment);
                groupMessageEvent.group = groupInfo;
                groupMessageEvent.user = userInfo;
                groupMessageEvent.message = chain;
                groupMessageEvent.time = Long.parseLong(event.getTimestamp());
                environment.getConnection().sendPacket(groupMessageEvent);
                Main.LOGGER.debug("Sent group message event: " + event);
            }
        }
    }

    public void subscribeNewMemberAdd() {
        environment.getKookClient().getEventManager().addClassListener(new NewMemberListener());
    }

    public class NewMemberListener implements Listener {

        @EventHandler
        public void onChannelJoin(GuildUserJoinEvent event) {
            if (event.getGuild().getDefaultChannel() == null)
                return;
            KOOKTextChannel groupInfo = new KOOKTextChannel(environment, event.getGuild().getDefaultChannel());
            KOOKUser userInfo = new KOOKUser(environment, event.getUser());
            OnMemberAddEvent memberAddEvent = new OnMemberAddEvent(environment);
            memberAddEvent.group = groupInfo;
            memberAddEvent.user = userInfo;
            environment.getConnection().sendPacket(memberAddEvent);
            Main.LOGGER.debug("Sent member join event: " + event);
        }
    }
}
