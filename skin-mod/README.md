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
- 比赛状态每秒同步；选边和投票后立即同步。通信协议为 8，多人客户端和服务端须使用同版 JAR。

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

2026-10-15：自制模组通信协议升级为 **8**，服务器和所有客户端必须更新本 JAR。
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
`ShopMixinTest` 用实际 Forge 0.8.5 Mixin / ASM 9.7.1 引擎应用十五个商店、回合、冒险模式与雷达目标，
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


### 比赛中小地图仅显示队友（2026-10-15，协议 8）

- 自制模组兼容已安装 Xaero Minimap 25.3.13，不修改其 JAR 或雷达配置。比赛运行时，服务端按 CSGameMap 的 T/CT 实际队伍为每位玩家同步本队 UUID 白名单；不依赖名称、颜色、客户端好友或 Xaero 分类规则。
- 只显示自己与本队玩家，敌方圆点、头像、名称及独立远距离追踪标记统一过滤。观战仍按本地玩家的阵营判断，不按摄像机跟随的敌方实体判断；未选 T/CT 的玩家不显示其他玩家。非玩家雷达实体沿用原逻辑。
- 服务端每 tick 检查阵营及比赛状态，仅状态变化时发包；开赛和结束时立即同步。队伍切换替换名单，比赛外恢复 Xaero 原显示规则。客户端连接与断线默认隐藏其他玩家，直到收到本次连接的服务端状态，避免复用旧服务器名单。
- 三个仅客户端的可选 Mixin 覆盖 RadarRenderer 的普通／单独绘制，以及 PlayerTracker 的渲染与隐藏检查；没有 Xaero 时不加载这些目标，不增加服务端 Xaero 依赖。
- `RadarPolicyTest` 验证队友／敌方、未知追踪 UUID、观战身份、换边、未选阵营、比赛外和重连初始策略；网络测试验证 UUID 名单与人数上限；真实 Mixin 引擎验证当前 Xaero 的全部四处过滤注入。
- 自动检查不等同于游戏验收。仍需两名玩家分别加入 T/CT，检验圆点／头像／名称、好友与远距离追踪、死亡观战、换边、重连和结束比赛后的恢复。
- 本轮按用户授权备份并更新仓库及 PCL 的自制 JAR；联机客户端和服务端需统一协议 8。


### 玩家摔落伤害减半（2026-10-15）

- 自制模组在服务端 `LivingDamageEvent` 的 LOWEST 优先级处理玩家的 `minecraft:fall` 伤害，将即将扣除的生命伤害乘以 0.5，保留小数，不改变原来的起伤高度。
- 整个存档生效，包括比赛内外和各维度；只处理服务端玩家。枪械、爆炸、火焰、撞墙、被落下的方块砸伤，以及非玩家实体的伤害保持原逻辑。已被取消的伤害事件不处理。
- 本次没有新增网络消息，通信协议仍为 8。更新自制 JAR 后重启游戏／服务器生效；实际摔落扣血仍需游戏内验收。


### LAV-AD 血量调整（2026-10-15）

- 当前 Superb Warfare 0.8.9 的 LAV-AD 默认最大血量为 300；自制模组仅针对 `superbwarfare:lav_ad`，使用原模组的实例 Override 将最大血量改为 150（v0.5.1 从先前的 75 翻倍）。
- 商店载具箱展开及已有载具重新加载时生效。保留载具原有受损比例，不在每次加载时继续翻倍；维修上限也为 150。其他实例 Override、武器、装甲、弹药、能量及其他车型保持原值。
- 使用原模组的载具数据同步与保存机制，不修改 Superb Warfare 原 JAR、商店配置或地图基准，通信协议仍为 8。更新自制 JAR 后重启游戏，实际部署及维修显示仍需游戏内验收。

### v0.5.1 商品数值

