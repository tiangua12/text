# AGENTS.md

给在本仓库工作的 AI 代理 / 协作者的项目指令与结构契约。
**操作部分（§3、§8、§9、§12）讲"怎么跑、怎么改"；架构部分（§4～§7、§10）讲"东西该放哪、为什么、谁依赖谁"。**

---

## 1. 这是什么

`text` 是一个 **Minecraft 1.21.1 / NeoForge 的 Create 附属模组**，核心内容是中国铁路风格的**轨道与转向架**：

- `text:track_32kg` —— 32kg/m 钢轨（复用 Create 标准轨距 `STANDARD`）
- `text:track_43kg` —— 43kg/m 钢轨，**自带独立轨距**（自定义 `TrackMaterial.TrackType`）
- `text:track_43kg_bogey` / `text:track_43kg_large_bogey` —— 43kg 专用转向架（SMALL / LARGE）
- 3 个转向架样式：Create 原版小、原版大、以及自定义 `hxd3c`（Co-Co 三轴电力机车转向架）

43kg 轨距与 Create 标准轨距**物理不兼容**（不能互转、不能换转向架、不能互相停留），
这条约束靠 13 个 mixin 在 Create 内部加装，是整个模组最容易踩坏的部分。

代码只有 26 个类，但**概念**有 6 个（轨道材料、轨距类型、轨道方块、转向架、轨图持久化、曲线碰撞）。
"架构看起来乱"的根源不是代码量，而是**边界没有被命名**——见 §4、§6、§7。

## 2. 技术栈（改动前必看）

| 项 | 值 | 出处 |
|---|---|---|
| Minecraft | 1.21.1 | `gradle.properties: minecraft_version` |
| NeoForge | 21.1.235 | `neo_version` |
| 构建插件 | `net.neoforged.moddev` **2.0.141** | `build.gradle` |
| Gradle wrapper | **9.2.1** | `gradle/wrapper/gradle-wrapper.properties` |
| Java | **21**（toolchain） | `build.gradle` + `org.gradle.java.installations.paths` |
| 映射 | Parchment 1.21.1 / 2024.11.17 | `parchment_*` |
| 硬依赖 | Create `6.0.10-281`（`slim`, `transitive=false`） | `dependencies` |
| 传递依赖（手动声明） | Ponder `1.0.82`、Flywheel `1.0.6`（compileOnly API + runtimeOnly）、Registrate `MC1.21-1.3.0+67` | 同上 |
| mod id / 包 | `text` / `com.easttown.text` | `gradle.properties` |
| 产物 | `build/libs/text-1.0.0.jar` | `mod_id` + `mod_version` |

Create 用的是 `slim` + `transitive = false`：**它的传递依赖不会自动进来**，删掉
`ponder` / `flywheel` / `registrate` 任意一行都会立刻编译失败。

## 3. 命令与环境

在仓库根目录执行（PowerShell 用 `.\gradlew.bat`，bash 用 `./gradlew`）：

```powershell
.\gradlew.bat compileJava        # 最快的编译反馈，改 Java 先跑这个
.\gradlew.bat build              # 完整构建；冷缓存 10~20 分钟，增量 1~3 分钟
.\gradlew.bat runClient          # 开发客户端，游戏目录 run/
.\gradlew.bat runServer          # 开发服务端（--nogui）
.\gradlew.bat runData            # 数据生成（当前项目未使用，输出会进 src/generated/resources）
.\gradlew.bat runGameTestServer  # GameTest（当前无 gametest，会直接退出）
.\gradlew.bat clean              # 清 build/；配置缓存开启，clean 后首次构建会重配置
```

验证产物：

```powershell
jar tf build/libs/text-1.0.0.jar | Select-String 'assets/text|data/create/tags|text.mixins.json'
```

### 环境约束（踩过）

- **网络必须走代理**。本机 DNS 对 `maven.neoforged.net` / `createmod` 等域名被污染，
  `gradle.properties` 末尾已写死 **HTTP 代理** `192.168.246.79:8502`。
  **不要改成 `systemProp.socksProxyHost`**：JVM 走 SOCKS 时在本地解析域名，会拿到污染 IP，
  表现为 `Remote host terminated the handshake`。
- **`JAVA_HOME` 指向 JDK 17**，gradle 用它跑构建没问题；编译目标由 toolchain 21 决定，
  具体路径由 `org.gradle.java.installations.paths=C:\Program Files\BellSoft\LibericaJDK-21`
  指定。删掉那行会退化成 foojay 现场下载 JDK 21（代理下极慢）。
