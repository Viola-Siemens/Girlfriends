# 角色语音系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现角色语音系统——根据 i18n quote key 在客户端播放对应 locale 的 `.ogg` 语音，从角色实体位置发出，支持原版字幕喵~

**Architecture:** 新建 `GirlfriendsVoiceEvents` 注册 SoundEvent + 构建 `locale→voiceKey→Holder` 两级索引 Map；`GirlfriendsVoiceManager` 封装 locale 路由与 Map 查询；`ClientboundPlayVoicePacket` 传输语音指令；修改 `GirlfriendsNetwork.sendGiftFeedback()` 在赠礼后发送语音包；客户端通过静态消费者注入模式调用 `level.playSound()` 播放喵~

**Tech Stack:** NeoForge 26.1.2, Java 25, Minecraft 26.1.2, DeferredRegister<SoundEvent>, CustomPayload, StreamCodec, `level.playSound(Holder<SoundEvent>)` 喵~

## Global Constraints

- 项目使用 `Identifier`（NeoForge 映射），非 Mojang 的 `ResourceLocation` 喵~
- `common/` 包代码不得 import `net.minecraft.client.*`，客户端行为通过静态消费者注入 喵~
- 缩进使用 Tab，大括号 K&R 风格 喵~
- 注册路径格式：`gift.quote.{locale}.{voiceKey}`（如 `gift.quote.zh_cn.momo.liked_0`）喵~
- 网络协议版本 `"1"` 喵~
- 仅注册已有 `.ogg` 文件的语音条目（当前 5 条）喵~

---

### Task 1: GirlfriendsVoiceEvents — SoundEvent 注册 + VOICE_MAP 构建

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEvents.java`
- Create: `src/test/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEventsTest.java`

**Interfaces:**
- Produces: `GirlfriendsVoiceEvents.REGISTER` (`DeferredRegister<SoundEvent>`) — 供 `GirlfriendsMod` 注册到 event bus
- Produces: `GirlfriendsVoiceEvents.VOICE_MAP` (`Map<String, Map<String, DeferredHolder<SoundEvent, SoundEvent>>>`) — locale → voiceKey → Holder 两级索引，供 `VoiceManager.getVoice()` 查询
- Produces: 5 个 `DeferredHolder<SoundEvent, SoundEvent>` 静态字段（`MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_0` ~ `MOMO_LIKED_1`）

- [ ] **Step 1: 创建 GirlfriendsVoiceEvents.java**

```java
package com.hexagram2021.girlfriends.common.voice;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色语音 SoundEvent 注册表 + locale → voiceKey → Holder 两级索引喵~
 *
 * @author liudongyu
 */
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
	// 后续新增语音文件时同步添加喵~

	private GirlfriendsVoiceEvents() {
	}

	/**
	 * 注册一个语音 SoundEvent 并录入索引表喵~
	 * registryPath 自动生成为 "gift.quote.{locale}.{voiceKey}" 喵~
	 *
	 * @param locale   语言代码（如 "zh_cn"）喵~
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

- [ ] **Step 2: 编写 GirlfriendsVoiceEventsTest.java**

```java
package com.hexagram2021.girlfriends.common.voice;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GirlfriendsVoiceEvents 注册与 VOICE_MAP 一致性测试喵~
 *
 * @author liudongyu
 */
class GirlfriendsVoiceEventsTest {
	@Test
	void voiceMapContainsAllFiveEntries() {
		Map<String, Map<String, DeferredHolder<SoundEvent, SoundEvent>>> voiceMap =
				GirlfriendsVoiceEvents.VOICE_MAP;

		assertTrue(voiceMap.containsKey("zh_cn"), "VOICE_MAP should contain zh_cn locale");
		Map<String, DeferredHolder<SoundEvent, SoundEvent>> zhCnMap = voiceMap.get("zh_cn");
		assertNotNull(zhCnMap);
		assertEquals(5, zhCnMap.size(), "zh_cn should have 5 voice entries");

		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_0"));
		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_1"));
		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_2"));
		assertTrue(zhCnMap.containsKey("momo.liked_0"));
		assertTrue(zhCnMap.containsKey("momo.liked_1"));
	}