- Dust2 T/CT 的 Superb Warfare RPG 商品均为 8000；`GunData.Ammo=1`、`VirtualAmmo=1`，购买时总计 2 发。装填状态同步设为非空、击锤打开，其他商品属性保留。
- RPG 使用 Superb Warfare 的原生弹药逻辑，不使用 TaCZ 虚拟弹药字段。背包里额外的兼容弹药仍可按原模组逻辑补给。
- 发布包及同步后的 Dust2 JSON 使用 ASCII Unicode 转义，字段值保持不变，避免 Java 17 不同默认编码导致地图读取失败。

## 竞技音频模块（候选版，2026-10-15）

模块集成在原自制 JAR 中，仍为 0.1.0、协议 8，不修改原模组 JAR、不新增网络消息。
本次交付为 `skin-mod/build/libs/mine-strike-skins-0.1.0.jar`。2026-10-15 经用户授权，已备份并替换仓库 `mods` 和 PCL 的同名自制 JAR，两处 SHA-256 均为 `3ddbed0ade7f9b6ac41e93a6b8e5280f5fdfd5692fecf86ba077c7452558ba19`；备份位于 `backups/audio-module-deploy-20261015-223919/`。未重新打包发布，实际听感待用户验收。
原反射方向测试已恢复，两处 Sound Physics 配置仍保持 `sound_direction_evaluation=true`。

首次使用此候选 JAR 启动客户端时生成 `config/mine-strike-audio-client.toml`。所有设置修改后完整重启 Minecraft，确保 HRTF、现有声源和衰减状态一起重新初始化。无新增按键或界面。

| 配置 | 默认值 | 作用 |
|---|---|---|
| enabled | true | 模块总开关；false 时恢复原声音处理路径 |
| preferHrtf | true | 请求原版 OpenAL 的 HRTF，不写入 options.txt；设备实际启用状态记录在日志 |
| directPosition | true | 选中的脚步、远处枪声和载具保留真实声源位置，覆盖 Sound Physics 的反射位置 |
| spatializeStereo | true | 使用 AL_SOFT_source_spatialize 对远处双声道声源进行空间化；有距离时由 OpenAL 下混为单声道再做双耳定位，不改音频资源或缓存 |
| adjustAttenuation | true | 启用脚步范围和远处 TaCZ 枪声衰减调整 |
| footstepRange | 32.0 | 客户端脚步最小衰减范围；保留原队友/敌人音量差，不扩大服务端发送范围 |
| taczFalloffExponent | 1.0 | TaCZ 远处开火按线性下降，范围沿用该枪声包原值；原逻辑为平方下降 |
| footstepVolume / gunVolume | 1.0 / 1.0 | 独立响度乘数，不通过放大原始音量间接改变传播范围 |
| vehicleVolume | 0.7 | 降低远处引擎、履带等连续声音对战斗声的掩盖；自己乘坐的载具排除 |
| dryCombatSounds | true | 去除选中脚步和远处枪声的辅助混响发送，保留直接声的遮挡滤波；引擎不做此处理 |
| limitShotTails | true | 针对能识别射手的 TaCZ 枪声限制旧尾音；不同玩家、不同声音分开计数 |
| maxShotTails | 3 | 旧尾音软上限，150 毫秒内的攻击声不裁剪，新枪声不因上限而拒绝播放 |
| diagnostics | false | 输出声音请求、声源开始及 TaCZ 已收到的声音包信息，用于定位遗漏环节 |

定位与空间化覆盖玩家类别的原版方块脚步、TaCZ 的 shoot/silence 类远处声音，以及 Superb Warfare 的第三人称/远处枪声、LAV-AD 开火和引擎/履带等声音。语音、菜单、换弹、第一人称枪声及自己乘坐的载具不走这些单独的处理规则。HRTF 是设备级请求，不局限于某一种声音。

声音实例不替换，不改变 TickableSoundInstance 的身份、tick、暂停、停止或循环逻辑；载具继续由原模组更新坐标。混音增益施加在原声音分类音量之后，分类静音不会被重新放大。辅助混响和位置在原声学计算完成后按已登记的声源处理；以实际 OpenAL 上下文（含线程本地上下文）和声源编号配对，避免游戏与语音 context 的同号声源互相影响。声源销毁和退出世界时清理登记。

