package com.reader.jetbrains.sources;

public final class ProcessorRule {
    public String from = "";
    public String regex = "";
    public String replace = "$1";

    public ProcessorRule copy() {
        ProcessorRule copy = new ProcessorRule();
        copy.from = from;
        copy.regex = regex;
        copy.replace = replace;
        return copy;
    }
}
