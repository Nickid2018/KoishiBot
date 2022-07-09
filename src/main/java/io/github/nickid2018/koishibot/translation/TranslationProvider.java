package io.github.nickid2018.koishibot.translation;

import java.io.IOException;

public interface TranslationProvider {

    String translate(String text, String from, String to) throws IOException;
}
