package com.reader.jetbrains.sources;

public final class SearchRule {
    public String url = "";
    public String method = "GET";
    public String dataPath = "";
    public String listSelector = "";
    public String titleSelector = "";
    public String urlSelector = "";

    public SearchRule copy() {
        SearchRule copy = new SearchRule();
        copy.url = url;
        copy.method = method;
        copy.dataPath = dataPath;
        copy.listSelector = listSelector;
        copy.titleSelector = titleSelector;
        copy.urlSelector = urlSelector;
        return copy;
    }
}
