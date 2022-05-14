package io.github.nickid2018.koishibot.translation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface TranslationProvider {

    String translate(String text, String from, String to) throws IOException;
}
