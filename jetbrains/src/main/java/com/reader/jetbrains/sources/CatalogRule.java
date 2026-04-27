package com.reader.jetbrains.sources;

public final class CatalogRule {
    public String url = "";
    public String dataPath = "";
    public String listSelector = "";
    public String titleSelector = "";
    public String urlSelector = "";
    public String itemIdField = "";
    public String itemTitleField = "";
    public String itemUrlField = "";

    public CatalogRule copy() {
        CatalogRule copy = new CatalogRule();
        copy.url = url;
        copy.dataPath = dataPath;
        copy.listSelector = listSelector;
        copy.titleSelector = titleSelector;
        copy.urlSelector = urlSelector;
        copy.itemIdField = itemIdField;
        copy.itemTitleField = itemTitleField;
        copy.itemUrlField = itemUrlField;
        return copy;
    }
}