TaCZ 的例子：基础音量为 1、范围 64 时，32 格处从原来的 0.25 改为 0.5，48 格从 0.0625 改为 0.25，64 格仍为 0。消音枪声仍使用它原来的范围，不统一扩成普通枪的范围。Superb Warfare 保留原来的传播及衰减曲线，第一版处理其方向、空间化、辅助混响和独立响度；没有证据时不统一重写该模组的远/极远声层。

只有 TaCZ 声音实例当前能可靠取得射手身份，因此尾音限制首先适用于 TaCZ。未可靠标识射手的 Superb Warfare 枪声不按坐标猜测合并，不跨玩家删声，也不截断载具循环。原模组原有的并发上限仍然存在。

### 自动验证

仍运行：

```sh
python3 skin-mod/verify.py --libraries "/Users/qbqb/Library/Application Support/minecraft/libraries"
```

新增 AudioPolicyTest 覆盖声音分类、第一人称/换弹排除、距离曲线单调性、消音范围边界、静音保持，以及不同射手/武器的尾音隔离、攻击声保留和清理。
真实 Mixin 引擎检查 Forge 47.4.0 SoundEngine 的缓冲/流式播放两条路径、起始/持续响度、Channel 首次 alSourcePlay 前的设置、Library HRTF、TaCZ 构造和收到声音包的入口、Superb Warfare 载具身份、Sound Physics 处理回调。
macOS 额外加载已安装的 OpenAL 原生库进行 SOFT_loopback 离线渲染，检验双声道空间化、转身 180°、24/32 格衰减以及恢复默认空间化，不播放到实际耳机。其他平台此离线原生测试会明确跳过，Java/字节码检查继续执行。

自动检查没有启动 Forge 世界，不代表耳机听感、所有模组联合运行或服务器远处声音发送已经验收。

### 双人游戏验收

1. 关闭游戏，备份旧自制 JAR 后安装候选 JAR。其他音频模组及其配置保持原样。启动后核对日志 `[audio] OpenAL HRTF` 的实际状态：1 表示启用，-1 表示扩展不支持，其他状态需按原生状态继续排查。
2. 总开关 enabled=false 与 true 分别完整重启做对照；之后每次仅改变一个模块开关。
3. 在同一地面、同一队伍关系下，另一玩家于固定位置走动；分别在 4/8/16/24/32 格监听，原地转身 180° 后方向应正确交换。再用现有墙体测试遮挡；不保存地图基准。
4. 分别测试默认 AK、第三人称素材为双声道的枪、消音枪、Superb Warfare RPG、LAV-AD 与移动载具。区分自己开火和另一玩家开火，按 16/32/48/64 格记录方向、响度和可闻性。
5. 两个敌人在不同方向同时开火，再加入载具引擎和移动脚步。检查每个敌人新开火的攻击声仍保留，同一人的旧尾音减少；检查引擎随车移动、熄火/离车/断线后能正常停止。
6. 检查语音、自己的第一人称枪声、换弹、分类音量为 0 时静音，以及退出重进、F3+T 资源重载、声音设备重载。
7. 如果仍有远处枪声消失，打开 diagnostics：TaCZ packet 行确认客户端处理到包，loaded=false 表示本机尚未加载射手实体，原模组会提前返回；packet 有但 request/source 无时排查枪包解析或加载。没有 packet 日志只能作为未进入该处理入口的线索，不能单独证明服务端未发送。第一版不伪造未收到的位置或枪声。

回退：将模块 enabled=false 并完整重启，或恢复备份的自制 JAR。现有 Sound Physics 的无效混响亮度参数问题仍单独记录，本模块未改写该配置，也不声称已经解决所有声音异常。

## 短曳光与近身擦弹强调（候选版，2026-10-15）

在已有音频版本上增加客户端弹道特效，JAR 仍为 `mine-strike-skins-0.1.0.jar`，协议仍为 8。本次只生成仓库构建，未替换仓库 `mods` 或 PCL 的已安装音频 JAR，未发版。

