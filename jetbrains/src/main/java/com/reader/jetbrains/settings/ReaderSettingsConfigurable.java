package com.reader.jetbrains.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.util.Objects;

public final class ReaderSettingsConfigurable implements Configurable {
    private JSpinner hintWidth;
    private JSpinner hintHeight;
    private JSpinner fontSize;
    private JSpinner maxLineChars;
    private JComboBox<String> defaultCharset;
    private JSpinner autoNextSeconds;
    private JBCheckBox compactPlatformBrowser;
    private JPanel panel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Reader Yip";
    }

    @Override
    public @Nullable JComponent createComponent() {
        ReaderSettingsService.StateData state = ReaderSettingsService.getInstance().getState();
        hintWidth = new JSpinner(new SpinnerNumberModel(state.hintWidth, 260, 1000, 10));
        hintHeight = new JSpinner(new SpinnerNumberModel(state.hintHeight, 160, 800, 10));
        fontSize = new JSpinner(new SpinnerNumberModel(state.fontSize, 10, 28, 1));
        maxLineChars = new JSpinner(new SpinnerNumberModel(state.maxLineChars, 0, 120, 1));
        defaultCharset = new JComboBox<>(new String[]{"UTF-8", "GBK", "GB18030", "Big5"});
        defaultCharset.setSelectedItem(state.defaultCharset == null || state.defaultCharset.isBlank() ? "UTF-8" : state.defaultCharset);
        autoNextSeconds = new JSpinner(new SpinnerNumberModel(state.autoNextSeconds, 5, 3600, 5));
        compactPlatformBrowser = new JBCheckBox("使用紧凑网页浮窗");
        compactPlatformBrowser.setSelected(!"dialog".equals(state.platformBrowserMode));

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("提示层宽度", hintWidth)
                .addLabeledComponent("提示层高度", hintHeight)
                .addLabeledComponent("正文字号", fontSize)
                .addLabeledComponent("单行最大字数（0 为不限制）", maxLineChars)
                .addLabeledComponent("默认 TXT 编码", defaultCharset)
                .addLabeledComponent("自动下一章间隔（秒）", autoNextSeconds)
                .addComponent(compactPlatformBrowser)
                .addComponent(new JBLabel("平台网页不再支持一键导入原生提示层。可用紧凑浮窗降低可见面积，阅读正文建议使用网页正文提取或在线书源。"))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        return panel;
    }

    @Override
    public boolean isModified() {
        ReaderSettingsService.StateData state = ReaderSettingsService.getInstance().getState();
        return intValue(hintWidth) != state.hintWidth
                || intValue(hintHeight) != state.hintHeight
                || intValue(fontSize) != state.fontSize
                || intValue(maxLineChars) != state.maxLineChars
                || !Objects.equals(defaultCharset.getSelectedItem(), state.defaultCharset)
                || intValue(autoNextSeconds) != state.autoNextSeconds
                || compactPlatformBrowser.isSelected() != !"dialog".equals(state.platformBrowserMode);
    }

    @Override
    public void apply() {
        ReaderSettingsService.StateData state = ReaderSettingsService.getInstance().getState();
        state.hintWidth = intValue(hintWidth);
        state.hintHeight = intValue(hintHeight);
        state.fontSize = intValue(fontSize);
        state.maxLineChars = intValue(maxLineChars);
        state.defaultCharset = String.valueOf(defaultCharset.getSelectedItem());
        state.autoNextSeconds = intValue(autoNextSeconds);
        state.platformBrowserMode = compactPlatformBrowser.isSelected() ? "popup" : "dialog";
    }

    @Override
    public void reset() {
        ReaderSettingsService.StateData state = ReaderSettingsService.getInstance().getState();
        hintWidth.setValue(state.hintWidth);
        hintHeight.setValue(state.hintHeight);
        fontSize.setValue(state.fontSize);
        maxLineChars.setValue(state.maxLineChars);
        defaultCharset.setSelectedItem(state.defaultCharset);
        autoNextSeconds.setValue(state.autoNextSeconds);
        compactPlatformBrowser.setSelected(!"dialog".equals(state.platformBrowserMode));
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
}
