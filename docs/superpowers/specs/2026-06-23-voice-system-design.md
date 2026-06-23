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
│        ├─ 提取 voiceKey: "momo.liked_0"                            │
│        ├─ 查找角色实体坐标 (CharacterWorldState → Entity)           │
│        ├─ player.connection.send(ClientboundPlayVoicePacket) [新增]│
│        └─ player.connection.send(ActionBar)    [现有：好感度变化]    │
│                                                                    │
│  ClientboundPlayVoicePacket(voiceKey, x, y, z)  ──────→ Client     │
└────────────────────────────────────────────────────────────────────┘

┌─ Client ───────────────────────────────────────────────────────────┐
│  ClientboundPlayVoicePacket handler (enqueueWork → Render Thread)  │
│    → VoiceManager.getVoice("momo.liked_0")                         │
│        ├─ getClientLocale() → "zh_cn"                              │
│        └─ voiceMap.get("zh_cn").get("momo.liked_0")                │
│          → DeferredHolder<SoundEvent, SoundEvent>                  │
│    → level.playSound(null, x, y, z, holder, SoundSource.VOICE,     │
│                       1.0F, 1.0F)                                  │
│                                                                   │
│    原版 SoundManager 自动:                                          │
│    → sounds.json 查找 .ogg 路径 → 播放                             │
│    → sounds.json 查找 subtitle key → 在右下角显示字幕                │
└────────────────────────────────────────────────────────────────────┘
```

## 组件设计

### 1. GirlfriendsVoiceEvents

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEvents.java`

注册所有语音 SoundEvent，并构建 `locale → voiceKey → DeferredHolder` 两级索引 Map。

```java
public final class GirlfriendsVoiceEvents {
    public static final DeferredRegister<SoundEvent> REGISTER =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GirlfriendsMod.MODID);

    /** locale → voiceKey → SoundEvent Holder 两级索引喵~ */
    static final Map<String, Map<String, DeferredHolder<SoundEvent, SoundEvent>>> VOICE_MAP =
        new LinkedHashMap<>();

    // 沫沫（当前已有 .ogg 的条目）喵~
    // registryPath 格式: gift.quote.{locale}.{voiceKey}，确保多语言 key 不冲突喵~
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_0 =
        register("zh_cn", "momo.favorite_girlfriends_bouquet_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_1 =
        register("zh_cn", "momo.favorite_girlfriends_bouquet_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_2 =
        register("zh_cn", "momo.favorite_girlfriends_bouquet_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_0 =
        register("zh_cn", "momo.liked_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_1 =
        register("zh_cn", "momo.liked_1");
    // ... 后续新增语音文件时同步添加

    /**
     * 注册一个语音 SoundEvent 并录入索引表喵~
     * registryPath 自动生成为 "gift.quote.{locale}.{voiceKey}" 喵~
     *
     * @param locale 语言代码（如 "zh_cn"）喵~
     * @param voiceKey 语音 key，与文本 JSON 中的后缀一致（如 "momo.liked_0"）喵~
     * @return DeferredHolder 喵~
     */
    private static DeferredHolder<SoundEvent, SoundEvent> register(String locale, String voiceKey) {
        String registryPath = "gift.quote." + locale + "." + voiceKey;
        DeferredHolder<SoundEvent, SoundEvent> holder = REGISTER.register(registryPath,
            () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, registryPath)));
        VOICE_MAP.computeIfAbsent(locale, k -> new LinkedHashMap<>()).put(voiceKey, holder);
        return holder;
    }
}
```

- 注册路径格式：`gift.quote.{locale}.{voiceKey}`（如 `gift.quote.zh_cn.momo.liked_0`），locale 嵌在 key 中确保多语言不冲突
- 使用 `SoundEvent.createVariableRangeEvent()` 创建，距离衰减范围为默认 16 格
- 按角色分组，便于维护
- `VOICE_MAP` 两级索引：`locale` → `voiceKey` → `DeferredHolder`
- **仅注册已有 `.ogg` 文件的语音条目**（当前 5 条：沫沫 favorite_bouquet 0~2 + liked 0~1）。新增语音文件时同步添加注册

### 2. GirlfriendsVoiceManager

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManager.java`

语音查找工具类，封装 locale 路由与 Map 查询。

```java
public final class GirlfriendsVoiceManager {
    private GirlfriendsVoiceManager() {}

