package com.reader.jetbrains.sources;

public final class BookInfoRule {
    public String bookIdField = "";
    public String titleField = "";
    public String authorField = "";
    public String descField = "";
    public String urlField = "";
    public String coverField = "";

    public BookInfoRule copy() {
        BookInfoRule copy = new BookInfoRule();
        copy.bookIdField = bookIdField;
        copy.titleField = titleField;
        copy.authorField = authorField;
        copy.descField = descField;
        copy.urlField = urlField;
        copy.coverField = coverField;
        return copy;
    }
}
