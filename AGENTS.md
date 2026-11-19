# AGENTS.md

给在本仓库工作的 AI 代理 / 协作者的项目指令与结构契约。
**开工前必读本文档。需要理解"为什么这么设计"时再读 `docs/ARCHITECTURE.md`（数据流、踩坑记录、问题推演都在那里）。**

---

## 1. 这是什么

`text` 是一个 **Minecraft 1.21.1 / NeoForge 的 Create 附属模组**，核心内容是中国铁路风格的**轨道与转向架**：

- `text:track_32kg` —— 32kg/m 钢轨，轨距 `text:gauge_1435`
- `text:track_43kg` —— 43kg/m 钢轨，轨距 `text:gauge_1435`（**与 32kg 共用同一 `TrackType`**）
- `text:track_43kg_bogey` / `text:track_43kg_large_bogey` —— 专用转向架（SMALL / LARGE）
- 3 个转向架样式：Create 原版小、原版大、自定义 `hxd3c`（Co-Co 三轴电力机车转向架）

**轨距模型**：两种钢轨共用自定义轨距 `text:gauge_1435`（`AllTrackTypes.GAUGE_1435`），
所以彼此可以互转、共用同一套转向架。Create 的 `create:standard` 是另一个 `TrackType`
对象，与本模组轨距**物理不兼容**（不能互转、不能换转向架、不能互相停留），
靠 13 个 mixin 在 Create 内部加装，是整个模组最容易踩坏的部分。

> Create 的 `TrackType` **没有轨距数值字段**，只有 `id` + `factory`，判定一律用引用相等。
> 「1435mm」这个数只活在渲染层（`BezierConnection` 硬编码 ±0.965，已由 mixin 改成
> 1435mm 对应偏移）。**改轨距 = 改 `TrackType` 引用，不是改数字。**

代码只有 35 个类，但**概念**有 7 个（轨道材料、轨距类型、轨道方块、转向架、轨图持久化、曲线碰撞、碰撞盒调试）。

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
| 产物 | `build/libs/text-1.2.0.jar` | `mod_id` + `mod_version` |

Create 用的是 `slim` + `transitive = false`：**它的传递依赖不会自动进来**，删掉
`ponder` / `flywheel` / `registrate` 任意一行都会立刻编译失败。

## 3. 命令与环境

在仓库根目录执行（PowerShell 用 `.\gradlew.bat`，bash 用 `./gradlew`）：

```powershell
.\gradlew.bat compileJava        # 最快的编译反馈，改 Java 先跑这个
.\gradlew.bat build              # 完整构建；冷缓存 10~20 分钟，增量 1~3 分钟
.\gradlew.bat runClient          # 开发客户端，游戏目录 run/
.\gradlew.bat runServer          # 开发服务端（--nogui）
.\gradlew.bat runData            # 数据生成（当前项目未使用）
.\gradlew.bat runGameTestServer  # GameTest（当前无 gametest，会直接退出）
.\gradlew.bat clean              # 清 build/；配置缓存开启，clean 后首次构建会重配置
```

验证产物：
```powershell
jar tf build/libs/text-1.2.0.jar | Select-String 'assets/text|data/create/tags|text.mixins.json'
```

### 环境约束（踩过）

- **网络必须走代理，但代理配置不在仓库里**。本机 DNS 对 `maven.neoforged.net` /
  `createmod` 等域名被污染，默认走 **HTTP 代理** `192.168.246.79:8502`，配置写在
  **用户级** `C:\Users\<你>\.gradle\gradle.properties`（该目录是指向 `F:\gradle` 的
  Junction），仓库的 `gradle.properties` 里**只留注释和踩坑记录**。
  **不要改回 `systemProp.socksProxyHost`**：JVM 走 SOCKS 时在本地解析域名，会拿到污染 IP，
  表现为 `Remote host terminated the handshake`。
  **也不要把代理写进项目的 `gradle.properties`**：那文件会推上 GitHub，CI 会把每次
  依赖请求发给一个不可达的内网地址，10 秒连接超时后整轮失败（症状：`exit code 1` +
  `No Gradle build results detected` + `timeout (10000ms)`）。
- **`JAVA_HOME` 指向 JDK 17**，gradle 用它跑构建没问题；编译目标由 toolchain 21 决定，
  具体路径由用户级 `org.gradle.java.installations.paths=C:\Program Files\BellSoft\LibericaJDK-21`
  指定。删掉那行会退化成 foojay 现场下载 JDK 21（代理下极慢）；CI 上不需要它
  （`actions/setup-java` 装的 JDK 就是 `JAVA_HOME`，toolchain 直接命中）。