    /**
     * 获取客户端当前语言代码喵~
     *
     * @return 当前硬编码返回 "zh_cn" 喵~
     */
    public static String getClientLocale() {
        // TODO 未来根据客户端 I18n 设置动态返回，如 "en_us"、"ja_jp" 等喵~
        return "zh_cn";
    }

    /**
     * 根据语音 key 获取 SoundEvent Holder（内部按当前 locale 路由）喵~
     *
     * @param voiceKey 语音 key，如 "momo.liked_0" 喵~
     * @return 对应的 DeferredHolder，未找到时返回 null 喵~
     */
    @Nullable
    public static DeferredHolder<SoundEvent, SoundEvent> getVoice(String voiceKey) {
        String locale = getClientLocale();
        Map<String, DeferredHolder<SoundEvent, SoundEvent>> localeMap =
            GirlfriendsVoiceEvents.VOICE_MAP.get(locale);
        if (localeMap == null) {
            return null;
        }
        return localeMap.get(voiceKey);
    }

    /**
     * 从完整 i18n quote key 提取语音 voiceKey 喵~
     * "girlfriends.gift.quote.momo.liked_0" → "momo.liked_0" 喵~
     *
     * @param quoteKey 完整 i18n key 喵~
     * @return voiceKey 喵~
     */
    public static String extractVoiceKey(String quoteKey) {
        return quoteKey.substring(GiftQuoteManager.QUOTE_KEY_PREFIX.length());
    }
}
```

### 3. ClientboundPlayVoicePacket

**文件**: `src/main/java/com/hexagram2021/girlfriends/common/network/ClientboundPlayVoicePacket.java`

```java
public record ClientboundPlayVoicePacket(String voiceKey, double x, double y, double z)
    implements CustomPayload {

    public static final CustomPayload.Type<ClientboundPlayVoicePacket> TYPE =
        new CustomPayload.Type<>(Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "play_voice"));

    public static final StreamCodec<ByteBuf, ClientboundPlayVoicePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundPlayVoicePacket::voiceKey,
            ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::x,
            ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::y,
            ByteBufCodecs.DOUBLE,      ClientboundPlayVoicePacket::z,
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
                String voiceKey = GirlfriendsVoiceManager.extractVoiceKey(result.quoteKey());
                player.connection.send(new ClientboundPlayVoicePacket(
                    voiceKey,
                    entity.getX(), entity.getY(), entity.getZ()));
            }
        }
    }
    // 现有：action bar
    // ...
}
```

`sendGiftFeedback()` 方法签名不变，实体查找在内部完成。

### 5. Client Handler（静态消费者注入模式）

**修改文件**: `GirlfriendsNetwork`（common）、`GirlfriendsModClient`（client）

遵循项目 `物理端隔离` 原则，`common/` 包不可直接引用 `net.minecraft.client.*`。

**common 侧（`GirlfriendsNetwork`）**：

```java
@Nullable
private static Consumer<ClientboundPlayVoicePacket> voiceHandler = null;

public static void setVoiceHandler(@Nullable Consumer<ClientboundPlayVoicePacket> handler) {
    voiceHandler = handler;
}

