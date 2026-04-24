package com.reader.jetbrains.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.model.Chapter;
import com.reader.jetbrains.parser.BookParser;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
@State(name = "ReaderPluginYipState", storages = @Storage("reader-plugin-yip.xml"))
public final class ReaderStateService implements PersistentStateComponent<ReaderStateService.StateData> {
    public static final class StateData {
        public String localBookPath = "";
        public String localBookRegex = BookParser.DEFAULT_CHAPTER_REGEX;
        public String localBookCharset = StandardCharsets.UTF_8.name();
        public int chapterIndex;
        public int hintScrollValue;
        public String lastPlatformUrl = "https://fanqienovel.com/";
    }

    private StateData state = new StateData();
    private Book book;
    private boolean hintVisible;
    private boolean autoNextRunning;

    public synchronized void load(Book book) {
        this.book = book;
        state.chapterIndex = 0;
        state.hintScrollValue = 0;
    }

    public synchronized void loadLocal(Path path, String regex, Charset charset, Book book) {
        state.localBookPath = path.toAbsolutePath().toString();
        state.localBookRegex = regex == null || regex.isBlank() ? BookParser.DEFAULT_CHAPTER_REGEX : regex;
        state.localBookCharset = charset == null ? StandardCharsets.UTF_8.name() : charset.name();
        load(book);
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
        restoreLocalBookIfNeeded();
        return book;
    }

    public synchronized Chapter currentChapter() {
        restoreLocalBookIfNeeded();
        if (book == null || book.isEmpty()) {
            return null;
        }
        int boundedIndex = Math.max(0, Math.min(state.chapterIndex, book.chapters().size() - 1));
        return book.chapters().get(boundedIndex);
    }

    public synchronized List<Chapter> chapters() {
        restoreLocalBookIfNeeded();
        return book == null ? List.of() : book.chapters();
    }

    public synchronized int chapterIndex() {
        return state.chapterIndex;
    }

    public synchronized void jumpTo(int index) {
        restoreLocalBookIfNeeded();
        if (book == null || book.isEmpty()) {
            return;
        }
        state.chapterIndex = Math.max(0, Math.min(index, book.chapters().size() - 1));
        state.hintScrollValue = 0;
    }

    public synchronized boolean nextChapter() {
        restoreLocalBookIfNeeded();
        if (book == null || book.isEmpty() || state.chapterIndex >= book.chapters().size() - 1) {
            return false;
        }
        state.chapterIndex++;
        state.hintScrollValue = 0;
        return true;
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

    private void restoreLocalBookIfNeeded() {
        if (book != null || state.localBookPath == null || state.localBookPath.isBlank()) {
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
}
