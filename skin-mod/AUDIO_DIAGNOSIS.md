# 音频排查记录（2026-10-15）

## 范围与状态

用户反馈：脚步方向混乱、很近才听到；敌方枪声难以定位，甚至听不到。
已检查仓库与 PCL 的模组、配置、日志及相关 JAR 字节码。初始 Git 工作区干净。
初次排查仅新增本记录。随后用户明确选择脚步单变量测试：已备份并仅将仓库 `config/sound_physics_remastered/soundphysics.properties` 中的 `sound_direction_evaluation` 从 true 改为 false。备份位于 `backups/audio-direction-test-<时间>/soundphysics.properties`。
已逐字节验证有效配置只有该值变化，PCL 配置未变。没有修改源码、JAR、地图或发布包，没有提交或推送。
本次为测试配置；当前 PCL 启动仍使用原值，游戏听感尚未验收。关闭该项会影响所有受此功能处理的游戏声源，不局限于脚步；音量、距离、遮挡、混响和枪声转换保持原值。
未找到磁盘上的 AGENTS.md，遵循用户在对话中提供的规则。

## 已确认的事实

1. 两处音频相关配置和模组 JAR 一致。主要音频组件为 Sound Physics Remastered 1.20.1-1.5.1、Simple Voice Chat 2.6.18、TaCZ 1.1.8-hotfix、TaCZ Tweaks 2.14.1、Superb Warfare 0.8.9、BlockOffensive 1.2.5.1、Explosion Overhaul 0.2.3.0。
2. `options.txt` 为 `directionalAudio:false`，游戏音量分类均为 1.0，游戏输出使用默认设备。此开关关闭不代表普通左右立体声关闭，也不能据此断言前后混淆的原因。
3. Sound Physics 开启遮挡、混响与反射方向评估。实际 `ReflectedAudio.shouldEvaluateDirection()` 逻辑表明：`redirect_non_occluded_sounds=true` 会跳过无遮挡声源的重定位；语音声源也被排除在反射重定位之外。配置名容易误读，不应反向修改该项。
4. 当前 `reverb_brightness=0.1`。`ReverbParams` 计算四组 decayHFRatio 时分别乘以 0.6、0.7、0.7、0.7，得到 0.06、0.07、0.07、0.07；LWJGL 的 EFX 常量规定最小值为 0.1。PCL 2026-10-15 19:33 日志分别在游戏和语音初始化时记录四条 AL_INVALID_VALUE。确认是无效混响参数；尚未证明它造成脚步定位或枪声消失。候选修正 0.2 可得到合法的 0.12、0.14、0.14、0.14，应单独验证新启动日志。
5. 语音配置为 `audio_type=NORMAL`、`freecam_mode=CAMERA`、`use_natives=true`。旧 ULT WEAR 输出设备打开失败后回退默认；旧麦克风也回退默认。没有证据将用户描述的脚步/枪声问题归因于语音设备。
6. Explosion Overhaul 的耳鸣和低通均关闭。其 Sound Physics 兼容 Mixin 组合增益/滤波，所检查代码没有交换左右坐标。没有证据认定它是根因。
7. 自制模组没有接管世界声音定位；有关声音代码为菜单按钮点击音效。

## 脚步链路

BlockOffensive 的 `StepSoundMixin` 在服务端接管已分队 CS 地图玩家的脚步，使用实际玩家世界 XYZ 发送原版 ClientboundSoundPacket，按接收者阵营选择音量。

- 队友普通/闷脚步系数为 0.15 / 0.05，敌方为 1.2 / 0.4。闷脚步是原模组的特定脚步分支，不能直接等同于隔墙。
- 对普通音量为 1 的地面，原版基础衰减范围为 16 格，敌方普通脚步约为 19.2 格；声音事件或资源覆盖可改变最终范围。闷脚步并不会按 0.4 缩成 6.4 格范围，但其响度更低。
- Sound Physics 还会增加遮挡/滤波并可能重定位被遮挡的声音。是否因此只在近处可闻、方向偏移，需要同一位置、同一地面、同一队伍的 A/B。
- `resourcepacks/CSGOSounds.zip` 含双声道石头、草、沙等脚步。但当前 resourcePacks 为空，最近 PCL 日志仅加载 vanilla、tacz_resources、mod_resources，不能把未启用的资源包认定为当前根因。

## 枪声链路

