# Mine Strike Skins

适用于本包 Minecraft 1.20.1、Forge 47.4.0、FPSMatch 1.2.5、BlockOffensive 1.2.5.1。

## 安装与操作

1. 关闭游戏，把 `mods/mine-strike-skins-0.1.0.jar` 复制到实际启动的版本目录的 `mods`。
2. 把 `config/mine-strike-skins.json` 复制到同一版本的 `config`。如果没有该文件，模组会在启动世界时生成默认配置。
3. 客户端、房主及独立服务器均需要安装新增模组；保留原来的枪包、刀包、Modern UI、FPSMatch 和 BlockOffensive。
4. 进入世界或赛前加入队伍时自动打开选择界面。也可以按 F6 打开，按键可在游戏的按键绑定中修改。
5. 按分类选皮肤，点击“保存选择”，等到显示“已保存”，再返回游戏并按原流程开始比赛。
6. 比赛开始后锁定，期间不能修改；新回合、换边、重连都继续使用本局选择。中途加入的玩家锁定其加入时已保存的选择。
7. 选择“默认外观”会恢复地图配置本身的装备。没有选择时也沿用地图原配置。

## 配置与保存位置

`config/mine-strike-skins.json` 为全地图共用皮肤列表，按 `skin.txt` 逐项生成，共五类、23 个外观。

- `key`：类别的稳定标识，已有玩家数据后不要随意改名。
- `name`：界面分类名称。
- `item`：物品类型，枪为 `tacz:modern_kinetic_gun`，刀为 `lrtactical:melee`。
- `tag`：枪用 `GunId`，刀用 `MeleeWeaponId`。
- `baseIds`：地图商店或初始装备中需要替换的基础 ID。
- `skins`：名称与对应 ID 列表。界面使用这些 ID 对应物品的实际图标。

新增皮肤时修改配置并重启世界/服务器即可。新增武器类别时同样可加一个 group，无需重新编译 UI。
M4A4 分类按用户清单提供；地图商店中没有 M4A4 商品时，选择外观不会自动新增该商品。
用户清单没有手枪皮肤，本版本不新增手枪选项；将手枪类别加入配置后，初始装备和商店同样支持替换。
资源索引不存在的皮肤显示“资源未加载”，禁止选择；服务端也会校验。

玩家选择存于实际游戏目录的 `mine-strike-data/player-skins/<UUID>.json`。
单人/局域网由房主保存，独立服务器由服务器保存；客户端不决定其他人的装备。
本局快照保存在服务器内存，服务器重启会结束本局锁定，长期选择仍保留。

## 源码位置

- `SkinSelectionFragment.java`：Modern UI 布局、卡片、真实物品图标和选择交互。

界面布局为顶部分类、左侧当前选择的大图预览与三列皮肤卡片、右侧已选装备清单、底部保存与返回。
按钮与卡片使用方形石块纹理、立体边缘和悬停/按压反馈，整块区域均可点击。
背景位于 `src/main/resources/assets/minestrike_skins/textures/gui/skin-background.png`，左侧保留人物展示，右侧包含皮肤选择和已选装备栏。
卡片使用较亮的石板底色，选中显示金色边框与“已选”标记；点击沿用 Minecraft 原版按钮音效。
文字使用 `assets/minestrike_skins/font/pixel.json` 引用 Minecraft 的 Unicode 位图字体。
`PreviewGeometry.java` 计算图标与滚动窗口的交集，防止绘制覆盖底部操作区；切换分类会回到列表顶部。
枪械读取 `TimelessAPI.getGunDisplay(item).getSlotTexture()`，刀具读取 `LrTacticalAPI.getMeleeDisplay(item).getSlotTexture()`。
Modern UI 给出的预览区域尺寸是物理像素，绘制前除以 GUI scale，防止图标偏移到裁剪区域外。
- `SkinClient.java`：F6 按键和客户端打开界面。
- `SkinNetwork.java`：客户端选择请求与服务端状态同步。
- `SkinCatalog.java`：分类、ID 白名单与基础装备匹配。
- `SkinStore.java`：按 UUID 保存 JSON，原子替换文件。
- `MineStrikeSkins.java`：服务器数据、个人商店、退款匹配、比赛快照。
- `mixin/CSGameMapMixin.java`：开局锁定、重置解锁、每回合初始装备替换。