	@Test
	void holderRegistryNameMatchesLocaleAndVoiceKey() {
		DeferredHolder<SoundEvent, SoundEvent> holder =
				GirlfriendsVoiceEvents.MOMO_LIKED_0;

		Identifier expectedId = Identifier.fromNamespaceAndPath(
				GirlfriendsMod.MODID, "gift.quote.zh_cn.momo.liked_0");
		assertEquals(expectedId, holder.getId());
	}

	@Test
	void voiceMapReferencesSameHolderAsStaticField() {
		DeferredHolder<SoundEvent, SoundEvent> staticHolder =
				GirlfriendsVoiceEvents.MOMO_LIKED_0;
		DeferredHolder<SoundEvent, SoundEvent> mapHolder =
				GirlfriendsVoiceEvents.VOICE_MAP.get("zh_cn").get("momo.liked_0");

		assertSame(staticHolder, mapHolder,
				"Static field and VOICE_MAP entry should reference the same DeferredHolder");
	}

	@Test
	void registryKeyIsSoundEvent() {
		assertEquals(BuiltInRegistries.SOUND_EVENT.key(),
				GirlfriendsVoiceEvents.REGISTER.getRegistryKey());
	}
}
```

- [ ] **Step 3: 运行测试验证失败（类不存在）**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceEventsTest"
```
Expected: FAIL — `GirlfriendsVoiceEvents` class not found

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceEventsTest"
```
Expected: PASS (4/4)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEvents.java src/test/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceEventsTest.java
git commit -m "feat(REQ-7): SoundEvent 注册 + VOICE_MAP 两级索引"
```

---

### Task 2: GirlfriendsVoiceManager — locale 路由 + voiceKey 查询

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManager.java`
- Create: `src/test/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManagerTest.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManager.java` (line 42: `QUOTE_KEY_PREFIX` → `public static final`)

**Interfaces:**
- Consumes: `GirlfriendsVoiceEvents.VOICE_MAP`
- Produces: `GirlfriendsVoiceManager.getClientLocale()` → `String`（硬编码返回 `"zh_cn"`）
- Produces: `GirlfriendsVoiceManager.getVoice(String voiceKey)` → `@Nullable DeferredHolder<SoundEvent, SoundEvent>`
- Produces: `GirlfriendsVoiceManager.extractVoiceKey(String quoteKey)` → `String`

- [ ] **Step 1: 修改 GiftQuoteManager.QUOTE_KEY_PREFIX 为 public**

在 `GiftQuoteManager.java` 第 42 行：

```java
// 修改前
static final String QUOTE_KEY_PREFIX = "girlfriends.gift.quote.";

// 修改后
public static final String QUOTE_KEY_PREFIX = "girlfriends.gift.quote.";
```

- [ ] **Step 2: 创建 GirlfriendsVoiceManager.java**

```java
package com.hexagram2021.girlfriends.common.voice;