- **git 会报 dubious ownership**（仓库 owner 是 `BUILTIN/Administrators`）。用
  `git -c safe.directory=F:/modmdk1.21.1/text status`，或一次性
  `git config --global --add safe.directory F:/modmdk1.21.1/text`。
- 构建目录已有历史产物（`build/` ≈100 MB，`run/` ≈28 MB），都在 `.gitignore` 里，别提交。
- 读中文文件**用 read / grep 工具**，不要用 shell `Get-Content`（输出层编码会把中文变成乱码）。

## 4. 架构

### 4.1 三条硬约束（先理解这三条，才看得懂为什么这么写）

| # | 约束 | 后果 |
|---|---|---|
| C1 | Create 没有提供"新轨距 / 新转向架 / 新轨图存储"的公开扩展点 | 只能继承 + 13 个 mixin 改内部行为；**mixin 是架构的一等公民，不是补丁** |
| C2 | 43kg 轨距必须与 `STANDARD` **物理隔离**（不能转向、不能换样式、不能搬车），但运行时列车/信号/车站必须仍从 `Create.RAILWAYS.trackNetworks` 查图 | 隔离靠 4 个"入口拦截"mixin；存储靠"运行时合并、持久化分离" |
| C3 | 轨道方块必须复用 Create 的 `AllBlockEntityTypes.TRACK` + `TrackBlockEntity` | 轨道方块只能继承 `TrackBlock`，不得注册新轨道 BE；只有转向架允许自有 BE |

另有一条 JVM 层面的隐性规则，违反后直接 `IllegalClassLoadError`：

> **R-JVM**：mixin handler 方法体被内联进目标类，**方法体内不得引用 `com.easttown.text.mixin` 包里的任何类**。
> 需要复用的逻辑必须下沉到非 mixin 包（这就是 `util/TrackCurveCollisionHelper` 存在的理由）。

### 4.2 分层（As-Is）

```
                        ┌──────────────────────────────────────────────────────┐
   L0 入口层            │ Text │ TextClient │ Config                          │
                        └───┬────────┬──────────────┬────────────────────────┘
                            │ register│              │ 配置事件
                        ┌───▼────────▼──────────────▼────────────────────────┐
   L1 注册层            │ AllBlocks │ AllItems │ AllBlockEntities │ AllTabs  │
                        │ AllTrackMaterials │ AllTrackTypes                  │
                        └───┬──────────────┬──────────────┬─────────────────┘
                            │ 持有实例      │              │
                        ┌───▼──────────┐ ┌─▼──────────────┐ ┌▼─────────────┐
   L2 内容层            │content/track │ │content/bogey   │ │client/       │
                        │Track43kgBlock│ │BogeyBlock + BE │ │Hxd3c 渲染    │
                        └───┬──────────┘ └─┬──────────────┘ └───────────────┘
                            │ 被 mixin 改    │
                        ┌───▼──────────────▼────────────────────────────────┐
   L3 行为补丁层        │ mixin/ （13 个，契约见 §4.7；只注入、不持有状态）  │
                        └───┬──────────────────────────────┬────────────────┘
                            │ 调用                          │ 调用
                        ┌───▼────────────────────┐  ┌─────▼─────────────────┐
   L4 服务层            │ util/CurveCollision    │  │ data/TextTrackSavedData│
                        └────────────────────────┘  └────────────────────────┘
```

**画出来才看得见的三个依赖环**（改任何一个都要按环的顺序想一遍）：

1. **注册层成环**：`AllBlocks → AllTrackMaterials → AllTrackTypes → AllBlocks`（方块 ↔ 材料 ↔ 样式）。
   拆分后环只剩这一个：`AllItems` / `AllBlockEntities` / `AllCreativeTabs` 都是单向引用前者，
   且引用都发生在注册 lambda 内（字段初始化阶段不 deref）。
   环靠 `NonNullSupplier.lazy` / `() -> () -> ...` 兜住；任何一处改成直接 `get()`
   都会在类初始化阶段炸（`NoClassDefFoundError` / 半初始化对象）。**全项目最脆的一环。**
2. **mixin → data → registry**：3 个持久化 mixin 都调 `TextTrackSavedData`，而它的
   `is43kgGraph` 又依赖 `AllTrackTypes.TRACK_43KG` 判轨距——持久化正确性依赖注册顺序。
3. **类型层 ⇄ mixin**：`Track43kgBlock` 的 javadoc 把 4 个 mixin 列为"轨距隔离的组成部分"，
   说明**行为的真正归属在 mixin**，方块类只是载体；代码里没有任何显式表达。

### 4.3 运行时生命周期

