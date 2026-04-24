package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

public final class HideNativeReaderAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null) {
            ReaderHintController.hide(project);
        }
    }
}