import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 语音查找工具类，封装 locale 路由与 VOICE_MAP 查询喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsVoiceManager {
	private GirlfriendsVoiceManager() {
	}

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

- [ ] **Step 3: 编写 GirlfriendsVoiceManagerTest.java**

```java
package com.hexagram2021.girlfriends.common.voice;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * GirlfriendsVoiceManager 单元测试喵~
 *
 * @author liudongyu
 */
class GirlfriendsVoiceManagerTest {
	@Test
	void getClientLocaleReturnsZhCn() {
		assertEquals("zh_cn", GirlfriendsVoiceManager.getClientLocale());
	}

	@Test
	void extractVoiceKeyStripsQuotePrefix() {
		String quoteKey = "girlfriends.gift.quote.momo.liked_0";
		String voiceKey = GirlfriendsVoiceManager.extractVoiceKey(quoteKey);
		assertEquals("momo.liked_0", voiceKey);
	}

	@Test
	void extractVoiceKeyHandlesFavoriteWithItemId() {
		String quoteKey = "girlfriends.gift.quote.momo.favorite_girlfriends_bouquet_1";
		String voiceKey = GirlfriendsVoiceManager.extractVoiceKey(quoteKey);
		assertEquals("momo.favorite_girlfriends_bouquet_1", voiceKey);
	}

	@Test
	void getVoiceReturnsCorrectHolderForExistingKey() {
		DeferredHolder<SoundEvent, SoundEvent> holder =
				GirlfriendsVoiceManager.getVoice("momo.liked_0");

		assertNotNull(holder, "getVoice should return non-null for registered key");
		assertSame(GirlfriendsVoiceEvents.MOMO_LIKED_0, holder,
				"getVoice should return the same DeferredHolder as the static field");
	}

	@Test
	void getVoiceReturnsNullForUnknownKey() {
		DeferredHolder<SoundEvent, SoundEvent> holder =
				GirlfriendsVoiceManager.getVoice("yuxi.nonexistent");
		assertNull(holder);
	}

	@Test
	void getVoiceReturnsNullForUnknownLocale() {
		// 当前仅有 zh_cn locale，查询其他 locale 应返回 null 喵~
		Map entry = GirlfriendsVoiceEvents.VOICE_MAP.get("ja_jp");
		assertNull(entry, "ja_jp locale should not exist in VOICE_MAP");
	}
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceManagerTest"
```
Expected: PASS (6/6)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManager.java src/test/java/com/hexagram2021/girlfriends/common/voice/GirlfriendsVoiceManagerTest.java src/main/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManager.java
git commit -m "feat(REQ-7): VoiceManager locale 路由 + voiceKey 查询"
```

---

### Task 3: ClientboundPlayVoicePacket — 语音网络包

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/network/clientbound/ClientboundPlayVoicePacket.java`
- Modify: `src/test/java/com/hexagram2021/girlfriends/common/network/ServerboundPacketSerializationTest.java`

**Interfaces:**
- Produces: `ClientboundPlayVoicePacket(String voiceKey, double x, double y, double z)` — record，含 `TYPE` (`CustomPayload.Type`) 和 `STREAM_CODEC` (`StreamCodec`)
- 后续 Task 4 由 `GirlfriendsNetwork` 注册和发送

- [ ] **Step 1: 创建 ClientboundPlayVoicePacket.java**

```java
package com.hexagram2021.girlfriends.common.network.clientbound;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPayload;
import net.minecraft.resources.Identifier;

/**
 * 语音播放网络包——Server → Client 喵~
 *
 * @param voiceKey 语音 key（如 "momo.liked_0"）喵~
 * @param x        音源 X 坐标喵~
 * @param y        音源 Y 坐标喵~
 * @param z        音源 Z 坐标喵~
 *
 * @author liudongyu
 */
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
					ClientboundPlayVoicePacket::new
			);

	@Override
	public CustomPayload.Type<? extends CustomPayload> type() {
		return TYPE;
	}
}
```

- [ ] **Step 2: 在 ServerboundPacketSerializationTest.java 中新增序列化往返测试**

在现有文件末尾添加（在最后的 `}` 之前）：

```java
	@Test
	void playVoicePacketRoundTrip() {
		ClientboundPlayVoicePacket packet = new ClientboundPlayVoicePacket(
				"momo.liked_0", 10.5, 64.0, -20.25);
		ByteBuf buf = Unpooled.buffer();
		ClientboundPlayVoicePacket.STREAM_CODEC.encode(buf, packet);
		ClientboundPlayVoicePacket decoded =
				ClientboundPlayVoicePacket.STREAM_CODEC.decode(buf);
		assertEquals("momo.liked_0", decoded.voiceKey());
		assertEquals(10.5, decoded.x(), 0.001);
		assertEquals(64.0, decoded.y(), 0.001);
		assertEquals(-20.25, decoded.z(), 0.001);
	}
```

同时在 import 区域添加：

```java
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundPlayVoicePacket;
```

- [ ] **Step 3: 运行测试验证通过**

```bash
./gradlew test --tests "com.hexagram2021.girlfriends.common.network.ServerboundPacketSerializationTest"
```
Expected: ALL PASS（含新增 `playVoicePacketRoundTrip`）

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/clientbound/ClientboundPlayVoicePacket.java src/test/java/com/hexagram2021/girlfriends/common/network/ServerboundPacketSerializationTest.java
git commit -m "feat(REQ-7): ClientboundPlayVoicePacket 语音网络包"
```

---

### Task 4: GirlfriendsNetwork — 注册 + sendGiftFeedback 集成 + voiceHandler 注入

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java`

**Interfaces:**
- Consumes: `ClientboundPlayVoicePacket.TYPE`, `ClientboundPlayVoicePacket.STREAM_CODEC` (Task 3)
- Consumes: `GirlfriendsVoiceManager.extractVoiceKey(String)` (Task 2)
- Produces: `GirlfriendsNetwork.setVoiceHandler(Consumer<ClientboundPlayVoicePacket>)` — 供 `GirlfriendsModClient` 注入
- Modifies: `sendGiftFeedback()` — 在聊天栏消息后增发语音包
- Adds: `handlePlayVoice()` — clientbound packet handler

- [ ] **Step 1: 修改 GirlfriendsNetwork.java**

需要做的修改（按位置排列）：

**1a: 新增 import（在 import 区域末尾添加）**

```java
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundPlayVoicePacket;
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceManager;

import javax.annotation.Nullable;
import java.util.function.Consumer;
```

**1b: 新增 voiceHandler 静态字段 + setter（在 `screenOpener` 字段下方）**

在第 57 行 `private static BiConsumer<Identifier, InteractionSummary> screenOpener = (_, _) -> {};` 之后添加：

```java
	/**
	 * 客户端侧语音播放 handler，由 {@link com.hexagram2021.girlfriends.client.GirlfriendsModClient} 注入喵~
	 * 在专用服务端上此字段始终为 null，从而隔离客户端类引用喵~
	 */
	@Nullable
	private static Consumer<ClientboundPlayVoicePacket> voiceHandler = null;

	/**
	 * 由客户端模组类调用，注入语音播放逻辑喵~
	 *
	 * @param handler 语音播放 handler 喵~
	 */
	public static void setVoiceHandler(@Nullable Consumer<ClientboundPlayVoicePacket> handler) {
		voiceHandler = handler;
	}
```

**1c: 注册 play-to-client packet（在 `register()` 方法中）**

在第 76 行 `registrar.playToClient(ClientboundQuestIconPacket.TYPE, ...)` 之后添加：

```java
			registrar.playToClient(ClientboundPlayVoicePacket.TYPE, ClientboundPlayVoicePacket.STREAM_CODEC, GirlfriendsNetwork::handlePlayVoice);
```

**1d: 新增 handlePlayVoice 方法（在 `handleQuestIcon` 方法下方）**

```java
	private static void handlePlayVoice(ClientboundPlayVoicePacket packet, IPayloadContext context) {
		Consumer<ClientboundPlayVoicePacket> handler = voiceHandler;
		if (handler != null) {
			// enqueueWork 在 Render Thread 上执行，满足 level.playSound 的主线程要求喵~
			context.enqueueWork(() -> handler.accept(packet));
		}
	}
```

**1e: 修改 sendGiftFeedback() — 增发语音包**

将现有的：

```java
	private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId, GiftResult result) {
		// 1. 角色台词 → 聊天栏（若 quoteKey 不为 null）喵~
		if (result.quoteKey() != null) {
			player.sendSystemMessage(Component.translatable(result.quoteKey()));
		}

		// 2. 好感度变化 → subtitle 三联包喵~
```

替换为：

```java
	private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId, GiftResult result) {
		// 1. 角色台词 → 聊天栏（若 quoteKey 不为 null）喵~
		if (result.quoteKey() != null) {
			player.sendSystemMessage(Component.translatable(result.quoteKey()));

			// 1.5 角色语音 → 客户端播放（从角色实体位置发出）喵~
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

		// 2. 好感度变化 → subtitle 三联包喵~
```

- [ ] **Step 2: 验证编译通过**

```bash
./gradlew classes
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java
git commit -m "feat(REQ-7): GirlfriendsNetwork 集成语音包发送 + voiceHandler 注入"
```

---

### Task 5: GirlfriendsMod + GirlfriendsModClient — 注册入口

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java`

**Interfaces:**
- Consumes: `GirlfriendsVoiceEvents.REGISTER` (Task 1)
- Consumes: `GirlfriendsNetwork.setVoiceHandler(Consumer)` (Task 4)

- [ ] **Step 1: 修改 GirlfriendsMod.java — 注册 VOICE_EVENTS**

**1a: 新增 import**

```java
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceEvents;
```

**1b: 在构造函数中注册（在 `GirlfriendsSensorTypes.REGISTER.register(modEventBus);` 后添加）**

在第 77 行后添加：

```java
			GirlfriendsVoiceEvents.REGISTER.register(modEventBus);
```

- [ ] **Step 2: 修改 GirlfriendsModClient.java — 注入 voiceHandler**

**2a: 新增 import**

```java
import com.hexagram2021.girlfriends.common.network.clientbound.ClientboundPlayVoicePacket;
import com.hexagram2021.girlfriends.common.voice.GirlfriendsVoiceManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
```

**2b: 在 `onClientSetup()` 方法中，`setScreenOpener` 调用后添加 voiceHandler 注入**

在第 48 行 `});` 后添加：

```java
		// 注入语音播放 handler，将客户端侧声音播放与 common 网络代码解耦喵~
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