- TaCZ 默认普通开火范围 64 格、消音 16 格。GunSoundInstance 对非相对声源使用 `(原始音量 × (1 - min(距离/范围, 1)))²`。原始音量为 1、范围 64 时，32 格系数 0.25，48 格为 0.0625，64 格为 0。实际武器 FireSound 可以覆盖范围，不能把默认值当成所有武器的最终值。
- 当前 `forceFirstPersonShootingSound=false`、`betterMonoConversion=false`。TaCZ Tweaks 已有第三人称声音单声道标记与转换实现，但该转换配置关闭。
- 本地资源检查发现第三人称双声道素材：maxstuff 44 个、Suffuse 3 个、Cold War 1 个、已解包默认枪包 2 个（按文件名含 3p 统计，不等同于实际选用数量）。因此部分武器存在双声道素材无法按世界位置正常定位的明确风险，尚未完成真实 Forge 加载后的缓存/转换验证。
- 默认 AK 同时提供 shoot / shoot_3p，不能仅凭第一人称素材是双声道推断敌方枪声有问题。
- Superb Warfare 的 RPG 第三人称/远处/极远处枪声、LAV-AD 开火与所检查的载具引擎素材均为单声道；RPG 第一人称与换弹为双声道。VehicleSoundInstance 每 tick 从载具更新 XYZ。它们应单独测试，不能套用 TaCZ 的配置。
- `features.bulletSounds=false` 只说明 TaCZ Tweaks 的额外弹道声音功能关闭；是否需要启用属于额外功能选择，不能据此解释枪口声音丢失。

## 自动验证结果

通过 ctypes 加载本机 LWJGL 3.3.1 和 3.3.3 自带的 arm64 OpenAL 库，用 SOFT_loopback 离线渲染 700Hz 固定声源：声源 (-8,0,0)，监听者位于原点、朝 -Z，再转向 +Z。

| 输入 | 朝 -Z 左/右 RMS | 朝 +Z 左/右 RMS | 结果 |
|---|---|---|---|
| 单声道，两套库结果相同 | 0.03237 / 0 | 0 / 0.03237 | 转身后正确交换 |
| 双声道，两侧输入相同，两套库结果相同 | 0.25894 / 0.25894 | 0.25894 / 0.25894 | 默认不随世界声源方向定位 |

各项 AL_error=0。此测试仅验证底层库默认行为，不覆盖游戏 Mixin、真实武器路径、耳机输出或听感。
OpenAL 默认仅空间化单声道的行为见 [官方扩展说明](https://openal-soft.org/openal-extensions/SOFT_source_spatialize.txt)。

## 单变量游戏验收（尚未执行）

先在无比赛的靶场或其他不破坏基准的场地，记录测试者位置、视角、声源位置和地面材质。两人测试保持阵营关系不变；不要保存新地图基准。

1. 固定声源：使用实际坐标在测试者左侧 8 格播放原版 `minecraft:block.note_block.hat`，转身 180°，保持声源世界坐标不变，重复播放。应从左侧变为右侧。不要转身后再用相对坐标重新生成声源。
2. 分开记录原版固定音、其他玩家脚步、TaCZ、Superb Warfare RPG、载具引擎、附近语音。枪声由另一玩家触发，不用自己的第一人称枪声判断敌方定位；语音使用附近说话而非群组通话。
3. 脚步在同一地面分别测试 4/8/16/24 格；枪声另测 16/32/48/64 格。先无遮挡，再使用现有墙体遮挡。记录每项可闻性、左右、转身后是否交换、是否只有尾音。
4. 第一组 A/B 只切换 Sound Physics 的 `enabled=true/false`，完整重启后重复相同测试；测试完恢复原值。此步骤隔离遮挡/混响/反射整体影响，不同时改音量、距离、HRTF 或语音。
5. 若方向问题集中于被遮挡声音，再单独测试 `sound_direction_evaluation=false`；不要把这一项与关闭整个模组混在一次对照中。
6. 单独测试 TaCZ 的 `betterMonoConversion=false/true`，重启清理声音缓存，选用已确认第三人称素材为双声道的同一把枪；同时用默认 AK 的单声道第三人称枪声作为对照。
7. 只有方向验证通过、仍明确距离不足时，才评估脚步或枪声范围；不先全局放大声音或修改所有武器。

有效配置变更前按用户要求确认并备份；PCL 同步和重新发版需用户另行明确。本轮没有运行 skin-mod/verify.py：没有修改自制源码，构建检查不能证明听感正确。

## 当前结论

未发现底层库默认左右颠倒；已经确认无效混响参数，发现部分第三人称枪声的双声道转换风险，以及脚步/枪声的实际距离与衰减限制。实际玩家所遇到的问题可能涉及多条链路，目前不能宣称单一根因或修复完成。
