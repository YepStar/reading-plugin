package com.reader.jetbrains.sources;

public final class RemoteChapter {
    public final String title;
    public final String url;
    public final String itemId;

    public RemoteChapter(String title, String url, String itemId) {
        this.title = title == null || title.isBlank() ? "未命名章节" : title.trim();
        this.url = url == null ? "" : url.trim();
        this.itemId = itemId == null ? "" : itemId.trim();
    }

    @Override
    public String toString() {
        return title;
    }
}