死亡后新回合及换边走 `CSGameMap.lambda$cleanupMap$66` 中的 `GiveStartKitsMap.givePlayerKits`。
补丁在原方法清空并发完初始装备后应用玩家皮肤，再同步背包；存活玩家保留装备的分支不会重染拾取的武器。

地图、公共商品 JSON 和原有模组 JAR 不修改。只在生成个人商店时复制商品并替换物品，购买和扣款继续使用 FPSMatch。
捡起其他人的武器不替换其皮肤；修改自己的选择不会改变公共商店。
替换 GunId/MeleeWeaponId 使用的是对应枪包/刀包中的实际变体，其属性由原资源决定，本模组不重写枪械或刀的伤害数据。

## 构建

本工程针对现有包的 SRG 运行时编译，不需要下载或重新映射其他模组。
使用 Java 17 的 `javac` 和 Python 3，指定启动器的 libraries 目录：

```sh
python3 skin-mod/build.py --libraries "/Users/qbqb/Library/Application Support/minecraft/libraries"
```

Windows 示例（在包根目录执行，安装好 Java 17 并把 javac 加入 PATH）：

```bat
python skin-mod/build.py --libraries "D:\Game\MC\.minecraft\libraries"
```

构建结果：`skin-mod/build/libs/mine-strike-skins-0.1.0.jar`。修改源码后重新构建，再替换 mods 中的 JAR。
本地检查命令：`python3 skin-mod/verify.py --libraries "/Users/qbqb/Library/Application Support/minecraft/libraries"`。
如果启动器没有安装 Forge 47.4.0 的 client 运行时 JAR，构建脚本会明确报错，需要先用该启动器安装/启动一次对应版本。
Mixin 接入精确匹配 BlockOffensive 1.2.5.1 的方法，更新比赛模组后需重新检查接入点。

## 游戏内验收

1. 两名玩家选不同 AK，分别购买，核对外观、价格、备弹与配件。
2. 买枪后不使用立即退款，确认退款金额和商品状态。
3. 选刀，开局及下一回合核对；T/CT 换边后继续核对。
4. 开局后按 F6，确认选项锁定；整场结束后重新选择。
5. 保存退出、再次进入，确认长期选择保留；断线重连确认本局选择保留。
6. 捡另一名玩家的枪，确认保留对方外观。

本地可验证编译、资源包结构、配置清单和数据持久化；实际 UI、枪包图标、购买及退款效果需要启动 Minecraft 检验。
此前 Modern UI 的 Emoji 字体崩溃未在本任务中修改；如仍发生，需按新的崩溃报告单独定位。


## 比赛阶段看板

2026-10-14 看板修正：PixelTile 绘制前使用 isShown() 检查自身及父页面，避免隐藏页的 Minecraft 独立绘制层叠到当前页。
PageVisibilityTest 在真实 Modern UI 视图层级上交替切页 20 次，验证 GONE、INVISIBLE 和已脱离层级的按钮不进入绘制。
CT 背景调整为接近 T 的身高比例，并改为举枪向斜前方瞄准；T 背景保持不变。使用内置 imagegen，最终编辑提示词：

> Edit ONLY the far-left SAS counterterrorist in image 1. Image 2 is scale reference only: make CT have roughly the same full-body height and visual prominence as the terrorist in image 2. Enlarge CT by about 22 percent, boots at approximately existing ground position and helmet/hood higher, entire body still fully inside far-left quarter. Preserve gas mask with TWO round blue/gold reflective lenses, circular mouth respirator and blue-rimmed side filter, navy uniform, olive tan tactical pouch vest, gray-green knee pads, dark combat boots; genuine cubic Minecraft anatomy/pixel textures. CHANGE POSE to tactical rifle aiming diagonally forward toward viewer's right: rifle shouldered, butt firmly at shoulder, cheek/head behind sights, both hands supporting rifle, muzzle raised to about shoulder level, facing three-quarter front, slight forward lean, knees flexed in steady staggered stance. Do NOT point weapon down. Do not crop character or barrel, do not extend gun beyond left 26 percent menu-free area. Preserve image 1 entire background architecture, A marking, sky, lighting, 16:9 framing and all open menu area unchanged. Image 2 must not affect background or CT identity. No text or UI.

