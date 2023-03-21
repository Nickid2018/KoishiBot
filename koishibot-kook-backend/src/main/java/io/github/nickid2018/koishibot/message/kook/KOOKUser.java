package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;

public class KOOKUser extends UserInfo {
    private GuildUser user;

    public KOOKUser(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKUser(Environment environment, GuildUser user) {
        super(environment);
        this.user = user;
        this.name = user.getName();
        this.userId = "kook.user" + user.getId();
        this.isStranger = true;
    }

    public GuildUser getUser() {
        return user;
    }

    public static String getNameInGroup(GuildUser user, GroupInfo group) {
        GuildUser u = ((KOOKTextChannel) group).getChannel().getGuild().getGuildUser(user.getId());
        return u == null ? user.getName() : u.getName();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        KOOKEnvironment environment = (KOOKEnvironment) env;
        user = environment.getUser(userId);
    }
}