- **git 会报 dubious ownership**（仓库 owner 是 `BUILTIN/Administrators`）。用
  `git -c safe.directory=F:/modmdk1.21.1/text status`，或一次性
  `git config --global --add safe.directory F:/modmdk1.21.1/text`。
- 构建目录已有历史产物（`build/` ≈100 MB，`run/` ≈28 MB），都在 `.gitignore` 里，别提交。
- 读中文文件**用 read / grep 工具**，不要用 shell `Get-Content`（输出层编码会把中文变成乱码）。

### CI（GitHub Actions）

工作流只有一个：`.github/workflows/build.yml`。**它跑的是和本地完全相同的 `./gradlew build`**，
唯一差异来自 `~/.gradle/gradle.properties`（CI 上没有，也不需要）。

| 触发 | 行为 |
|---|---|
| push（任意分支） | 构建 + 上传 artifact `text-jar`（`build/libs/*.jar`） |
| pull_request | 同上，但不进 Release 步骤（token 只读，且该步骤带 tag 条件） |
| push tag（`v*`） | 构建 + 上传 artifact + **自动建 GitHub Release**，附 jar 并自动生成 release notes |

流水线要点：

- `actions/checkout@v7` 带 `fetch-depth: 0` + `fetch-tags: true` —— release notes 需要完整历史与 tag。
- `actions/setup-java@v6`（temurin 21）装的就是 `JAVA_HOME`，toolchain 直接命中，
  所以仓库里**不需要** `org.gradle.java.installations.paths`。
- 构建命令固定 `./gradlew build --stacktrace --warning-mode all`：前者让失败可定位，
  后者把 Gradle 弃用警告暴露出来（当前有 Gradle 10 相关的 deprecation）。
- `chmod +x ./gradlew` 是兜底：Windows 上提交的 `gradlew` 可能丢可执行位。
- 权限只给 job 级 `contents: write`（Release 需要），不要提到文件顶层。
- `fail_on_unmatched_files: true`：版本号改了名却匹配不到 jar 时宁可失败，也不产生空 Release。

**CI 挂掉的第一嫌疑永远是仓库里的机器专属配置**。症状：`exit code 1` +
`No Gradle build results detected` + `timeout (10000ms)` —— 说明依赖请求发给了一个不可达的内网地址。
仓库的 `gradle.properties` 只允许放"对 CI 也成立"的东西（版本号、编译选项）；
代理、本机 JDK 路径一律留在用户级文件里。

## 4. 硬约束（违反即坏）

### 4.1 三条架构硬约束

| # | 约束 | 后果 |
|---|---|---|
| C1 | Create 没有提供"新轨距 / 新转向架 / 新轨图存储"的公开扩展点 | 只能继承 + 13 个 mixin 改内部行为；**mixin 是架构的一等公民，不是补丁** |
| C2 | 本模组轨距 `text:gauge_1435`（32kg + 43kg 共用）必须与 Create 的 `create:standard` **物理隔离**（不能转向、不能换样式、不能搬车），但运行时列车/信号/车站必须仍从 `Create.RAILWAYS.trackNetworks` 查图 | 隔离靠 4 个"入口拦截"mixin；存储靠"运行时合并、持久化分离" |
| C3 | 轨道方块必须复用 Create 的 `AllBlockEntityTypes.TRACK` + `TrackBlockEntity` | 轨道方块只能继承 `TrackBlock`，不得注册新轨道 BE；只有转向架允许自有 BE |

### 4.2 R-JVM（违反即 `IllegalClassLoadError`）

> **mixin handler 方法体被内联进目标类，方法体内不得引用 `com.easttown.text.mixin` 包里的任何类。**
> 需要复用的逻辑必须下沉到非 mixin 包（这就是 `util/TrackCurveCollisionHelper` 存在的理由）。

### 4.3 注册层成环（全项目最脆的一环）

`AllBlocks → AllTrackMaterials → AllTrackTypes → AllBlocks`。
环靠 `NonNullSupplier.lazy` / `() -> () -> ...` 兜住；**任何一处改成直接 `get()` 都会在类初始化阶段炸**
（`NoClassDefFoundError` / 半初始化对象）。改注册代码前先读 `AllBlocks` / `AllTrackMaterials` 的 javadoc。

