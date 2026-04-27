package com.reader.jetbrains.sources;

public final class BookSource {
    public String id = "";
    public String name = "";
    public String type = "html";
    public String baseUrl = "";
    public boolean enabled = true;
    public SearchRule search = new SearchRule();
    public CatalogRule catalog = new CatalogRule();
    public ContentRule content = new ContentRule();
    public BookInfoRule info = new BookInfoRule();
    public java.util.Map<String, ProcessorRule> processor = new java.util.LinkedHashMap<>();

    public BookSource copy() {
        BookSource copy = new BookSource();
        copy.id = id;
        copy.name = name;
        copy.type = type;
        copy.baseUrl = baseUrl;
        copy.enabled = enabled;
        copy.search = search.copy();
        copy.catalog = catalog.copy();
        copy.content = content.copy();
        copy.info = info.copy();
        copy.processor = new java.util.LinkedHashMap<>();
        processor.forEach((key, value) -> copy.processor.put(key, value.copy()));
        return copy;
    }

    @Override
    public String toString() {
        return (enabled ? "" : "[停用] ") + (name == null || name.isBlank() ? id : name);
    }
}
