package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.At;

public class QQAt extends AtMessage implements QQMessage {

    private At at;

    public QQAt(QQEnvironment environment) {
        super(environment);
    }

    protected QQAt(QQEnvironment environment, At at) {
        super(environment);
        this.at = at;
        this.user = environment.getUser("qq.user" + at.getTarget(), true);
    }

    public At getMessage() {
        return at;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        User user = ((QQUser) this.user).getUser();
        if (user == null)
            return;
        at = new At(user.getId());
    }
}
