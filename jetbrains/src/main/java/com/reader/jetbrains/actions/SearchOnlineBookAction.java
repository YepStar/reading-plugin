package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.reader.jetbrains.sources.BookSource;
import com.reader.jetbrains.sources.BookSourceClient;
import com.reader.jetbrains.sources.BookSourceService;
import com.reader.jetbrains.sources.RemoteChapter;
import com.reader.jetbrains.sources.SearchResult;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
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
            Messages.showWarningDialog(project, "没有启用的书源，请先打开书源管理。", "Reader Yip");
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
            Messages.showErrorDialog(project, "搜索失败：\n" + data.error.getMessage(), "Reader Yip");
            return;
        }
        if (data.results.isEmpty()) {
            Messages.showInfoMessage(project, "没有搜索到结果。", "Reader Yip");
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
            Messages.showErrorDialog(project, "目录加载失败：\n" + data.error.getMessage(), "Reader Yip");
            return;
        }
        if (data.chapters.isEmpty()) {
            Messages.showInfoMessage(project, "该书源没有返回目录。", "Reader Yip");
            return;
        }

        RemoteChapter chapter = choose(project, "选择章节", data.chapters);
        if (chapter == null) {
            return;
        }
        int chapterIndex = data.chapters.indexOf(chapter);
        List<RemoteChapter> chapters = data.chapters;
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
            Messages.showErrorDialog(project, "正文加载失败：\n" + data.error.getMessage(), "Reader Yip");
            return;
        }
        if (data.text == null || data.text.isBlank()) {
            Messages.showWarningDialog(project, "正文接口已返回，但未解析到正文文本。请检查 content.dataPath。", "Reader Yip");
            return;
        }

        ReaderStateService readerState = project.getService(ReaderStateService.class);
        readerState.loadRemote(source.id, result, chapters, chapterIndex, data.text);
        if (readerState.currentChapter() == null) {
            Messages.showErrorDialog(project, "在线章节已请求，但没有写入阅读状态。", "Reader Yip");
            return;
        }
        Editor editor = ReaderActionUtil.editor(event, project);
        if (editor != null) {
            ReaderHintController.show(project, editor);
        } else {
            Messages.showInfoMessage(project, "在线章节已导入。打开任意编辑器标签页后，可显示原生提示层。", "Reader Yip");
        }
    }

    private static BookSource chooseSource(Project project, BookSourceService service, List<BookSource> sources) {
        String[] names = sources.stream().map(BookSource::toString).toArray(String[]::new);
        String selected = service.selectedSource().toString();
        int choice = ReaderDialogs.chooseIndex(project, "在线搜索", "选择搜索书源：", names, selected);
        if (choice < 0) {
            return null;
        }
        BookSource source = sources.get(choice);
        service.select(source.id);
        return source;
    }

    private static <T> T choose(Project project, String title, List<T> values) {
        JBList<T> list = new JBList<>(values);
        list.setSelectedIndex(0);
        list.ensureIndexIsVisible(0);
        list.setBorder(JBUI.Borders.empty());
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(520, 560));

        DialogBuilder builder = new DialogBuilder(project);
        builder.setTitle(title);
        builder.setCenterPanel(scrollPane);
        builder.setOkActionEnabled(true);
        if (!builder.showAndGet()) {
            return null;
        }
        int index = list.getSelectedIndex();
        return index < 0 ? null : values.get(index);
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
