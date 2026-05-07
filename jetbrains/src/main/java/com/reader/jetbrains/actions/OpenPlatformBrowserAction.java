package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.settings.ReaderSettingsService;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.BrowserDialog;
import com.reader.jetbrains.ui.PlatformBrowserPopup;
import org.jetbrains.annotations.NotNull;

public final class OpenPlatformBrowserAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ReaderStateService state = project.getService(ReaderStateService.class);
        String lastUrl = state.lastPlatformUrl();
        String[] options = {
                lastUrl,
                "https://fanqienovel.com/",
                "https://www.qidian.com/",
                "https://weread.qq.com/"
        };
        String selected = ReaderDialogs.editableChoice(
                project,
                "平台网页浮窗",
                "选择或输入要打开的平台页面。登录状态会由 WebStorm 内嵌浏览器保留。",
                options,
                lastUrl
        );
        if (selected == null || selected.isBlank()) {
            return;
        }
        String url = selected;
        if (url == null || url.isBlank()) {
            return;
        }
        String normalizedUrl = normalizeUrl(url);
        state.setLastPlatformUrl(normalizedUrl);
        if ("dialog".equals(ReaderSettingsService.getInstance().platformBrowserMode())) {
            new BrowserDialog(project, normalizedUrl).show();
        } else {
            PlatformBrowserPopup.show(project, normalizedUrl);
        }
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ? trimmed : "https://" + trimmed;
    }
}
