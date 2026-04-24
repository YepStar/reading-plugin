package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.BrowserDialog;
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
                "上次页面：" + lastUrl,
                "https://fanqienovel.com/",
                "https://www.qidian.com/",
                "https://weread.qq.com/",
                "自定义 URL"
        };
        String selected = (String) Messages.showEditableChooseDialog(
                "选择要打开的平台页面。登录状态会由 WebStorm 内嵌浏览器保留。",
                "平台网页登录",
                Messages.getQuestionIcon(),
                options,
                options[0],
                null
        );
        if (selected == null || selected.isBlank()) {
            return;
        }
        String url;
        if (selected.startsWith("上次页面：")) {
            url = lastUrl;
        } else if (selected.equals("自定义 URL")) {
            url = Messages.showInputDialog(project, "请输入 URL", "平台网页登录", Messages.getQuestionIcon(), "https://", null);
        } else {
            url = selected;
        }
        if (url == null || url.isBlank()) {
            return;
        }
        String normalizedUrl = normalizeUrl(url);
        state.setLastPlatformUrl(normalizedUrl);
        new BrowserDialog(project, normalizedUrl).show();
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ? trimmed : "https://" + trimmed;
    }
}
