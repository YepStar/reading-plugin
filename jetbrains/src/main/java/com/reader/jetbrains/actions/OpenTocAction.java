package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.reader.jetbrains.model.Chapter;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
import java.util.List;

public final class OpenTocAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ReaderStateService state = project.getService(ReaderStateService.class);
        List<Chapter> chapters = state.chapters();
        if (chapters.isEmpty()) {
            Messages.showWarningDialog(project, "当前没有已导入的书籍或网页正文。", "Reader Yip");
            return;
        }

        JBList<Chapter> list = new JBList<>(chapters);
        list.setSelectedIndex(state.chapterIndex());
        list.ensureIndexIsVisible(state.chapterIndex());
        list.setBorder(JBUI.Borders.empty());
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(420, 520));
        DialogBuilder builder = new DialogBuilder(project);
        builder.setTitle("目录");
        builder.setCenterPanel(scrollPane);
        builder.setOkActionEnabled(true);
        if (!builder.showAndGet()) {
            return;
        }
        int index = list.getSelectedIndex();
        if (index < 0) {
            return;
        }
        state.jumpTo(index);
        try {
            RemoteChapterLoader.ensureLoaded(project, index);
        } catch (Exception exception) {
            Messages.showErrorDialog(project, "在线章节加载失败：\n" + exception.getMessage(), "Reader Yip");
            return;
        }
        Editor editor = ReaderActionUtil.editor(event, project);
        if (editor != null) {
            ReaderHintController.show(project, editor);
        }
    }
}