```
FML 构造 Text(modBus, container)
├── container.registerConfig(COMMON, Config.SPEC)
├── AllBlocks.register / AllItems.register / AllBlockEntities.register / AllCreativeTabs.register
│    └─ 期间 AllTrackMaterials.TRACK_32KG/TRACK_43KG 首次构建
│         └─ 引用 AllTrackTypes.TRACK_43KG（轨距）→ 触发 AllTrackTypes 类初始化
│              └─ 3 个 BogeyStyle 构建（renderer / visual 仍是 lazy）
└── modBus.addListener(Config::onConfigEvent)

FML 构造 TextClient(container)（仅客户端）
├── registerExtensionPoint(IConfigScreenFactory, ConfigurationScreen)
├── registerTrackModelSwaps()   // MODEL_SWAPPER，key = 方块 id（不是模型 id）
└── modBus.addListener(TextClient::registerBogeyRenderers)
     ├── registerBlockEntityRenderer(43kg BE, BogeyBlockEntityRenderer)
     └── VisualizerRegistry.setVisualizer(43kg BE, … skipVanillaRender = true)   // 防双渲染
```

### 4.4 43kg 轨图：运行时合并、持久化分离

```
放置 43kg 轨道 ─► TrackGraph 仍进 Create.RAILWAYS.trackNetworks（对列车/信号完全透明）
                └─ "物理隔离"由 TrackEdgeGaugeMixin 保证（跨轨距不能转向 ⇒ 图自然分开）

保存 create_tracks
├── RailwaySavedDataMixin.save(HEAD)       → 标记 text_track_43kg 为 dirty
├── RailwaySavedDataMixin.save(@ModifyArg) → 过滤掉含 43kg 边的图（不写进 create_tracks）
└── TextTrackSavedData.save()              → 从 RAILWAYS 抽出 43kg 图 → 写 text_track_43kg

加载
├── GlobalRailwayManagerMixin.loadTrackData(TAIL) → 43kg 图合并回 trackNetworks
├── TrainReadMixin.read(HEAD)                     → 列车反序列化前**再合并一次**
│      （RailwaySavedData.load 先读图后读车，此时 43kg 图还没合并 ⇒ 车 graph=null ⇒ 进存档全车脱轨）
└── GlobalRailwayManagerMixin.markTracksDirty(TAIL) → 同步标脏
```

这套协作有 4 个参与方、2 个混入点、1 个 SavedData，**没有单一入口**；改任意一处都要连着看另外三处。

### 4.5 轨距拦截矩阵（4 个入口，缺一即"能开过去"）

| 场景 | 拦截者 | 原版行为 | 失败表现（若移除） |
|---|---|---|---|
| 轨道边转向 | `TrackEdgeGaugeMixin.canTravelTo(RETURN)` | 只查方向对齐 | 列车在节点处拐进异轨距 |
| 扳手切换转向架样式 | `BogeyStyleGaugeMixin.useItemOn(HEAD)` | 组内循环切换 | 43kg 车变标准转向架、凭空"换轨距" |
| 扳手搬迁列车 | `TrainRelocatorGaugeMixin.relocate(HEAD)` | 不查目标轨距 | 整列车被搬到异轨距轨道 |
| 行驶中不兼容 | `TrainDerailMixin.tick(TAIL)` | 仅 `blocked`（下 tick 可被输入恢复） | 车在异轨距上"慢慢挪" |
| 装配转向架 | `Track43kgBlock.getBogeyAnchor`（覆写，非 mixin） | 硬编码标准转向架 | 43kg 轨道上装出标准转向架 |

### 4.6 弯道碰撞数据流（跨 4 处代码）

```
放置弯道  TrackPlacementCurveMixin(placeTracks RETURN)
          ├─ TrackCurveCollisionHelper.getCurve(info)     ← 反射读包私有字段 curve
          └─ fillCurveCollision(level, bc, material)
               ├─ 只服务端 / 只 Config.CURVE_COLLISION_ENABLED / 只 TRACK_32KG / 只平面弯道
               └─ 把曲线 1.3 格内的可替换方块设为 SHAPE=NONE + HAS_BE=false 的轨道方块
                  （blockstate 映射 air 模型 ⇒ 不可见，但保留 16×16×2 碰撞、不建 BE）

拆除节点  TrackBlockCurveMixin(onRemove HEAD) → clearCurveCollision(bc) 按包围盒清 NONE 块

关闭配置  Config.onConfigEvent → 遍历所有已加载区块的所有 Y 层，删掉所有 NONE 块
```

现状缺陷：`FILL_DISTANCE = 1.3` 是硬编码（不在 Config）；关闭配置的清理是**主线程全图扫描**，
开服/改配置时明显卡顿；反射读 `PlacementInfo.curve` 无兜底日志。

