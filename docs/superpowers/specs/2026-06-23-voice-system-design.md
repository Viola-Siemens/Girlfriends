# 角色语音系统设计

## 概述

为角色赠礼台词接入语音播放系统。玩家向角色赠送礼物后，客户端根据 quote i18n key 自动播放对应的角色语音（`.ogg` 文件），语音从角色实体位置发出，支持原版字幕显示。

### 设计目标

- **key 一致**：语音 SoundEvent ID 与文本 i18n key 保持对应，便于维护
- **locale 路由**：语音系统支持按玩家客户端语言选择不同的语音文件（当前仅 `zh_cn`，后续扩展 `en_us`、`ja_jp` 等）
- **原版兼容**：SoundEvent 逐条注册 + sounds.json 映射，支持字幕、资源包覆盖、`/playsound` 调试
- **scope**：当前仅接入 `GiftQuoteManager` 赠礼台词流程，后续可扩展至其他语音场景（问候、死亡等）

## 架构

```
┌─ Server ───────────────────────────────────────────────────────────┐
│  GiftService.applyGift()                                           │
│    → GiftResult.quoteKey ("girlfriends.gift.quote.momo.liked_0")   │
│    → GirlfriendsNetwork.sendGiftFeedback()                         │
│        ├─ player.sendSystemMessage(quoteKey)   [现有：聊天栏文字]    │
│        ├─ 查找角色实体坐标 (CharacterWorldState → Entity)           │
│        ├─ player.connection.send(ClientboundPlayVoicePacket) [新增]│
│        └─ player.connection.send(ActionBar)    [现有：好感度变化]    │
│                                                                    │
│  ClientboundPlayVoicePacket  ──────────────────────────→ Client    │
└────────────────────────────────────────────────────────────────────┘

┌─ Client ───────────────────────────────────────────────────────────┐
│  ClientboundPlayVoicePacket handler                                │
│    → VoiceManager.quoteKeyToVoiceId(quoteKey)                      │
│        "girlfriends.gift.quote.momo.liked_0"                       │
│        → ResourceLocation("girlfriends", "gift.quote.momo.liked_0")│
│    → SoundEvent.createVariableRangeEvent(voiceId)                  │
│    → PositionedSoundInstance(soundEvent, SoundSource.VOICE, ...)   │
│    → Minecraft.getInstance().getSoundManager().play(...)           │
│                                                                   │
│    原版 SoundManager 自动:                                          │
│    → sounds.json 查找 .ogg 路径 → 播放                             │
│    → sounds.json 查找 subtitle key → 在右下角显示字幕                │
└────────────────────────────────────────────────────────────────────┘
```

## 组件设计

### 1. GirlfriendsVoiceManager

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManager.java`

纯工具类，无状态，位于 `common/` 包下供 server/client 共用。

```java
public final class GirlfriendsVoiceManager {
    private GirlfriendsVoiceManager() {}

    /**
     * 将台词 i18n key 转换为语音 ResourceLocation 喵~
     * 输入:  "girlfriends.gift.quote.momo.liked_0"
     * 输出:  ResourceLocation("girlfriends", "gift.quote.momo.liked_0")
     */
    public static ResourceLocation quoteKeyToVoiceId(String quoteKey) {
        int firstDot = quoteKey.indexOf('.');
        return ResourceLocation.fromNamespaceAndPath(
            quoteKey.substring(0, firstDot), quoteKey.substring(firstDot + 1));
    }
}
```

转换规则：取 i18n key 的第一个 `.` 为界，左侧为 namespace，右侧为 path。例如 `girlfriends.gift.quote.momo.liked_0` → `girlfriends:gift.quote.momo.liked_0`。

当前 locale 硬编码为 `zh_cn`（sounds.json 中映射至 `zh_cn/` 子目录）。未来多 locale 支持时在此层扩展路由逻辑。

### 2. GirlfriendsVoiceEvents

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEvents.java`

```java
public final class GirlfriendsVoiceEvents {
    public static final DeferredRegister<SoundEvent> REGISTER =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GirlfriendsMod.MODID);

    // 沫沫 (29 条)
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_0 =
        register("gift.quote.momo.favorite_girlfriends_bouquet_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_0 =
        register("gift.quote.momo.liked_0");
    // ... 按角色分组，随语音文件增加而扩展

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return REGISTER.register(path,
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, path)));
    }
}
```

- 注册路径与 i18n key 的 path 部分一致（如 `gift.quote.momo.liked_0`）
- 使用 `SoundEvent.createVariableRangeEvent()` 创建，距离衰减范围为默认 16 格
- 按角色分组，便于维护
- **仅注册已有 `.ogg` 文件的语音条目**（当前 5 条：沫沫 favorite_bouquet 0~2 + liked 0~1）。新增语音文件时同步添加注册

