package io.github.nickid2018.koishibot.filter;

import java.util.List;

public class SingleChar implements Comparable<SingleChar> {

    public final char character;
    public List<SingleChar> next = null;

    public SingleChar(char c) {
        this.character = c;
    }

    @Override
    public int compareTo(SingleChar single) {
        return character - single.character;
    }
}
