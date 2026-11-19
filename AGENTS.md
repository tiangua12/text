# AGENTS.md

给在本仓库工作的 AI 代理 / 协作者的项目指令。先读这一页，再动代码。

## 1. 这是什么

`text` 是一个 **Minecraft 1.21.1 / NeoForge 的 Create 附属模组**，核心内容是中国铁路风格的
**轨道与转向架**：

- `text:track_32kg` —— 32kg/m 钢轨（复用 Create 标准轨距 `STANDARD`）
- `text:track_43kg` —— 43kg/m 钢轨，**自带独立轨距**（自定义 `TrackMaterial.TrackType`）
- `text:track_43kg_bogey` / `text:track_43kg_large_bogey` —— 43kg 专用转向架（SMALL / LARGE）
- 3 个转向架样式：Create 原版小、原版大、以及自定义 `hxd3c`（Co-Co 三轴电力机车转向架）

43kg 轨距与 Create 标准轨距**物理不兼容**（不能互转、不能换转向架、不能互相停留），
这条约束靠 13 个 mixin 在 Create 内部加装，是整个模组最容易踩坏的部分。

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

## 3. 命令

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

  ```powershell
  git -c safe.directory=F:/modmdk1.21.1/text status
  ```

  或一次性 `git config --global --add safe.directory F:/modmdk1.21.1/text`。
- 构建目录已有历史产物（`build/` ≈100 MB，`run/` ≈28 MB），都在 `.gitignore` 里，别提交。

## 4. 目录地图

```
src/main/java/com/easttown/text/
├── Text.java                  # @Mod 主类：注册 4 个 DeferredRegister + 配置事件
├── TextClient.java            # 客户端：MODEL_SWAPPER 注册 TrackModel、BE 渲染器 + Flywheel visualizer
├── Config.java                # ModConfigSpec（COMMON）：轨道定位距离、曲线碰撞开关 + 关闭时的清理逻辑
├── registry/
│   ├── AllTracks.java         # Block / Item / BlockEntityType / 创造标签页
│   ├── AllTrackMaterials.java # TrackMaterial（32kg、43kg），双层 NonNullSupplier.lazy 解耦初始化顺序
│   ├── AllTrackTypes.java     # 43kg TrackType + 3 个 BogeyStyle（共用 cycle group text:track_43kg）
│   ├── Track43kgBlock.java    # extends TrackBlock
│   ├── Track43kgBogeyBlock.java / Track43kgBogeyBlockEntity.java  # 43kg 转向架（复用 StandardBogeyBlockEntity）
│   └── Hxd3cBogeyRenderer.java / Hxd3cBogeyVisual.java            # HXD3C 外观
├── mixin/                     # 13 个 mixin，见 §5
└── util/TrackCurveCollisionHelper.java   # 曲线下方不可见碰撞块的放置/判定
src/main/resources/
├── text.mixins.json           # mixin 必须在 mixins 数组登记（defaultRequire: 1）
├── assets/text/{blockstates,models,textures,lang}
└── data/{create,minecraft,neoforge,text}/…
src/main/templates/META-INF/neoforge.mods.toml   # 由 generateModMetadata 展开变量
```

## 5. Create 轨道系统接入面（改这里前先读懂）

- **注册**：`TrackMaterial`（`TrackMaterialFactory.make().lang().block().particle().trackType().standardModels().build()`）
  → `TrackType`（即"轨距"）→ `TrackBlock` / `TrackBlockItem`。
- **BE**：32kg/43kg 轨道**复用 Create 的 `AllBlockEntityTypes.TRACK`**，不要注册新的轨道 BE 类型
  （网络、渲染、破坏动画全靠它）。43kg 转向架才有自己的 BE 类型。
- **渲染**：非曲线形状走 blockstate + OBJ 模型；曲线走 Flywheel `PartialModel`
  `text:block/track/<material>/{tie,segment_left,segment_right}`；上升轨道的倾斜由
  `TextClient` 注册的 `TrackModel` 包装提供（key = **方块注册 id**，不是模型 id）。
- **mixin 职责分组**（新增前先确认是否已有同类实现）：
  - 轨距隔离：`TrackEdgeGaugeMixin`（禁止跨轨距转向）、`BogeyStyleGaugeMixin`（禁止跨轨距换样式）、
    `TrainRelocatorGaugeMixin`（列车重定位轨距校验）、`TrainDerailMixin`
  - 曲线碰撞：`TrackPlacementCurveMixin`、`TrackBlockCurveMixin` + `util/TrackCurveCollisionHelper`
  - 放宽 Create 内部限位：`CTrainsMixin`（128）、`TrackTargetingBlockItemMixin`（16 / 16.0）
  - 曲线几何：`BezierSegmentAnglesMixin`、`BezierGirderAnglesMixin`
  - 持久化：`RailwaySavedDataMixin`、`GlobalRailwayManagerMixin`、`TrainReadMixin`

## 6. 代码约定