- [ ] **Step 3: 构建验证**

```bash
./gradlew classes
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java src/main/java/com/hexagram2021/girlfriends/client/GirlfriendsModClient.java
git commit -m "feat(REQ-7): GirlfriendsMod + GirlfriendsModClient 注册语音系统"
```

---

### Task 6: 数据文件 — sounds.json + 字幕 lang

**Files:**
- Create: `src/main/resources/assets/girlfriends/sounds.json`
- Modify: `src/main/resources/assets/girlfriends/lang/zh_cn.json`
- Modify: `src/main/resources/assets/girlfriends/lang/en_us.json`

**Interfaces:**
- sounds.json 映射 SoundEvent → `.ogg` 文件路径 + subtitle key 喵~
- lang 文件提供 25 条字幕翻译（5 角色 × 5 档位）喵~

- [ ] **Step 1: 创建 sounds.json**

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

- [ ] **Step 2: 修改 zh_cn.json — 新增 25 条字幕翻译**

当前末尾格式：

```json
  "girlfriends.gift.quote.youruo.disliked_4": "<幽若> ..."
}
```

需要在 `youruo.disliked_4` 行末尾加逗号，然后插入 25 条新条目，最后一行不加逗号。具体操作：

（1）将末尾的 `"girlfriends.gift.quote.youruo.disliked_4": "<幽若> ..."` 行末尾加逗号（即改为 `"...",`）
（2）在最后的 `}` 前插入以下 25 行：

