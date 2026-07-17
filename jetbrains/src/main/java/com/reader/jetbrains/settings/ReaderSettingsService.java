package com.reader.jetbrains.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
import java.awt.Point;

@Service(Service.Level.APP)
@State(name = "ReaderYipSettings", storages = @Storage("reader-yip-settings.xml"))
public final class ReaderSettingsService implements PersistentStateComponent<ReaderSettingsService.StateData> {
    private static final String DEFAULT_PLATFORM_URL = "https://fanqienovel.com/";
    private static final CredentialAttributes FANQIE_API_KEY = new CredentialAttributes("Reader Yip: Fanqie OIAPI Key");

    public static final class StateData {
        public int hintWidth = 420;
        public int hintHeight = 260;
        public int fontSize = 14;
        public int horizontalPadding = 8;
        public int verticalPadding = 8;
        public int maxLineChars = 0;
        public String defaultCharset = "UTF-8";
        public int autoNextSeconds = 90;
        public String platformBrowserMode = "popup";
        public int platformPopupX = -1;
        public int platformPopupY = -1;
        public int platformPopupWidth = 520;
        public int platformPopupHeight = 360;
        public String lastPlatformUrl = "";
    }

    private StateData state = new StateData();

    public static ReaderSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(ReaderSettingsService.class);
    }

    @Override
    public synchronized @NotNull StateData getState() {
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull StateData state) {
        this.state = state;
    }

    public synchronized int hintWidth() {
        return clamp(state.hintWidth, 260, 1000);
    }

    public synchronized int hintHeight() {
        return clamp(state.hintHeight, 160, 800);
    }

    public synchronized int fontSize() {
        return clamp(state.fontSize, 10, 28);
    }

    public synchronized int maxLineChars() {
        return Math.max(0, Math.min(120, state.maxLineChars));
    }

    public synchronized int horizontalPadding() {
        return clamp(state.horizontalPadding, 0, 80);
    }

    public synchronized int verticalPadding() {
        return clamp(state.verticalPadding, 0, 80);
    }

    public synchronized String defaultCharset() {
        return state.defaultCharset == null || state.defaultCharset.isBlank() ? "UTF-8" : state.defaultCharset;
    }

    public synchronized int autoNextSeconds() {
        return clamp(state.autoNextSeconds, 5, 3600);
    }

    public String fanqieApiKey() {
        String value = PasswordSafe.getInstance().getPassword(FANQIE_API_KEY);
        return value == null ? "" : value.trim();
    }

    public void setFanqieApiKey(String value) {
        String key = value == null ? "" : value.trim();
        PasswordSafe.getInstance().setPassword(FANQIE_API_KEY, key.isBlank() ? null : key);
    }

    public synchronized String platformBrowserMode() {
        return "dialog".equals(state.platformBrowserMode) ? "dialog" : "popup";
    }

    public synchronized String lastPlatformUrl() {
        return state.lastPlatformUrl == null || state.lastPlatformUrl.isBlank()
                ? DEFAULT_PLATFORM_URL
                : state.lastPlatformUrl;
    }

    public synchronized void setLastPlatformUrl(String url) {
        if (url != null && !url.isBlank()) {
            state.lastPlatformUrl = url;
        }
    }

    public synchronized void migrateLastPlatformUrl(String url) {
        if ((state.lastPlatformUrl == null || state.lastPlatformUrl.isBlank()) && url != null && !url.isBlank()) {
            state.lastPlatformUrl = url;
        }
    }

    public synchronized Dimension platformPopupSize() {
        return new Dimension(clamp(state.platformPopupWidth, 320, 2000), clamp(state.platformPopupHeight, 180, 1600));
    }

    public synchronized Point platformPopupLocation() {
        if (state.platformPopupX < 0 || state.platformPopupY < 0) {
            return null;
        }
        return new Point(state.platformPopupX, state.platformPopupY);
    }

    public synchronized void savePlatformPopupBounds(Point location, Dimension contentSize) {
        if (location == null || contentSize == null || contentSize.width <= 0 || contentSize.height <= 0) {
            return;
        }
        state.platformPopupX = location.x;
        state.platformPopupY = location.y;
        state.platformPopupWidth = contentSize.width;
        state.platformPopupHeight = contentSize.height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