- TaCZ 普通子弹与 Superb Warfare `ProjectileEntity` 普通枪械/机枪子弹，在原生 tick 前后读取实际客户端位移，生成暖白色短光迹。头部较亮、尾部透明，并带窄光晕和正面可见的小亮点。
- 默认最长 6 格，约 0.15 游戏秒（3 tick）消退；观察距离最多 128 格。不一次画出枪口到落点的完整长线。
- 运动线段经过摄像机 3 格以内时，在最近经过位置附近保留一小段更醒目的光迹，宽度约为普通情况的 1.8 倍，让单 tick 内快速擦过的子弹也能看见方向。它是短暂视觉残影，不是弹速变化或伤害提示。
- 使用世界深度测试、仅颜色写入与原版位置颜色着色器，不使用穿墙或屏幕覆盖绘制。轨迹在首个碰撞方块截断，此弹之后不再添加新光迹，避免客户端弹丸等待服务器移除时产生墙后延伸。
- 截断按方块碰撞形状，玻璃等也会截断增强光迹；穿透弹出墙后的后续增强光迹暂不处理。原弹丸穿透、弹速、伤害、实体命中与移除仍由原模组决定。
- 爆炸弹药、RPG、火箭、炮弹使用原特效。默认跳过已识别为本机射手的子弹，保留原版第一人称曳光；无法识别射手的客户端弹丸可能仍显示增强光迹。
- 每帧最多绘制 256 条，按离摄像机的距离优先选择；缓存最多为绘制预算的四倍。退出、切换世界、总开关关闭后清理缓存。没有新增屏幕边缘提示、按键或图片资源。

首次安装候选 JAR 启动客户端后生成 `config/mine-strike-tracers-client.toml`，修改后完整重启：

| 设置 | 默认值 | 作用 |
|---|---|---|
| enabled | true | 弹道增强总开关 |
| tacz / superbWarfare | true / true | 两种弹丸独立开关 |
| showOwnShots | false | 是否同时增强自己的子弹 |
| length | 6.0 | 最大短光迹长度，单位格 |
| lifetime | 0.15 | 消退时间，单位游戏秒 |
| width | 0.025 | 核心光迹基础半宽，单位格 |
| nearRadius / nearBoost | 3.0 / 1.8 | 近身擦过的识别距离与宽度倍率 |
| maxDistance | 128.0 | 客户端绘制距离 |
| maxStreaks | 256 | 每帧绘制预算，优先附近轨迹 |

### 验证与实战验收

执行原 `skin-mod/verify.py --libraries ...`：新增 TrailGeometryTest 检查方向、6 格长度上限、插值/消退、近身高速弹的局部强调、零位移/无效坐标、沿视线/垂直方向退化，以及截断后不会绘制到终点之外。真实 Mixin 引擎检查 TaCZ 和 Superb Warfare 弹丸 tick 接入，并核对原 tick 的所有调用仍按原顺序保留；检查效果代码不调用移动、删除、伤害或修改方块的方法。已有音频和比赛检查继续执行。

自动检查不能替代真实世界的渲染验收，尤其是 Oculus 光影、Embeddium、Fabulous 模式与世界深度缓冲：

1. 两名玩家在无遮挡靶场，分别用 TaCZ 与 Superb Warfare 普通枪、机枪，在 8/24/48 格射击；侧面、斜向与正面观察短光迹，头部较亮的一端应沿实际飞行方向前进。
2. 让子弹从身体附近经过，检查近身短残影可见、没有屏幕闪烁或满屏长线；转身观察不同来弹方向。
3. 对着现有厚墙和玻璃射击：增强光迹应在碰撞面结束，墙后的不可见部分不透出。不要修改或保存地图基准。
4. 检查 RPG、炮弹和爆炸弹药仍使用原特效；检查自己的第一人称曳光、瞄准、音频、原弹速与伤害流程。
5. 多人持续扫射，检查帧率、近处光迹优先级、停火后及时消退；分别开启/关闭光影，测试资源重载、退出重进与跨维度没有残留。

仅增强客户端实际观察到的弹丸运动，不通过开火声音推测弹道，也不恢复从未同步到本机的弹丸。回退可关闭本配置 enabled 并重启，或恢复之前已备份的音频版 JAR。