### 4.4 轨距拦截矩阵（4 个入口，缺一即"能开过去"）

| 场景 | 拦截者 | 原版行为 | 失败表现（若移除） |
|---|---|---|---|
| 轨道边转向 | `TrackEdgeGaugeMixin.canTravelTo(RETURN)` | 只查方向对齐 | 列车在节点处拐进异轨距 |
| 扳手切换转向架样式 | `BogeyStyleGaugeMixin.useItemOn(HEAD)` | 组内循环切换 | 本模组车变标准转向架、凭空"换轨距" |
| 扳手搬迁列车 | `TrainRelocatorGaugeMixin.relocate(HEAD)` | 不查目标轨距 | 整列车被搬到异轨距轨道 |
| 行驶中不兼容 | `TrainDerailMixin.tick(TAIL)` | 仅 `blocked`（下 tick 可被输入恢复） | 车在异轨距上"慢慢挪" |
| 装配转向架 | `Track43kgBlock.getBogeyAnchor`（覆写，非 mixin） | 硬编码标准转向架 | 本模组轨道上装出标准转向架 |

## 5. 现状坐标

### 5.1 目录树

```
src/main/java/com/easttown/text/
├── Text.java  TextClient.java  Config.java
├── registry/                  # 只放 All* 注册表
│   ├── AllBlocks.java  AllItems.java  AllBlockEntities.java  AllCreativeTabs.java
│   ├── AllTrackMaterials.java # TrackMaterial（32kg、43kg），双层 lazy
│   └── AllTrackTypes.java     # 名不副实：TrackType + 3 个 BogeyStyle（P1-1）
├── content/
│   ├── track/Track43kgBlock.java
│   └── bogey/Track43kgBogeyBlock.java, Track43kgBogeyBlockEntity.java
├── client/Hxd3cBogeyRenderer.java, Hxd3cBogeyVisual.java
│          TrainBoxRenderer.java, ClientTrainBoxState.java   # /textbox 线框（仅客户端）
├── command/TextCommands.java  # /textbox 调试指令
├── net/TextNetwork.java, TrainBoxPayload.java   # 服务端→客户端的开关包
├── mixin/                     # 13 个，契约见 §5.2
├── data/TextTrackSavedData.java
└── util/TrackCurveCollisionHelper.java   # 实际是"mixin 可调用的服务层"（R-JVM）
         TrainBoxGeometry.java              # 车厢碰撞盒几何（双端共用）
src/main/resources/
├── text.mixins.json           # mixin 必须在此登记（defaultRequire: 1）
├── assets/text/{blockstates,models,textures,lang}
└── data/{create,minecraft,neoforge,text}/…
src/main/templates/META-INF/neoforge.mods.toml   # generateModMetadata 展开变量
```

### 5.2 mixin 契约表（13 个）

| # | 类 | 目标 | 注入点 | 失败表现 |
|---|---|---|---|---|
| 1 | `TrackEdgeGaugeMixin` | `TrackEdge` | `canTravelTo` RETURN | 列车可拐进异轨距 |
| 2 | `BogeyStyleGaugeMixin` | `AbstractBogeyBlock` | `useItemOn` HEAD | 转向架"变轨距" |
| 3 | `TrainRelocatorGaugeMixin` | `TrainRelocator` | `relocate` HEAD | 车被搬走 |
| 4 | `TrainDerailMixin` | `Train` | `tick` TAIL | 车在异轨距"磨蹭" |
| 5 | `TrackPlacementCurveMixin` | `TrackPlacement` | `placeTracks` RETURN | 弯道内部踩空 |
| 6 | `TrackBlockCurveMixin` | `TrackBlock` | `onRemove` HEAD | 幽灵隐形方块 |
| 7 | `BezierSegmentAnglesMixin` | `BezierConnection$SegmentAngles` | `<init>` Redirect(0.965) | 直道/弯道铁轨错位 |
| 8 | `BezierGirderAnglesMixin` | `BezierConnection$GirderAngles` | `<init>` Redirect(0.965) | 梁与轨不齐 |
| 9 | `RailwaySavedDataMixin` | `RailwaySavedData` | `save` HEAD + ModifyArg | 图重复写/丢失 |
| 10 | `GlobalRailwayManagerMixin` | `GlobalRailwayManager` | `loadTrackData` TAIL、`markTracksDirty` TAIL | 存档后 43kg 线消失 |
| 11 | `TrainReadMixin` | `Train` | `read` HEAD | 进存档全车脱轨 |
| 12 | `CTrainsMixin` | `CTrains` | `<init>` ModifyConstant(128) | 长轨放不下 |
| 13 | `TrackTargetingBlockItemMixin` | `TrackTargetingBlockItem` | `useOn` 两处常量 16 | 绑定距离写死 |

