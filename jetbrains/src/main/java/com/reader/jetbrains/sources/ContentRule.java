package com.reader.jetbrains.sources;

public final class ContentRule {
    public String url = "";
    public String dataPath = "";
    public String selector = "";

    public ContentRule copy() {
        ContentRule copy = new ContentRule();
        copy.url = url;
        copy.dataPath = dataPath;
        copy.selector = selector;
        return copy;
    }
}