### 3. ClientboundPlayVoicePacket

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/network/ClientboundPlayVoicePacket.java`

```java
public record ClientboundPlayVoicePacket(String quoteKey, double x, double y, double z)
    implements CustomPayload {

    public static final CustomPayload.Type<ClientboundPlayVoicePacket> TYPE =
        new CustomPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GirlfriendsMod.MODID, "play_voice"));

    public static final StreamCodec<ByteBuf, ClientboundPlayVoicePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundPlayVoicePacket::quoteKey,
            ByteBufCodecs.DOUBLE,        ClientboundPlayVoicePacket::x,
            ByteBufCodecs.DOUBLE,        ClientboundPlayVoicePacket::y,
            ByteBufCodecs.DOUBLE,        ClientboundPlayVoicePacket::z,
            ClientboundPlayVoicePacket::new);
}
```

网络协议版本沿用现有 `"1"`。

### 4. Gift 反馈集成

**修改文件**: `GirlfriendsNetwork.sendGiftFeedback()`

在现有 `sendGiftFeedback()` 方法中，发送聊天栏文字之后、发送 action bar 之前，插入语音包发送逻辑：

```java
private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId, GiftResult result) {
    if (result.quoteKey() != null) {
        // 现有：聊天栏文字
        player.sendSystemMessage(Component.translatable(result.quoteKey()));

        // 新增：发送语音包
        GirlfriendsWorldData data = getWorldData(player);
        CharacterWorldState state = data.getExistingCharacterState(girlfriendTypeId);
        if (state != null && state.isAlive() && state.getEntityUuid() != null) {
            Entity entity = player.level().getEntity(state.getEntityUuid());
            if (entity != null) {
                player.connection.send(new ClientboundPlayVoicePacket(
                    result.quoteKey(),
                    entity.getX(), entity.getY(), entity.getZ()));
            }
        }
    }
    // 现有：action bar
    // ...
}
```

**实体查找**：复用 `canReachEntity()` 现有逻辑（`CharacterWorldState` → entity UUID → `ServerLevel.getEntity()`）。若实体已不存在（死亡卸载等），静默跳过语音，不影响文字消息发送。

`sendGiftFeedback()` 方法签名不变，实体查找在内部完成。

### 5. Client Handler（静态消费者注入模式）

**修改文件**: `GirlfriendsNetwork`（common）、`GirlfriendsModClient`（client）

遵循项目 `物理端隔离` 原则，`common/` 包不可直接引用 `net.minecraft.client.*`。采用与 `screenOpener` 相同的静态消费者注入模式。

**common 侧（`GirlfriendsNetwork`）**：

```java
// 静态消费者字段，默认为空操作
@Nullable
private static Consumer<ClientboundPlayVoicePacket> voiceHandler = null;

/**
 * 注入语音播放 handler（由 GirlfriendsModClient 在 clientSetup 中调用）喵~
 */
public static void setVoiceHandler(@Nullable Consumer<ClientboundPlayVoicePacket> handler) {
    voiceHandler = handler;
}

// packet handler 中
private static void handlePlayVoice(ClientboundPlayVoicePacket packet, IPayloadContext context) {
    Consumer<ClientboundPlayVoicePacket> handler = voiceHandler;
    if (handler != null) {
        context.enqueueWork(() -> handler.accept(packet));
    }
}
```

**client 侧（`GirlfriendsModClient`）**：

```java
// 在 onClientSetup 中注入
GirlfriendsNetwork.setVoiceHandler(packet -> {
    ResourceLocation voiceId = GirlfriendsVoiceManager.quoteKeyToVoiceId(packet.quoteKey());
    SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(voiceId);
    Minecraft.getInstance().getSoundManager().play(
        new PositionedSoundInstance(soundEvent, SoundSource.VOICE,
            1.0F, 1.0F, RandomSource.create(),
            packet.x(), packet.y(), packet.z()));
});
```

`SoundSource.VOICE` 使语音受"语音"音量滑块控制。原版 `SoundManager` 自动完成：
1. 从 `sounds.json` 查找 `.ogg` 文件路径并播放
2. 从 `sounds.json` 查找 `subtitle` key，若玩家开启了字幕则在右下角显示

### 6. GirlfriendsMod 修改

- 注册 `GirlfriendsVoiceEvents.REGISTER`：`modEventBus.addListener(GirlfriendsVoiceEvents.REGISTER::register)`
- 注册 `ClientboundPlayVoicePacket`：在 `GirlfriendsNetwork.register()` 中添加 play-to-client 注册

## 数据文件

### sounds.json

**文件**: `src/main/resources/assets/girlfriends/sounds.json`（新建，手动维护）

```json
{
  "gift.quote.momo.favorite_girlfriends_bouquet_0": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/favorite_girlfriends_bouquet_0"],
    "subtitle": "girlfriends.subtitle.voice.momo.favorite"
  },
  "gift.quote.momo.liked_0": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/liked_0"],
    "subtitle": "girlfriends.subtitle.voice.momo.liked"
  }
}
```

- key：与 SoundEvent 注册 path 一致（`gift.quote.{character}.{quote_name}`）
- `sounds`：指向 locale 子目录的 `.ogg` 文件，路径 `{namespace}:{locale}/{character}/{quote_name}`
- `subtitle`：字幕 i18n key，按角色 × 情绪档位分组
- `category`：`"entity"`（当前版本已废弃，由播放时传入的 `SoundSource` 决定；保留以兼容低版本移植）
- **仅录入已有 `.ogg` 文件的条目**（当前 5 条），新增语音文件时同步追加

### 语音文件目录

```
assets/girlfriends/sounds/zh_cn/
  momo/
    favorite_girlfriends_bouquet_0.ogg
    favorite_girlfriends_bouquet_1.ogg
    favorite_girlfriends_bouquet_2.ogg
    liked_0.ogg
    liked_1.ogg
    ... (后续添加其他台词)
  yuxi/
    ... (后续添加)
  meishu/
    ... (后续添加)
  wanying/
    ... (后续添加)
  youruo/
    ... (后续添加)
