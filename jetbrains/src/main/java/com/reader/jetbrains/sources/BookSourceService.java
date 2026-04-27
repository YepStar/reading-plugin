package com.reader.jetbrains.sources;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.reader.jetbrains.sources.json.SimpleJson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@State(name = "ReaderBookSourceState", storages = @Storage("reader-book-sources.xml"))
public final class BookSourceService implements PersistentStateComponent<BookSourceService.StateData> {
    public static final class StateData {
        public List<BookSource> sources = new ArrayList<>();
        public String selectedSourceId = "";
    }

    private StateData state = new StateData();

    public synchronized List<BookSource> sources() {
        ensureDefaults();
        return state.sources.stream().map(BookSource::copy).toList();
    }

    public synchronized List<BookSource> enabledSources() {
        return sources().stream().filter(source -> source.enabled).toList();
    }

    public synchronized Optional<BookSource> source(String id) {
        ensureDefaults();
        return state.sources.stream()
                .filter(source -> source.id.equals(id))
                .findFirst()
                .map(BookSource::copy);
    }

    public synchronized BookSource selectedSource() {
        ensureDefaults();
        if (state.selectedSourceId != null && !state.selectedSourceId.isBlank()) {
            Optional<BookSource> selected = source(state.selectedSourceId);
            if (selected.isPresent()) {
                return selected.get();
            }
        }
        return enabledSources().stream().findFirst().orElseGet(() -> sources().get(0));
    }

    public synchronized void select(String id) {
        if (id != null && !id.isBlank()) {
            state.selectedSourceId = id;
        }
    }

    public synchronized void save(BookSource source) {
        ensureDefaults();
        if (source.id == null || source.id.isBlank()) {
            source.id = "custom-" + System.currentTimeMillis();
        }
        for (int i = 0; i < state.sources.size(); i++) {
            if (state.sources.get(i).id.equals(source.id)) {
                state.sources.set(i, source.copy());
                return;
            }
        }
        state.sources.add(source.copy());
    }

    public synchronized void remove(String id) {
        ensureDefaults();
        state.sources.removeIf(source -> source.id.equals(id));
        if (state.sources.isEmpty()) {
            resetDefaults();
        }
        if (id != null && id.equals(state.selectedSourceId)) {
            state.selectedSourceId = "";
        }
    }

    public synchronized void resetDefaults() {
        state.sources = defaultSources();
        state.selectedSourceId = state.sources.isEmpty() ? "" : state.sources.get(0).id;
    }

    public synchronized String toJson(BookSource source) {
        return SimpleJson.stringify(toMap(source));
    }