分组：**轨距隔离 1-4**、**曲线碰撞 5-6**、**曲线几何 7-8**、**持久化 9-11**、**放宽限位 12-13**。
新增 mixin 前先确认是否属于已有分组——多数需求应当扩展现有 mixin，而不是加第 14 个。

### 5.3 重构进度

| 现状 | 目标 | 批次 | 状态 |
|---|---|---|---|
| `AllTracks`（一锅） | `AllBlocks` / `AllItems` / `AllBlockEntities` / `AllCreativeTabs` | 2 | ✅ 完成 |
| `Track43kgBlock`（在 registry） | `content/track` | 2 | ✅ 完成 |
| `Track43kgBogeyBlock(Entity)`（在 registry） | `content/bogey` | 2 | ✅ 完成 |
| `Hxd3cBogeyRenderer/Visual`（在 registry） | `client/` | 2 | ✅ 完成（仍 lazy） |
| `util/TrackCurveCollisionHelper` | `feature/curvecollision/CurveCollision` | 2 | 待做 |
| `AllTrackTypes` | `registry/AllTrackStyles` | 2 | 待做 |
| `TextClient` 内的 MODEL_SWAPPER 注册 | `client/TrackModelSwaps` | 2 | 待做 |
| 13 个 mixin | 按 feature 分包 | 3 | 待做（`text.mixins.json` 同步改） |
| 4 个轨距 mixin 内重复判定 | 抽 `GaugeRules` | 3 | 待做 |
| `Config.onConfigEvent` 全图扫描 | "标记 dirty + 懒清理" | 3 | 待做 |

### 5.4 当前已知缺陷（改相关代码前对照）

- **P0-2** 43kg 持久化没有单一入口（4 个参与方靠注释维系）
- **P0-3** 轨距判定散落 4 处
- **P0-4** 曲线碰撞横跨 mixin / util / Config 三处；关闭配置时全图扫描会卡
- **P0-5** 注册层成环无编译期保护
- **P0-6** R-JVM 无机械化保护
- **P1-1** `AllTrackTypes` 名不副实
- **P1-3** `util/` 身份混杂：`TrackCurveCollisionHelper` 是"mixin 服务层"（R-JVM 约束逼出来的），
  `TrainBoxGeometry` 是双端共用的纯几何计算——两者定位不同，新工具类别无脑往里塞
- **P1-4** 三套命名前缀（`All*` / `Track43kg*` / `Hxd3c*`）没有明文约定

### 5.5 调试指令 `/textbox`

限权限等级 2（OP）。开关状态是**客户端位掩码**，服务端只下发"切哪一层"，多人下每人独立。

| 子命令 | 对应位 | 画什么 |
|---|---|---|
| `/textbox`（默认） / `boxes` | `FLAG_BOXES` | **不规则碰撞体（青）**：逐方块碰撞形状 |
| `/textbox hull` | `FLAG_HULL` | 整体外框 OBB（黄），仅定位 |
| `/textbox aabb` | `FLAG_AABB` | 实体碰撞箱（绿） |
| `/textbox off` | `OP_ALL_OFF` | 全关 |
| `/textbox dump` | — | 打印每节车厢的方块数 / 碰撞盒数 / 包围盒数值 |

**"不规则碰撞体"才是 Create 真正拿来判定碰撞的几何**（6.0.10 源码核对）：

`Contraption#invalidateColliders()` 遍历 `blocks`，对每个方块取
`info.state().getCollisionShape(collisionLevel, localPos, CollisionContext.empty())`，
再用 `collisionShape.forAllBoxes(populate)` 拆成若干 AABB，连同方块偏移塞进
`CollisionList`（扁平 double 数组 `centerX/Y/Z + extentsX/Y/Z`）。楼梯、台阶、栅栏因此
保留本来的形状。`/textbox` 直接读这份现成缓存（`Contraption.getSimplifiedEntityColliders()`），
不重算形状——重算需要 contraption 自己的假世界，外部拿不到。

**三种"碰撞"不要混为一谈**：

