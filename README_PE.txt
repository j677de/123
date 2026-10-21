勇者之路 本地服务器 + GM（正式核心版 v5）

目标：让游戏客户端通过 127.0.0.1:9055 / 8058 在同一台 Android 设备离线运行。
本工程不使用 AndroidX 或第三方服务器库，方便 AndroidPE / JKAS 本地编译。

当前 v5 已落实的正式核心：
1. 本地账户/session/区域进入
2. 单角色持久化、角色资料、属性与加点
3. 背包/仓库分页、物品详情、移动、丢弃、出售
4. 装备/卸装/全部卸装/自动装备
5. 地图、怪物、战斗开始/停止/手动战斗
6. 挂机计时收益：经验、金币、等级提升、属性点
7. 9055 HTTP + 8058 WebSocket，ping/pong 与战斗状态推送
8. 游戏设置 SQLite 持久化
9. 强化等级持久化基础
10. 商店购买基础闭环
11. GM 后台：角色、货币、背包、战斗、存档维护
12. 服务前台常驻、顶部安全区域

说明：
- v5 是“正式核心主循环版”，不是宣称所有客户端外围系统都已完全还原。
- 交易所、活动、邮件、复杂合成/荧光/转职等路由已开始兼容，仍需继续按游戏 APK 的真实 DTO 与规则补齐。
- 不使用 Mock 开关；所有已实现的主循环数据直接写入 SQLite。

AndroidPE 编译：
  rkb setup ide
  cd YongzheLocalServer_PE_GM_v5
  chmod +x build_pe.sh
  ./build_pe.sh

APK：app/build/outputs/apk/debug/app-debug.apk

服务：
  HTTP http://127.0.0.1:9055
  GM   http://127.0.0.1:9055/gm
  WS   ws://127.0.0.1:8058/afk/ws


v5.1 修复说明：
- HTTP 请求体按 Content-Length 字节读取，避免中文 UTF-8 数据导致请求体截断。
- HTTP 与 WebSocket 握手按原始字节流逐行读取，避免 BufferedReader 预读吞掉后续帧。
- 尚未执行 Android Gradle 实际构建；AndroidPE 的 rootfs/终端故障是独立问题。
- 原工程中大量外围接口仍是占位响应，不能宣称完整游戏兼容。
