package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.At;

public class QQAt extends AtMessage {

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
        at = new At(((QQUser) user).getUser().getId());
    }
}