F6 打开看板，顶部切换“皮肤选择 / 比赛阶段”。沿用原卡片、像素字体、半透明面板和 Minecraft 按钮音效。
“比赛阶段”使用当前存档唯一的 CSGameMap；不跨存档、不提供地图选择。靶场等没有 CS 配置的存档禁用比赛操作。

- 选择阵营：点击 T / CT。所有在线玩家均计入服务器人数，未选边者也阻止开赛；比赛中禁止换队。
- 游戏准备：准备 / 取消准备。全体在线玩家均选边并准备后立即调用原开赛流程，允许单人、单边或人数不均。
- 原计时自动开赛已由准备逻辑接管；原命令方块或命令也无法跳过全员准备检查。
- 当前 CS 比赛的队伍容量不受原 5 人上限限制。人数多于出生点时在内存中复用已有点，不写入地图配置。
- 单边模式避免因另一队无人立即触发团灭胜利，炸弹和时间结束仍沿用原逻辑。
- 结束投票：未开赛禁用，开赛后发起 / 同意 / 撤回。服务器全部在线玩家同意后立即调用 resetGame，执行原清理与传送。
- 换队清除该玩家的准备；断线清除其准备与结束投票。重连需要重新准备或同意。比赛结束清除所有准备与投票。
- 比赛状态每秒同步；选边和投票后立即同步。通信协议为 7，多人客户端和服务端须使用同版 JAR。

核心实现：MatchLobby.java（服务端）、MatchVotes.java（人数与投票）、SkinNetwork.java（通信）、
SkinSelectionFragment.java（界面）、CSGameMapMixin.java / BaseTeamMixin.java（原逻辑接入）。

### 自动检测与游戏验收

verify.py 新增 MatchVotesTest，覆盖未选边玩家、单边/单人/人数不均、新玩家加入、换队、断线重连、
准备取消、全体结束同意和撤回。通信测试新增比赛操作与中文比赛状态的编解码检查。
同时检查原模组自动开赛、团灭判断、出生点分配、队伍容量方法及新背景资源。
这些检查不等同于游戏内验证 Mixin 加载、绘制、音效或多人流程。

游戏内验收：
1. 重启后 F6，切换双页；检查新背景、按钮音效、缩小窗口滚动、切页不丢失当前页面。
2. 两名玩家入服，只一人选边并准备时不得开赛；两人同边并准备后开赛。
3. 三人按 2T / 1CT 准备后开赛；未选边第三人必须阻止开赛。
4. 断线重连后准备/同意不保留；选择其他阵营后重新准备。
5. 开赛前结束按钮禁用；一人发起结束不结束，全体同意后立即清理、解锁皮肤并回到原结束传送点。
6. 检查原皮肤保存、购买、死亡后装备、换边装备和退款。

### 比赛背景生成记录

2026-10-14：按 IMG_9562.JPG 的防毒面具 CT 和 IMG_9561.JPG 的头套 T 调整人物，使用内置 imagegen 编辑。
保留两张原背景的场景、人物位置和菜单留白。更新后的最终提示词如下：

**CT / skin-background.png**

> Edit image 1 (the Minecraft menu background). Image 2 is CHARACTER APPEARANCE reference only, a photograph of a screen; ignore its photographed monitor, interface, real room and background. Replace ONLY the far-left soldier's appearance with a faithful Minecraft voxel interpretation of the image 2 SAS counterterrorist: full dark charcoal gas mask with TWO large round blue/gold reflective glass eyepieces, circular mouth respirator, large cylindrical side filter with blue rim, dark hood, navy blue full uniform, olive/tan tactical vest with several rectangular ammo pouches, green belt and thigh straps, gray-green hard knee pads, black combat boots and gloves. These gas mask features are essential; NO helmet with goggles, NO exposed eyes or face. Keep the character completely cubic block-built with pixel textures to match image 1; circular goggles should be stepped pixel circles on a cubic gas-mask head, not a realistic human. Preserve original image 1 character size, far LEFT position, full body and boots, original rifle held across chest pointed down and pose; image 2 controls clothing and identity, not its pistol pose. Preserve image 1 entire desert courtyard, A marking, sky, lighting, geometry, colors, exact wide 16:9 framing and empty central/right space for menu. No UI, no words, no new figures.

**T / match-background.png**

