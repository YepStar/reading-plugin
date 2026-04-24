package com.reader.jetbrains.model;

public final class Chapter {
    private final String title;
    private final String text;

    public Chapter(String title, String text) {
        this.title = title == null || title.isBlank() ? "Untitled" : title.trim();
        this.text = text == null ? "" : text.trim();
    }

    public String title() {
        return title;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return title;
    }
}