```

未来多 locale 支持时新增 `ja_jp/`、`en_us/` 等同级目录，与 `zh_cn/` 结构完全一致。

### 语言文件

**修改文件**: `lang/zh_cn.json`、`lang/en_us.json`（各新增 25 条）

字幕 i18n key 格式：`girlfriends.subtitle.voice.{character}.{tier}`

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.subtitle.voice.momo.favorite` | 沫沫：喜悦 | Momo: Delighted |
| `girlfriends.subtitle.voice.momo.liked` | 沫沫：开心 | Momo: Happy |
| `girlfriends.subtitle.voice.momo.accepted` | 沫沫：平常 | Momo: Neutral |
| `girlfriends.subtitle.voice.momo.rejected` | 沫沫：疑惑 | Momo: Puzzled |
| `girlfriends.subtitle.voice.momo.disliked` | 沫沫：嫌恶 | Momo: Disgusted |
| `girlfriends.subtitle.voice.yuxi.favorite` | 渔溪：喜悦 | Yuxi: Delighted |
| ... | ... | ... |

共 5 角色 × 5 档位 = 25 条。

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 角色实体不存在（已死亡/卸载） | 静默跳过语音，文字消息与 action bar 正常发送 |
| `.ogg` 文件缺失 | `SoundManager` 自动输出 warn 日志，无异常抛出 |
| 客户端未安装模组 | 服务端发包无响应，无副作用 |
| `quoteKey` 无对应 SoundEvent 注册 | 客户端 warn 日志，静默跳过 |

## 测试

| 测试类 | 验证内容 |
|--------|----------|
| `GirlfriendsVoiceManagerTest`（新增） | `quoteKeyToVoiceId()` 转换正确性（验证 i18n key → ResourceLocation 映射无误） |
| `GirlfriendsVoiceEventsTest`（新增） | 当前已注册 SoundEvent 的 ResourceLocation 与 i18n key path 一致性验证 |
| `ServerboundPacketSerializationTest`（扩展） | `ClientboundPlayVoicePacket` 序列化/反序列化往返 |

## 文件操作清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `common/voice/GirlfriendsVoiceManager.java` | quote key → voice ID 转换 |
| 新增 | `common/voice/GirlfriendsVoiceEvents.java` | `DeferredRegister<SoundEvent>` 注册已有语音的 SoundEvent（当前 5 条，随文件增加而扩展） |
| 新增 | `common/network/ClientboundPlayVoicePacket.java` | 语音播放网络包 |
| 修改 | `common/network/GirlfriendsNetwork.java` | 注册 packet codec + `sendGiftFeedback()` 增发语音包 |
| 修改 | `GirlfriendsMod.java` | 注册 `VOICE_EVENTS`、注册 play-to-client 包 |
| 修改 | `GirlfriendsModClient.java` | client handler 注册 |
| 新增 | `assets/girlfriends/sounds.json` | SoundEvent → .ogg 映射（手动维护，与语音文件同步增长） |
| 修改 | `assets/girlfriends/lang/zh_cn.json` | 新增 25 条字幕翻译 |
| 修改 | `assets/girlfriends/lang/en_us.json` | 新增 25 条字幕翻译 |
| 新增 | `test/.../voice/GirlfriendsVoiceManagerTest.java` | VoiceManager 单元测试 |
| 新增 | `test/.../voice/GirlfriendsVoiceEventsTest.java` | SoundEvent 注册验证 |
| 修改 | `test/.../network/ServerboundPacketSerializationTest.java` | 语音包序列化测试 |
