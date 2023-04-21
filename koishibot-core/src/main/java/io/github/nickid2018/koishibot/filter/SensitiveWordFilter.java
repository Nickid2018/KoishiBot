package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class SensitiveWordFilter extends SensitiveFilter {

    private static final List<SingleChar> SINGLE_CHAR_LIST = new ArrayList<>();
    private final static char REPLACE_CHARACTER = '*';
    private final static char[] SKIP_CHARACTERS = new char[]{
            '!', '*', '-', '+', '_', '=', ',', '.'
    };

    public String filter(String text, MutableBoolean filtered) {
        if (SINGLE_CHAR_LIST.size() == 0)
            return text;
        char[] chars = text.toCharArray();
        CharList filteredChars = new CharArrayList();
        int matchSize;
        boolean matchFlag;
        for (int offset = 0; offset < chars.length; offset++) {
            char c = chars[offset];
            SingleChar single = binarySearch(c, SINGLE_CHAR_LIST);
            if (single != null) {
                matchFlag = false;
                matchSize = offset + 1;
                while (matchSize < chars.length) {
                    if (skip(chars[matchSize])) {
                        matchSize++;
                        continue;
                    }
                    if (single.next != null) {
                        single = binarySearch(chars[matchSize], single.next);
                        if (single == null)
                            break;
                        matchSize++;
                    } else {
                        matchFlag = true;
                        break;
                    }
                }
                if (single != null && single.next == null)
                    matchFlag = true;
                if (matchFlag) {
                    offset = matchSize - 1;
                    filteredChars.addElements(filteredChars.size(), "*filtered*".toCharArray());
                    filtered.setValue(true);
                } else
                    filteredChars.add(c);
            } else
                filteredChars.add(c);
        }
        return new String(filteredChars.toCharArray());
    }

    public static void loadWordFromFile(String path) throws IOException {
        File file = new File(path);
        if (file.isFile() && file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8
            ))) {
                loadWords(bufferedReader.lines().collect(Collectors.toList()));
            }
        }
    }

    public static void loadWords(List<String> words) {
        if (words == null)
            return;
        SINGLE_CHAR_LIST.clear();
        for (String word : words) {
            if (word == null)
                continue;
            word = word.trim();
            if (word.isEmpty())
                continue;
            List<SingleChar> now = SINGLE_CHAR_LIST;
            SingleChar singleCharData = null;
            for (char ch : word.toCharArray()) {
                if (singleCharData != null) {
                    if (singleCharData.next == null)
                        singleCharData.next = new ArrayList<>();
                    now = singleCharData.next;
                }
                singleCharData = null;
                for (SingleChar next : now) {
                    if (next.character == ch) {
                        singleCharData = next;
                        break;
                    }
                }
                if (singleCharData == null)
                    now.add(singleCharData = new SingleChar(ch));
            }
        }
        sort(SINGLE_CHAR_LIST);
        SENSITIVE_LOGGER.info("Sensitive Library loaded with {} words.", SINGLE_CHAR_LIST.size());
    }

    private static void sort(List<SingleChar> singleChars) {
        if (singleChars == null)
            return;
        Collections.sort(singleChars);
        for (SingleChar single : singleChars)
            sort(single.next);
    }

    private static boolean skip(char c) {
        for (char c1 : SKIP_CHARACTERS)
            if (c1 == c)
                return true;
        return false;
    }

    private static SingleChar binarySearch(char c, List<SingleChar> singleCharList) {
        int left = 0;
        int right = singleCharList.size() - 1;
        while (left <= right) {
            int key = (left + right) / 2;
            SingleChar single = singleCharList.get(key);
            if (single.character == c)
                return single;
            else if (single.character > c)
                right = key - 1;
            else
                left = key + 1;
        }
        return null;
    }

}