    @SuppressWarnings("unchecked")
    public synchronized BookSource fromJson(String json) {
        Object parsed = SimpleJson.parse(json);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("书源 JSON 必须是对象");
        }
        return fromMap((Map<String, Object>) map);
    }

    @Override
    public synchronized @NotNull StateData getState() {
        ensureDefaults();
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull StateData state) {
        this.state = state;
    }

    private void ensureDefaults() {
        if (state.sources == null || state.sources.isEmpty()) {
            resetDefaults();
        }
        if (state.selectedSourceId == null || state.selectedSourceId.isBlank()) {
            state.selectedSourceId = state.sources.get(0).id;
        }
    }

    private static List<BookSource> defaultSources() {
        List<BookSource> sources = new ArrayList<>();
        sources.add(htmlSource(
                "xbiqugu",
                "书香小说",
                "http://www.xbiqugu.net/",
                "http://www.xbiqugu.net/modules/article/waps.php?searchkey=${key}",
                ".grid tr:not(:first-child)",
                "a",
                "a",
                "${bookUrl}",
                "#list dl dd",
                "a",
                "a",
                "${chapterUrl}",
                "#content"
        ));
        sources.add(htmlSource(
                "xbiquzw",
                "笔尖小说",
                "http://www.xbiquzw.com/",
                "http://www.xbiquzw.com/modules/article/soshu.php?searchkey=${key}",
                ".grid tr:not(:first-child)",
                "a",
                "a",
                "${bookUrl}",
                "#list dl dd",
                "a",
                "a",
                "${chapterUrl}",
                "#content"
        ));
        BookSource kuwo = new BookSource();
        kuwo.id = "kuwo";
        kuwo.name = "酷我小说";
        kuwo.type = "json";
        kuwo.baseUrl = "http://appi.kuwo.cn/";
        kuwo.search.url = "http://appi.kuwo.cn/novels/api/book/search?keyword=${key}&pi=${page}&ps=30";
        kuwo.search.dataPath = "$.data[*]";
        kuwo.info.bookIdField = "book_id";
        kuwo.info.titleField = "title";
        kuwo.info.authorField = "author_name";
        kuwo.info.descField = "intro";
        kuwo.info.coverField = "cover_url";
        kuwo.catalog.url = "http://appi.kuwo.cn/novels/api/book/${bookId}/chapters?paging=0";
        kuwo.catalog.dataPath = "$.data[*]";
        kuwo.catalog.itemIdField = "chapter_id";
        kuwo.catalog.itemTitleField = "chapter_title";
        kuwo.content.url = "http://appi.kuwo.cn/novels/api/book/${bookId}/chapters/${itemId}";
        kuwo.content.dataPath = "$.data.content";
        sources.add(kuwo);
        return sources;
    }

    private static BookSource htmlSource(String id,
                                         String name,
                                         String baseUrl,
                                         String searchUrl,
                                         String searchList,
                                         String searchTitle,
                                         String searchHref,
                                         String catalogUrl,
                                         String catalogList,
                                         String catalogTitle,
                                         String catalogHref,
                                         String contentUrl,
                                         String contentSelector) {
        BookSource source = new BookSource();
        source.id = id;
        source.name = name;
        source.type = "html";
        source.baseUrl = baseUrl;
        source.search.url = searchUrl;
        source.search.listSelector = searchList;
        source.search.titleSelector = searchTitle;
        source.search.urlSelector = searchHref;
        source.catalog.url = catalogUrl;
        source.catalog.listSelector = catalogList;
        source.catalog.titleSelector = catalogTitle;
        source.catalog.urlSelector = catalogHref;
        source.content.url = contentUrl;
        source.content.selector = contentSelector;
        return source;
    }

    private static Map<String, Object> toMap(BookSource source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", source.id);
        map.put("name", source.name);
        map.put("type", source.type);
        map.put("baseUrl", source.baseUrl);
        map.put("enabled", source.enabled);
        map.put("search", ruleMap(source.search.url, source.search.dataPath, source.search.listSelector, source.search.titleSelector, source.search.urlSelector));
        Map<String, Object> catalog = ruleMap(source.catalog.url, source.catalog.dataPath, source.catalog.listSelector, source.catalog.titleSelector, source.catalog.urlSelector);
        catalog.put("itemIdField", source.catalog.itemIdField);
        catalog.put("itemTitleField", source.catalog.itemTitleField);
        catalog.put("itemUrlField", source.catalog.itemUrlField);
        map.put("catalog", catalog);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("url", source.content.url);
        content.put("dataPath", source.content.dataPath);
        content.put("selector", source.content.selector);
        map.put("content", content);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("bookIdField", source.info.bookIdField);
        info.put("titleField", source.info.titleField);
        info.put("authorField", source.info.authorField);
        info.put("descField", source.info.descField);
        info.put("urlField", source.info.urlField);
        info.put("coverField", source.info.coverField);
        map.put("info", info);
        return map;
    }

    private static Map<String, Object> ruleMap(String url, String dataPath, String listSelector, String titleSelector, String urlSelector) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("url", url);
        map.put("dataPath", dataPath);
        map.put("listSelector", listSelector);
        map.put("titleSelector", titleSelector);
        map.put("urlSelector", urlSelector);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static BookSource fromMap(Map<String, Object> map) {
        BookSource source = new BookSource();
        source.id = text(map.get("id"));
        source.name = text(map.get("name"));
        source.type = text(map.getOrDefault("type", "html"));
        source.baseUrl = text(map.get("baseUrl"));
        source.enabled = !"false".equalsIgnoreCase(text(map.get("enabled")));
        if (map.get("search") instanceof Map<?, ?> search) {
            readSearch(source.search, (Map<String, Object>) search);
        }
        if (map.get("catalog") instanceof Map<?, ?> catalog) {
            readCatalog(source.catalog, (Map<String, Object>) catalog);
        }
        if (map.get("content") instanceof Map<?, ?> content) {
            source.content.url = text(content.get("url"));
            source.content.dataPath = text(content.get("dataPath"));
            source.content.selector = text(content.get("selector"));
        }
        if (map.get("info") instanceof Map<?, ?> info) {
            source.info.bookIdField = text(info.get("bookIdField"));
            source.info.titleField = text(info.get("titleField"));
            source.info.authorField = text(info.get("authorField"));
            source.info.descField = text(info.get("descField"));
            source.info.urlField = text(info.get("urlField"));
            source.info.coverField = text(info.get("coverField"));
        }
        return source;
    }

    private static void readSearch(SearchRule rule, Map<String, Object> map) {
        rule.url = text(map.get("url"));
        rule.dataPath = text(map.get("dataPath"));
        rule.listSelector = text(map.get("listSelector"));
        rule.titleSelector = text(map.get("titleSelector"));
        rule.urlSelector = text(map.get("urlSelector"));
    }

    private static void readCatalog(CatalogRule rule, Map<String, Object> map) {
        rule.url = text(map.get("url"));
        rule.dataPath = text(map.get("dataPath"));
        rule.listSelector = text(map.get("listSelector"));
        rule.titleSelector = text(map.get("titleSelector"));
        rule.urlSelector = text(map.get("urlSelector"));
        rule.itemIdField = text(map.get("itemIdField"));
        rule.itemTitleField = text(map.get("itemTitleField"));
        rule.itemUrlField = text(map.get("itemUrlField"));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
