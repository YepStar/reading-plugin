package com.reader.jetbrains.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.model.Chapter;
import com.reader.jetbrains.parser.BookParser;
import com.reader.jetbrains.sources.RemoteChapter;
import com.reader.jetbrains.sources.SearchResult;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
@State(name = "ReaderPluginYipState", storages = @Storage("reader-plugin-yip.xml"))
public final class ReaderStateService implements PersistentStateComponent<ReaderStateService.StateData> {
    public static final class StateData {
        public String readerType = "";
        public String localBookPath = "";
        public String localBookRegex = BookParser.DEFAULT_CHAPTER_REGEX;
        public String localBookCharset = StandardCharsets.UTF_8.name();
        public int chapterIndex;
        public int hintScrollValue;
        public String lastPlatformUrl = "https://fanqienovel.com/";
        public String remoteSourceId = "";
        public String remoteBookId = "";
        public String remoteBookTitle = "";
        public String remoteBookAuthor = "";
        public String remoteBookUrl = "";
        public String remoteBookDescription = "";
        public List<String> remoteChapterTitles = new ArrayList<>();
        public List<String> remoteChapterUrls = new ArrayList<>();
        public List<String> remoteChapterItemIds = new ArrayList<>();
        public List<String> remoteChapterTexts = new ArrayList<>();
    }

    private StateData state = new StateData();
    private Book book;
    private String remoteSourceId = "";
    private SearchResult remoteBook;
    private List<RemoteChapter> remoteChapters = List.of();
    private boolean hintVisible;
    private boolean autoNextRunning;

    public synchronized void load(Book book) {
        this.book = book;
        this.remoteSourceId = "";
        this.remoteBook = null;
        this.remoteChapters = List.of();
        state.readerType = "memory";
        clearRemoteState();
        state.chapterIndex = 0;
        state.hintScrollValue = 0;
    }

