package com.reader.jetbrains.parser;

import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.model.Chapter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class BookParser {
    public static final String DEFAULT_CHAPTER_REGEX = "(?m)^\\s*(第\\s*[0-9一二三四五六七八九十百千万零〇两]+\\s*[章节卷回部篇].*)$";

    private BookParser() {
    }

    public static Book parse(Path path, String chapterRegex, Charset charset) throws IOException {
        String name = fileTitle(path);
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".epub")) {
            return parseEpub(path, name);
        }
        return parseText(path, name, chapterRegex, charset == null ? StandardCharsets.UTF_8 : charset);
    }

    public static Book fromWebText(String title, String text, String chapterRegex) {
        return new Book(title, splitByRegex(text, chapterRegex));
    }

    private static Book parseText(Path path, String title, String chapterRegex, Charset charset) throws IOException {
        String text = Files.readString(path, charset);
        return new Book(title, splitByRegex(text, chapterRegex));
    }

    private static Book parseEpub(Path path, String fallbackTitle) throws IOException {
        List<EpubDocument> documents = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(BookParser::isReadableEpubDocument)
                    .forEach(entry -> documents.add(readEpubDocument(zipFile, entry)));
        }

        documents.sort(Comparator.comparing(EpubDocument::name));
        List<Chapter> chapters = documents.stream()
                .map(document -> new Chapter(document.title(), document.text()))
                .filter(chapter -> !chapter.text().isBlank())
                .toList();

        if (chapters.isEmpty()) {
            chapters = List.of(new Chapter(fallbackTitle, ""));
        }
        return new Book(fallbackTitle, chapters);
    }

    private static boolean isReadableEpubDocument(ZipEntry entry) {
        String name = entry.getName().toLowerCase();
        return name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm");
    }

    private static EpubDocument readEpubDocument(ZipFile zipFile, ZipEntry entry) {
        try {
            String html = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            String title = firstNonBlank(extractTitle(html), fileTitle(Path.of(entry.getName())));
            return new EpubDocument(entry.getName(), title, htmlToPlainText(html));
        } catch (IOException ignored) {
            return new EpubDocument(entry.getName(), fileTitle(Path.of(entry.getName())), "");
        }
    }

    private static List<Chapter> splitByRegex(String text, String chapterRegex) {
        String normalizedText = normalizeText(text);
        Pattern pattern = Pattern.compile(
                chapterRegex == null || chapterRegex.isBlank() ? DEFAULT_CHAPTER_REGEX : chapterRegex,
                Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(normalizedText);
        List<Chapter> chapters = new ArrayList<>();
        int bodyStart = 0;
        String currentTitle = "Start";

        while (matcher.find()) {
            if (matcher.start() > bodyStart) {
                String body = normalizedText.substring(bodyStart, matcher.start()).trim();
                if (!body.isBlank()) {
                    chapters.add(new Chapter(currentTitle, body));
                }
            }
            currentTitle = firstMatchedGroup(matcher);
            bodyStart = matcher.end();
        }

        String tail = normalizedText.substring(Math.min(bodyStart, normalizedText.length())).trim();
        if (!tail.isBlank()) {
            chapters.add(new Chapter(currentTitle, tail));
        }
        if (chapters.isEmpty()) {
            chapters.add(new Chapter("Full Text", normalizedText));
        }
        return chapters;
    }

    private static String firstMatchedGroup(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String value = matcher.group(i);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return matcher.group().trim();
    }

    public static String htmlToPlainText(String html) {
        String text = html
                .replaceAll("(?is)<(script|style|svg|canvas|noscript|iframe)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)</div\\s*>", "\n")
                .replaceAll("(?i)</h[1-6]\\s*>", "\n")
                .replaceAll("(?is)<[^>]+>", " ");
        return decodeEntities(normalizeText(text));
    }

    private static String extractTitle(String html) {
        Matcher matcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);
        return matcher.find() ? htmlToPlainText(matcher.group(1)).trim() : "";
    }

    private static String normalizeText(String text) {
        return decodeEntities(text == null ? "" : text)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String decodeEntities(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String fileTitle(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private record EpubDocument(String name, String title, String text) {
    }
}
