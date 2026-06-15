# 玩家与角色交互界面设计规格

> 日期: 2026-06-15 | 状态: 设计完成 | 关联需求: REQ-8

## 1. 概述

为《Girlfriends》模组实现玩家与角色实体的交互界面系统，包含三个界面：
- **主交互界面** (`MainInteractionScreen`)：右键角色实体后打开，承担委托、关系、跟随、送礼四大功能的入口
- **委托查看界面** (`QuestViewScreen`)：展示委托详情，支持接受或拒绝
- **送礼界面** (`GiftScreen`)：从玩家背包选择物品赠送给角色

## 2. 架构

### 2.1 架构选择

混合架构：
- 主交互界面和委托查看界面：纯客户端 `Screen`，不关联 `AbstractContainerMenu`
- 送礼界面：纯客户端 `Screen`，直接渲染玩家背包、发送槽位索引包，服务端 handler 做权威物品校验和扣减

### 2.2 文件清单

```
src/main/java/com/hexagram2021/girlfriends/
├── client/
│   └── screen/
│       ├── GirlfriendsScreens.java              (Screen 注册入口)
│       ├── MainInteractionScreen.java           (主交互界面)
│       ├── QuestViewScreen.java                 (委托查看界面)
│       └── GiftScreen.java                      (送礼界面)
└── common/
    └── network/
        └── serverbound/
            ├── ServerboundDeliverQuestPacket.java        (新增)
            ├── ServerboundConfirmIntimacyPacket.java     (新增)
            ├── ServerboundInviteHomePacket.java          (新增)
            └── ServerboundGiveGiftFromSlotPacket.java    (新增)
```

### 2.3 Screen 注册

`GirlfriendsScreens` 作为客户端 Screen 注册入口，由 `GirlfriendsModClient.onRegisterMenuScreens` 调用（当前为空白方法体）。

`MainInteractionScreen` 和 `QuestViewScreen` 不关联 `MenuType`，通过 `Minecraft.getInstance().setScreen()` 直接打开。

## 3. 主交互界面 `MainInteractionScreen`

### 3.1 打开方式

采用 pending 标记机制解决时序问题（客户端 `mobInteract` 返回时服务端数据包尚未到达）：

1. `GirlfriendEntity.mobInteract()` 客户端侧：调用 `ClientInteractionStore.markPendingInteraction(girlfriendTypeId)` 设置待处理标记，然后返回 `InteractionResult.SUCCESS`
2. 服务端 `GirlfriendEntity.mobInteract()`：构建 `InteractionSummary` → 通过 `ClientboundSyncInteractionDataPacket` 发送至客户端
3. `ClientboundSyncInteractionDataPacket` 客户端 handler（`GirlfriendsNetwork`）：调用 `ClientInteractionStore.setSummary()` 存入缓存 → 检查 `ClientInteractionStore.consumePendingInteraction(girlfriendTypeId)` → 若返回 true，从缓存读取 `InteractionSummary`，构造 `MainInteractionScreen` 并 `setScreen`

> `ClientInteractionStore` 新增两个方法：`markPendingInteraction(Identifier)` 设置标记，`consumePendingInteraction(Identifier)` 原子获取并清除标记。

### 3.2 界面布局

```
┌─────────────────────────────────┐
│         角色名称                  │  ← 标题
│      好感阶段 + 进度条            │  ← 如 "亲密 732/900"
├─────────────────────────────────┤
│  [查看委托] 或 [交付委托]         │  ← 同位置切换
│  [确立关系] 或 [邀请同居]         │  ← 同位置切换，默认隐藏
│  [跟随模式: 无 / 跟随 / 停留]    │  ← 显示当前模式，点击切换
│  [   赠送礼物   ]                │
├─────────────────────────────────┤
│           [  关闭  ]             │
└─────────────────────────────────┘
```

### 3.3 按钮状态表

| 按钮 | 可见条件 | 可点击条件 |
|---|---|---|
| **查看委托** | `currentQuest != null` 且 `ownerPlayerUuid != player` | `currentQuest != null`（有则亮，无则灰） |
| **交付委托** | `currentQuest != null` 且 `ownerPlayerUuid == player` | 委托全部目标完成（`isCompleted()` 为 true），否则灰 |
| **确立关系** | `needsIntimacyConfirmation == true`（好感 >= 700 且未确认亲密） | 始终可点击 |
| **邀请同居** | `canInviteHome == true`（好感 >= 900 且已确认亲密） | 始终可点击 |
| **跟随模式** | 始终可见 | `canFollow == true`（已确认亲密），否则点击无反应 |
| **赠送礼物** | 始终可见 | `canGiveGift == true` |

