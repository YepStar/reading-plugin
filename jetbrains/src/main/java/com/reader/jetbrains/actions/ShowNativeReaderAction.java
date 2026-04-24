package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

public final class ShowNativeReaderAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        if (project.getService(ReaderStateService.class).currentChapter() == null) {
            Messages.showWarningDialog(project, "Open a TXT, EPUB, or web page first.", "Reader");
            return;
        }
        Editor editor = ReaderActionUtil.editor(event, project);
        if (editor == null) {
            Messages.showWarningDialog(project, "Open an editor tab before showing the native hint.", "Reader");
            return;
        }
        ReaderHintController.show(project, editor);
    }
}