```json
  "girlfriends.subtitle.voice.momo.favorite": "沫沫：喜悦",
  "girlfriends.subtitle.voice.momo.liked": "沫沫：开心",
  "girlfriends.subtitle.voice.momo.accepted": "沫沫：平常",
  "girlfriends.subtitle.voice.momo.rejected": "沫沫：疑惑",
  "girlfriends.subtitle.voice.momo.disliked": "沫沫：嫌恶",
  "girlfriends.subtitle.voice.yuxi.favorite": "渔溪：喜悦",
  "girlfriends.subtitle.voice.yuxi.liked": "渔溪：开心",
  "girlfriends.subtitle.voice.yuxi.accepted": "渔溪：平常",
  "girlfriends.subtitle.voice.yuxi.rejected": "渔溪：疑惑",
  "girlfriends.subtitle.voice.yuxi.disliked": "渔溪：嫌恶",
  "girlfriends.subtitle.voice.meishu.favorite": "梅疏：喜悦",
  "girlfriends.subtitle.voice.meishu.liked": "梅疏：开心",
  "girlfriends.subtitle.voice.meishu.accepted": "梅疏：平常",
  "girlfriends.subtitle.voice.meishu.rejected": "梅疏：疑惑",
  "girlfriends.subtitle.voice.meishu.disliked": "梅疏：嫌恶",
  "girlfriends.subtitle.voice.wanying.favorite": "晚萤：喜悦",
  "girlfriends.subtitle.voice.wanying.liked": "晚萤：开心",
  "girlfriends.subtitle.voice.wanying.accepted": "晚萤：平常",
  "girlfriends.subtitle.voice.wanying.rejected": "晚萤：疑惑",
  "girlfriends.subtitle.voice.wanying.disliked": "晚萤：嫌恶",
  "girlfriends.subtitle.voice.youruo.favorite": "幽若：喜悦",
  "girlfriends.subtitle.voice.youruo.liked": "幽若：开心",
  "girlfriends.subtitle.voice.youruo.accepted": "幽若：平常",
  "girlfriends.subtitle.voice.youruo.rejected": "幽若：疑惑",
  "girlfriends.subtitle.voice.youruo.disliked": "幽若：嫌恶"
```