| 层 | 实现 | 参与实际判定吗 |
|---|---|---|
| 车厢 ↔ 方块 / 装置侧碰撞 | 逐方块 `getCollisionShape` 拆盒（上面那套） | **是**，不规则 |
| 列车 ↔ 列车 | `Train#collideWithOtherTrains`：车厢中心**线段** + `0.125` 格半径球（`VecHelper.intersect` / `intersectSphere(..., .125f)`） | **是**，但很粗，没有盒子 |
| 实体碰撞箱 / 渲染包围盒 | `getBoundingBox()` / `createRenderBoundingBox().inflate(2)` | AABB 只做实体碰撞与视锥裁剪；OBB 外框纯参考 |

改这块时注意：
- `OrientedBB.extents` 是**包私有且没有 getter**（只有 `getCenter()` / `getRotation()`），别想着读 Create 那个类的尺寸。
- 小盒变换后用 **8 个角点连 12 条棱**，不要转成世界 AABB——车厢在弯道上是斜的，轴对齐会把盒子撑大、转角失真。
- 一个车厢可能上百个盒子，绘制有距离剔除（128 格），别去掉。

## 6. 代码约定

- 注册类：`public final class Xxx`，私有构造器，静态字段全大写 `DeferredHolder` / `DeferredRegister`。
- NeoForge 21.1 的 `@Nonnull` 分析噪声大：现有代码大量使用 `@SuppressWarnings("null")`。**保留它们**，新代码照此办理。
- mixin handler 方法统一前缀 `text$`；注入点选 `@Inject` / `@ModifyConstant` / `@Redirect`，不用 `@Overwrite`。target 为内部类时用 `targets = "全限定$内部类"` 字符串。
- mixin 类命名 `<目标类>Mixin`，**必须在 `text.mixins.json` 的 `mixins` 数组里登记**，否则静默不生效。
- **handler 方法体内只引用非 mixin 包**（R-JVM，§4.2）。
- Javadoc **用中文写"为什么"**：Create 侧的原始行为、注入位置、改动的副作用。这是本仓库最重要的知识载体。
- 局部变量用 `var`；单语句 `if` 不加花括号；缩进 4 空格（个别文件有历史 tab，新增代码用空格）。
- 静态初始化顺序：`TrackMaterial` 与方块 holder 互相引用，必须保持双层 `NonNullSupplier.lazy`，不要在静态块里立即 `get()`。

## 7. 资源约定

- **1.21 目录是单数**：`data/<ns>/tags/block`、`data/<ns>/tags/item`、`data/text/loot_table/blocks/`。
- 自定义 OBJ 加载器写 `"loader": "neoforge:obj"`（1.21.1 **没有** `forge:obj` 别名），配 `"flip_v": true`、`"render_type": "minecraft:cutout_mipped"`；MTL 的 `map_Kd` 用 `text:block/track/<material>/<tex>` 形式，`newmtl` 名字必须与 OBJ 的 `usemtl` 逐字一致。
- blockstate variants 键：`shape=<TrackShape 小写>,turn=<bool>,waterlogged=<bool>`，19 shape × 4 = 76 个变体；`shape=none` 指向 `minecraft:block/air`。
- 语言键：`block.text.<registry名>`、`bogey.style.text.<style名>`、`itemGroup.text`、`text.configuration.*`。`en_us.json` 与 `zh_cn.json` 键必须同步。
- 当前**不使用数据生成器**（`src/generated` 不存在），资源全部手写 JSON。不要顺手引入 datagen 流程（会打乱 `processResources` 与 `generateModMetadata` 的顺序）。
- 文件编码：全部 **UTF-8（无 BOM）**，`javac` 也按 UTF-8 编译。
- 加一种轨道 = 手改 11+ 个资源文件（5 tag + 1 loot + 2 lang + blockstate + models + textures），**没有脚本也没有校验，是必漏项**。

## 8. 扩展 checklist

**加一种新轨道（新材料，沿用现有轨距）**：`AllTrackMaterials` → `AllBlocks` + `AllItems` + `AllCreativeTabs` → blockstate（19 shape × 4）→ 12 组形状模型 + 3 个 partial 模型 → 贴图 → 5 个 tag → loot_table → `en_us` / `zh_cn`。
若是**新轨距**，再加 `AllTrackTypes` 的 `TrackType` + 专属转向架方块 + BE + `GaugeRules` 白名单 + §4.4 的四个拦截点复查。

