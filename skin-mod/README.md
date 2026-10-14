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
- 比赛状态每秒同步；选边和投票后立即同步。通信协议为 2，多人客户端和服务端须使用同版 JAR。

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