### 4.7 Mixin 契约表（13 个，改任何一个都要过一遍）

| # | 类 | 目标 | 注入点 | 为什么需要 | 失败表现 |
|---|---|---|---|---|---|
| 1 | `TrackEdgeGaugeMixin` | `TrackEdge` | `canTravelTo` RETURN | 跨轨距禁止转向 ⇒ 图物理隔离 | 列车可拐进异轨距 |
| 2 | `BogeyStyleGaugeMixin` | `AbstractBogeyBlock` | `useItemOn` HEAD | 禁止跨轨距换样式 | 转向架"变轨距" |
| 3 | `TrainRelocatorGaugeMixin` | `TrainRelocator` | `relocate` HEAD | 禁止把车搬到异轨距 | 车被搬走 |
| 4 | `TrainDerailMixin` | `Train` | `tick` TAIL | 不兼容 ⇒ 脱轨（而非 blocked） | 车在异轨距"磨蹭" |
| 5 | `TrackPlacementCurveMixin` | `TrackPlacement` | `placeTracks` RETURN | 弯道补碰撞 | 弯道内部踩空 |
| 6 | `TrackBlockCurveMixin` | `TrackBlock` | `onRemove` HEAD | 拆弯道清填充块 | 幽灵隐形方块 |
| 7 | `BezierSegmentAnglesMixin` | `BezierConnection$SegmentAngles` | `<init>` Redirect(0.965) | 弯道铁轨横向偏移改 1435mm | 直道/弯道铁轨错位 |
| 8 | `BezierGirderAnglesMixin` | `BezierConnection$GirderAngles` | `<init>` Redirect(0.965) | 弯道桁架梁偏移 | 梁与轨不齐 |
| 9 | `RailwaySavedDataMixin` | `RailwaySavedData` | `save` HEAD + ModifyArg | 43kg 图从 create_tracks 剔除 | 图重复写/丢失 |
| 10 | `GlobalRailwayManagerMixin` | `GlobalRailwayManager` | `loadTrackData` TAIL、`markTracksDirty` TAIL | 加载合并 + 标脏 | 存档后 43kg 线消失 |
| 11 | `TrainReadMixin` | `Train` | `read` HEAD | 读车前先合并 43kg 图 | 进存档全车脱轨 |
| 12 | `CTrainsMixin` | `CTrains` | `<init>` ModifyConstant(128) | 放开轨道最长放置长度 | 长轨放不下 |
| 13 | `TrackTargetingBlockItemMixin` | `TrackTargetingBlockItem` | `useOn` 两处常量 16 | 信号/车站绑定距离走配置 | 绑定距离写死 |

分组：**轨距隔离 1-4**、**曲线碰撞 5-6**、**曲线几何 7-8**、**持久化 9-11**、**放宽限位 12-13**。
新增 mixin 前先确认是否属于已有分组——多数需求应当扩展现有 mixin，而不是加第 14 个。

## 5. 目录地图（As-Is）

```
src/main/java/com/easttown/text/
├── Text.java                  # @Mod 主类：注册 4 个注册表类 + 配置事件
├── TextClient.java            # 客户端：MODEL_SWAPPER 注册 TrackModel、BE 渲染器 + Flywheel visualizer
├── Config.java                # ModConfigSpec（COMMON）：轨道定位距离、曲线碰撞开关 + 关闭时清理
├── registry/                  # 只放 All* 注册表（一个概念一个类）
│   ├── AllBlocks.java         # Block 注册 + 方块属性工厂
│   ├── AllItems.java          # Item 注册（TrackBlockItem / BlockItem）
│   ├── AllBlockEntities.java  # BlockEntityType（只有 43kg 转向架需要）
│   ├── AllCreativeTabs.java   # 创造模式标签页
│   ├── AllTrackMaterials.java # TrackMaterial（32kg、43kg），双层 lazy 解耦初始化顺序
│   └── AllTrackTypes.java     # 43kg TrackType + 3 个 BogeyStyle（名不副实，见 P1-1）
├── content/
│   ├── track/Track43kgBlock.java              # extends TrackBlock，只覆写 getBogeyAnchor
│   └── bogey/Track43kgBogeyBlock.java          # extends StandardBogeyBlock
│            Track43kgBogeyBlockEntity.java    # extends StandardBogeyBlockEntity
├── client/                    # 纯客户端渲染（不发往服务端逻辑）
│   ├── Hxd3cBogeyRenderer.java   # 经典渲染路径（放置状态 / 物品栏）
│   └── Hxd3cBogeyVisual.java     # Flywheel 视觉（contraption 内）
├── mixin/                     # 13 个，契约见 §4.7
├── data/TextTrackSavedData.java          # 43kg 独立存档 text_track_43kg
└── util/TrackCurveCollisionHelper.java   # 名字叫 util，实际是"mixin 可调用的服务层"（R-JVM）
src/main/resources/
├── text.mixins.json           # mixin 必须在 mixins 数组登记（defaultRequire: 1）
├── assets/text/{blockstates,models,textures,lang}
└── data/{create,minecraft,neoforge,text}/…
src/main/templates/META-INF/neoforge.mods.toml   # 由 generateModMetadata 展开变量
```

