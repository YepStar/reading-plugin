# Reader for JetBrains

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
- A `Reader` tool window panel with the main actions.

## How to Open

- `View > Tool Windows > Reader`
- `Tools > Reader`
- Search actions with `Shift Shift`, then type `Reader`

## Main Actions

- `Reader | Open Local Book`
- `Reader | Show Native Reader`
- `Reader | Hide Native Reader`
- `Reader | Open Table of Contents`
- `Reader | Next Chapter`
- `Reader | Toggle Auto Next Chapter`
- `Reader | Open Platform Browser`
- `Reader | Open Web Reader`

Default shortcuts are declared in `src/main/resources/META-INF/plugin.xml`.

## Build

```bash
./gradlew buildPlugin
```

This project uses the IntelliJ Platform Gradle Plugin and downloads the target IDE during the first build.