> Edit image 1 (Minecraft menu background). Image 2 is CHARACTER APPEARANCE reference only, photograph of a screen; ignore its photographed monitor, UI, real room and background. Replace ONLY the far RIGHT terrorist with a faithful Minecraft cubic voxel interpretation of this reference terrorist: dark olive green knitted BALACLAVA fully covering the square head, with TWO separate eye holes and a distinct mouth hole, exposed skin only within those openings. No cap, no headband, no uncovered head or scarf covering the mouth. Pink/dusty rose plaid short-sleeve collared shirt, light gray-beige body armor vest with green shoulder straps, olive green cargo trousers with thigh pockets, dark tactical gloves, tan combat boots and green belt with pouches. Maintain image 1 full-body far-right position, same size and silhouette footprint, slight head tilt, fierce defiant eyes, cigarette through the mouth hole. Preserve original image 1 pose: anatomical RIGHT arm hanging vertically (viewer's left), anatomical LEFT hand holding the AK resting on LEFT shoulder (viewer's right). Convert reference outfit into genuine block-built Minecraft character anatomy and pixel textures, no realistic curved human anatomy. Keep image 1 background courtyard, wooden door, walls, palm trees, sky, lighting, wide 16:9 composition and open left 70 percent exactly unchanged. No extra characters, no UI, no text.


使用内置 imagegen，旧背景仅作风格参考；新资源为 assets/minestrike_skins/textures/gui/match-background.png。
最终提示词：

> Generate a NEW wide 16:9 Minecraft voxel game menu background. Reference image is STYLE REFERENCE ONLY: match bright warm sandy block-built Counter Strike desert map, blue sky, crisp Minecraft pixel textures, cubic character anatomy, polished in-game rendering. Different coherent location: sunlit desert alley courtyard with low sandstone walls, a modest wooden double door and crates, not the same arch or A graffiti. Left 70 percent is open uncluttered scenery for menu overlays, no text/UI. On far RIGHT 25 percent place one full-body adult male Minecraft blocky terrorist character facing camera, boots visible with margin, similar size to the reference character, brown jacket, pale shirt, dark trousers and scarf. His anatomical RIGHT arm hangs vertically relaxed at his side (viewer's left); his anatomical LEFT hand holds a blocky AK resting over his LEFT shoulder (viewer's right), barrel angled up safely. Slightly tilted square head, defiant fierce scowling face, cigarette held in corner of mouth, small curl of smoke. Character stands entirely within rightmost quarter leaving generous blank space to left. No counterterrorist, no extra people, no logos, no words, no interface. Preserve exact requested pose.


## 地图基准与回合恢复

没有保存/更新基准的界面按钮。只提供原版管理员权限等级 2 的指令：

```text
/minestrike baseline save
/minestrike baseline restore
/minestrike baseline status
```

`save` 用于首次保存和后续更新：先进入对应存档，结束比赛，完成地图编辑，离开载具，再执行。
保存期间不要修改地图，等待聊天中的“地图基准保存完成”。每个存档独立保存。
`status` 显示当前地图、是否有基准、保存/恢复任务状态及完整文件路径。
`restore` 在未开赛时读取已有基准，恢复当前地图比赛区域；要求管理员权限等级 2，且没有其他保存/恢复任务。
恢复前所有玩家下车并传送到比赛结束点；完成后清除准备状态、保持未开赛，不自动开始比赛，也不覆盖已有基准。
未保存基准时拒绝恢复；与自动回合恢复一样，不恢复地图比赛区域之外的建筑。
单人 1.20.1 可通过“对局域网开放 → 允许作弊”取得指令权限；独立服务器由 OP 执行。

文件位于实际服务端当前存档内部的 `mine-strike-data/map-baselines/`，地图名经 URL-safe Base64 编码后加 `.nbt`。
例如 Dust2 为 `ZHVzdDI.nbt`。文件包含压缩 NBT：版本、地图名、维度、三维区域、方块状态调色板、
含空气的完整方块索引、方块实体 NBT（例如箱内物品）和持久实体 NBT。
更新采用临时文件写入后替换；上一份保留为 `.nbt.previous`。JAR 更新不会覆盖这些文件。

恢复范围严格采用当前 FPSMatch 的 `mapArea` 三维坐标，不恢复整个存档、天气或游戏规则。
新增建筑必须处于该范围内；需要更高、更大的战场时，先按实际玩法调整比赛区域，再重新保存基准。
普通方块建筑统一恢复。普通实体及支持标准实体 NBT 的模组对象重新生成；Superb Warfare 原载具的
Health、Energy、Inventory 等由该模组自身 NBT 序列化处理，具体型号仍需游戏内测试。
玩家、临时掉落物、弹药抛射物和 C4 不纳入初始实体基准。基准载具必须无人乘坐。

