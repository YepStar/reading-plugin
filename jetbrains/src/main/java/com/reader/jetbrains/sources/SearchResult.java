package com.reader.jetbrains.sources;

public final class SearchResult {
    public final String sourceId;
    public final String bookId;
    public final String title;
    public final String author;
    public final String url;
    public final String description;

    public SearchResult(String sourceId, String bookId, String title, String author, String url, String description) {
        this.sourceId = sourceId;
        this.bookId = bookId;
        this.title = title == null || title.isBlank() ? "未命名" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.url = url == null ? "" : url.trim();
        this.description = description == null ? "" : description.trim();
    }

    @Override
    public String toString() {
        return author.isBlank() ? title : title + " - " + author;
    }
}