private static void handlePlayVoice(ClientboundPlayVoicePacket packet, IPayloadContext context) {
    Consumer<ClientboundPlayVoicePacket> handler = voiceHandler;
    if (handler != null) {
        // enqueueWork 在 Render Thread 上执行，满足 level.playSound 的主线程要求喵~
        context.enqueueWork(() -> handler.accept(packet));
    }
}
```

**client 侧（`GirlfriendsModClient`）**：

```java
GirlfriendsNetwork.setVoiceHandler(packet -> {
    DeferredHolder<SoundEvent, SoundEvent> holder =
        GirlfriendsVoiceManager.getVoice(packet.voiceKey());
    if (holder != null) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.playSound(null,
                packet.x(), packet.y(), packet.z(),
                holder, SoundSource.VOICE, 1.0F, 1.0F);
        }
    }
});
```

关键点：
- `context.enqueueWork()` 确保在 Render Thread 上执行，满足 `level.playSound()` 的主线程要求
- `SoundSource.VOICE` 使语音受"语音"音量滑块控制
- 原版自动处理：sounds.json 查找 `.ogg` 路径 → 播放 + subtitle 字幕显示

### 6. GirlfriendsMod 修改

- 注册 `GirlfriendsVoiceEvents.REGISTER`：`modEventBus.addListener(GirlfriendsVoiceEvents.REGISTER::register)`
- 注册 `ClientboundPlayVoicePacket`：在 `GirlfriendsNetwork.register()` 中添加 play-to-client 注册

## 数据文件

### sounds.json

**文件**: `src/main/resources/assets/girlfriends/sounds.json`（新建，手动维护）

```json
{
  "gift.quote.zh_cn.momo.favorite_girlfriends_bouquet_0": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/favorite_girlfriends_bouquet_0"],
    "subtitle": "girlfriends.subtitle.voice.momo.favorite"
  },
  "gift.quote.zh_cn.momo.favorite_girlfriends_bouquet_1": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/favorite_girlfriends_bouquet_1"],
    "subtitle": "girlfriends.subtitle.voice.momo.favorite"
  },
  "gift.quote.zh_cn.momo.favorite_girlfriends_bouquet_2": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/favorite_girlfriends_bouquet_2"],
    "subtitle": "girlfriends.subtitle.voice.momo.favorite"
  },
  "gift.quote.zh_cn.momo.liked_0": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/liked_0"],
    "subtitle": "girlfriends.subtitle.voice.momo.liked"
  },
  "gift.quote.zh_cn.momo.liked_1": {
    "category": "entity",
    "sounds": ["girlfriends:zh_cn/momo/liked_1"],
    "subtitle": "girlfriends.subtitle.voice.momo.liked"
  }
}
```

- key：格式 `gift.quote.{locale}.{character}.{quote_name}`，与 SoundEvent 注册 path 一致（如 `gift.quote.zh_cn.momo.liked_0`），locale 嵌入 key 确保多语言命名无冲突
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
| voiceKey 在 VOICE_MAP 中未找到 | `getVoice()` 返回 null，client handler 静默跳过 |
| `.ogg` 文件缺失 | `SoundManager` 自动输出 warn 日志，无异常抛出 |
| 客户端未安装模组 | 服务端发包无响应，无副作用 |

## 测试

| 测试类 | 验证内容 |
|--------|----------|
| `GirlfriendsVoiceManagerTest`（新增） | `extractVoiceKey()` 截取正确性、`getVoice()` 按 locale + voiceKey 返回正确 Holder |
| `GirlfriendsVoiceEventsTest`（新增） | 当前已注册 SoundEvent 与 VOICE_MAP 条目一致性验证 |
| `ServerboundPacketSerializationTest`（扩展） | `ClientboundPlayVoicePacket` 序列化/反序列化往返 |

## 文件操作清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `common/voice/GirlfriendsVoiceManager.java` | voiceKey 提取 + locale 路由 + Map 查询 |
| 新增 | `common/voice/GirlfriendsVoiceEvents.java` | `DeferredRegister<SoundEvent>` + VOICE_MAP 构建 |
| 新增 | `common/network/ClientboundPlayVoicePacket.java` | 语音播放网络包（voiceKey + 坐标） |
| 修改 | `common/network/GirlfriendsNetwork.java` | 注册 packet codec + `sendGiftFeedback()` 增发语音包 + voiceHandler 注入 |
| 修改 | `GirlfriendsMod.java` | 注册 `VOICE_EVENTS`、注册 play-to-client 包 |
| 修改 | `GirlfriendsModClient.java` | client handler 注入（level.playSound） |
| 新增 | `assets/girlfriends/sounds.json` | SoundEvent → .ogg 映射（手动维护，与语音文件同步增长） |
| 修改 | `assets/girlfriends/lang/zh_cn.json` | 新增 25 条字幕翻译 |
| 修改 | `assets/girlfriends/lang/en_us.json` | 新增 25 条字幕翻译 |
| 新增 | `test/.../voice/GirlfriendsVoiceManagerTest.java` | VoiceManager 单元测试 |
| 新增 | `test/.../voice/GirlfriendsVoiceEventsTest.java` | SoundEvent 注册 + VOICE_MAP 验证 |
| 修改 | `test/.../network/ServerboundPacketSerializationTest.java` | 语音包序列化测试 |
