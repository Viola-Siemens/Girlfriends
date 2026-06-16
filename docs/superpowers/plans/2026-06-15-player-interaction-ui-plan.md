# 玩家角色交互界面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现玩家右键 GirlfriendEntity 打开的主交互界面、委托查看界面和送礼界面，含 4 个新增网络包、pending 标记机制和 InteractionSummary 扩展。

**Architecture:** 纯客户端 Screen 体系（无 AbstractContainerMenu），通过 pending 标记机制在主界面打开时获取服务端同步的 InteractionSummary。送礼界面直接渲染本地背包发送槽位索引包，服务端 handler 做权威校验和物品扣减。

**Tech Stack:** Java 25, NeoForge 26.1.2, Minecraft 26.1.2, JUnit Jupiter 5.13.4

---

### Task 1: 扩展 InteractionSummary 添加 questCompleted 和 followMode 字段

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummary.java:27-67`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/NetworkCodecs.java:30-62,114-139`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummaryService.java:42-68,78-86,150-175`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/quest/QuestService.java:247` (resolveObjectives 改为 public)

- [ ] **Step 1: 在 InteractionSummary 中新增 followMode 字段，在 QuestContentSummary 中新增 questCompleted 字段**

在 `InteractionSummary.java` 的顶层 record 中新增 `FollowMode followMode` 参数：

```java
import com.hexagram2021.girlfriends.common.blessing.FollowMode;

public record InteractionSummary(
        Identifier girlfriendTypeId,
        AffectionStage stage,
        double stageProgress,
        boolean canGiveGift,
        boolean canAcceptQuest,
        boolean canFollow,
        boolean canInviteHome,
        FollowMode followMode,
        List<KnownGiftPreferenceSummary> knownGiftPreferences,
        @Nullable QuestContentSummary currentQuest,
        boolean needsIntimacyConfirmation
) {
```

在 `QuestContentSummary` record 中新增 `boolean questCompleted` 参数：

```java
public record QuestContentSummary(
        Identifier questId,
        QuestType questType,
        QuestState questState,
        String titleKey,
        String descriptionKey,
        List<String> objectiveSummaryKeys,
        boolean questCompleted
) {
}
```

- [ ] **Step 2: 更新 NetworkCodecs 序列化**

在 `NetworkCodecs.java` 的 `writeInteractionSummary` 中，`canInviteHome` 之后新增：

```java
buffer.writeEnum(summary.followMode());
```

在 `readInteractionSummary` 中对应位置新增：

```java
buffer.readEnum(FollowMode.class),
```

更新后的 `readInteractionSummary`：

```java
public static InteractionSummary readInteractionSummary(RegistryFriendlyByteBuf buffer) {
    return new InteractionSummary(
            buffer.readIdentifier(),
            buffer.readEnum(AffectionStage.class),
            buffer.readDouble(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readEnum(FollowMode.class),
            readKnownGiftPreferences(buffer),
            readQuestContentSummary(buffer),
            buffer.readBoolean()
    );
}
```

新增 import：`import com.hexagram2021.girlfriends.common.blessing.FollowMode;`

在 `writeQuestContentSummary` 末尾新增：

```java
buffer.writeBoolean(summary.questCompleted());
```

在 `readQuestContentSummary` 末尾新增读取：

```java
return new QuestContentSummary(
        buffer.readIdentifier(),
        buffer.readEnum(QuestType.class),
        buffer.readEnum(QuestState.class),
        buffer.readUtf(),
        buffer.readUtf(),
        readStrings(buffer),
        buffer.readBoolean()
);
```

- [ ] **Step 3: 更新 InteractionSummaryService 注入 QuestService 并计算 questCompleted**

在 `InteractionSummaryService.java` 中新增第三个构造参数 `QuestService questService`：

```java
private final GirlfriendsWorldData worldData;
private final RelationshipService relationshipService;
private final QuestService questService;

public InteractionSummaryService(
        GirlfriendsWorldData worldData,
        RelationshipService relationshipService,
        QuestService questService
) {
    this.worldData = worldData;
    this.relationshipService = relationshipService;
    this.questService = questService;
}
```

将 `buildQuestContentSummary` 改为非静态方法，接收 `QuestInstance` 并计算 `questCompleted`：

```java
@Nullable
private QuestContentSummary buildQuestContentSummary(@Nullable QuestInstance quest) {
    if(quest == null) {
        return null;
    }
    Identifier questId = parseQuestIdentifierOrNull(quest.getQuestId());
    if(questId == null) {
        return null;
    }
    List<String> objectiveSummaryKeys = Lists.newArrayList();
    for(String key : quest.getProgress().keySet()) {
        if(key.startsWith("objective_")) {
            objectiveSummaryKeys.add(QUEST_KEY_PREFIX + questId.getPath() + "." + key);
        }
    }
    objectiveSummaryKeys.sort(String::compareTo);

    boolean questCompleted = false;
    QuestObjectiveGroup objectives = this.questService.resolveObjectives(quest);
    if(objectives != null) {
        questCompleted = objectives.isCompleted(quest);
    }

    return new QuestContentSummary(
            questId,
            quest.getQuestType(),
            quest.getState(),
            QUEST_KEY_PREFIX + questId.getPath() + ".title",
            QUEST_KEY_PREFIX + questId.getPath() + ".description",
            List.copyOf(objectiveSummaryKeys),
            questCompleted
    );
}
```