- 注册类：`public final class Xxx`，私有构造器，静态字段全大写 `DeferredHolder` / `DeferredRegister`。
- NeoForge 21.1 的 `@Nonnull` 分析噪声大：现有代码大量使用 `@SuppressWarnings("null")`
  （`DeferredHolder.get()`、`BlockEntityType.Builder.build(null)` 等）。**保留它们**，新代码照此办理。
- mixin handler 方法统一前缀 `text$`（如 `text$rejectCrossGaugeTurn`）；注入点选 `@Inject` /
  `@ModifyConstant`，不用 `@Overwrite`。target 为内部类时用 `targets = "全限定$内部类"` 字符串。
- mixin 类命名 `<目标类>Mixin`，并在 `text.mixins.json` 的 `mixins` 数组里登记，否则静默不生效。
- Javadoc **用中文写"为什么"**：说明 Create 侧的原始行为、注入位置、以及改动的副作用
  （现有 13 个 mixin 全部如此），这是本仓库最重要的知识载体。
- 局部变量用 `var`；单语句 `if` 不加花括号；缩进 4 空格（mixin/个别文件有历史 tab，新增代码用空格）。
- 静态初始化顺序：`TrackMaterial` 与方块 holder 互相引用，必须保持 `NonNullSupplier.lazy` 双层包裹，
  不要在静态块里立即 `get()`。

## 7. 资源约定

- **1.21 目录是单数**：`data/<ns>/tags/block`、`data/<ns>/tags/item`、`data/text/loot_table/blocks/`。
- 自定义 OBJ 加载器写 `"loader": "neoforge:obj"`（1.21.1 **没有** `forge:obj` 别名），
  配 `"flip_v": true`、`"render_type": "minecraft:cutout_mipped"`；MTL 的 `map_Kd` 用
  `text:block/track/<material>/<tex>` 形式的资源路径，`newmtl` 名字必须与 OBJ 的 `usemtl` 逐字一致。
- blockstate variants 键：`shape=<TrackShape 小写>,turn=<bool>,waterlogged=<bool>`，
  19 个 shape × 4 = 76 个变体；`shape=none` 指向 `minecraft:block/air`。
- 语言键：`block.text.<registry名>`、`bogey.style.text.<style名>`、`itemGroup.text`、
  `text.configuration.*`。`en_us.json` 与 `zh_cn.json` 键必须同步。
- 当前**不使用数据生成器**（`src/generated` 不存在），blockstates / models / loot_table / tags
  都是手写 JSON。改动时保持手写风格，不要顺手引入 datagen 流程（会打乱 `build.gradle` 的
  `processResources` 与 `generateModMetadata` 顺序）。
- 文件编码：全部 **UTF-8（无 BOM）**，`javac` 也按 UTF-8 编译。

## 8. 已知缺口 / 待补

1. **`neoforge.mods.toml` 没有声明 Create 依赖**（目前只有 `neoforge` + `minecraft`）。
   生产环境缺少 Create 时只会静默出错，发布前应补：

   ```toml
   [[dependencies.${mod_id}]]
       modId="create"
       type="required"
       versionRange="[6.0.10,)"
       ordering="NONE"
       side="BOTH"
   ```

   （改的是 `src/main/templates/META-INF/neoforge.mods.toml`，由 `generateModMetadata` 展开。）
2. `Config.onConfigEvent` 用反射调用 `ChunkMap#getChunks()`（protected），依赖 NeoForge 模块开放；
   升级 Minecraft 版本时这是首批要复查的点。
3. `run/mods/` 只放了两个 Sodium jar，用于客户端性能测试；Create 与 Ponder/Flywheel 走 dev
   classpath，不要把它们的 jar 丢进 `run/mods/`（会和 dev 依赖冲突）。
4. `.verify_*` 目录是历史验证残留，已 gitignore，可安全删除。
5. `PLAN.md` 是 32kg 轨道那一阶段的完整设计记录（Create API 核实表、资产改写规则、风险清单），
   涉及轨道/模型细节时值得先读。

## 9. 交付前检查

1. `.\gradlew.bat build` 0 error；`build/libs/text-1.0.0.jar` 时间戳刷新。
2. jar 内抽查：`assets/text/blockstates/track_43kg.json`、`assets/text/models/block/track/*.obj`、
   `text.mixins.json`、`data/create/tags/block/tracks.json`。
3. `runClient` 冒烟：创造标签页四件物品齐全；直/斜/十字/上升/传送形态渲染正常；曲线（BE 部分）
   无紫黑缺失纹理；43kg 轨道与标准轨道相邻时**不能**互通/互换转向架；挖掘掉落自身；
   控制台无 `Exception loading blockstate definition`、无 `Missing model`、无 mixin 应用失败日志。
4. 改了 mixin：确认 `text.mixins.json` 已登记，并看 `run/.mixin.out/`（`-Dmixin.debug.export=true`
   已开）里的导出类确认注入落地。
