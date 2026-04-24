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
            Messages.showWarningDialog(project, "No book is loaded.", "Reader");
            return;
        }
        ScheduledExecutorService running = EXECUTORS.remove(project);
        if (running != null) {
            running.shutdownNow();
            state.setAutoNextRunning(false);
            Messages.showInfoMessage(project, "Auto next chapter stopped.", "Reader");
            return;
        }

        String secondsText = Messages.showInputDialog(
                project,
                "Seconds between chapters.",
                "Auto Next Chapter",
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
            Editor editor = ReaderActionUtil.editor(event, project);
            if (editor != null) {
                ReaderHintController.show(project, editor);
            }
        }), seconds, seconds, TimeUnit.SECONDS);
        Messages.showInfoMessage(project, "Auto next chapter started.", "Reader");
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
