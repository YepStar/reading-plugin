package com.reader.jetbrains.state;

import com.intellij.openapi.components.Service;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.model.Chapter;

import java.util.List;

@Service(Service.Level.PROJECT)
public final class ReaderStateService {
    private Book book;
    private int chapterIndex;
    private int hintScrollValue;
    private boolean hintVisible;
    private boolean autoNextRunning;

    public synchronized void load(Book book) {
        this.book = book;
        chapterIndex = 0;
        hintScrollValue = 0;
    }

    public synchronized Book book() {
        return book;
    }

    public synchronized Chapter currentChapter() {
        if (book == null || book.isEmpty()) {
            return null;
        }
        int boundedIndex = Math.max(0, Math.min(chapterIndex, book.chapters().size() - 1));
        return book.chapters().get(boundedIndex);
    }

    public synchronized List<Chapter> chapters() {
        return book == null ? List.of() : book.chapters();
    }

    public synchronized int chapterIndex() {
        return chapterIndex;
    }

    public synchronized void jumpTo(int index) {
        if (book == null || book.isEmpty()) {
            return;
        }
        chapterIndex = Math.max(0, Math.min(index, book.chapters().size() - 1));
        hintScrollValue = 0;
    }

    public synchronized boolean nextChapter() {
        if (book == null || book.isEmpty() || chapterIndex >= book.chapters().size() - 1) {
            return false;
        }
        chapterIndex++;
        hintScrollValue = 0;
        return true;
    }

    public synchronized int hintScrollValue() {
        return hintScrollValue;
    }

    public synchronized void setHintScrollValue(int hintScrollValue) {
        this.hintScrollValue = Math.max(0, hintScrollValue);
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
}