每场比赛和每个小回合开始前都读取基准；分批加载区块、清理新增对象、恢复方块/容器及初始实体。
完成前暂停原比赛 tick，并禁止玩家干扰保存/恢复。恢复前玩家下车并传送到原比赛结束点；
完成后才进入原装备发放、出生点传送和开局流程。临时加载区块会释放，原强制加载区块保持原样。
未保存基准时不能准备开赛，会提示保存指令；不自动记录任何基准。

地图内出现的实体带持久比赛标记，移出比赛范围的已加载对象也会清理；旧对象所在区块以后才加载时，
按恢复代次拒绝旧对象回到游戏。更新基准后新一轮按更新后的快照恢复。
恢复期间取消该维度 Explosion Overhaul 的旧异步挖坑任务，并清理区域内残留方块/流体计划 tick。
该功能不处理其他模组在外部数据库、独立物理世界等处存储的状态；后续新增此类模组需针对适配。
恢复失败不会继续开始新回合，聊天和服务器日志会显示原因。

### 验收步骤

1. 重启并进入已编辑好的比赛存档；比赛未运行时执行 `save`，等待完成，用 `status` 确认路径。
2. 测试前记录一段墙、一块原空气位置、一只箱子的内容和一辆无人载具的初始位置/状态。
3. 破坏墙，在原空气处放方块，改变箱内物品，新增载具或移动初始载具。
4. 所有人选边准备：恢复完成后检查墙补回、新方块清除、箱内物品及初始载具恢复。
5. 比赛中再次破坏/新增，进入下个小回合后重复检查；旧掉落物及新增载具不应残留。
6. 把新增载具移出比赛区域后测试重置；对卸载区块的对象，返回并重新加载该区块确认旧对象不再出现。
7. 结束比赛，再执行 `save` 更新，确认后续恢复采用新基准，上一版文件仍存在。

自动检测新增 BaselineDataTest，验证区域索引、边界隔离、压缩存储、空气/方块/容器/载具 NBT 保留、
更新生效、上一版备份、非法索引/格式/过大区域拒绝以及失败不覆盖有效基准。
verify.py 同时核查 startGame / startNewRound / BaseMap.mapTick 的接入目标。
自动检测没有启动真实 Forge 世界，不能替代上述地形、载具、多人及性能验收。


## 六分类商店与独立商品编辑

2026-10-15：自制模组通信协议升级为 **7**，服务器和所有客户端必须更新本 JAR。
原 FPSMatch / BlockOffensive / TaCZ JAR 不改动。

- 商店固定六列：装备、手枪、中型武器、步枪、投掷物、神秘武器。
- 原五分类的商品、顺序、空槽、价格、NBT、购买上限、组 ID 和监听器保留；旧配置在内存中补入空的 MYSTERY 分类。
- 打开商店编辑工具，点击右上角“编辑模式”。各分类末尾有独立的“+ 添加商品”；已有方格右上角“×”删除该方格。
- 删除后仅该分类下方商品上移；其他分类不变。不再追加整行、不补齐其他分类，每类可有 0–32 个方格。
- 普通模式点击商品进入单品编辑；编辑模式同样可点卡片修改，只有右上角删除区域执行删除。
- 单品编辑可连续点击自己的背包物品，逐项加入商品，不移动或消耗背包物品；完整保留其 NBT。
- 可编辑名称、价格、组 ID、购买上限和原虚拟弹药输入；恢复默认名称将移除自定义名称。监听模块保留并显示。
- 新增、删除、保存商品都会立即保存当前地图配置，返回无需再执行 `/fpsm save`。新增后取消编辑会留下空方格，可在编辑模式删除。
- T / CT 配置分别编辑，要求管理员权限等级 2、比赛未开始且地图无保存/恢复任务。
- 多管理员编辑使用菜单版本检查；商店已被其他操作修改时拒绝旧菜单提交，返回总览后重新打开商品即可。