修改 `build()` 方法中的调用，加入 `followMode`：

```java
public InteractionSummary build(UUID playerUuid, Identifier girlfriendTypeId) {
    PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
    AffectionStage stage = this.relationshipService.getEffectiveStage(relation);
    CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
    FollowMode followMode = state != null ? state.getFollowMode() : FollowMode.NONE;
    return new InteractionSummary(
            girlfriendTypeId,
            stage,
            computeStageProgress(relation.getAffection(), stage),
            canGiveGift(state),
            canAcceptQuest(state),
            canFollow(relation, state),
            canInviteHome(relation, stage),
            followMode,
            buildKnownGiftPreferences(relation),
            buildQuestContentSummary(state != null ? state.getCurrentQuest() : null),
            needsIntimacyConfirmation(relation)
    );
}
```

新增 import：`import com.hexagram2021.girlfriends.common.blessing.FollowMode;`

修改 `buildQuestIcon()` 方法中的调用：

```java
@Nullable
public QuestIconSummary buildQuestIcon(Identifier girlfriendTypeId) {
    CharacterWorldState state = this.worldData.getExistingCharacterState(girlfriendTypeId);
    QuestContentSummary quest = buildQuestContentSummary(state != null ? state.getCurrentQuest() : null);
    if(quest == null) {
        return null;
    }
    return new QuestIconSummary(girlfriendTypeId, quest.questId(), quest.questType(), quest.questState(), quest.titleKey(), quest.objectiveSummaryKeys());
}
```

新增 `resolveObjectives` 公开方法到 `QuestService`（如果还没有 public 访问）：

检查 `QuestService.resolveObjectives` 当前是 `private`，需要改为 `public`：

```java
@Nullable
public QuestObjectiveGroup resolveObjectives(QuestInstance questInstance) {
    if(questInstance.getQuestType() == QuestType.FIXED) {
        QuestDefinition definition = this.fixedQuestDefinitionGetter.apply(questInstance.getQuestId());
        if(definition != null) {
            return definition.objectives();
        }
        return this.isFallbackFixedQuest(questInstance) ? QuestObjectiveGroup.empty() : null;
    }
    // random quest...
    Identifier characterId = questInstance.getCharacterId();
    if(characterId == null) {
        return QuestObjectiveGroup.empty();
    }
    QuestDefinition definition = this.randomQuestDefinitionGetter.apply(characterId);
    if(definition != null) {
        return definition.objectives();
    }
    return QuestObjectiveGroup.empty();
}
```

> 注意：原 `resolveObjectives` 方法约在 QuestService.java 第 247 行，将其 `private` 改为 `public`。

- [ ] **Step 4: 更新调用方 GirlfriendEntity.mobInteract()**

在 `GirlfriendEntity.java` 第 207-212 行，创建 `InteractionSummaryService` 时传入 `QuestService`：

```java
RelationshipService relationshipService = new RelationshipService(data);
QuestService questService = new QuestService(data, relationshipService);
InteractionSummaryService summaryService = new InteractionSummaryService(data, relationshipService, questService);
```

更新 import 添加 `QuestService`。

- [ ] **Step 5: 更新 GirlfriendsEntityEvents 中的调用**

在 `GirlfriendEntityEvents.java` 中搜索 `InteractionSummaryService` 或 `buildQuestIcon` 的调用处（如果存在），同步更新构造函数调用。

- [ ] **Step 6: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 7: 运行现有测试确认无回归**

```bash
./gradlew test
```

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummary.java \
        src/main/java/com/hexagram2021/girlfriends/common/network/NetworkCodecs.java \
        src/main/java/com/hexagram2021/girlfriends/common/network/InteractionSummaryService.java \
        src/main/java/com/hexagram2021/girlfriends/common/quest/QuestService.java \
        src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendEntity.java
git commit -m "feat(REQ-8): QuestContentSummary 新增 questCompleted 字段"
```

---

### Task 2: 在 ClientInteractionStore 中添加 pending 标记机制

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/ClientInteractionStore.java`

- [ ] **Step 1: 添加 pending 字段和方法**

在 `ClientInteractionStore.java` 中新增：

```java
private static final Set<Identifier> PENDING_INTERACTIONS = ConcurrentHashMap.newKeySet();

/**
 * 标记待处理的交互，用于 mobInteract 客户端侧触发界面打开喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 */
public static void markPendingInteraction(Identifier girlfriendTypeId) {
    PENDING_INTERACTIONS.add(girlfriendTypeId);
}

/**
 * 原子获取并清除待处理交互标记喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @return 是否存在待处理标记喵~
 */
public static boolean consumePendingInteraction(Identifier girlfriendTypeId) {
    return PENDING_INTERACTIONS.remove(girlfriendTypeId);
}
```

新增 import：`import java.util.Set;`

- [ ] **Step 2: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/ClientInteractionStore.java
git commit -m "feat(REQ-8): ClientInteractionStore 新增 pending 交互标记机制"
```

---

### Task 3: 更新 GirlfriendEntity.mobInteract() 客户端侧注册 pending 标记

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendEntity.java:196-198`

- [ ] **Step 1: 添加 pending 标记调用**

在 `GirlfriendEntity.java` 的 `mobInteract` 客户端分支中，返回 `InteractionResult.SUCCESS` 之前新增：

