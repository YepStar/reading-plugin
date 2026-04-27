package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.sources.BookSource;
import com.reader.jetbrains.sources.BookSourceClient;
import com.reader.jetbrains.sources.BookSourceService;
import com.reader.jetbrains.sources.RemoteChapter;
import com.reader.jetbrains.sources.SearchResult;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SearchOnlineBookAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        BookSourceService sourceService = project.getService(BookSourceService.class);
        List<BookSource> sources = sourceService.enabledSources();
        if (sources.isEmpty()) {
            Messages.showWarningDialog(project, "没有启用的书源，请先打开书源管理。", "Reader-plugin-yip");
            return;
        }

        BookSource source = chooseSource(project, sourceService, sources);
        if (source == null) {
            return;
        }
        String keyword = Messages.showInputDialog(project, "输入书名或关键词：", "在线搜索", Messages.getQuestionIcon());
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        BookSourceClient client = new BookSourceClient();
        SearchData data = new SearchData();
        boolean ok = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                data.results = client.search(source, keyword.trim());
            } catch (Exception exception) {
                data.error = exception;
            }
        }, "搜索在线小说", true, project);
        if (!ok) {
            return;
        }
        if (data.error != null) {
            Messages.showErrorDialog(project, "搜索失败：\n" + data.error.getMessage(), "Reader-plugin-yip");
            return;
        }
        if (data.results.isEmpty()) {
            Messages.showInfoMessage(project, "没有搜索到结果。", "Reader-plugin-yip");
            return;
        }

        SearchResult result = choose(project, "选择搜索结果", data.results);
        if (result == null) {
            return;
        }

        data.clear();
        ok = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                data.chapters = client.catalog(source, result);
            } catch (Exception exception) {
                data.error = exception;
            }
        }, "加载目录", true, project);
        if (!ok) {
            return;
        }
        if (data.error != null) {
            Messages.showErrorDialog(project, "目录加载失败：\n" + data.error.getMessage(), "Reader-plugin-yip");
            return;
        }
        if (data.chapters.isEmpty()) {
            Messages.showInfoMessage(project, "该书源没有返回目录。", "Reader-plugin-yip");
            return;
        }

        RemoteChapter chapter = choose(project, "选择章节", data.chapters);
        if (chapter == null) {
            return;
        }
        int chapterIndex = data.chapters.indexOf(chapter);
        data.clear();
        ok = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                data.text = client.chapterContent(source, result, chapter);
            } catch (Exception exception) {
                data.error = exception;
            }
        }, "加载正文", true, project);
        if (!ok) {
            return;
        }
        if (data.error != null) {
            Messages.showErrorDialog(project, "正文加载失败：\n" + data.error.getMessage(), "Reader-plugin-yip");
            return;
        }

        project.getService(ReaderStateService.class).loadRemote(source.id, result, data.chapters, chapterIndex, data.text);
        Editor editor = ReaderActionUtil.editor(event, project);
        if (editor != null) {
            ReaderHintController.show(project, editor);
        }
    }

    private static BookSource chooseSource(Project project, BookSourceService service, List<BookSource> sources) {
        String[] names = sources.stream().map(BookSource::toString).toArray(String[]::new);
        String selected = service.selectedSource().toString();
        int choice = Messages.showChooseDialog(project, "选择搜索书源：", "在线搜索", Messages.getQuestionIcon(), names, selected);
        if (choice < 0) {
            return null;
        }
        BookSource source = sources.get(choice);
        service.select(source.id);
        return source;
    }

    private static <T> T choose(Project project, String title, List<T> values) {
        String[] labels = values.stream().map(Object::toString).toArray(String[]::new);
        int choice = Messages.showChooseDialog(project, title + "：", title, Messages.getQuestionIcon(), labels, labels[0]);
        if (choice < 0) {
            return null;
        }
        return values.get(choice);
    }

    private static final class SearchData {
        private List<SearchResult> results = List.of();
        private List<RemoteChapter> chapters = List.of();
        private String text = "";
        private Exception error;

        private void clear() {
            results = List.of();
            chapters = List.of();
            text = "";
            error = null;
        }
    }
}
