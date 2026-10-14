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
