package com.reader.jetbrains.model;

import java.util.List;

public final class Book {
    private final String title;
    private final List<Chapter> chapters;

    public Book(String title, List<Chapter> chapters) {
        this.title = title == null || title.isBlank() ? "Untitled Book" : title.trim();
        this.chapters = List.copyOf(chapters);
    }

    public String title() {
        return title;
    }

    public List<Chapter> chapters() {
        return chapters;
    }

    public boolean isEmpty() {
        return chapters.isEmpty();
    }
}
