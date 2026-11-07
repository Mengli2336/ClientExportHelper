# Client Export Helper

一个 Fabric **客户端** mod：把客户端能拿到的进度（advancement）、进度完成情况（progress）和统计数据（statistics）导出成 JSON 文件，方便做 wiki、数据包、进度树网站或存档迁移。

> **26.1.2 分支的移植由 DeepSeek 完成**：包括构建链升级（Gradle 9.8.2 / Loom 1.19 / Java 25）、API 迁移（`Identifier`、`ClientCommands`、`ItemStackTemplate`）、资源格式更新（`pack.mcmeta` 的 `min_format` / `max_format`、mixin `JAVA_25`），以及实机导出验证。原始 mod 作者为 [QWERTY770](https://github.com/QWERTY770/ClientExportHelper)。

## 功能

- 导出客户端已同步的全部进度定义，按命名空间分文件
- 导出每条进度的完成情况（已完成条件、完成时间、百分比）
- 导出统计数据（按 stat 类型分组）
- 导出的进度文件结构与原版数据包一致，可直接作为数据包加载

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| Fabric Loader | 0.20.1 及以上 |
| Fabric API | 0.155.3+26.1.2（或其他 26.1.x 版本） |
| Java | 25 |

只装在客户端即可，不需要装到服务端。

## 安装

1. 安装对应 26.1.2 的 Fabric Loader；
2. 把 `client-export-helper-26.1.2-<version>.jar` 放进 `.minecraft/mods/`；
3. 同时安装 Fabric API；
4. 启动游戏，日志出现 `Client Export Helper initialized!` 即加载成功。

## 命令

主命令 `/clientexport`，别名 `/clex`。

| 命令 | 作用 |
|---|---|
| `/clientexport` | 显示 `Client Export Helper 3.1.0 running!`，用于确认 mod 正常 |
| `/clientexport advancements` | 导出所有命名空间的进度定义，每个命名空间一个文件 |
| `/clientexport advancements <namespace>` | 只导出指定命名空间，例如 `/clientexport advancements minecraft` |
| `/clientexport progress` | 导出所有命名空间的进度完成情况 |
| `/clientexport progress <namespace>` | 只导出指定命名空间，例如 `/clientexport progress isletopia_advancements` |
| `/clientexport statistics` | 导出统计数据 |

`namespace` 是字符串参数（`StringArgumentType.string()`），填进度 ID 中冒号前的部分，例如 `minecraft`、`isletopia_advancements`；不带参数就导出全部命名空间。参数没有 Tab 补全，也可以加引号写成 `"minecraft"`。

执行成功会在聊天栏回显数量，例如：

```
成功导出2462个进度
成功导出2462条进度完成进展
成功导出2740条统计数据
```

## 输出文件

写入游戏目录下的 `export/` 文件夹（开发环境是 `run/export/`），文件名格式：

```
<namespace>-<type>-<yyyy-MM-dd-HHmmss>.json
statistics-<yyyy-MM-dd-HHmmss>.json
```

例如：

```
export/minecraft-advancements-2026-11-07-201514.json
export/minecraft-progress-2026-11-07-201541.json
export/isletopia_advancements-advancements-2026-11-07-201514.json
export/statistics-2026-11-07-201614.json
```

每次执行都会生成新文件（时间戳精确到秒，同一秒内重复执行会写入同一个文件），不会覆盖旧文件。

### 进度定义（advancements）

文件名里带 `advancements` 的那种，内容是「进度 ID → 进度定义」的对象：

```json
{
  "minecraft:story/mine_stone": {
    "parent": "minecraft:story/root",
    "display": {
      "icon": { "id": "minecraft:wooden_pickaxe" },
      "title": { "translate": "advancements.story.mine_stone.title" },
      "description": { "translate": "advancements.story.mine_stone.description" },
      "announce_to_chat": false
    },
    "criteria": {
      "get_stone": { "trigger": "minecraft:impossible" }
    },
    "requirements": [["get_stone"]],
    "sends_telemetry_event": true
  }
}
```

几点说明：

- `criteria` 里的 trigger 统一是 `minecraft:impossible`。原版不会把 criterion 的真实定义同步给客户端，用该占位符是为了让文件仍然是合法的数据包格式。
- `requirements` 是服务端下发的真实需求分组，可以用来还原「与/或」关系。
- 取默认值的字段会被省略（例如 `show_toast`、`hidden`、`frame`），这与原版数据包的写法一致。
- 只有带 `display` 的进度才有标题、图标等信息；配方类进度通常没有 `display`。
- 图标若有附魔组件，会额外出现 mod 自定义键 `clientexport:icon_enchantments`：

```json
"icon": { "id": "minecraft:enchanted_book" },
"clientexport:icon_enchantments": { "minecraft:sharpness": 5 }
```

### 进度完成情况（progress）

```json
{
  "minecraft:story/mine_stone": {
    "criteria": { "get_stone": "2026-11-07 20:08:44 +0800" },
    "done": true,
    "remaining": [],
    "progress": 1.0
  }
}
```

- `criteria`：已完成的条件及其完成时间（时区为客户端本地时区）。
- `done`：按 `requirements` 的分组判定是否已经完成。
- `progress`：完成度百分比（0.0 ~ 1.0），同样按需求组计算。
- `remaining`：仍然没有完成的条件名列表，是**逐条件**列出的。

配方类进度常见「或」组（`[["has_the_recipe", "has_cobblestone"]]`），只要组内任意一条完成就算完成，所以会出现 `done: true`、`progress: 1.0` 但 `remaining` 里还列着另一个未完成条件的情况，这是正常的。

### 统计数据（statistics）

```json
{
  "stats": {
    "minecraft:custom": { "minecraft:interact_with_crafting_table": 1381 },
    "minecraft:mined": { "minecraft:white_bed": 44 },
    "minecraft:killed": { "minecraft:cod": 5 }
  },
  "DataVersion": 4790
}
```

`DataVersion` 是客户端当前版本的数据版本号（26.1.2 为 4790）。

## 注意事项

### 统计数据为 0 条？

原版只在**打开「统计信息」界面**的时候才向服务端发请求、把统计数据同步到客户端。刚进游戏直接执行 `/clientexport statistics` 会得到空表。正确顺序：

1. 进入世界/服务器；
2. 按 `Esc` → 「统计信息」，停留一两秒让数据同步；
3. 关闭界面，再执行 `/clientexport statistics`。

### 需要先进过服务器/存档

mod 导出的是**已同步到客户端**的数据。没进过服务器时进度树是空的，统计也是空的。

### 附魔图标

附魔在服务端注册，客户端通常解析不出图标上的 `minecraft:enchantments` 组件。mod 会把能读到的部分写到 `clientexport:icon_enchantments`，并把原组件从图标中移除，保证导出文件能被原版正常解析。读不到时就不写这个键。

## 从源码构建

```bash
./gradlew build
```

- 需要 JDK 25（`build.gradle` 里 `options.release = 25`）
- 产物在 `build/libs/`：`client-export-helper-26.1.2-<version>.jar` 与 `-sources.jar`
- 26.1.2 官方不再混淆，构建过程没有 `remapJar` 步骤，jar 里的类名就是 Mojang 官方名

## 分支与版本

| 分支 | Minecraft | Java | 说明 |
|---|---|---|---|
| `26.1.x` | 26.1.2 | 25 | 当前默认分支，由 DeepSeek 移植 |
| `1.21.x` | 1.21.7 | 21 | 原始版本 |

下载构建产物：<https://github.com/Mengli2336/ClientExportHelper/releases>

## 兼容性验证

26.1.2 分支的实际测试结果（Fabric Loader 0.20.1 + Fabric API 0.155.3+26.1.2 + Java 25）：

- 客户端：三条命令均正常，导出 2462 条进度定义、2462 条进度完成情况、2740 条统计数据
- 导出数据：criteria / requirements / parent 逐条校验一致，无错误条目
- 服务端：把导出的 2462 个进度文件打包成数据包放进 26.1.2 服务端，启动日志输出 `Loaded 2597 advancements`，没有任何解析错误

## 已知限制

- 只导出客户端拿得到的数据；criterion 的真实触发条件无法获取，故用 `minecraft:impossible` 占位。
- 进度完成时间依赖客户端本地时区，跨时区对比时需要注意。
- 统计数据的完整性取决于是否打开过统计界面，以及服务端是否允许同步。
- 带 `namespace` 参数导出时，聊天栏回显的数量是客户端已同步的总条数，不是该命名空间的条数（导出文件里只有该命名空间）。

## 许可证

见仓库根目录的 [LICENSE](LICENSE)。