```java
@Override
public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (this.level().isClientSide()) {
        ClientInteractionStore.markPendingInteraction(this.getGirlfriendTypeId());
        return InteractionResult.SUCCESS;
    }
    // ... 服务端逻辑不变
}
```

新增 import：`import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;`

- [ ] **Step 2: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/entity/GirlfriendEntity.java
git commit -m "feat(REQ-8): mobInteract 客户端侧设置 pending 交互标记"
```

---

### Task 4: 更新 ClientboundSyncInteractionDataPacket handler 打开主界面

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java:51-53`
- Create: `src/main/java/com/hexagram2021/girlfriends/client/screen/MainInteractionScreen.java`（仅声明类骨架，使编译通过）

- [ ] **Step 1: 创建 MainInteractionScreen 骨架**

创建 `MainInteractionScreen.java`，暂时仅声明构造方法签名和必要导入（内容在 Task 7 中补充）：

```java
package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 玩家与角色交互主界面喵~
 *
 * @author liudongyu
 */
public class MainInteractionScreen extends Screen {
    private final Identifier girlfriendTypeId;
    private final InteractionSummary summary;

    public MainInteractionScreen(Identifier girlfriendTypeId, InteractionSummary summary) {
        super(Component.translatable("screen.girlfriends.interaction"));
        this.girlfriendTypeId = girlfriendTypeId;
        this.summary = summary;
    }
}
```

- [ ] **Step 2: 更新 GirlfriendsNetwork 客户端 handler**

在 `GirlfriendsNetwork.java` 第 51-53 行，扩展 `handleSyncInteractionData`：

```java
private static void handleSyncInteractionData(ClientboundSyncInteractionDataPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> {
        ClientInteractionStore.setSummary(packet.summary());
        if (ClientInteractionStore.consumePendingInteraction(packet.summary().girlfriendTypeId())) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hexagram2021.girlfriends.client.screen.MainInteractionScreen(
                    packet.summary().girlfriendTypeId(),
                    packet.summary()
                )
            );
        }
    });
}
```

新增 import：
```java
import com.hexagram2021.girlfriends.client.screen.MainInteractionScreen;
```

- [ ] **Step 3: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java \
        src/main/java/com/hexagram2021/girlfriends/client/screen/MainInteractionScreen.java
