# Reader Yip for JetBrains

中文说明见仓库根目录 [README_zh.md](../README_zh.md)。

JetBrains IDE reading plugin inspired by W-Reader, with a smaller core model and a native editor hint reading surface.

## Implemented in this scaffold

- Open local `.txt` and `.epub` files.
- Split chapters with a configurable regular expression.
- Jump through a table of contents.
- Show/hide a JetBrains-native editor hint reader.
- Move to the next chapter manually or on an automatic timer.
- Move to the previous chapter.
- Open login-capable platform pages in an embedded JCEF browser.
- Open platform pages in a compact popup or full dialog, without importing logged-in page DOM.
- Open a third-party novel web page and extract likely article text into the reader.
- Search online book sources loaded from the repository-level `book-source-config/` directory.
- Manage online sources with JSON edit, enable/disable, delete, reset, and default selection.
- A `Reader` tool window panel with the main actions.

## How to Open

- `View > Tool Windows > Reader Yip`
- `Tools > Reader Yip`
- Search actions with `Shift Shift`, then type `Reader Yip`

## Main Actions

- `Reader Yip | 打开本地 TXT / EPUB`
- `Reader Yip | 显示/隐藏原生提示层`
- `Reader Yip | 打开目录`
- `Reader Yip | 上一章`
- `Reader Yip | 下一章`
- `Reader Yip | 自动下一章 开/关`
- `Reader Yip | 平台网页浮窗`
- `Reader Yip | 网页正文提取`
- `Reader Yip | 在线书源搜索`
- `Reader Yip | 书源管理`

Default shortcuts are declared in `src/main/resources/META-INF/plugin.xml`.

## Build

```bash
../scripts/package-jetbrains.sh
```

The package script creates:

```bash
jetbrains/build/distributions/reader-jetbrains-0.1.0.zip
```

## Publish

Version `0.1.0` is configured in `gradle.properties` and `plugin.xml`.

Publishing is wired through the IntelliJ Platform Gradle Plugin `publishPlugin` task. Use a JetBrains Marketplace token:

```bash
export JETBRAINS_MARKETPLACE_TOKEN=...
scripts/publish-jetbrains.sh
```

The default publish channel is `default`. Override it with:

```bash
PLUGIN_CHANNELS=eap scripts/publish-jetbrains.sh
```

Run publishing with JDK 17 or 21. Do not commit tokens.