### 3.4 跟随模式切换

- 当前模式显示在按钮上，点击循环：`NONE → FOLLOW → STAY → NONE → ...`
- 仅在 `canFollow` 为 true 时响应点击
- 选中的新模式暂存于 Screen 实例字段，**关闭 Screen 时**（`onClose`）发送 `ServerboundSetFollowModePacket(girlfriendTypeId, selectedMode)`
- 家园模式（HOME）是邀请入住后的特殊情况，不进入切换逻辑

### 3.5 各按钮行为

| 按钮 | 行为 |
|---|---|
| 查看委托 | 从 `summary.currentQuest()` 获取 `QuestContentSummary`，构造 `QuestViewScreen` 并 `setScreen` |
| 交付委托 | 发送 `ServerboundDeliverQuestPacket(girlfriendTypeId)`，直接完成 |
| 确立关系 | 发送 `ServerboundConfirmIntimacyPacket(girlfriendTypeId)` |
| 邀请同居 | 发送 `ServerboundInviteHomePacket(girlfriendTypeId)` |
| 赠送礼物 | 打开 `GiftScreen(girlfriendTypeId)` |

## 4. 委托查看界面 `QuestViewScreen`

### 4.1 打开方式

主界面点击"查看委托"按钮后，将 `InteractionSummary.currentQuest()` 传入构造函数。

### 4.2 界面布局

```
┌─────────────────────────────────┐
│        委托：花影寻踪             │  ← Component.translatable(titleKey)
│          (固定委托)               │  ← quest type 副标题
├─────────────────────────────────┤
│  委托描述文本...                  │  ← Component.translatable(descriptionKey)
│                                 │
│  目标：                          │
│  ☐ 目标一 (0/5)                 │  ← objectiveSummaryKeys 逐条渲染
│  ☐ 目标二                        │
│                                 │
│  奖励：                          │  ← TODO: rewardKeys 待后续补充
├─────────────────────────────────┤
│  [  接受委托  ]  [ 我再想想 ]    │
└─────────────────────────────────┘
```

### 4.3 按钮行为

- **接受委托**：发送 `ServerboundAcceptQuestPacket(girlfriendTypeId)`（复用现有包），发送后关闭两层 Screen（委托界面 + 主界面），或仅关闭委托界面回到主界面
- **我再想想**：`onClose()` 回到主界面

### 4.4 委托完成判定（交付按钮灰显依据）

交付按钮在客户端侧无法直接获取 `isCompleted()` 结果。方案：在 `InteractionSummary` 中新增 `boolean questCompleted` 字段，由 `InteractionSummaryService` 在构建时填充，当 `currentQuest != null && ownerPlayerUuid == player` 时计算并写入。

当前 `QuestContentSummary` 不含 `isCompleted` 和 `rewardKeys` 字段，本次实现中：
- `isCompleted`：优先新增（用于交付按钮灰显）
- `rewardKeys`：留 TODO，后续补充

## 5. 送礼界面 `GiftScreen`

### 5.1 打开方式

主界面点击"赠送礼物"按钮后，构造 `GiftScreen(girlfriendTypeId)` 并 `setScreen`。

### 5.2 布局

9 列 × 4 行网格（36 格 = 9 热键栏 + 27 背包存储），与玩家物品栏布局一致：
- 槽位 0~8：底部热键栏
- 槽位 9~35：上三行背包存储

渲染使用 `InventoryScreen` 风格，本地读取 `Minecraft.getInstance().player.getInventory()`。

### 5.3 偏好高亮

从 `ClientInteractionStore.getSummary(girlfriendTypeId).knownGiftPreferences()` 获取偏好列表，遍历 36 格匹配：

| 偏好档位 | 高亮颜色 |
|---|---|
| `FAVORITE` | 金色边框 |
| `LIKED` | 绿色边框 |
| `ACCEPTED` | 蓝色边框 |
| `DISLIKED` | 红色边框 |
| 未知/不匹配 | 无高亮 |

### 5.4 交互

- 空槽位点击无反应
- 非空槽位点击 → 发送 `ServerboundGiveGiftFromSlotPacket(slotIndex, girlfriendTypeId)` → 立即 `onClose()`
- 若服务端判定为 `REJECTED`，不扣减物品，聊天栏发送相应提示文本

## 6. 新增网络包

四个新增 serverbound 包，遵循现有 `record` + `TYPE` + `STREAM_CODEC` 模式。

### 6.1 包清单