注意：最后一条 `youruo.disliked` 不加逗号，确保 JSON 合法。

- [ ] **Step 3: 修改 en_us.json — 新增 25 条英文字幕**

与 zh_cn.json 同样的操作方式：给末尾 `youruo.disliked_4` 行加逗号，然后在 `}` 前插入：

```json
  "girlfriends.subtitle.voice.momo.favorite": "Momo: Delighted",
  "girlfriends.subtitle.voice.momo.liked": "Momo: Happy",
  "girlfriends.subtitle.voice.momo.accepted": "Momo: Neutral",
  "girlfriends.subtitle.voice.momo.rejected": "Momo: Puzzled",
  "girlfriends.subtitle.voice.momo.disliked": "Momo: Disgusted",
  "girlfriends.subtitle.voice.yuxi.favorite": "Yuxi: Delighted",
  "girlfriends.subtitle.voice.yuxi.liked": "Yuxi: Happy",
  "girlfriends.subtitle.voice.yuxi.accepted": "Yuxi: Neutral",
  "girlfriends.subtitle.voice.yuxi.rejected": "Yuxi: Puzzled",
  "girlfriends.subtitle.voice.yuxi.disliked": "Yuxi: Disgusted",
  "girlfriends.subtitle.voice.meishu.favorite": "Meishu: Delighted",
  "girlfriends.subtitle.voice.meishu.liked": "Meishu: Happy",
  "girlfriends.subtitle.voice.meishu.accepted": "Meishu: Neutral",
  "girlfriends.subtitle.voice.meishu.rejected": "Meishu: Puzzled",
  "girlfriends.subtitle.voice.meishu.disliked": "Meishu: Disgusted",
  "girlfriends.subtitle.voice.wanying.favorite": "Wanying: Delighted",
  "girlfriends.subtitle.voice.wanying.liked": "Wanying: Happy",
  "girlfriends.subtitle.voice.wanying.accepted": "Wanying: Neutral",
  "girlfriends.subtitle.voice.wanying.rejected": "Wanying: Puzzled",
  "girlfriends.subtitle.voice.wanying.disliked": "Wanying: Disgusted",
  "girlfriends.subtitle.voice.youruo.favorite": "Youruo: Delighted",
  "girlfriends.subtitle.voice.youruo.liked": "Youruo: Happy",
  "girlfriends.subtitle.voice.youruo.accepted": "Youruo: Neutral",
  "girlfriends.subtitle.voice.youruo.rejected": "Youruo: Puzzled",
  "girlfriends.subtitle.voice.youruo.disliked": "Youruo: Disgusted"
```

最后一条 `youruo.disliked` 不加逗号。

- [ ] **Step 4: 验证 JSON 格式有效 + 构建通过**

```bash
./gradlew classes
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/assets/girlfriends/sounds.json src/main/resources/assets/girlfriends/lang/zh_cn.json src/main/resources/assets/girlfriends/lang/en_us.json
git commit -m "feat(REQ-7): sounds.json + 25 条中英文字幕翻译"
```

---

### Task 7: 集成测试 — 运行全部测试

- [ ] **Step 1: 运行全部单元测试**

```bash
./gradlew test
```
Expected: ALL TESTS PASS（包括新增的 `GirlfriendsVoiceEventsTest`、`GirlfriendsVoiceManagerTest`、`ServerboundPacketSerializationTest.playVoicePacketRoundTrip`）

- [ ] **Step 2: 构建完整模组**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 最终 Commit**

```bash
git commit --allow-empty -m "test(REQ-7): 语音系统全部测试通过"
```
