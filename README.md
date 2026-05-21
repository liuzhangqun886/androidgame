# MiniGames · 小游戏合集

Android 原生小游戏合集，Kotlin + Jetpack Compose + Material 3，多模块结构。

## 计划清单

| 状态 | 游戏 | 模块 |
|---|---|---|
| ✅ | 贪吃蛇 Snake | `:game-snake` |
| ⏳ | 2048 | `:game-2048` |
| ⏳ | 打砖块 Breakout | `:game-breakout` |
| ⏳ | 扫雷 Minesweeper | `:game-minesweeper` |
| ⏳ | 推箱子 Sokoban | `:game-sokoban` |
| ⏳ | 摇骰子 Dice | `:game-dice` |

## 工程结构

```
:app           启动器（大厅列表）
:core          GameEntry 注册接口、共享主题
:game-snake    贪吃蛇
:game-...      其他游戏（后续追加）
```

## 技术栈

- Kotlin 2.0
- Jetpack Compose（BOM 2024.09）
- Material 3
- AGP 8.5
- compileSdk 34 / minSdk 26 / targetSdk 34
- JDK 17

## 构建

需要 Android Studio Koala / Ladybug 或更新版本：

```bash
./gradlew :app:assembleDebug
```

## 增加一个新游戏

1. 新建 `game-xxx/` 模块，参考 `game-snake/`
2. 在 `settings.gradle.kts` 加入 `include(":game-xxx")`
3. 在 `:app` 的 `build.gradle.kts` 增加 `implementation(project(":game-xxx"))`
4. 在 `app/.../GameRegistry.kt` 中追加 `GameEntry`