    public synchronized void loadRemote(String sourceId,
                                        SearchResult remoteBook,
                                        List<RemoteChapter> chapters,
                                        int chapterIndex,
                                        String chapterText) {
        this.remoteSourceId = sourceId == null ? "" : sourceId;
        this.remoteBook = remoteBook;
        this.remoteChapters = List.copyOf(chapters);
        List<Chapter> localChapters = new ArrayList<>();
        List<String> loadedTexts = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            RemoteChapter chapter = chapters.get(i);
            String text = i == chapterIndex ? chapterText : "";
            localChapters.add(new Chapter(chapter.title, text));
            loadedTexts.add(text);
        }
        this.book = new Book(remoteBook == null ? "在线小说" : remoteBook.title, localChapters);
        state.chapterIndex = Math.max(0, Math.min(chapterIndex, Math.max(0, chapters.size() - 1)));
        state.hintScrollValue = 0;
        state.readerType = "remote";
        state.remoteSourceId = this.remoteSourceId;
        state.remoteBookId = remoteBook == null ? "" : remoteBook.bookId;
        state.remoteBookTitle = remoteBook == null ? "在线小说" : remoteBook.title;
        state.remoteBookAuthor = remoteBook == null ? "" : remoteBook.author;
        state.remoteBookUrl = remoteBook == null ? "" : remoteBook.url;
        state.remoteBookDescription = remoteBook == null ? "" : remoteBook.description;
        state.remoteChapterTitles = chapters.stream().map(chapter -> chapter.title).toList();
        state.remoteChapterUrls = chapters.stream().map(chapter -> chapter.url).toList();
        state.remoteChapterItemIds = chapters.stream().map(chapter -> chapter.itemId).toList();
        state.remoteChapterTexts = loadedTexts;
    }

    public synchronized void loadLocal(Path path, String regex, Charset charset, Book book) {
        state.localBookPath = path.toAbsolutePath().toString();
        state.localBookRegex = regex == null || regex.isBlank() ? BookParser.DEFAULT_CHAPTER_REGEX : regex;
        state.localBookCharset = charset == null ? StandardCharsets.UTF_8.name() : charset.name();
        load(book);
        state.readerType = "local";
    }

    public synchronized String lastPlatformUrl() {
        return state.lastPlatformUrl == null || state.lastPlatformUrl.isBlank()
                ? "https://fanqienovel.com/"
                : state.lastPlatformUrl;
    }

    public synchronized void setLastPlatformUrl(String url) {
        if (url != null && !url.isBlank()) {
            state.lastPlatformUrl = url;
        }
    }

    public synchronized Book book() {
        restoreBookIfNeeded();
        return book;
    }

    public synchronized Chapter currentChapter() {
        restoreBookIfNeeded();
        if (book == null || book.isEmpty()) {
            return null;
        }
        int boundedIndex = Math.max(0, Math.min(state.chapterIndex, book.chapters().size() - 1));
        return book.chapters().get(boundedIndex);
    }

    public synchronized List<Chapter> chapters() {
        restoreBookIfNeeded();
        return book == null ? List.of() : book.chapters();
    }

    public synchronized int chapterIndex() {
        return state.chapterIndex;
    }

    public synchronized void jumpTo(int index) {
        restoreBookIfNeeded();
        if (book == null || book.isEmpty()) {
            return;
        }
        state.chapterIndex = Math.max(0, Math.min(index, book.chapters().size() - 1));
        state.hintScrollValue = 0;
    }

    public synchronized boolean nextChapter() {
        restoreBookIfNeeded();
        if (book == null || book.isEmpty() || state.chapterIndex >= book.chapters().size() - 1) {
            return false;
        }
        state.chapterIndex++;
        state.hintScrollValue = 0;
        return true;
    }

    public synchronized boolean previousChapter() {
        restoreBookIfNeeded();
        if (book == null || book.isEmpty() || state.chapterIndex <= 0) {
            return false;
        }
        state.chapterIndex--;
        state.hintScrollValue = 0;
        return true;
    }

    public synchronized boolean isRemoteChapterMissing(int index) {
        return remoteBook != null
                && index >= 0
                && book != null
                && index < book.chapters().size()
                && book.chapters().get(index).text().isBlank();
    }

    public synchronized String remoteSourceId() {
        return remoteSourceId;
    }

    public synchronized SearchResult remoteBook() {
        return remoteBook;
    }

    public synchronized RemoteChapter remoteChapter(int index) {
        if (index < 0 || index >= remoteChapters.size()) {
            return null;
        }
        return remoteChapters.get(index);
    }

    public synchronized void replaceChapterText(int index, String text) {
        if (book == null || index < 0 || index >= book.chapters().size()) {
            return;
        }
        List<Chapter> chapters = new ArrayList<>(book.chapters());
        Chapter old = chapters.get(index);
        chapters.set(index, new Chapter(old.title(), text));
        book = new Book(book.title(), chapters);
        if ("remote".equals(state.readerType)) {
            ensureRemoteTextSize();
            state.remoteChapterTexts.set(index, text == null ? "" : text);
        }
    }

    public synchronized int hintScrollValue() {
        return state.hintScrollValue;
    }

    public synchronized void setHintScrollValue(int hintScrollValue) {
        state.hintScrollValue = Math.max(0, hintScrollValue);
    }

    public synchronized boolean hintVisible() {
        return hintVisible;
    }

    public synchronized void setHintVisible(boolean hintVisible) {
        this.hintVisible = hintVisible;
    }

    public synchronized boolean autoNextRunning() {
        return autoNextRunning;
    }

    public synchronized void setAutoNextRunning(boolean autoNextRunning) {
        this.autoNextRunning = autoNextRunning;
    }

    @Override
    public synchronized @NotNull StateData getState() {
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull StateData state) {
        this.state = state;
    }

    private void restoreBookIfNeeded() {
        if (book != null) {
            return;
        }
        if ("remote".equals(state.readerType)) {
            restoreRemoteBookIfNeeded();
            return;
        }
        restoreLocalBookIfNeeded();
    }

    private void restoreLocalBookIfNeeded() {
        if (state.localBookPath == null || state.localBookPath.isBlank()) {
            return;
        }
        Path path = Path.of(state.localBookPath);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            Charset charset = state.localBookCharset == null || state.localBookCharset.isBlank()
                    ? StandardCharsets.UTF_8
                    : Charset.forName(state.localBookCharset);
            book = BookParser.parse(path, state.localBookRegex, charset);
            if (!book.isEmpty()) {
                state.chapterIndex = Math.max(0, Math.min(state.chapterIndex, book.chapters().size() - 1));
            }
        } catch (Exception ignored) {
            book = null;
        }
    }

    private void restoreRemoteBookIfNeeded() {
        if (state.remoteSourceId == null || state.remoteSourceId.isBlank() || state.remoteChapterTitles == null || state.remoteChapterTitles.isEmpty()) {
            return;
        }
        remoteSourceId = state.remoteSourceId;
        remoteBook = new SearchResult(
                state.remoteSourceId,
                state.remoteBookId,
                state.remoteBookTitle,
                state.remoteBookAuthor,
                state.remoteBookUrl,
                state.remoteBookDescription
        );
        List<RemoteChapter> chapters = new ArrayList<>();
        List<Chapter> localChapters = new ArrayList<>();
        ensureRemoteStateLists();
        for (int i = 0; i < state.remoteChapterTitles.size(); i++) {
            String title = state.remoteChapterTitles.get(i);
            String url = i < state.remoteChapterUrls.size() ? state.remoteChapterUrls.get(i) : "";
            String itemId = i < state.remoteChapterItemIds.size() ? state.remoteChapterItemIds.get(i) : "";
            String text = i < state.remoteChapterTexts.size() ? state.remoteChapterTexts.get(i) : "";
            chapters.add(new RemoteChapter(title, url, itemId));
            localChapters.add(new Chapter(title, text));
        }
        remoteChapters = chapters;
        book = new Book(state.remoteBookTitle, localChapters);
        if (!book.isEmpty()) {
            state.chapterIndex = Math.max(0, Math.min(state.chapterIndex, book.chapters().size() - 1));
        }
    }

    private void clearRemoteState() {
        state.remoteSourceId = "";
        state.remoteBookId = "";
        state.remoteBookTitle = "";
        state.remoteBookAuthor = "";
        state.remoteBookUrl = "";
        state.remoteBookDescription = "";
        state.remoteChapterTitles = new ArrayList<>();
        state.remoteChapterUrls = new ArrayList<>();
        state.remoteChapterItemIds = new ArrayList<>();
        state.remoteChapterTexts = new ArrayList<>();
    }

    private void ensureRemoteStateLists() {
        if (state.remoteChapterTitles == null) {
            state.remoteChapterTitles = new ArrayList<>();
        }
        if (state.remoteChapterUrls == null) {
            state.remoteChapterUrls = new ArrayList<>();
        }
        if (state.remoteChapterItemIds == null) {
            state.remoteChapterItemIds = new ArrayList<>();
        }
        if (state.remoteChapterTexts == null) {
            state.remoteChapterTexts = new ArrayList<>();
        }
    }

    private void ensureRemoteTextSize() {
        ensureRemoteStateLists();
        while (state.remoteChapterTexts.size() < book.chapters().size()) {
            state.remoteChapterTexts.add("");
        }
    }
}