| 包 | Payload | 注册名 |
|---|---|---|
| `ServerboundDeliverQuestPacket` | `Identifier girlfriendTypeId` | `deliver_quest` |
| `ServerboundConfirmIntimacyPacket` | `Identifier girlfriendTypeId` | `confirm_intimacy` |
| `ServerboundInviteHomePacket` | `Identifier girlfriendTypeId` | `invite_home` |
| `ServerboundGiveGiftFromSlotPacket` | `Identifier girlfriendTypeId` + `int slotIndex` | `give_gift_from_slot` |

### 6.2 服务端 Handler 逻辑

**`ServerboundDeliverQuestPacket`**：
1. `canReachEntity` 校验（距离 ≤8、实体存在且存活）
2. `QuestService.completeCurrentQuest(playerUuid, girlfriendTypeId)`
3. 完成后发 `ClientboundQuestIconPacket` 更新图标
4. 聊天栏消息提示

**`ServerboundConfirmIntimacyPacket`**：
1. `canReachEntity` 校验
2. `BindingService.confirmIntimacy(playerUuid, girlfriendTypeId)`
3. 聊天栏消息提示

**`ServerboundInviteHomePacket`**：
1. `canReachEntity` 校验
2. 获取玩家重生点（`player.getRespawnPosition()` / `player.getRespawnDimension()`）
3. `BedValidator.isValid()` 校验重生点为双人床
4. `HomeService.inviteHome(playerUuid, girlfriendTypeId, dimension, x, y, z)`
5. 聊天栏消息提示

**`ServerboundGiveGiftFromSlotPacket`**：
1. `canReachEntity` 校验
2. 校验 `slotIndex` 合法（0~35）
3. `player.getInventory().getItem(slotIndex)` 取物品（服务端权威）
4. 若物品为空 → 静默返回
5. `GiftService.applyGiftItem(playerUuid, girlfriendTypeId, itemStack)`
6. 若未被拒绝 → `player.getInventory().removeItem(slotIndex, 1)`
7. 聊天栏消息提示（含偏好档位和好感度变化）

### 6.3 注册

在 `GirlfriendsNetwork.register()` 中新增 4 行：
```java
registrar.playToServer(ServerboundDeliverQuestPacket.TYPE, ServerboundDeliverQuestPacket.STREAM_CODEC, GirlfriendsNetwork::handleDeliverQuest);
registrar.playToServer(ServerboundConfirmIntimacyPacket.TYPE, ServerboundConfirmIntimacyPacket.STREAM_CODEC, GirlfriendsNetwork::handleConfirmIntimacy);
registrar.playToServer(ServerboundInviteHomePacket.TYPE, ServerboundInviteHomePacket.STREAM_CODEC, GirlfriendsNetwork::handleInviteHome);
registrar.playToServer(ServerboundGiveGiftFromSlotPacket.TYPE, ServerboundGiveGiftFromSlotPacket.STREAM_CODEC, GirlfriendsNetwork::handleGiveGiftFromSlot);
```

### 6.4 复用现有包

| 包 | 用途 |
|---|---|
| `ServerboundAcceptQuestPacket` | 委托查看界面"接受委托"按钮 |
| `ServerboundSetFollowModePacket` | 主界面 onClose 时发送跟随模式变更 |

> `ServerboundGiveGiftPacket`（现有，使用主手物品）在交互界面体系中被 `ServerboundGiveGiftFromSlotPacket` 替代，保留原包以兼容未来可能的快捷操作。

## 7. 数据流

### 7.1 主界面打开流程

```
玩家右键 GirlfriendEntity
  → GirlfriendEntity.mobInteract() [client]: 
      ClientInteractionStore.markPendingInteraction(girlfriendTypeId)
      return InteractionResult.SUCCESS
  → GirlfriendEntity.mobInteract() [server]: 
      构建 InteractionSummary
      → ClientboundSyncInteractionDataPacket → client
  → 客户端接收 ClientboundSyncInteractionDataPacket:
      ClientInteractionStore.setSummary(summary)
      if consumePendingInteraction(girlfriendTypeId):
          new MainInteractionScreen(girlfriendTypeId, summary) → setScreen
```

### 7.2 操作支线

```
主界面:
├─ [查看委托] → new QuestViewScreen(summary.currentQuest()) → setScreen
│   ├─ [接受委托] → ServerboundAcceptQuestPacket → server → onClose
│   └─ [我再想想] → onClose
├─ [交付委托] → ServerboundDeliverQuestPacket → server → onClose
├─ [确立关系] → ServerboundConfirmIntimacyPacket → server → onClose
├─ [邀请同居] → ServerboundInviteHomePacket → server → onClose
├─ [跟随模式] → 暂存 Screen 字段, onClose 发 ServerboundSetFollowModePacket
└─ [赠送礼物] → new GiftScreen(girlfriendTypeId) → setScreen
    └─ 点击槽位 → ServerboundGiveGiftFromSlotPacket → onClose
```

