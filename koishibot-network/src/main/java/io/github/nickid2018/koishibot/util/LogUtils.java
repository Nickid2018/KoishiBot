package io.github.nickid2018.koishibot.util;

import org.slf4j.Logger;

public class LogUtils {

    public enum FontColor {
        BLACK("30"),
        RED("31"),
        GREEN("32"),
        YELLOW("33"),
        BLUE("34"),
        PURPLE("35"),
        CYAN("36"),
        WHITE("37");
        private final String code;

        FontColor(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static void info(FontColor color, Logger logger, String message, Object... args) {
        logger.info("\33[0;%sm%s\33[0m".formatted(color.code, message), args);
    }

    public static void error(Logger logger, String message, Throwable throwable) {
        if (throwable == null)
            logger.error("\33[0;31m%s\33[0m".formatted(message));
        else
            logger.error("\33[0;31m%s\33[0m".formatted(message), throwable);
    }
}
