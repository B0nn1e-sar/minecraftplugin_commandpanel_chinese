# CommandPanel

面向 Minecraft 新手的可视化指令面板。玩家在聊天栏输入 `/cp`，就能打开一个箱子界面，通过点击按钮完成常用操作，不需要记住复杂指令。

## 功能

- **世界规则开关**：死亡不掉落、日夜交替、天气变化、火焰蔓延、生物破坏、自然回血、立即重生。
- **个人操作**：设置个人重生点、回到重生点、清除重生点、回到世界出生点。
- **传送到玩家**：打开在线玩家列表，点击头像传送；带冷却和跨世界开关。
- **回到上次位置**：传送错了可以一键返回。
- **完全由 `config.yml` 驱动**：服主可以增删按钮、改文字、改权限，不需要改代码。
- **新手友好**：短指令 `/cp`，进服可点击提示，按钮带中文说明和当前状态。
- **安全细节**：传送/重生自动寻找安全落点，防止卡方块；点击冷却防止连点。

## 兼容性

- **Paper 1.21.1**（使用 Paper API）
- **Java 21**
- 不支持 Spigot / CraftBukkit；如需 1.20.x 或其他版本，需要调整 `build.gradle.kts` 中的 `paper-api` 版本并重新编译。

## 安装

1. 构建：

   ```bash
   # Windows
   gradlew.bat build

   # macOS / Linux
   ./gradlew build
   ```

2. 把 `build/libs/commandpanel-1.0.0.jar` 放进服务器的 `plugins/` 目录。
3. 重启服务器。首次启动会生成 `plugins/CommandPanel/config.yml`。
4. 玩家输入 `/cp`、`/panel`、`/caidan` 或 `/mianban` 打开面板。

## 指令与权限

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/cp` | 打开主面板 | `commandpanel.use` |
| `/cp open <面板>` | 打开指定面板 | `commandpanel.use` |
| `/cp reload` | 重载配置 | `commandpanel.reload` |
| `/cp help` | 查看帮助 | `commandpanel.use` |

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `commandpanel.use` | 所有人 | 打开面板 |
| `commandpanel.gamerule` | 所有人 | 切换世界规则 |
| `commandpanel.teleport` | 所有人 | 传送到玩家 |
| `commandpanel.spawn` | 所有人 | 设置/清除/回到重生点 |
| `commandpanel.back` | 所有人 | 回到上次位置 |
| `commandpanel.reload` | OP | 重载配置 |
| `commandpanel.bypass.cooldown` | OP | 跳过点击/传送冷却 |
| `commandpanel.*` | OP | 以上全部 |

> **注意**：按默认设置，普通玩家也能切换世界规则（例如关闭死亡不掉落、关闭生物破坏）。如果是公共服务器，建议用 LuckPerms 收回 `commandpanel.gamerule`，或把 `config.yml` 中相关按钮的 `permission` 改成只有管理员拥有的权限节点。

## 配置说明

打开 `plugins/CommandPanel/config.yml`。修改后执行 `/cp reload`，或重启服务器。

### 面板结构

```yaml
panels:
  main:
    title: "&8指令面板 &7· &f主菜单"
    rows: 6
    fill:
      material: BLACK_STAINED_GLASS_PANE
      name: " "
    buttons:
      keep-inventory:
        slot: 10
        material: ORANGE_DYE
        on-material: LIME_DYE
        off-material: GRAY_DYE
        name: "&e死亡不掉落"
        lore:
          - "&7当前: %state%"
        permission: commandpanel.gamerule
        action:
          type: gamerule
          rule: keepInventory
          scope: all
```

- `slot`：0–53，必须在 `rows * 9` 范围内。
- `material`：按钮物品；`on-material` / `off-material` 用于 true/false 开关。
- `name` / `lore`：支持 `&` 颜色代码。
- `permission`：留空表示不额外检查权限。
- `action`：单个动作；也可以用 `actions:` 列表按顺序执行多个动作。
- `fill`：自动填充没有按钮的槽位，可删掉。

### 支持的动作

| `type` | 参数 | 说明 |
| --- | --- | --- |
| `gamerule` | `rule`、`scope` | 切换布尔型游戏规则；`scope` 为 `current`（当前世界）或 `all`（所有世界） |
| `command` | `command`、`as` | 执行指令；`as` 为 `player` 或 `console` |
| `open` | `panel` | 打开另一个面板 |
| `teleport-player` | 无 | 打开在线玩家列表 |
| `set-spawn` | 无 | 把当前位置设为个人重生点 |
| `clear-spawn` | 无 | 清除个人重生点 |
| `spawn` | 无 | 回到个人重生点；没有则回世界出生点 |
| `world-spawn` | 无 | 回到当前世界出生点 |
| `back` | 无 | 回到上一次由面板传送前的位置 |
| `close` | 无 | 关闭面板 |
| `message` | `text` | 给玩家发送一条消息 |

### 指令动作的占位符

`command` 动作支持：

- `%player%`：玩家名
- `%uuid%`：玩家 UUID
- `%world%`：当前世界名
- `%x%`、`%y%`、`%z%`：玩家当前方块坐标

例如以控制台身份执行：

```yaml
action:
  type: command
  command: "say %player% 打开了指令面板"
  as: console
```

### 按钮文字占位符

- `%state%`：开关状态（开启 / 关闭 / 部分开启）
- `%scope%`：作用范围（当前世界 / 所有世界）
- `%rule%`：规则名
- `%teleport-cooldown%`：传送冷却秒数
- `%online%`：当前在线人数
- `%player%`、`%world%`：当前玩家/世界

### 常用设置

```yaml
settings:
  click-cooldown-ms: 350      # 点击冷却
  hide-no-permission: true    # 无权限按钮是否隐藏
  join-hint: true             # 进服提示
  teleport:
    enabled: true             # 是否允许传送到玩家
    cooldown-seconds: 3       # 传送冷却
    safe-landing: true        # 自动找安全落点
    cross-world: true         # 允许跨世界
  spawn:
    safe-respawn: true        # 重生点被堵住时自动找安全落点
```