**加一种转向架样式**：`AllTrackTypes`（新 `BogeyStyle`；cycle group 相同才能扳手循环切换）→ 需要新外观则在 `client/` 加 renderer + visual → 模型 → lang ×2 → 若尺寸集合与现有不同，复查 `BogeyStyleGaugeMixin` 的切换逻辑（`docs/ARCHITECTURE.md` §7 有踩坑记录）。

**加一个 mixin**：查 §5.2 分组表 → 归入已有分组 → 在 `text.mixins.json` 登记 → handler 命名 `text$xxx` → 方法体内只引用非 mixin 包 → 在 §5.2 表格加一行。

## 9. 交付前检查

1. `.\gradlew.bat build` 0 error；`build/libs/text-1.2.0.jar` 时间戳刷新。
2. jar 内抽查：`assets/text/blockstates/track_43kg.json`、`assets/text/models/block/track/*.obj`、`text.mixins.json`、`data/create/tags/block/tracks.json`。
3. 结构类改动额外核验：
   - `text.mixins.json` 的 `mixins` 条数 == 带 `@Mixin` 的类数；`package` 字段与目录一致。
   - 移动式重构：jar 内文件清单与改动前**逐条 diff 应完全一致**。
4. `runClient` 冒烟：创造页四件物品齐；直/斜/十字/上升/传送形态渲染正常；曲线（BE 部分）无紫黑缺失纹理；**32kg 与 43kg 之间能自由转、换、搬、开（同轨距）**，而**本模组轨距与 Create 标准轨在转、换、搬、开四个入口都被拦**；挖掘掉落正确（转向架掉 `create:railway_casing`）；控制台无 `Exception loading blockstate definition`、无 `Missing model`、无 mixin 应用失败日志。
5. 存档往返：放一条 32kg + 43kg 互连的线 → 退出 → `create_tracks` 里**没有**它的图、`text_track_43kg` 里有 → 重进存档列车不脱轨、能跨 32kg/43kg 行驶。
6. 改了 mixin：确认已登记，并看 `run/.mixin.out/`（`-Dmixin.debug.export=true` 已开）里的导出类确认注入落地。

## 10. 已知缺口（发布前处理）

1. ~~**`neoforge.mods.toml` 没有声明 Create 依赖**~~ **已补齐**：文件末尾已声明
   `modId="create" type="required" versionRange="[6.0.10,)"`。升级 Create 时同步这里与
   `gradle.properties: create_version`，两处版本要一起动。
2. `Config.onConfigEvent` 用反射调用 `ChunkMap#getChunks()`（protected），依赖 NeoForge 模块开放；升级 MC 版本时首批复查。
3. `run/mods/` 只有两个 Sodium jar（客户端性能测试）；Create 与 Ponder/Flywheel 走 dev classpath，不要把它们的 jar 丢进 `run/mods/`。
4. `.verify_*` 目录是历史验证残留，已 gitignore，可安全删除。
5. `PLAN.md` 是 32kg 轨道那一阶段的完整设计记录，涉及轨道/模型细节时值得先读。

## 11. 发版流程

发布由 CI 的 tag 步骤完成，本地不需要 `publish`、也不需要手动上传。

```bash
# 1) 改版本号（唯一版本源）
#    gradle.properties: mod_version=1.2.0
# 2) 本地先确认构建干净
.\gradlew.bat build
# 3) 提交并打 tag（tag 名必须以 v 开头，与工作流条件 startsWith(github.ref, 'refs/tags/') 匹配）
git add -A
git commit -m "release: v1.2.0"
git tag v1.2.0
git push origin main --tags
# 4) CI 跑完 → Releases 页面出现 v1.2.0，附件是 text-1.2.0.jar
```

注意：

- **`mod_version` 与 tag 名之间没有强制校验**。工作流只校验"jar 文件能匹配到"
  （`fail_on_unmatched_files`），不会比对版本号。打错 tag 会得到版本号与 tag 不一致的 Release。
- 产物名固定 `text-${mod_version}.jar`（`build.gradle` 的 `base.archivesName = mod_id`）。
- release notes 由 commit / PR 自动生成；想让人读得懂，就在打 tag 前把 PR 标题和 commit message 写清楚。

---

**需要理解"为什么这么设计"时读 `docs/ARCHITECTURE.md`**：运行时生命周期、43kg 持久化全流程、曲线碰撞数据流、问题推演、踩坑记录都在那里。
**违反本文档 §4 的任何一条前，先读 `docs/ARCHITECTURE.md` 对应章节。**
