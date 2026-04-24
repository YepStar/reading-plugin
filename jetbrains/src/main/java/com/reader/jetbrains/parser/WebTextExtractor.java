package com.reader.jetbrains.parser;

import com.reader.jetbrains.model.Book;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WebTextExtractor {
    private static final Pattern MAIN_PATTERN = Pattern.compile(
            "(?is)<(article|main)[^>]*>(.*?)</\\1>|<div[^>]+(?:id|class)=[\"'][^\"']*(?:content|chapter|article|read|book)[^\"']*[\"'][^>]*>(.*?)</div>"
    );

    private WebTextExtractor() {
    }

    public static Book extract(String url, String chapterRegex) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 Reader JetBrains Plugin")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String html = response.body();
        String title = extractTitle(html, url);
        String body = bestReadableHtml(html);
        return BookParser.fromWebText(title, BookParser.htmlToPlainText(body), chapterRegex);
    }

    public static Book extractFromHtml(String url, String title, String html, String chapterRegex) {
        String readableTitle = title == null || title.isBlank() ? extractTitle(html, url) : title;
        String body = bestReadableHtml(html == null ? "" : html);
        return BookParser.fromWebText(readableTitle, BookParser.htmlToPlainText(body), chapterRegex);
    }

    private static String bestReadableHtml(String html) {
        Matcher matcher = MAIN_PATTERN.matcher(html);
        String best = "";
        while (matcher.find()) {
            String candidate = firstNonBlank(matcher.group(2), matcher.group(3));
            if (plainLength(candidate) > plainLength(best)) {
                best = candidate;
            }
        }
        return best.isBlank() ? html : best;
    }

    private static int plainLength(String html) {
        return BookParser.htmlToPlainText(html == null ? "" : html).length();
    }

    private static String extractTitle(String html, String fallback) {
        Matcher matcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);
        if (!matcher.find()) {
            return fallback;
        }
        String title = BookParser.htmlToPlainText(matcher.group(1)).replaceAll("\\s+", " ").trim();
        return title.isBlank() ? fallback : title;
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