### 资源 ↔ 代码对应（手工同步，无生成脚本）

| 概念 | 代码 | blockstate | model(s) | texture | tag | loot |
|---|---|---|---|---|---|---|
| 32kg 轨道 | `AllBlocks.TRACK_32KG` + `AllItems.TRACK_32KG` + Create `TrackBlock` | `track_32kg.json` | 12 组形状 + `track_32kg/{tie,segment_left,segment_right}` | `block/track/track_32kg/*` | 5 个 | 自身 |
| 43kg 轨道 | `Track43kgBlock` + `AllTrackMaterials.TRACK_43KG` | `track_43kg.json` | 同上（43kg 目录） | 同上 | 5 个 | 自身 |
| 43kg 转向架 | `Track43kgBogeyBlock` + BE + 3 个 BogeyStyle | `track_43kg_bogey.json` / `…_large_bogey.json` | `item/*`、`hxd3c/bogey/*` | — | — | **`create:railway_casing`**（刻意：拆转向架只回收外壳） |

语言键固定 7 条：`itemGroup.text`、`block.text.<id>`×4、`bogey.style.text.<style>`×3。
**加一种轨道 = 手改 11+ 个资源文件**（5 tag + 1 loot + 2 lang + blockstate + models + textures），
当前没有脚本也没有校验，是必漏项。

### Create 轨道系统接入面（改这里前先读懂）

- **注册**：`TrackMaterial`（`TrackMaterialFactory.make().lang().block().particle().trackType().standardModels().build()`）
  → `TrackType`（即"轨距"）→ `TrackBlock` / `TrackBlockItem`。
- **BE**：32kg/43kg 轨道复用 Create 的 `AllBlockEntityTypes.TRACK`（C3），不要注册新的轨道 BE 类型。
- **渲染**：非曲线形状走 blockstate + OBJ 模型；曲线走 Flywheel `PartialModel`
  `text:block/track/<material>/{tie,segment_left,segment_right}`；上升轨道的倾斜由
  `TextClient` 注册的 `TrackModel` 包装提供（key = **方块注册 id**，不是模型 id）。

## 6. 现状问题清单

### P0 结构性问题

- ~~**P0-1**~~ **已修**：`Hxd3cBogeyRenderer/Visual` 已移到 `client/`。它们仍被 `AllTrackTypes`
  （服务端加载路径）静态 import，但实例化是 lazy supplier，安全。
- **P0-2 43kg 持久化没有单一入口**（§4.4）：4 个参与方靠注释维系。建议抽
  `persistence/RailwayGraphBridge`（`excludeFromCreateTracks` / `mergeInto` / `markDirty`）作为唯一 API。
- **P0-3 轨距判定散落 4 处**，每处各写一遍"取 material → 比较 trackType"。建议抽 `GaugeRules`。
- **P0-4 曲线碰撞横跨 mixin / util / Config 三处**，且关闭配置时做全图扫描（§4.6）。
- **P0-5 注册层成环**（§4.2-1）：约束已写进 `AllBlocks` / `AllTrackMaterials` 的 javadoc，
  但没有编译期保护。改注册代码前先读那两段注释。
- **P0-6 R-JVM 无机械化保护**：只写在注释里。至少列入 review 清单，最好在构建前加一条 grep 校验。

### P1 包归属与命名

- **P1-1** `AllTrackTypes` 名不副实：既有 `TrackType`（轨距）又有 3 个 `BogeyStyle`。建议改名 `AllTrackStyles` 或拆分。
- ~~**P1-2**~~ **已修**：`registry/` 现在只放 `All*` 注册表；方块/BE 在 `content/`，渲染在 `client/`。
- **P1-3** `util/` 只有 1 个类，身份是"mixin 可调用服务层"，不是通用工具。
- **P1-4** 三套命名前缀（`All*` / `Track43kg*` / `Hxd3c*`）各都合理，但没有任何地方写明这个约定。