## C4 安装播报与滴滴声增强（构建版，2026-10-15）

保留已有音频与短曳光功能。只构建验证，未替换仓库 `mods` 或 PCL 的已安装 JAR，未发版；版本仍为 0.1.0，协议仍为 8。

- 安装成功后使用 FPSMatch 现有声音包向本场全部加入者（含观战）播报一次，替换原来安装位置的距离声音，不重复叠加。客户端使用无方向、无距离衰减的 MASTER 播报，原素材增强约 2 dB；仍遵守游戏主音量和静音设置。
- C4 滴滴声使用增强约 9 dB 的单声道素材。服务端发送基础范围与客户端衰减范围均为 96 格，保留炸弹真实位置、左右方向与距离衰减；96 格是衰减终点，不保证边缘处仍清晰可闻。
- 仅对这两种 C4 声音绕过 Sound Physics 墙体低通、反射与方向重算，使其不会被墙体压得过轻。其他声音仍按现有音频设置处理。
- 不改变安装/拆除时间、滴滴频率、倒计时、爆炸伤害、HUD 或地图基准。不修改 BlockOffensive/FPSMatch 原 JAR；增强素材放在自制模组的独立资源命名空间。
- 服务端/房主需要新自制 JAR 才能扩大广播与发送范围，客户端需要同版 JAR 才能使用增强素材与播放方式。客户端播放处理与公共 Mixin 分离，公共接入不加载客户端播放类。

自动验证使用真实 Mixin 引擎检查两处音频调用各接入一次、其余炸弹方法保持原样、本场广播包含观战，并检查单声道 Vorbis 资源和 Sound Physics 接入。离线测得增强素材峰值约为：播报 -1.0 dBFS、滴滴 -9.9 dBFS，未发现削波；这些检查不代表实战听感已验收。

游戏验收：两队及观战分别在安装点附近和地图远端确认播报只响一次、响度不随位置变化；在 8/24/48/72 格测试滴滴声，原地转身 180° 检查方向交换，再隔墙对比可闻性；确认拆除、爆炸与计时正常。检查游戏主音量为 0 时仍静音，不保存新地图基准。

## 已安装 C4 顶部脉冲标志（构建版，2026-10-15）

安装后将顶部中央红色 `--:--` 替换为像素 C4：炸药棒、绑带、控制器、显示屏和状态灯，暗红常显、亮红脉冲并带轻微红晕。沿用原计时文字的中心点和缩放，其他头像、人数、比分、金钱与拆除进度保持原样。使用 GUI 几何绘制，无新增图片或光影依赖。

脉冲以服务端实际炸弹剩余 tick 和总时长计算，频率从约 0.8 Hz 连续加快至 3.2 Hz；最后几秒最急促。客户端在一 tick 内插值，使用连续相位，避免切换快慢档时跳闪。不显示额外数字倒计时。原模组仅向观战同步 `BombFuseS2CPacket`，本补丁把该现有数据包的接收者扩大到本场两队与观战，保留计时数值、发送节奏和原协议。

只在比赛进行、原计时为 `--:--`、炸弹剩余时间大于 0 且未进入回合结算时绘制；拆除/爆炸/回合结束后按原状态恢复，下一回合正常计时。修改不写入炸弹计时、不改滴滴频率和爆炸逻辑。服务端/房主与客户端均需新自制 JAR；未安装新服务端时参赛者可能收不到计时数据，此时保留原 `--:--`。

自动检查覆盖连续加速、亮度范围、状态切换，使用真实 Mixin 引擎验证 HUD 绘制接入、普通文字回退，并核对 `setFuse` 除接收者查询外的指令不变、其余炸弹方法保持原样。此前音频与弹道检查继续执行。`build/c4-hud-preview.png` 为几何绘制的放大预览，不是游戏截图。

实际验收需分别从 CT、T、观战视角检查安装后的图标位置、不同 GUI 缩放下的可读性、临近爆炸的闪烁加速，以及拆除、爆炸和下一回合及时恢复。当前仅构建验证，未同步仓库 `mods`、PCL 或发布包。
