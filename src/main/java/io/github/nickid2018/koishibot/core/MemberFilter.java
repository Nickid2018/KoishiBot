package io.github.nickid2018.koishibot.core;

import net.mamoe.mirai.contact.ContactOrBot;

import java.util.HashMap;
import java.util.Map;

public class MemberFilter {

    private static final Map<Long, Long> USER_REQUEST_TIME = new HashMap<>();
    private static final Map<Long, Integer> USER_REQUEST_FAIL = new HashMap<>();
    private static final Map<Long, Long> USER_BAN_TIME = new HashMap<>();

    private static final int REQUEST_MAX_FAIL = 5;
    private static final long REQUEST_DURATION = 2000;
    private static final long FIRST_BAN = 3600_000;

    public static boolean shouldNotResponse(ContactOrBot member) {
        return shouldNotResponse(member.getId());
    }

    public static boolean shouldNotResponse(long member) {
        long nowTime = System.currentTimeMillis();
        if (USER_BAN_TIME.containsKey(member) && USER_BAN_TIME.get(member) >= nowTime)
            return true;
        if (USER_REQUEST_TIME.containsKey(member) && nowTime - USER_REQUEST_TIME.get(member) <= REQUEST_DURATION) {
            int times = USER_REQUEST_FAIL.getOrDefault(member, 0);
            times++;
            if (times >= REQUEST_MAX_FAIL)
                USER_BAN_TIME.put(member, nowTime + FIRST_BAN);
            USER_REQUEST_FAIL.put(member, times);
            USER_REQUEST_TIME.put(member, nowTime);
            return true;
        }
        USER_REQUEST_FAIL.remove(member);
        USER_BAN_TIME.remove(member);
        return false;
    }

    public static void refreshRequestTime(ContactOrBot member) {
        refreshRequestTime(member.getId());
    }

    public static void refreshRequestTime(long member) {
        USER_REQUEST_TIME.put(member, System.currentTimeMillis());
    }

    static {
        USER_BAN_TIME.put(2854196306L, Long.MAX_VALUE); // QQ小冰
        USER_BAN_TIME.put(2854196310L, Long.MAX_VALUE); // QQ管家
    }
}