总览与单品编辑使用自制独立面板，不触发 JEI 容器搜索覆盖层；总览滚轮浏览，不分页。
购买页六列在同一页整体缩放，使用原商品购买/退款网络与扣款流程。
三个页面共用阵营背景：CT 蓝色夜景、T 金黄色沙漠场景。
图片资源为 `assets/minestrike_skins/textures/gui/shop-ct.png` 和 `shop-t.png`，
生成提示词保存在 `art/shop-background-prompts.md`。

### 虚拟弹药说明

原 `getAmmoCount()` 读取的是最大备用弹药；原保存函数 `setTotalDummyAmmo` 却将输入按总数分配到弹匣和备用。
例如标准弹匣 30、输入 90，原函数写入弹匣 30、备用 60（枪膛和扩容弹匣仍由原模组处理）。
本次保留该输入与分配算法；未改动弹药输入时不重新分配，避免只改名或价格就改变原商品弹药。

### 验证与验收

`verify.py` 覆盖独立增删/上移、不同分类长度、六列索引、空分类、上限、中文名称和编辑消息往返。
`ShopMixinTest` 用实际 Forge 0.8.5 Mixin / ASM 9.7.1 引擎应用十二个商店、回合与冒险模式目标，
验证第六枚举初始化、MYSTERY 名称查找，以及旧五分类补入第六类时保留原商品和各分类长度。
自动检查不启动真实世界，不等同于界面和多人验收。

游戏内仍需确认：普通右键打开、T/CT 背景、搜索层无覆盖、每列独立增删、删除后上移、
背包物品复制和 NBT、改名/默认名、保存和取消、多管理员旧菜单拒绝、神秘武器购买退款、
皮肤替换、换边/死亡/重连和重启后的配置持久化。


### 删除后的界面切换与视觉修正（2026-10-15）

修复原先把关闭操作放入 `Screen.removed / m_7861_` 引发的递归关闭和栈溢出。
移除通知沿用原版空实现；仅显式返回 / Esc 走 `onClose / m_7379_`，单品页只在用户主动返回时发返回请求。
`ShopLifecycleTest` 在三个实际界面类上执行共 3000 次移除通知，防止删除、保存、切换页面再次触发关闭或网络操作。
面板改为 CT 蓝灰 / T 暖棕半透明，卡片和按钮增加高光及暗边，统一像素字体并增大标题，移除重复提示和计数。
背景通过渲染位置略向右调整并留出覆盖余量，不修改五人群像图片本身。


### 多物品商品（2026-10-15，协议 5）

- 单品编辑左侧显示物品列表：第一项为主物品，其余为副物品。点击背包连续添加，点击每项右上角 × 删除；删除主物品后下一项自动成为主物品。最多 36 项，超过显示区域可在列表内滚动。
- 复制背包中该格的完整数量和 NBT，原物品不消耗；名称和弹药字段仅修改主物品，副物品保持复制时的属性。副物品也支持按 GunId/MeleeWeaponId 应用个人皮肤。
- 套餐只按商品价格扣一次钱、计算一次购买次数。主副物品全部放入背包，避免多把枪互相替换；背包无法容纳整套时不发放、不扣款。原单物品商品仍沿用原购买行为。
- 退款须符合原锁定、购买次数和分组条件，并找齐同一购买批次的全部物品及数量。服务端校验整套 NBT（包括弹药、附件、皮肤和数量），整套回收成功才退全款。缺失、消耗或改动物品后不支持部分退款；同类旧物品不能代替缺少的套餐物品。
- 多物品保存在原商品主 ItemStack 的 `MineStrikeBundleItems` NBT 中，原地图商品 JSON 的格式和位置不变；只有点击“保存商品”才提交列表修改，返回取消本次修改。没有副物品时移除该 NBT，继续兼容原单物品配置。
- `InventoryPlanTest` 验证空间不足不产生部分交易、完整退款、不同 NBT/购买批次隔离、堆叠拆分及 1000 次购买/退款数量守恒。网络测试增加列表删除、空列表、列表上限和套餐退款状态；真实 Mixin 测试增加 ShopSlot 发放、退款与回合重置目标。
- 游戏内仍需验证多物品编辑及取消、保存后重启、数量和附件 NBT、主副皮肤、背包不足、整套退款、使用/丢弃/拾取后的退款、多人与小回合重置。自动检查不等同于游戏验收。


### 每项物品仅本小局使用（2026-10-15，协议 6）

