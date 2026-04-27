package com.reader.jetbrains.sources;

import com.reader.jetbrains.sources.json.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookSourceClient {
    private static final Pattern LINK_PATTERN = Pattern.compile("<a\\b[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<SearchResult> search(BookSource source, String keyword) throws IOException, InterruptedException {
        String body = get(render(source.search.url, source, keyword, null, null));
        if ("json".equalsIgnoreCase(source.type)) {
            return searchJson(source, body);
        }
        return searchHtml(source, body);
    }

    public List<RemoteChapter> catalog(BookSource source, SearchResult result) throws IOException, InterruptedException {
        String body = get(render(source.catalog.url, source, null, result, null));
        if ("json".equalsIgnoreCase(source.type)) {
            return catalogJson(source, result, body);
        }
        return catalogHtml(source, body, result.url);
    }

    public String chapterContent(BookSource source, SearchResult result, RemoteChapter chapter) throws IOException, InterruptedException {
        String body = get(render(source.content.url, source, null, result, chapter));
        if ("json".equalsIgnoreCase(source.type)) {
            Object value = SimpleJson.path(SimpleJson.parse(body), source.content.dataPath);
            return cleanText(value == null ? "" : String.valueOf(value));
        }
        String block = extractBySelector(body, source.content.selector);
        return cleanText(stripTags(block.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")));
    }

    private List<SearchResult> searchJson(BookSource source, String body) {
        Object items = SimpleJson.path(SimpleJson.parse(body), source.search.dataPath);
        if (!(items instanceof List<?> list)) {
            return List.of();
        }
        List<SearchResult> results = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String bookId = processed(source, "bookId", map, source.info.bookIdField);
            String title = firstNonBlank(
                    SimpleJson.stringField(item, source.info.titleField),
                    SimpleJson.stringField(item, "articlename"),
                    SimpleJson.stringField(item, "book_name"),
                    SimpleJson.stringField(item, "name"),
                    SimpleJson.stringField(item, "title")
            );
            String author = SimpleJson.stringField(item, source.info.authorField);
            String desc = SimpleJson.stringField(item, source.info.descField);
            String url = SimpleJson.stringField(item, source.info.urlField);
            results.add(new SearchResult(source.id, bookId, title, author, url, desc));
        }
        return results;
    }

    private List<SearchResult> searchHtml(BookSource source, String body) {
        List<String> rows = htmlRows(body, source.search.listSelector);
        List<SearchResult> results = new ArrayList<>();
        for (String row : rows) {
            Link link = firstLink(row);
            if (link == null || link.title.isBlank()) {
                continue;
            }
            results.add(new SearchResult(source.id, "", link.title, "", absolute(source.baseUrl, link.url), ""));
        }
        return results;
    }

    private List<RemoteChapter> catalogJson(BookSource source, SearchResult result, String body) {
        Object items = SimpleJson.path(SimpleJson.parse(body), source.catalog.dataPath);
        if (!(items instanceof List<?> list)) {
            return List.of();
        }
        List<RemoteChapter> chapters = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String title = firstNonBlank(
                        SimpleJson.stringField(item, source.catalog.itemTitleField),
                        SimpleJson.stringField(item, "chapter_title"),
                        SimpleJson.stringField(item, "chaptername"),
                        SimpleJson.stringField(item, "chapterTitle"),
                        SimpleJson.stringField(item, "title"),
                        SimpleJson.stringField(item, "name")
                );
                String itemId = processed(source, "itemId", map, source.catalog.itemIdField);
                String url = source.catalog.itemUrlField == null || source.catalog.itemUrlField.isBlank()
                        ? ""
                        : SimpleJson.stringField(item, source.catalog.itemUrlField);
                chapters.add(new RemoteChapter(title, url, itemId));
                continue;
            }
            String title = item == null ? "" : String.valueOf(item);
            if (title.isBlank()) {
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("chapterTitle", title);
            values.put("title", title);
            chapters.add(new RemoteChapter(title, "", processed(source, "itemId", values, "chapterTitle")));
        }
        return chapters;
    }

    private List<RemoteChapter> catalogHtml(BookSource source, String body, String bookUrl) {
        List<String> rows = htmlRows(body, source.catalog.listSelector);
        List<RemoteChapter> chapters = new ArrayList<>();
        for (String row : rows) {
            Link link = firstLink(row);
            if (link == null || link.title.isBlank()) {
                continue;
            }
            chapters.add(new RemoteChapter(link.title, absolute(bookUrl.isBlank() ? source.baseUrl : bookUrl, link.url), ""));
        }
        return chapters;
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 Reader-plugin-yip")
                .header("Accept", "text/html,application/json,*/*")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    private static String render(String template, BookSource source, String keyword, SearchResult result, RemoteChapter chapter) {
        String rendered = template == null ? "" : template;
        rendered = rendered.replace("${key}", encode(keyword == null ? "" : keyword));
        rendered = rendered.replace("${page}", "1");
        rendered = rendered.replace("${bookId}", encode(result == null ? "" : result.bookId));
        rendered = rendered.replace("${bookUrl}", result == null ? "" : result.url);
        rendered = rendered.replace("${itemId}", encode(chapter == null ? "" : chapter.itemId));
        rendered = rendered.replace("${chapterUrl}", chapter == null ? "" : chapter.url);
        return absolute(source.baseUrl, rendered);
    }

    private static String processed(BookSource source, String key, Map<?, ?> values, String fallbackField) {
        ProcessorRule rule = source.processor == null ? null : source.processor.get(key);
        String value = "";
        if (rule != null && rule.from != null && !rule.from.isBlank()) {
            value = value(values, rule.from);
        }
        if (value.isBlank()) {
            value = value(values, fallbackField);
        }
        if (rule == null || rule.regex == null || rule.regex.isBlank()) {
            return value;
        }
        return value.replaceFirst(rule.regex, rule.replace == null || rule.replace.isBlank() ? "$1" : rule.replace);
    }

    private static String value(Map<?, ?> values, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        Object direct = values.get(key);
        if (direct != null) {
            return String.valueOf(direct);
        }
        return switch (key) {
            case "chapterTitle" -> firstNonBlank(
                    string(values, "chapterTitle"),
                    string(values, "chapter_title"),
                    string(values, "chaptername"),
                    string(values, "title"),
                    string(values, "name")
            );
            case "chapterUrl" -> firstNonBlank(string(values, "url"), string(values, "chapter_url"), string(values, "chapterurl"));
            default -> "";
        };
    }

    private static String string(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static List<String> htmlRows(String html, String selector) {
        if (selector == null || selector.isBlank()) {
            return List.of(html);
        }
        if (selector.contains("tr")) {
            return matchAll(html, Pattern.compile("<tr\\b[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        }
        if (selector.contains("dd")) {
            String block = selector.startsWith("#") ? extractBySelector(html, selector.substring(0, selector.indexOf(' '))) : html;
            return matchAll(block, Pattern.compile("<dd\\b[^>]*>(.*?)</dd>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        }
        return List.of(extractBySelector(html, selector));
    }

    private static List<String> matchAll(String html, Pattern pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    private static Link firstLink(String html) {
        Matcher matcher = LINK_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return new Link(matcher.group(1), cleanText(stripTags(matcher.group(2))));
    }

    private static String extractBySelector(String html, String selector) {
        if (selector == null || selector.isBlank()) {
            return html;
        }
        if (selector.startsWith("#")) {
            String id = Pattern.quote(selector.substring(1));
            Pattern pattern = Pattern.compile("<([a-zA-Z0-9]+)\\b[^>]*id=[\"']" + id + "[\"'][^>]*>(.*?)</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(2);
            }
        }
        return html;
    }

    private static String stripTags(String html) {
        return html.replaceAll("(?is)<script\\b.*?</script>", "")
                .replaceAll("(?is)<style\\b.*?</style>", "")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");
    }

    private static String cleanText(String text) {
        return text.replace("\r", "\n")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String absolute(String base, String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        URI uri = URI.create(url);
        if (uri.isAbsolute()) {
            return url;
        }
        return URI.create(base == null || base.isBlank() ? "http://localhost/" : base).resolve(url).toString();
    }

    private record Link(String url, String title) {
    }
}
