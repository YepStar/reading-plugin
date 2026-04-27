package com.reader.jetbrains.actions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ToggleAutoNextChapterAction extends AnAction implements Disposable {
    private static final Map<Project, ScheduledExecutorService> EXECUTORS = new ConcurrentHashMap<>();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ReaderStateService state = project.getService(ReaderStateService.class);
        if (state.chapters().isEmpty()) {
            Messages.showWarningDialog(project, "当前没有已导入的书籍或网页正文。", "Reader-plugin-yip");
            return;
        }
        ScheduledExecutorService running = EXECUTORS.remove(project);
        if (running != null) {
            running.shutdownNow();
            state.setAutoNextRunning(false);
            Messages.showInfoMessage(project, "已停止自动下一章。", "Reader-plugin-yip");
            return;
        }

        String secondsText = Messages.showInputDialog(
                project,
                "请输入自动切换章节的间隔秒数。",
                "自动下一章",
                Messages.getQuestionIcon(),
                "90",
                null
        );
        int seconds = parseSeconds(secondsText);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        EXECUTORS.put(project, executor);
        state.setAutoNextRunning(true);
        executor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(() -> {
            if (project.isDisposed()) {
                stop(project);
                return;
            }
            if (!state.nextChapter()) {
                stop(project);
                return;
            }
            try {
                RemoteChapterLoader.ensureLoaded(project, state.chapterIndex());
            } catch (Exception exception) {
                stop(project);
                Messages.showErrorDialog(project, "在线章节加载失败：\n" + exception.getMessage(), "Reader-plugin-yip");
                return;
            }
            Editor editor = ReaderActionUtil.editor(event, project);
            if (editor != null) {
                ReaderHintController.show(project, editor);
            }
        }), seconds, seconds, TimeUnit.SECONDS);
        Messages.showInfoMessage(project, "已启动自动下一章。", "Reader-plugin-yip");
    }

    private static int parseSeconds(String secondsText) {
        try {
            return Math.max(5, Integer.parseInt(secondsText == null ? "90" : secondsText.trim()));
        } catch (NumberFormatException ignored) {
            return 90;
        }
    }

    private static void stop(Project project) {
        ScheduledExecutorService executor = EXECUTORS.remove(project);
        if (executor != null) {
            executor.shutdownNow();
        }
        if (!project.isDisposed()) {
            project.getService(ReaderStateService.class).setAutoNextRunning(false);
        }
    }

    @Override
    public void dispose() {
        EXECUTORS.values().forEach(ScheduledExecutorService::shutdownNow);
        EXECUTORS.clear();
    }
}