git commit -m "feat(REQ-8): 客户端收到 InteractionSummary 后打开主交互界面"
```

---

### Task 5: 创建 4 个新增 Serverbound 网络包

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundDeliverQuestPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundConfirmIntimacyPacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundInviteHomePacket.java`
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/ServerboundGiveGiftFromSlotPacket.java`

- [ ] **Step 1: 创建 ServerboundDeliverQuestPacket**

遵循现有 `ServerboundAcceptQuestPacket` 模式：

```java
package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收交付委托请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundDeliverQuestPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
    public static final Type<ServerboundDeliverQuestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "deliver_quest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeliverQuestPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundDeliverQuestPacket::write,
            ServerboundDeliverQuestPacket::read
    );

    private static ServerboundDeliverQuestPacket read(RegistryFriendlyByteBuf buffer) {
        return new ServerboundDeliverQuestPacket(buffer.readIdentifier());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(this.girlfriendTypeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 2: 创建 ServerboundConfirmIntimacyPacket**

```java
package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收确立关系请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundConfirmIntimacyPacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
    public static final Type<ServerboundConfirmIntimacyPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "confirm_intimacy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundConfirmIntimacyPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundConfirmIntimacyPacket::write,
            ServerboundConfirmIntimacyPacket::read
    );

    private static ServerboundConfirmIntimacyPacket read(RegistryFriendlyByteBuf buffer) {
        return new ServerboundConfirmIntimacyPacket(buffer.readIdentifier());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(this.girlfriendTypeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 3: 创建 ServerboundInviteHomePacket**

```java
package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收邀请同居请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @author liudongyu
 */
public record ServerboundInviteHomePacket(Identifier girlfriendTypeId) implements CustomPacketPayload {
    public static final Type<ServerboundInviteHomePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "invite_home"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundInviteHomePacket> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundInviteHomePacket::write,
            ServerboundInviteHomePacket::read
    );

    private static ServerboundInviteHomePacket read(RegistryFriendlyByteBuf buffer) {
        return new ServerboundInviteHomePacket(buffer.readIdentifier());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(this.girlfriendTypeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 4: 创建 ServerboundGiveGiftFromSlotPacket**

```java
package com.hexagram2021.girlfriends.common.network.serverbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端接收从背包槽位送礼请求的数据包喵~
 *
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param slotIndex 物品栏槽位索引 (0~35) 喵~
 * @author liudongyu
 */
public record ServerboundGiveGiftFromSlotPacket(Identifier girlfriendTypeId, int slotIndex) implements CustomPacketPayload {
    public static final Type<ServerboundGiveGiftFromSlotPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "give_gift_from_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGiveGiftFromSlotPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundGiveGiftFromSlotPacket::write,
            ServerboundGiveGiftFromSlotPacket::read
    );

    private static ServerboundGiveGiftFromSlotPacket read(RegistryFriendlyByteBuf buffer) {
        return new ServerboundGiveGiftFromSlotPacket(buffer.readIdentifier(), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(this.girlfriendTypeId);
        buffer.writeVarInt(this.slotIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/serverbound/
git commit -m "feat(REQ-8): 新增 4 个 Serverbound 网络包 (交付委托/确立关系/邀请同居/背包送礼)"
```

---

### Task 6: 在 GirlfriendsNetwork 中注册新增包并实现 handler

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java`

- [ ] **Step 1: 添加包导入**

在 `GirlfriendsNetwork.java` 顶部添加：

```java
import com.hexagram2021.girlfriends.common.binding.BindingService;
import com.hexagram2021.girlfriends.common.home.BedValidator;
import com.hexagram2021.girlfriends.common.home.HomeAnchor;
import com.hexagram2021.girlfriends.common.home.HomeService;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundDeliverQuestPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundConfirmIntimacyPacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundInviteHomePacket;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundGiveGiftFromSlotPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
```

- [ ] **Step 2: 注册 4 个新包**

在 `register()` 方法第 48 行后新增：

```java
registrar.playToServer(ServerboundDeliverQuestPacket.TYPE, ServerboundDeliverQuestPacket.STREAM_CODEC, GirlfriendsNetwork::handleDeliverQuest);
registrar.playToServer(ServerboundConfirmIntimacyPacket.TYPE, ServerboundConfirmIntimacyPacket.STREAM_CODEC, GirlfriendsNetwork::handleConfirmIntimacy);
registrar.playToServer(ServerboundInviteHomePacket.TYPE, ServerboundInviteHomePacket.STREAM_CODEC, GirlfriendsNetwork::handleInviteHome);
registrar.playToServer(ServerboundGiveGiftFromSlotPacket.TYPE, ServerboundGiveGiftFromSlotPacket.STREAM_CODEC, GirlfriendsNetwork::handleGiveGiftFromSlot);
```

- [ ] **Step 3: 实现 handleDeliverQuest handler**

```java
private static void handleDeliverQuest(ServerboundDeliverQuestPacket packet, IPayloadContext context) {
    if(context.player() instanceof ServerPlayer player) {
        GirlfriendsWorldData data = getWorldData(player);
        if(canReachEntity(player, data, packet.girlfriendTypeId())) {
            QuestService questService = new QuestService(data, new RelationshipService(data));
            if(questService.completeCurrentQuest(player.getUUID(), packet.girlfriendTypeId())) {
                Identifier girlfriendTypeId = packet.girlfriendTypeId();
                InteractionSummaryService summaryService = new InteractionSummaryService(
                        data, new RelationshipService(data), questService
                );
                QuestIconSummary questIcon = summaryService.buildQuestIcon(girlfriendTypeId);
                if(questIcon != null) {
                    player.connection.send(new ClientboundQuestIconPacket(questIcon));
                }
                player.sendSystemMessage(Component.translatable("message.girlfriends.quest_completed"));
            }
        }
    }
}
```

- [ ] **Step 4: 实现 handleConfirmIntimacy handler**

```java
private static void handleConfirmIntimacy(ServerboundConfirmIntimacyPacket packet, IPayloadContext context) {
    if(context.player() instanceof ServerPlayer player) {
        GirlfriendsWorldData data = getWorldData(player);
        if(canReachEntity(player, data, packet.girlfriendTypeId())) {
            RelationshipService relationshipService = new RelationshipService(data);
            new BindingService(data, relationshipService)
                    .confirmIntimacy(player.getUUID(), packet.girlfriendTypeId());
            player.sendSystemMessage(Component.translatable("message.girlfriends.intimacy_confirmed"));
        }
    }
}
```

- [ ] **Step 5: 实现 handleInviteHome handler**

```java
private static void handleInviteHome(ServerboundInviteHomePacket packet, IPayloadContext context) {
    if(context.player() instanceof ServerPlayer player) {
        GirlfriendsWorldData data = getWorldData(player);
        if(canReachEntity(player, data, packet.girlfriendTypeId())) {
            BlockPos respawnPos = player.getRespawnPosition();
            if(respawnPos == null) {
                player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_no_respawn"));
                return;
            }
            Identifier dimension = player.getRespawnDimension().location();
            BedValidator bedValidator = (anchor) -> {
                // 校验重生点是否为双人床，此处使用简单的方块状态检查
                // 后续可替换为更完善的 BedValidator 实现
                return player.level().getBlockState(
                        new BlockPos(anchor.x(), anchor.y(), anchor.z())
                ).is(net.minecraft.world.level.block.BedBlock.class);
            };
            HomeService homeService = new HomeService(data, new RelationshipService(data), bedValidator);
            if(homeService.inviteHome(player.getUUID(), packet.girlfriendTypeId(), dimension, respawnPos.getX(), respawnPos.getY(), respawnPos.getZ())) {
                player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_success"));
            } else {
                player.sendSystemMessage(Component.translatable("message.girlfriends.invite_home_failed"));
            }
        }
    }
}
```

- [ ] **Step 6: 实现 handleGiveGiftFromSlot handler**

```java
private static void handleGiveGiftFromSlot(ServerboundGiveGiftFromSlotPacket packet, IPayloadContext context) {
    if(context.player() instanceof ServerPlayer player) {
        GirlfriendsWorldData data = getWorldData(player);
        if(canReachEntity(player, data, packet.girlfriendTypeId())) {
            int slotIndex = packet.slotIndex();
            if(slotIndex < 0 || slotIndex >= 36) {
                return;
            }
            ItemStack itemStack = player.getInventory().getItem(slotIndex);
            if(itemStack.isEmpty()) {
                return;
            }
            RelationshipService relationshipService = new RelationshipService(data);
            GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
                    (playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
            GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), itemStack);
            if(!result.rejected()) {
                player.getInventory().removeItem(slotIndex, 1);
            }
            player.sendSystemMessage(Component.translatable(result.messageKey()));
        }
    }
}
```

新增 import：
```java
import com.hexagram2021.girlfriends.common.gift.GiftResult;
```

- [ ] **Step 7: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java
git commit -m "feat(REQ-8): 注册 4 个新增 Serverbound 包并实现 handler"
```

---

### Task 7: 创建 MainInteractionScreen

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/screen/MainInteractionScreen.java`（替换骨架）
- Create: `src/main/java/com/hexagram2021/girlfriends/client/screen/GirlfriendsScreens.java`

- [ ] **Step 1: 创建 GirlfriendsScreens 静态入口**

创建 `GirlfriendsScreens.java`：

```java
package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/**
 * Girlfriends Screen 注册与管理入口喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsScreens {
    /**
     * 从缓存获取 InteractionSummary 并打开主交互界面喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @return 是否成功打开喵~
     */
    public static boolean openMainInteractionScreen(Identifier girlfriendTypeId) {
        InteractionSummary summary = ClientInteractionStore.getSummary(girlfriendTypeId);
        if(summary == null) {
            return false;
        }
        Minecraft.getInstance().setScreen(new MainInteractionScreen(girlfriendTypeId, summary));
        return true;
    }

    private GirlfriendsScreens() {
    }
}
```

- [ ] **Step 2: 实现 MainInteractionScreen 完整 UI**

替换 `MainInteractionScreen.java` 骨架为完整实现：

```java
package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.*;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * 玩家与角色交互主界面喵~
 *
 * @author liudongyu
 */
public class MainInteractionScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CENTER_X_OFFSET = 100;

    private final Identifier girlfriendTypeId;
    private final InteractionSummary summary;
    private FollowMode selectedFollowMode;

    public MainInteractionScreen(Identifier girlfriendTypeId, InteractionSummary summary) {
        super(Component.translatable("screen.girlfriends.interaction"));
        this.girlfriendTypeId = girlfriendTypeId;
        this.summary = summary;
        // 初始化为当前角色跟随模式，HOME 模式则退化为 NONE 喵~
        this.selectedFollowMode = summary.followMode() == FollowMode.HOME ? FollowMode.NONE : summary.followMode();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2 - CENTER_X_OFFSET;
        int y = 60;

        // 查看委托 / 交付委托按钮（同位置）喵~
        QuestContentSummary quest = this.summary.currentQuest();
        UUID playerUuid = this.minecraft.player.getUUID();
        if(quest != null && !java.util.Objects.equals(quest.questState().name(), "AVAILABLE")) {
            // 已接取 → 显示交付委托喵~
            boolean canDeliver = quest.questCompleted();
            this.addRenderableWidget(
                    Button.builder(Component.translatable("button.girlfriends.deliver_quest"), btn -> {
                                PacketDistributor.sendToServer(new ServerboundDeliverQuestPacket(this.girlfriendTypeId));
                                this.onClose();
                            })
                            .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            ).active = canDeliver;
        } else {
            // 未接取 → 显示查看委托喵~
            boolean hasQuest = quest != null;
            Button viewQuestBtn = this.addRenderableWidget(
                    Button.builder(Component.translatable("button.girlfriends.view_quest"), btn -> {
                                if(quest != null) {
                                    this.minecraft.setScreen(new QuestViewScreen(this.girlfriendTypeId, quest));
                                }
                            })
                            .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
            viewQuestBtn.active = hasQuest;
        }
        y += 25;

        // 确立关系 / 邀请同居按钮（同位置，条件性显示）喵~
        if(this.summary.needsIntimacyConfirmation()) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("button.girlfriends.confirm_intimacy"), btn -> {
                                PacketDistributor.sendToServer(new ServerboundConfirmIntimacyPacket(this.girlfriendTypeId));
                                this.onClose();
                            })
                            .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
            y += 25;
        } else if(this.summary.canInviteHome()) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("button.girlfriends.invite_home"), btn -> {
                                PacketDistributor.sendToServer(new ServerboundInviteHomePacket(this.girlfriendTypeId));
                                this.onClose();
                            })
                            .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
            y += 25;
        }

        // 跟随模式切换按钮喵~
        Component followText = Component.translatable("button.girlfriends.follow_mode", Component.translatable(this.selectedFollowMode.getSerializedName()));
        Button followBtn = this.addRenderableWidget(
                Button.builder(followText, btn -> {
                            if(this.summary.canFollow()) {
                                this.selectedFollowMode = switch(this.selectedFollowMode) {
                                    case NONE -> FollowMode.FOLLOW;
                                    case FOLLOW -> FollowMode.STAY;
                                    case STAY -> FollowMode.NONE;
                                    default -> FollowMode.NONE;
                                };
                                btn.setMessage(Component.translatable("button.girlfriends.follow_mode",
                                        Component.translatable(this.selectedFollowMode.getSerializedName())));
                            }
                        })
                        .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        y += 25;

        // 赠送礼物按钮喵~
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.girlfriends.give_gift"), btn -> {
                            this.minecraft.setScreen(new GiftScreen(this.girlfriendTypeId));
                        })
                        .bounds(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        y += 35;

        // 关闭按钮喵~
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.girlfriends.close"), btn -> this.onClose())
                        .bounds(centerX, y + 5, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        // 标题 - 角色名喵~
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("girlfriend." + this.girlfriendTypeId.getPath() + ".name"),
                centerX, 20, 0xFFFFFF);
        // 好感阶段 + 进度喵~
        AffectionStage stage = this.summary.stage();
        String progressText = String.format("%s %.0f/%.0f",
                Component.translatable("affection." + stage.name().toLowerCase()).getString(),
                stage.getMinAffection() + this.summary.stageProgress() * (stage.getMaxAffection() - stage.getMinAffection()),
                stage.getMaxAffection()
        );
        guiGraphics.drawCenteredString(this.font, progressText, centerX, 40, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if(this.summary.canFollow()) {
            PacketDistributor.sendToServer(new ServerboundSetFollowModePacket(this.girlfriendTypeId, this.selectedFollowMode));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

修复编译错误（如有），特别是检查 `FollowMode.getSerializedName()` 和 `AffectionStage.getSerializedName()` 方法是否存在——两者实现 `StringRepresentable`，方法为 `getSerializedName()`。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/client/screen/MainInteractionScreen.java \
        src/main/java/com/hexagram2021/girlfriends/client/screen/GirlfriendsScreens.java
git commit -m "feat(REQ-8): 实现主交互界面 MainInteractionScreen"
```

---

### Task 8: 创建 QuestViewScreen

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/client/screen/QuestViewScreen.java`

- [ ] **Step 1: 实现 QuestViewScreen**

创建 `QuestViewScreen.java`：

```java
package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundAcceptQuestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 委托查看界面喵~
 *
 * @author liudongyu
 */
public class QuestViewScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private final Identifier girlfriendTypeId;
    private final QuestContentSummary quest;

    public QuestViewScreen(Identifier girlfriendTypeId, QuestContentSummary quest) {
        super(Component.translatable(quest.titleKey()));
        this.girlfriendTypeId = girlfriendTypeId;
        this.quest = quest;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        // 接受委托按钮喵~
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.girlfriends.accept_quest"), btn -> {
                            PacketDistributor.sendToServer(new ServerboundAcceptQuestPacket(this.girlfriendTypeId));
                            // 关闭当前界面和主界面喵~
                            this.minecraft.setScreen(null);
                        })
                        .bounds(centerX - BUTTON_WIDTH - 10, this.height - 50, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        // 我再想想按钮喵~
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.girlfriends.think_again"), btn -> this.onClose())
                        .bounds(centerX + 10, this.height - 50, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int y = 20;

        // 标题喵~
        guiGraphics.drawCenteredString(this.font, Component.translatable(this.quest.titleKey()), centerX, y, 0xFFFFFF);
        y += 15;

        // 委托类型副标题喵~
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("quest.girlfriends.type." + this.quest.questType().name().toLowerCase()),
                centerX, y, 0xAAAAAA);
        y += 20;

        // 委托描述喵~
        Component description = Component.translatable(this.quest.descriptionKey());
        for(Component line : this.font.split(description, this.width - 40)) {
            guiGraphics.drawString(this.font, line, 20, y, 0xCCCCCC);
            y += this.font.lineHeight + 2;
        }
        y += 10;

        // 目标列表喵~
        if(!this.quest.objectiveSummaryKeys().isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("label.girlfriends.objectives"), 20, y, 0xFFFFFF);
            y += this.font.lineHeight + 2;
            for(String key : this.quest.objectiveSummaryKeys()) {
                guiGraphics.drawString(this.font, "  " + Component.translatable(key).getString(), 20, y, 0xCCCCCC);
                y += this.font.lineHeight + 2;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

检查 `QuestType.name().toLowerCase()` 可用——所有 Java enum 均有 `name()` 方法。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/client/screen/QuestViewScreen.java
git commit -m "feat(REQ-8): 实现委托查看界面 QuestViewScreen"
```

---

### Task 9: 创建 GiftScreen

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/client/screen/GiftScreen.java`

- [ ] **Step 1: 实现 GiftScreen**

创建 `GiftScreen.java`：

```java
package com.hexagram2021.girlfriends.client.screen;

import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.network.ClientInteractionStore;
import com.hexagram2021.girlfriends.common.network.InteractionSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.KnownGiftPreferenceSummary;
import com.hexagram2021.girlfriends.common.network.serverbound.ServerboundGiveGiftFromSlotPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 送礼界面喵~
 * 渲染 9 列 x 4 行玩家背包网格，高亮已知偏好物品喵~
 *
 * @author liudongyu
 */
public class GiftScreen extends Screen {
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int LEFT_OFFSET = 8;
    private static final int TOP_OFFSET = 18;

    private final Identifier girlfriendTypeId;
    private final Map<Integer, GiftPreferenceLevel> slotPreferenceMap = new HashMap<>();
    private boolean hasData;

    public GiftScreen(Identifier girlfriendTypeId) {
        super(Component.translatable("screen.girlfriends.gift"));
        this.girlfriendTypeId = girlfriendTypeId;
    }

    @Override
    protected void init() {
        super.init();
        InteractionSummary summary = ClientInteractionStore.getSummary(this.girlfriendTypeId);
        this.hasData = summary != null;
        if(summary != null) {
            List<KnownGiftPreferenceSummary> preferences = summary.knownGiftPreferences();
            Inventory inventory = this.minecraft.player.getInventory();
            for(int i = 0; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                if(!stack.isEmpty()) {
                    GiftPreferenceLevel level = resolvePreferenceLevel(preferences, stack);
                    if(level != null) {
                        this.slotPreferenceMap.put(i, level);
                    }
                }
            }
        }
    }

    @Nullable
    private static GiftPreferenceLevel resolvePreferenceLevel(List<KnownGiftPreferenceSummary> preferences, ItemStack stack) {
        Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        for(KnownGiftPreferenceSummary pref : preferences) {
            if(pref.tag()) {
                // 标签偏好匹配：检查物品是否持有该标签喵~
                if(stack.is(net.minecraft.tags.ItemTags.create(pref.itemOrTagId()))) {
                    return pref.level();
                }
            } else if(pref.itemOrTagId().equals(itemId)) {
                return pref.level();
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int leftPos = (this.width - COLS * SLOT_SIZE) / 2;
        int topPos = (this.height - ROWS * SLOT_SIZE) / 2;

        Inventory inventory = this.minecraft.player.getInventory();
        for(int row = 0; row < ROWS; row++) {
            for(int col = 0; col < COLS; col++) {
                int slotIndex = row * COLS + col;
                int x = leftPos + col * SLOT_SIZE;
                int y = topPos + row * SLOT_SIZE;

                ItemStack stack = inventory.getItem(slotIndex);
                // 绘制槽位背景喵~
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x8B8B8B8B);
                if(!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, x + 1, y + 1);
                    guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
                }

                // 偏好高亮喵~
                GiftPreferenceLevel level = this.slotPreferenceMap.get(slotIndex);
                if(level != null) {
                    int color = switch(level) {
                        case FAVORITE -> 0xFFFFD700;   // 金色喵~
                        case LIKED -> 0xFF00FF00;      // 绿色喵~
                        case ACCEPTED -> 0xFF4169E1;    // 蓝色喵~
                        case DISLIKED -> 0xFFFF4444;    // 红色喵~
                        default -> 0xFFFFFFFF;
                    };
                    // 绘制边框喵~
                    guiGraphics.fill(x, y, x + SLOT_SIZE, y + 1, color);
                    guiGraphics.fill(x, y, x + 1, y + SLOT_SIZE, color);
                    guiGraphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
                    guiGraphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
                }
            }
        }

        // 标题喵~
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, TOP_OFFSET - 10, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 0) {
            int leftPos = (this.width - COLS * SLOT_SIZE) / 2;
            int topPos = (this.height - ROWS * SLOT_SIZE) / 2;
            int col = (int)((mouseX - leftPos) / SLOT_SIZE);
            int row = (int)((mouseY - topPos) / SLOT_SIZE);
            if(col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int slotIndex = row * COLS + col;
                Inventory inventory = this.minecraft.player.getInventory();
                if(!inventory.getItem(slotIndex).isEmpty()) {
                    PacketDistributor.sendToServer(new ServerboundGiveGiftFromSlotPacket(this.girlfriendTypeId, slotIndex));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/hexagram2021/girlfriends/client/screen/GiftScreen.java
git commit -m "feat(REQ-8): 实现送礼界面 GiftScreen"
```

---

### Task 10: 创建 ClientInteractionStoreTest 单元测试

**Files:**
- Create: `src/test/java/com/hexagram2021/girlfriends/common/network/ClientInteractionStoreTest.java`

- [ ] **Step 1: 编写 pending 标记测试**

创建 `ClientInteractionStoreTest.java`：

```java
package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClientInteractionStore 单元测试喵~
 *
 * @author liudongyu
 */
public class ClientInteractionStoreTest {
    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

    @Test
    void markPendingAndConsumeReturnsTrue() {
        ClientInteractionStore.markPendingInteraction(TEST_ID);
        assertTrue(ClientInteractionStore.consumePendingInteraction(TEST_ID));
    }

    @Test
    void consumeWithoutMarkReturnsFalse() {
        assertFalse(ClientInteractionStore.consumePendingInteraction(TEST_ID));
    }

    @Test
    void consumeIsIdempotent() {
        ClientInteractionStore.markPendingInteraction(TEST_ID);
        assertTrue(ClientInteractionStore.consumePendingInteraction(TEST_ID));
        assertFalse(ClientInteractionStore.consumePendingInteraction(TEST_ID));
    }

    @Test
    void markMultipleAndConsumeEach() {
        Identifier id1 = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");
        Identifier id2 = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "yuxi");
        ClientInteractionStore.markPendingInteraction(id1);
        ClientInteractionStore.markPendingInteraction(id2);
        assertTrue(ClientInteractionStore.consumePendingInteraction(id1));
        assertFalse(ClientInteractionStore.consumePendingInteraction(id1));
        assertTrue(ClientInteractionStore.consumePendingInteraction(id2));
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.network.ClientInteractionStoreTest"
```

Expected: 4 tests PASS.

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/hexagram2021/girlfriends/common/network/ClientInteractionStoreTest.java
git commit -m "test(REQ-8): ClientInteractionStore pending 标记机制测试"
```

---

### Task 11: 编写网络包往返序列化测试

**Files:**
- Create: `src/test/java/com/hexagram2021/girlfriends/common/network/ServerboundPacketSerializationTest.java`

- [ ] **Step 1: 创建测试类**

创建 `ServerboundPacketSerializationTest.java`：

```java
package com.hexagram2021.girlfriends.common.network;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import com.hexagram2021.girlfriends.common.network.serverbound.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新增 Serverbound 包序列化往返测试喵~
 *
 * @author liudongyu
 */
public class ServerboundPacketSerializationTest {
    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "momo");

    @Test
    void deliverQuestPacketRoundTrip() {
        ServerboundDeliverQuestPacket packet = new ServerboundDeliverQuestPacket(TEST_ID);
        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
        ServerboundDeliverQuestPacket.STREAM_CODEC.encode(registryBuf, packet);
        ServerboundDeliverQuestPacket decoded = ServerboundDeliverQuestPacket.STREAM_CODEC.decode(registryBuf);
        assertEquals(TEST_ID, decoded.girlfriendTypeId());
    }

    @Test
    void confirmIntimacyPacketRoundTrip() {
        ServerboundConfirmIntimacyPacket packet = new ServerboundConfirmIntimacyPacket(TEST_ID);
        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
        ServerboundConfirmIntimacyPacket.STREAM_CODEC.encode(registryBuf, packet);
        ServerboundConfirmIntimacyPacket decoded = ServerboundConfirmIntimacyPacket.STREAM_CODEC.decode(registryBuf);
        assertEquals(TEST_ID, decoded.girlfriendTypeId());
    }

    @Test
    void inviteHomePacketRoundTrip() {
        ServerboundInviteHomePacket packet = new ServerboundInviteHomePacket(TEST_ID);
        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
        ServerboundInviteHomePacket.STREAM_CODEC.encode(registryBuf, packet);
        ServerboundInviteHomePacket decoded = ServerboundInviteHomePacket.STREAM_CODEC.decode(registryBuf);
        assertEquals(TEST_ID, decoded.girlfriendTypeId());
    }

    @Test
    void giveGiftFromSlotPacketRoundTrip() {
        ServerboundGiveGiftFromSlotPacket packet = new ServerboundGiveGiftFromSlotPacket(TEST_ID, 15);
        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, null);
        ServerboundGiveGiftFromSlotPacket.STREAM_CODEC.encode(registryBuf, packet);
        ServerboundGiveGiftFromSlotPacket decoded = ServerboundGiveGiftFromSlotPacket.STREAM_CODEC.decode(registryBuf);
        assertEquals(TEST_ID, decoded.girlfriendTypeId());
        assertEquals(15, decoded.slotIndex());
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.network.ServerboundPacketSerializationTest"
```

Expected: 4 tests PASS.

注意：`RegistryFriendlyByteBuf` 构造可能需要 Minecraft 环境。如果测试无法在纯 JUnit 环境下运行（因为 `RegistryFriendlyByteBuf` 需要注册表上下文），则调整测试策略——使用 `net.minecraft.network.FriendlyByteBuf` 替代，或使用 NeoForge 测试框架的 `@ExtendWith` 注解。

如果 `RegistryFriendlyByteBuf(ByteBuf, null)` 不工作，将测试类标记为使用 NeoForge 测试扩展（参考现有测试模式）。

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/hexagram2021/girlfriends/common/network/ServerboundPacketSerializationTest.java
git commit -m "test(REQ-8): 新增 Serverbound 包序列化往返测试"
```

---

### Task 12: 运行全部测试并 runClient 验证

**Files:** 无（验证阶段）

- [ ] **Step 1: 运行全部单元测试**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test
```

确认无回归失败。

- [ ] **Step 2: 启动客户端调试**

```bash
./gradlew runClient
```

在游戏中创造一个沫沫实体并右键交互，验证：
1. 主界面正常打开，显示好感阶段和进度
2. 查看委托/交付委托按钮根据状态正确显示
3. 确立关系按钮在好感度 >= 700 且未确认时出现
4. 跟随模式按钮点击循环切换
5. 赠送礼物按钮打开送礼界面
6. 送礼界面显示背包物品和偏好高亮
7. 点击物品送出礼物并关闭界面

- [ ] **Step 3: 提交（如有调试修复）**

```bash
git add -A
git commit -m "fix(REQ-8): runClient 调试修复"
```

---

### 附录：需要新增的本地化键

以下本地化键需要在 `src/main/resources/assets/girlfriends/lang/zh_cn.json` 中定义：

```json
{
  "screen.girlfriends.interaction": "角色交互",
  "screen.girlfriends.gift": "赠送礼物",
  "screen.girlfriends.quest": "委托详情",
  "button.girlfriends.view_quest": "查看委托",
  "button.girlfriends.deliver_quest": "交付委托",
  "button.girlfriends.confirm_intimacy": "确立关系",
  "button.girlfriends.invite_home": "邀请同居",
  "button.girlfriends.follow_mode": "跟随模式: %s",
  "button.girlfriends.give_gift": "赠送礼物",
  "button.girlfriends.close": "关闭",
  "button.girlfriends.accept_quest": "接受委托",
  "button.girlfriends.think_again": "我再想想",
  "label.girlfriends.objectives": "目标：",
  "message.girlfriends.quest_completed": "委托已完成！",
  "message.girlfriends.intimacy_confirmed": "你们的关系更进了一步...",
  "message.girlfriends.invite_home_success": "角色已邀请入住！",
  "message.girlfriends.invite_home_failed": "邀请入住失败，请检查重生点是否为双人床。",
  "message.girlfriends.invite_home_no_respawn": "请先设置重生点再邀请入住。"
}
```

以及 `en_us.json` 中对等翻译。