### P2 注释漂移与魔法数（批次 1 一并修）

| # | 位置 | 问题 |
|---|---|---|
| P2-1 | `TrainDerailMixin` javadoc | 引用**不存在**的 `CarriageBogeyGaugeMixin`（实际是 `BogeyStyleGaugeMixin`） |
| P2-2 | `Track43kgBogeyBlockEntity` javadoc | 引用不存在的 `TRACK_43KG_COMMON_STYLE`（实际 `TRACK_43KG_STYLE`） |
| P2-3 | `BezierGirderAnglesMixin` | 注释说"梁设定为 ±1.0 格"，常量实为 `1.435/2+0.04 = 0.7575`；还留着"目前两者都返回相同偏移量"的临时语气 |
| P2-4 | `BezierSegmentAnglesMixin` | 注释说每侧 0.7625，代码算得 0.7575（与 P2-3 同源，两常量等值） |
| P2-5 | `CTrainsMixin` | 无任何注释就把 128 改成 `Integer.MAX_VALUE`，全局副作用未记录 |
| P2-6 | `TrackTargetingBlockItemMixin` | `double` 变体返回 `getAsInt()`，未说明"配置只有 int" |
| P2-7 | `CurveCollision.FILL_DISTANCE` | 魔法数 1.3，未进 Config |
| P2-8 | ~~`AllTracks`~~ | **已随拆分消失**：拆成 4 个注册类，新文件统一 4 空格缩进，被注释掉的 `.noCollission()` 改为 javadoc 说明 |

## 7. 目标结构（To-Be）与迁移进度

原则：**注册表 / 内容类型 / 客户端渲染 / 功能特性 / 持久化 五件事各占一个包**，`mixin/` 只留注入点。

```
com.easttown.text
├── Text.java  TextClient.java  Config.java          # 入口层不变
├── registry/            # ✅ 已达成：只放 All*
│   ├── AllBlocks.java        # Block
│   ├── AllItems.java         # Item
│   ├── AllBlockEntities.java # BlockEntityType
│   ├── AllCreativeTabs.java  # CreativeModeTab
│   ├── AllTrackMaterials.java
│   └── AllTrackTypes.java    # 待改名 AllTrackStyles
├── content/             # ✅ 已达成
│   ├── track/Track43kgBlock.java
│   └── bogey/Track43kgBogeyBlock.java, Track43kgBogeyBlockEntity.java
├── client/              # ✅ 已达成（Hxd3c 双类已迁入）
│   ├── Hxd3cBogeyRenderer.java, Hxd3cBogeyVisual.java
│   └── TrackModelSwaps.java  # ← 待抽：MODEL_SWAPPER 注册目前还在 TextClient
├── feature/gauge/       # ← 待做（批次 3）
│   ├── GaugeRules.java       # 新增：isSameGauge(material, style) 等唯一判定入口
│   └── (4 个轨距 mixin)
├── feature/curvecollision/   # ← 待做（批次 2 尾 / 3）
│   ├── CurveCollision.java   # ← TrackCurveCollisionHelper 改名
│   └── (2 个曲线 mixin)
├── persistence/         # ← 待做（批次 3）
│   ├── TextTrackSavedData.java
│   └── (3 个持久化 mixin)
└── mixin/               # 13 个，暂时全部留在这里；feature 分包后此包可消失
```

| 现状 | 目标 | 批次 | 状态 |
|---|---|---|---|
| `AllTracks`（Block+Item+BE+Tab 一锅） | `AllBlocks` / `AllItems` / `AllBlockEntities` / `AllCreativeTabs` | 2 | ✅ **已完成**（compileJava 通过） |
| `Track43kgBlock`（在 registry） | `content/track` | 2 | ✅ 已完成 |
| `Track43kgBogeyBlock(Entity)`（在 registry） | `content/bogey` | 2 | ✅ 已完成 |
| `Hxd3cBogeyRenderer/Visual`（在 registry） | `client/` | 2 | ✅ 已完成（renderer/visual 仍 lazy） |
| `util/TrackCurveCollisionHelper` | `feature/curvecollision/CurveCollision` | 2 | 待做（R-JVM 约束不变，仍须在 mixin 包外） |
| `AllTrackTypes` | `registry/AllTrackStyles` | 2 | 待做 |
| 13 个 mixin | 按 feature 分包 | 3 | 待做（`text.mixins.json` 的 `package` 与 `mixins` 数组要同步改） |
| 4 个轨距 mixin 内重复判定 | 抽 `GaugeRules` | 3 | 待做（行为等价才允许合并） |
| `Config.onConfigEvent` 全图扫描 | "标记 dirty + 下次加载懒清理" | 3 | 待做（涉及存档行为） |

