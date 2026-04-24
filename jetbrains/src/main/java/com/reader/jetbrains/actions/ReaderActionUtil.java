package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.parser.BookParser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class ReaderActionUtil {
    private ReaderActionUtil() {
    }

    static Editor editor(AnActionEvent event, Project project) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        return editor != null ? editor : FileEditorManager.getInstance(project).getSelectedTextEditor();
    }

    static String askChapterRegex(Project project) {
        String value = Messages.showInputDialog(
                project,
                "Chapter regex. The first capture group is used as the chapter title.",
                "Chapter Split",
                Messages.getQuestionIcon(),
                BookParser.DEFAULT_CHAPTER_REGEX,
                null
        );
        return value == null || value.isBlank() ? BookParser.DEFAULT_CHAPTER_REGEX : value;
    }

    static Charset askCharset(Project project) {
        String value = Messages.showInputDialog(
                project,
                "Text charset for TXT files.",
                "Charset",
                Messages.getQuestionIcon(),
                StandardCharsets.UTF_8.name(),
                null
        );
        if (value == null || value.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(value.trim());
        } catch (Exception ignored) {
            Messages.showWarningDialog(project, "Unknown charset. UTF-8 will be used.", "Reader");
            return StandardCharsets.UTF_8;
        }
    }
}
