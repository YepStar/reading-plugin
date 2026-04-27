package com.reader.jetbrains.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressManager;
import com.reader.jetbrains.sources.BookSource;
import com.reader.jetbrains.sources.BookSourceClient;
import com.reader.jetbrains.sources.BookSourceService;
import com.reader.jetbrains.sources.RemoteChapter;
import com.reader.jetbrains.sources.SearchResult;
import com.reader.jetbrains.state.ReaderStateService;

final class RemoteChapterLoader {
    private RemoteChapterLoader() {
    }

    static void ensureLoaded(Project project, int index) throws Exception {
        ReaderStateService state = project.getService(ReaderStateService.class);
        if (!state.isRemoteChapterMissing(index)) {
            return;
        }
        BookSource source = project.getService(BookSourceService.class)
                .source(state.remoteSourceId())
                .orElseThrow(() -> new IllegalStateException("当前在线书源不存在"));
        SearchResult book = state.remoteBook();
        RemoteChapter chapter = state.remoteChapter(index);
        if (book == null || chapter == null) {
            return;
        }
        LoadResult result = new LoadResult();
        boolean ok = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                result.text = new BookSourceClient().chapterContent(source, book, chapter);
            } catch (Exception exception) {
                result.error = exception;
            }
        }, "加载在线章节", true, project);
        if (!ok) {
            return;
        }
        if (result.error != null) {
            throw result.error;
        }
        state.replaceChapterText(index, result.text);
    }

    private static final class LoadResult {
        private String text = "";
        private Exception error;
    }
}