### 7.3 数据一致性

界面初始化时从 `ClientInteractionStore` 打快照。界面打开期间若服务端状态变更（如他人完成委托），客户端缓存可能过时。主界面和委托界面关闭后重新右键打开即可获取最新数据。

## 8. 错误处理与边界情况

| 场景 | 处理方式 |
|---|---|
| 没有委托 | 查看委托按钮置灰不可点击（客户端过滤） |
| 委托未完成就尝试交付 | 按钮置灰不可点击（客户端过滤） |
| 好感度不足操作 | 确立关系/邀请同居按钮不显示（客户端过滤）；跟随模式点击无反应 |
| 送礼时槽位为空 | 客户端过滤，空槽位点击无反应 |
| 送礼被拒绝（REJECTED） | 服务端不扣物品，聊天栏提示 |
| 邀请同居无重生点或重生点非双人床 | 服务端 `BedValidator` 校验失败，聊天栏提示 |
| 交互界面打开时实体消失/死亡 | 服务端 `canReachEntity` 校验失败，静默忽略 |
| 每日送礼上限已满（>= 15） | `GiftService.applyGiftItem` 内部检测并拒绝，聊天栏提示 |
| 多人绑定冲突 | `BindingService.confirmIntimacy` 内部校验，失败聊天栏提示 |
| slotIndex 超出范围 | 服务端 handler 校验 `0 <= slotIndex < 36`，非法则静默返回 |

## 9. 主界面打开时序

### 9.1 `GirlfriendEntity.mobInteract()` 客户端侧

在现有的 `if (this.level().isClientSide())` 分支中，返回 `InteractionResult.SUCCESS` 之前增加一行：

```java
ClientInteractionStore.markPendingInteraction(this.getGirlfriendTypeId());
```

`getGirlfriendTypeId()` 为 `GirlfriendEntity` 抽象方法，各子类实现返回对应的 `Identifier`。

### 9.2 `ClientboundSyncInteractionDataPacket` 客户端 handler

在现有 handler（`GirlfriendsNetwork` 约第 51-53 行）中，`setSummary` 之后增加 pending 检查：

```java
ClientInteractionStore.setSummary(packet.summary());
if (ClientInteractionStore.consumePendingInteraction(packet.summary().girlfriendTypeId())) {
    Minecraft.getInstance().setScreen(new MainInteractionScreen(
        packet.summary().girlfriendTypeId(), packet.summary()
    ));
}
```

### 9.3 `ClientInteractionStore` 新增方法

```java
private static final Set<Identifier> PENDING_INTERACTIONS = ConcurrentHashMap.newKeySet();

public static void markPendingInteraction(Identifier girlfriendTypeId) {
    PENDING_INTERACTIONS.add(girlfriendTypeId);
}

public static boolean consumePendingInteraction(Identifier girlfriendTypeId) {
    return PENDING_INTERACTIONS.remove(girlfriendTypeId);
}
```

## 10. `InteractionSummary` 扩展

为支持交付委托按钮灰显判定，在 `QuestContentSummary` 中新增字段：

```java
// InteractionSummary.java — QuestContentSummary 新增
boolean questCompleted;  // 委托所有目标是否已完成（仅当 ownerPlayerUuid == player 时有意义）
```

`InteractionSummaryService.buildQuestContentSummary()` 在构建时通过 `QuestService` 计算并填充此字段。

## 11. 测试策略

| 测试范围 | 方式 |
|---|---|
| 新增 4 个 Serverbound 包的序列化/反序列化 | 单元测试：write → read 往返验证 |
| `ServerboundGiveGiftFromSlotPacket` handler 物品扣减 | 单元测试：验证 slotIndex 合法性、拒绝不扣物、接受扣 1 |
| `ServerboundInviteHomePacket` handler 重生点/床校验 | `HomeServiceTest` 中新增，注入模拟 `BedValidator` |
| Screen 按钮状态逻辑 | 构造不同 `InteractionSummary` 快照，验证按钮可见性/灰显/隐藏 |
| 跟随模式循环切换 | 验证 `NONE → FOLLOW → STAY → NONE` 循环，非 `canFollow` 时不变 |
| `QuestContentSummary.questCompleted` 序列化 | 往返测试 |

客户端 Screen 渲染测试依赖 Minecraft 环境，通过 `runClient` 实际调试验证。

## 12. 不在范围内（后续 Story）

- 聊天栏角色回应文本
- 委托奖励实际发放
- GUI 美化（角色立绘、背景纹理等）
- 礼物偏好揭示动画
- 渔溪/梅疏/晚萤/幽若实体渲染器注册