- 在单品编辑的每项主／副物品卡片底部，点击“原有规则”切换为“仅本小局”，再次点击可关闭。默认关闭；需点击“保存商品”提交，返回取消本次修改。套餐各项独立设置，单物品商品同样支持。
- 配置标记为 ItemStack NBT 的 `MineStrikeRoundOnly`。实际购买发放时增加 `MineStrikeIssuedRound` 随机局编号，商品模板不带编号；未标记的物品不改变原发放、保留和清理逻辑。
- 在原模组 `roundVictory`（含刀局）结算、`cleanupMap` 及 `resetGame` 节点作废该地图局编号，立即清理所有在线玩家物品栏、护甲／副手、末影箱、鼠标持有物品和打开容器内过期物品，并移除已加载的原版掉落物和 FPSMatch 枪械掉落实体。转交其他玩家也随原局到期。
- 离线玩家重新登录及每个服务端玩家 tick 检查过期物品；未加载掉落物在重新加载时拒绝生成。存进关闭箱子的过期物品再次进入玩家物品栏或打开菜单后清理，不能带到后续小局使用。只处理带已发放局编号的过期物品，不按物品 ID 批量删除同类正常物品。
- 局编号只保留当前运行中的有效局，结算即移除，不累计历史列表；服务端重启后旧编号也视为过期。商店配置仅保存开关，不会随清理被删除。已放置方块仍由地图基准恢复处理。
- `RoundItemsTest` 验证默认关闭、模板保护、跨地图隔离、掉落／转交副本、离线压缩 NBT、下一局与重启过期、关闭开关恢复原标签；网络测试验证每项开关，实际 Mixin 引擎验证胜负结算、地图清理和原单物品购买发放拦截。
- 游戏内还需验收同一套餐混合设置、保留与清除、刀局／胜负结算／结束比赛、离线重连、FPSMatch 掉落物、转交、打开容器以及保存后重启。自动检查不等同于游戏验收。


### 商店方块部署（2026-10-15，协议 7）

- 方块物品的卡片底部新增“禁止部署／允许部署”开关，默认关闭，主副物品分别设置，保存商品后生效。与“仅本小局”相互独立：允许部署决定是否可放到地图上，仅本小局决定未放置的剩余物品是否在结算时清除。
- 仅接入继承 `BlockItem`、走标准 `ItemStack.useOn → ForgeHooks.onPlaceItemIntoWorld` 流程的方块。车辆、实体炮台、生成蛋或其他自行生成实体的部署器不在此接口内，需要按具体物品适配；编辑器不为非方块显示部署开关。
- 比赛仍使用原冒险模式；只对带 `MineStrikeAllowDeploy` 开关、并在购买发放时取得 `MineStrikeDeployMap` 标记的方块放开 CanPlaceOn 检查。普通同类方块、商品模板均不自动获得权限，CanDestroy 检查不改动，也不切换玩家模式。
- 服务端要求已开始比赛、玩家属于购买物品时的地图、维度一致且地图无恢复任务；只允许空气位置，禁止替换原地图方块。全部位置须在地图整数坐标范围内，避开包点区域和每个 T/CT 出生点附近（水平各 2 格、竖直各 3 格，含边界）。
- 多格放置逐个检查 Forge 快照。任何一个位置不符就取消整次放置，沿用 Forge 的方块与数量回滚，不强行解除其他模组的取消事件；所有事件处理完成且放置成功后才记入清理列表。
- 小局胜负结算、地图清理、结束比赛和强制新局前清除记录的位置，清空可清理的方块容器并以不产生掉落的方式置为空气；基准恢复继续负责整个地图及附带变化。记录不积累历史；异常退出后依靠下次开赛的基准恢复，不新增地图存档或覆盖基准。
- `ShopDeploymentsTest` 覆盖默认关闭、仅购买授权、模板隔离、地图与包点边界（含反向坐标）、出生点保护；实际 Mixin 验证 ItemStack 冒险放置与 Forge 入口、并检查破坏权限未注入自制调用。
- 游戏内仍需验收具体模组方块、主副物品、放置消耗与失败回滚、门／床等多格方块、区域拒绝、服务器取消、结算清理与基准恢复；自动检查不等同于游戏验证。
- 本轮按用户要求只更新源码、文档和仓库 `mods/mine-strike-skins-0.1.0.jar`，PCL 自制 JAR 暂不更新。之后联机需将客户端和服务端统一升级到协议 7。