### 重构路线（每批独立可构建、可验证）

1. **批次 1 · 零风险**：修 P2 全部（注释漂移 + 常量注释对齐 + 补 `CTrainsMixin` 说明 + 统一缩进）。只改注释和空白，`compileJava` 即可验证。
2. **批次 2 · 纯移动**：按 §7 拆包，只改 package 与 import；`Hxd3c*` 移动时保持 lazy supplier 形态。验证：`build` + jar 内文件清单逐条 diff（应与改动前完全一致）。
3. **批次 3 · 行为等价重构**：抽 `GaugeRules`、抽 `RailwayGraphBridge`、`FILL_DISTANCE` 入 Config、配置关闭清理改懒执行。每个重构单独提交，各自附 runClient 冒烟。
4. **批次 4 · 防回归**：`text.mixins.json` 与包结构一致性检查、资源清单校验（tags / loot / lang 键比对）。

## 8. 代码约定

- 注册类：`public final class Xxx`，私有构造器，静态字段全大写 `DeferredHolder` / `DeferredRegister`。
- NeoForge 21.1 的 `@Nonnull` 分析噪声大：现有代码大量使用 `@SuppressWarnings("null")`
  （`DeferredHolder.get()`、`BlockEntityType.Builder.build(null)` 等）。**保留它们**，新代码照此办理。
- mixin handler 方法统一前缀 `text$`（如 `text$rejectCrossGaugeTurn`）；注入点选 `@Inject` /
  `@ModifyConstant` / `@Redirect`，不用 `@Overwrite`。target 为内部类时用 `targets = "全限定$内部类"` 字符串。
- mixin 类命名 `<目标类>Mixin`，并在 `text.mixins.json` 的 `mixins` 数组里登记，否则静默不生效。
- **handler 方法体内只引用非 mixin 包**（R-JVM，§4.1）。
- Javadoc **用中文写"为什么"**：Create 侧的原始行为、注入位置、改动的副作用
  （现有 13 个 mixin 全部如此），这是本仓库最重要的知识载体。
- 局部变量用 `var`；单语句 `if` 不加花括号；缩进 4 空格（个别文件有历史 tab，新增代码用空格）。
- 静态初始化顺序：`TrackMaterial` 与方块 holder 互相引用，必须保持双层 `NonNullSupplier.lazy`，
  不要在静态块里立即 `get()`。

## 9. 资源约定

- **1.21 目录是单数**：`data/<ns>/tags/block`、`data/<ns>/tags/item`、`data/text/loot_table/blocks/`。
- 自定义 OBJ 加载器写 `"loader": "neoforge:obj"`（1.21.1 **没有** `forge:obj` 别名），
  配 `"flip_v": true`、`"render_type": "minecraft:cutout_mipped"`；MTL 的 `map_Kd` 用
  `text:block/track/<material>/<tex>` 形式资源路径，`newmtl` 名字必须与 OBJ 的 `usemtl` 逐字一致。
- blockstate variants 键：`shape=<TrackShape 小写>,turn=<bool>,waterlogged=<bool>`，
  19 个 shape × 4 = 76 个变体；`shape=none` 指向 `minecraft:block/air`。
- 语言键：`block.text.<registry名>`、`bogey.style.text.<style名>`、`itemGroup.text`、
  `text.configuration.*`。`en_us.json` 与 `zh_cn.json` 键必须同步。
- 当前**不使用数据生成器**（`src/generated` 不存在），资源全部手写 JSON。不要顺手引入 datagen
  流程（会打乱 `processResources` 与 `generateModMetadata` 的顺序）。
- 文件编码：全部 **UTF-8（无 BOM）**，`javac` 也按 UTF-8 编译。

## 10. 扩展 checklist

**加一种新轨道（新材料，沿用现有轨距）**
`AllTrackMaterials` → `AllBlocks`（方块）+ `AllItems`（物品）+ `AllCreativeTabs`（展示项）→ blockstate（19 shape × 4）→ 12 组形状模型 +
3 个 partial 模型 → 贴图 → 5 个 tag → loot_table → `en_us` / `zh_cn`。
若是**新轨距**，再加 `AllTrackTypes` 的 `TrackType` + 专属转向架方块 + BE + `GaugeRules` 白名单 +
§4.5 的四个拦截点复查。

**加一种转向架样式**
`AllTrackTypes`（新 `BogeyStyle`；cycle group 相同才能扳手循环切换）→ 需要新外观则在
`client/` 加 renderer + visual → 模型 → lang ×2 → 若尺寸集合与现有不同，复查
`BogeyStyleGaugeMixin` 的切换逻辑（附录 A 有踩坑记录）。

