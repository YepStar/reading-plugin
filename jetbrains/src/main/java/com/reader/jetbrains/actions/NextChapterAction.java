package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

public final class NextChapterAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ReaderStateService state = project.getService(ReaderStateService.class);
        if (!state.nextChapter()) {
            Messages.showInfoMessage(project, "已经是最后一章。", "Reader Yip");
            return;
        }
        try {
            RemoteChapterLoader.ensureLoaded(project, state.chapterIndex());
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
