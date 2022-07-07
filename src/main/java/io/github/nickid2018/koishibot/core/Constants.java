package io.github.nickid2018.koishibot.core;

import java.util.Calendar;

public class Constants {

    public static final int TIME_OF_514;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.MAY, 14, 5, 14, 11);
        TIME_OF_514 = Math.toIntExact(calendar.getTimeInMillis() / 1000);
    }
}