**加一个 mixin**
查 §4.7 分组表 → 归入已有分组 → 在 `text.mixins.json` 登记 → handler 命名 `text$xxx`
→ 方法体内只引用非 mixin 包 → 在 §4.7 表格加一行。

## 11. 已知缺口 / 待补

1. **`neoforge.mods.toml` 没有声明 Create 依赖**（目前只有 `neoforge` + `minecraft`）。
   生产环境缺 Create 时只会静默出错，发布前应补：

   ```toml
   [[dependencies.${mod_id}]]
       modId="create"
       type="required"
       versionRange="[6.0.10,)"
       ordering="NONE"
       side="BOTH"
   ```

   （改 `src/main/templates/META-INF/neoforge.mods.toml`，由 `generateModMetadata` 展开。）
2. `Config.onConfigEvent` 用反射调用 `ChunkMap#getChunks()`（protected），依赖 NeoForge 模块开放；
   升级 MC 版本时这是首批要复查的点。
3. `run/mods/` 只有两个 Sodium jar（客户端性能测试）；Create 与 Ponder/Flywheel 走 dev classpath，
   不要把它们的 jar 丢进 `run/mods/`（会和 dev 依赖冲突）。
4. `.verify_*` 目录是历史验证残留，已 gitignore，可安全删除。
5. `PLAN.md` 是 32kg 轨道那一阶段的完整设计记录（Create API 核实表、资产改写规则、风险清单），
   涉及轨道/模型细节时值得先读。

## 12. 交付前检查

1. `.\gradlew.bat build` 0 error；`build/libs/text-1.0.0.jar` 时间戳刷新。
2. jar 内抽查：`assets/text/blockstates/track_43kg.json`、`assets/text/models/block/track/*.obj`、
   `text.mixins.json`、`data/create/tags/block/tracks.json`。
3. 结构类改动额外核验：
   - `text.mixins.json` 的 `mixins` 条数 == 带 `@Mixin` 的类数；`package` 字段与目录一致。
   - 移动式重构：jar 内文件清单与改动前**逐条 diff 应完全一致**。
4. `runClient` 冒烟：创造页四件物品齐；直/斜/十字/上升/传送形态渲染正常；曲线（BE 部分）无紫黑
   缺失纹理；43kg 与标准轨在**转、换、搬、开**四个入口都被拦；挖掘掉落正确（转向架掉 `create:railway_casing`）；
   控制台无 `Exception loading blockstate definition`、无 `Missing model`、无 mixin 应用失败日志。
5. 存档往返：放 43kg 线 → 退出 → `create_tracks` 里**没有** 43kg 图、`text_track_43kg` 里有 → 重进存档列车不脱轨。
6. 改了 mixin：确认已登记，并看 `run/.mixin.out/`（`-Dmixin.debug.export=true` 已开）里的导出类确认注入落地。

---

## 附录 A：从注释里捡回来的踩坑记录（勿删）

- **转向架样式不能合并成"一个样式两档"**：扳手切换时若目标样式不支持当前尺寸，Create 会把尺寸改到
  目标样式支持的档位并换方块，合并后会卡死在某一档再也切不回（`AllTrackTypes` javadoc 有完整推演）。
- **`StandardBogeyBlockEntity` 需要 `(type,pos,state)` 构造器**，但 `BlockEntityType.Builder.of` 只接受
  两参工厂 ⇒ 现有 `cachedType` 静态缓存 + `init(type)` 的变通写法（由 `AllBlockEntities` 在注册时调用）。
- **43kg 转向架 BE 必须有独立类型**：复用 `create:bogey` 会因 `validBlocks` 校验直接崩。
- **`skipVanillaRender` 必须返回 true**：否则 BE 渲染器与 Flywheel visual 同时画，转向架被画两层
  （HXD3C 三轴变"六轴"）。
- **HXD3C 模型超出 Create 的 ±2 格渲染包围盒** ⇒ `createRenderBoundingBox().inflate(4)`，否则两端被裁剪。
- **`TrackPlacement.PlacementInfo.curve` 是包私有**，用反射读而不是 `@Accessor`：`@Accessor` 必须放
  mixin 包，而 handler 内联后引用 mixin 包类会 `IllegalClassLoadError`（R-JVM）。
- **`TrackBlock.HAS_BE` 是隐形填充块的判据**：`SHAPE=NONE && !HAS_BE` 才是本模组生成的碰撞块，
  判断漏掉任一条都会误删玩家的正常轨道。
