# 书源配置说明

`book-source-config` 目录用于维护插件内置书源。每个 `.json` 文件是一份书源，`sources.list` 决定打包资源中的加载顺序。

插件启动后会用本目录中的同名内置书源覆盖项目里已持久化的旧内置配置；自定义书源会保留。这样修改 `biquge.json` 后不需要手动清理 `reader-book-sources.xml`。

## 顶层字段

- `id`：书源唯一标识，保存阅读状态和默认书源时使用。
- `name`：界面显示名称。
- `type`：解析类型，当前支持 `html` 和 `json`。
- `baseUrl`：相对链接补全时使用的基础地址。
- `enabled`：是否默认启用。
- `search`：搜索接口或搜索页规则。
- `catalog`：目录接口或目录页规则。
- `content`：正文接口或正文页规则。
- `info`：搜索结果中的书籍字段映射。
- `processor`：字段后处理规则，用于从原始字段里提取 `bookId`、`itemId` 等运行时变量。

## URL 模板变量

`search.url`、`catalog.url`、`content.url` 支持以下变量：

- `${key}`：搜索关键词，会自动 URL 编码。
- `${page}`：当前固定为 `1`。
- `${bookId}`：搜索结果解析出的书籍 ID。
- `${bookUrl}`：搜索结果解析出的书籍页面 URL。
- `${itemId}`：目录章节解析出的章节 ID。
- `${chapterUrl}`：目录章节解析出的章节页面 URL。
- `${apiKey}`：从 WebStorm 的 `设置 > 工具 > Reader Yip` 读取番茄 OIAPI Key，并由 Password Safe 在本机跨项目保存。

## JSON 书源

JSON 书源使用 `dataPath` 取数据：

- `search.dataPath`：搜索结果数组路径，例如 `$.data[*]`。
- `catalog.dataPath`：章节目录数组路径，例如 `$.list[*]`。
- `content.dataPath`：正文文本路径，例如 `$.txt`。

字段映射：

- `info.bookIdField`：搜索结果中的书籍 ID 字段。
- `info.titleField`：搜索结果中的书名字段。
- `info.authorField`：作者字段。
- `info.descField`：简介字段。
- `info.urlField`：书籍页面 URL 字段。
- `catalog.itemIdField`：章节 ID 字段。
- `catalog.itemTitleField`：章节标题字段。
- `catalog.itemUrlField`：章节页面 URL 字段。

如果字段不能直接使用，可以通过 `processor` 提取。

目录数组既可以是对象数组，也可以是字符串数组。

多层数组可以连续使用 `[*]` 展平，例如 `$.data[*][*]` 可将按卷分组的章节列表展平为单一目录。

对象数组示例：

```json
{
  "list": [
    { "chapter_id": "12", "chapter_title": "第12章 风起" }
  ]
}
```

字符串数组示例：

```json
{
  "list": [
    "第1章 蜂落",
    "第2章 猫面"
  ]
}
```

对于字符串数组，插件会把每个字符串同时当作 `chapterTitle` 和 `title`，再交给 `processor.itemId` 提取 `${itemId}`。

## HTML 书源

HTML 书源使用简化选择器：

- 搜索列表当前支持表格行，如 `.grid tr:not(:first-child)`。
- 目录列表当前支持 `dd` 链接列表，如 `#list dl dd`。
- 正文容器当前支持 ID 选择器，如 `#content`。

搜索和目录项会读取第一条 `<a href="...">标题</a>` 作为标题和 URL。

## processor 后处理

`processor` 是一个对象，key 是要生成的运行时字段。当前实现识别：

- `bookId`：搜索结果阶段生成 `${bookId}`。
- `itemId`：目录阶段生成 `${itemId}`。

规则字段：

- `from`：原始字段名。也支持内置别名 `chapterTitle`，会按 `chapterTitle`、`chapter_title`、`chaptername`、`title`、`name` 顺序取第一个非空值。
- `regex`：Java 正则表达式。
- `replace`：替换结果，默认可写 `$1`。

示例：

```json
"processor": {
  "bookId": {
    "from": "url_list",
    "regex": "/book/(\\d+)/",
    "replace": "$1"
  },
  "itemId": {
    "from": "chapterTitle",
    "regex": "^第(\\d+)章",
    "replace": "$1"
  }
}
```

这个示例表示：

- 从搜索结果的 `url_list` 中提取数字作为 `bookId`。
- 从章节标题中提取章节序号作为 `itemId`。

## 注意事项

- 配置文件必须是标准 JSON，不能在 JSON 内写注释。
- 当前插件不执行远程脚本，也不执行任意 JavaScript processor。
- 番茄 OIAPI Key 不应直接写入书源 JSON；请使用 `${apiKey}` 占位符并在 Reader Yip 设置中输入。
- 依赖加密、签名、多页拼接或复杂 DOM 处理的站点，需要在插件代码中增加专门 processor。
