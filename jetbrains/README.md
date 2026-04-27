# Reader-plugin-yip for JetBrains

中文说明见仓库根目录 [README_zh.md](../README_zh.md)。

JetBrains IDE reading plugin inspired by W-Reader, with a smaller core model and a native editor hint reading surface.

## Implemented in this scaffold

- Open local `.txt` and `.epub` files.
- Split chapters with a configurable regular expression.
- Jump through a table of contents.
- Show/hide a JetBrains-native editor hint reader.
- Move to the next chapter manually or on an automatic timer.
- Open login-capable platform pages in an embedded JCEF browser.
- Open a third-party novel web page and extract likely article text into the reader.
- Search online book sources loaded from the repository-level `book-source-config/` directory.
- Manage online sources with JSON edit, enable/disable, delete, reset, and default selection.
- A `Reader` tool window panel with the main actions.

## How to Open

- `View > Tool Windows > Reader-plugin-yip`
- `Tools > Reader-plugin-yip`
- Search actions with `Shift Shift`, then type `Reader-plugin-yip`

## Main Actions

- `Reader-plugin-yip | 打开本地 TXT / EPUB`
- `Reader-plugin-yip | 显示/隐藏原生提示层`
- `Reader-plugin-yip | 打开目录`
- `Reader-plugin-yip | 下一章`
- `Reader-plugin-yip | 自动下一章 开/关`
- `Reader-plugin-yip | 平台网页登录`
- `Reader-plugin-yip | 网页正文提取`
- `Reader-plugin-yip | 在线书源搜索`
- `Reader-plugin-yip | 书源管理`

Default shortcuts are declared in `src/main/resources/META-INF/plugin.xml`.

## Build

```bash
./gradlew buildPlugin
```

This project uses the IntelliJ Platform Gradle Plugin and downloads the target IDE during the first build.
