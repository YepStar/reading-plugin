package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.ui.BrowserDialog;
import org.jetbrains.annotations.NotNull;

public final class OpenPlatformBrowserAction extends AnAction {
    private static final String[] OPTIONS = {
            "https://fanqienovel.com/",
            "https://www.qidian.com/",
            "https://weread.qq.com/",
            "Custom URL"
    };

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        String selected = (String) Messages.showEditableChooseDialog(
                "Open a login-capable platform page.",
                "Platform Browser",
                Messages.getQuestionIcon(),
                OPTIONS,
                OPTIONS[0],
                null
        );
        if (selected == null || selected.isBlank()) {
            return;
        }
        String url = selected.equals("Custom URL")
                ? Messages.showInputDialog(project, "URL", "Platform Browser", Messages.getQuestionIcon(), "https://", null)
                : selected;
        if (url == null || url.isBlank()) {
            return;
        }
        new BrowserDialog(project, normalizeUrl(url)).show();
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ? trimmed : "https://" + trimmed;
    }
}
