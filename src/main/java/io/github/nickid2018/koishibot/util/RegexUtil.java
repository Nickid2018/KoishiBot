package io.github.nickid2018.koishibot.util;

import java.util.regex.Pattern;

public class RegexUtil {

    public static boolean match(Pattern pattern, String str) {
        return pattern.matcher(str).matches();
    }
}
