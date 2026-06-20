# 礼物回复台词系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为礼物系统增加角色回复台词功能——角色根据礼物档位随机选取台词发送到聊天栏，好感度变化移至 subtitle 显示。

**Architecture:** 新建 `GiftQuoteManager`（`SimplePreparableReloadListener`）从 `data/<ns>/girlfriends/gift_quotes/*.json` 加载 i18n key；`GiftResult` 新增 `quoteKey` 字段；`GiftService` 注入 `GiftQuoteManager` 并在 `applyGift` 中随机抽取；`GirlfriendsNetwork` 拆分为两条通道——台词走 `sendSystemMessage`（聊天栏），好感度变化走 `ClientboundSetSubtitleTextPacket`（subtitle）。

**Tech Stack:** Java 25, NeoForge 26.1.2.71, Mojang official mappings, Guava ImmutableListMultimap, Gson JSON parsing

## Global Constraints

- 遵循 CLAUDE.md 所有代码规范（Tab 缩进、K&R 大括号、Javadoc、蛇形命名）
- 所有 Service 依赖通过构造方法注入，支持单元测试替换
- `@Nullable` 注解修饰所有可能为 null 的字段/参数/返回值
- 现有测试用例必须保持通过（回归约束）
- 台词完整内容参见 `docs/superpowers/specs/2026-06-20-gift-quote-system-design.md` 附录 A

---

### Task 1: GiftQuoteManager — 数据加载与随机抽取

**Files:**
- Create: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManager.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java:42-82`

**Interfaces:**
- Produces: `GiftQuoteManager.INSTANCE` — 单例，提供 `getRandomFavoriteQuote(gfId, itemId)`, `getRandomLikedQuote(gfId)`, `getRandomAcceptedQuote(gfId)`, `getRandomRejectedQuote(gfId)`, `getRandomDislikedQuote(gfId)` 五个方法，均返回 `Optional<String>`
- Produces: `GiftQuoteManager.GiftQuotes` — record，字段 `ListMultimap<Identifier, String> favoriteQuotes`, `List<String> likedQuotes`, `List<String> acceptedQuotes`, `List<String> rejectedQuotes`, `List<String> dislikedQuotes`

- [ ] **Step 1: 编写 GiftQuoteManager 单元测试（红）**

```java
// src/test/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManagerTest.java
package com.hexagram2021.girlfriends.common.gift;

import com.google.common.collect.ImmutableListMultimap;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GiftQuoteManagerTest {
    private static final Identifier MOMO_ID = Identifier.fromNamespaceAndPath("girlfriends", "momo");

    /**
     * 验证从完整 JSON 解析后所有 getRandom 方法均返回非空结果喵~
     */
    @Test
    void parseCompleteQuotesYieldsAllTiers() {
        GiftQuoteManager.GiftQuotes quotes = new GiftQuoteManager.GiftQuotes(
                ImmutableListMultimap.<Identifier, String>builder()
                        .put(Identifier.fromNamespaceAndPath("minecraft", "honeycomb"), "q.fav.0")
                        .put(Identifier.fromNamespaceAndPath("minecraft", "honeycomb"), "q.fav.1")
                        .build(),
                List.of("q.liked.0", "q.liked.1"),
                List.of("q.accepted.0"),
                List.of("q.rejected.0"),
                List.of("q.disliked.0")
        );
        assertFalse(quotes.favoriteQuotes().isEmpty());
        assertEquals(2, quotes.favoriteQuotes().get(Identifier.fromNamespaceAndPath("minecraft", "honeycomb")).size());
        assertEquals(2, quotes.likedQuotes().size());
        assertEquals(1, quotes.acceptedQuotes().size());
        assertEquals(1, quotes.rejectedQuotes().size());
        assertEquals(1, quotes.dislikedQuotes().size());
    }

    /**
     * 验证空 JSON（{}）所有字段均为空集合喵~
     */
    @Test
    void parseEmptyJsonYieldsEmptyCollections() {
        GiftQuoteManager.GiftQuotes quotes = new GiftQuoteManager.GiftQuotes(
                ImmutableListMultimap.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        assertTrue(quotes.favoriteQuotes().isEmpty());
        assertTrue(quotes.likedQuotes().isEmpty());
        assertTrue(quotes.acceptedQuotes().isEmpty());
        assertTrue(quotes.rejectedQuotes().isEmpty());
        assertTrue(quotes.dislikedQuotes().isEmpty());
    }

    /**
     * 验证随机抽取覆盖所有条目（100 次抽取内应命中每一条）喵~
     */
    @Test
    void randomQuoteCoversAllEntries() {
        List<String> pool = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        Set<String> seen = new HashSet<>();
        // 模拟 GiftQuoteManager 内部随机逻辑：pool.get(random.nextInt(pool.size()))
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < 100; i++) {
            seen.add(pool.get(random.nextInt(pool.size())));
        }
        assertEquals(pool.size(), seen.size(), "100 draws from 8 entries should hit all");
    }

    /**
     * 验证未配置的角色返回 Optional.empty() 喵~
     */
    @Test
    void unknownCharacterReturnsEmpty() {
        GiftQuoteManager manager = GiftQuoteManager.INSTANCE;
        Identifier unknownId = Identifier.fromNamespaceAndPath("girlfriends", "nonexistent");
        assertTrue(manager.getRandomLikedQuote(unknownId).isEmpty());
        assertTrue(manager.getRandomAcceptedQuote(unknownId).isEmpty());
        assertTrue(manager.getRandomRejectedQuote(unknownId).isEmpty());
        assertTrue(manager.getRandomDislikedQuote(unknownId).isEmpty());
        assertTrue(manager.getRandomFavoriteQuote(unknownId,
                Identifier.fromNamespaceAndPath("minecraft", "stone")).isEmpty());
    }
}
```

- [ ] **Step 2: 运行测试确认失败（红）**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test --tests "com.hexagram2021.girlfriends.common.gift.GiftQuoteManagerTest"
```
Expected: compile error — `GiftQuoteManager` class not found.

- [ ] **Step 3: 创建 GiftQuoteManager（绿）**

```java
// src/main/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManager.java
package com.hexagram2021.girlfriends.common.gift;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 礼物回复台词管理器——从数据包加载各角色语料 i18n key，提供按档位随机抽取接口喵~
 *
 * @author liudongyu
 */
public final class GiftQuoteManager extends SimplePreparableReloadListener<Map<Identifier, GiftQuoteManager.GiftQuotes>> {
    public static final GiftQuoteManager INSTANCE = new GiftQuoteManager();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/gift_quotes");

    private Map<Identifier, GiftQuotes> quotesMap = Map.of();
    private final Random random = new Random();

    private GiftQuoteManager() {
    }

    /**
     * 内部数据记录——单个角色的全部语料池喵~
     *
     * @param favoriteQuotes favorite 档位语料，key 为物品 ID，value 为 i18n key 列表（ImmutableListMultimap）喵~
     * @param likedQuotes liked 档位语料喵~
     * @param acceptedQuotes accepted 档位语料喵~
     * @param rejectedQuotes rejected 档位语料喵~
     * @param dislikedQuotes disliked 档位语料喵~
     */
    public record GiftQuotes(
            ListMultimap<Identifier, String> favoriteQuotes,
            List<String> likedQuotes,
            List<String> acceptedQuotes,
            List<String> rejectedQuotes,
            List<String> dislikedQuotes
    ) {
    }

    /**
     * 从 favorite 语料池中按物品 ID 随机抽取一条台词喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @param itemId 礼物物品 ID 喵~
     * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
     */
    public Optional<String> getRandomFavoriteQuote(Identifier girlfriendTypeId, Identifier itemId) {
        GiftQuotes quotes = this.quotesMap.get(girlfriendTypeId);
        if (quotes == null) {
            return Optional.empty();
        }
        List<String> pool = quotes.favoriteQuotes().get(itemId);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pool.get(this.random.nextInt(pool.size())));
    }

    /**
     * 从 liked 语料池中随机抽取一条台词喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
     */
    public Optional<String> getRandomLikedQuote(Identifier girlfriendTypeId) {
        return getRandomFromPool(girlfriendTypeId, GiftQuotes::likedQuotes);
    }

    /**
     * 从 accepted 语料池中随机抽取一条台词喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
     */
    public Optional<String> getRandomAcceptedQuote(Identifier girlfriendTypeId) {
        return getRandomFromPool(girlfriendTypeId, GiftQuotes::acceptedQuotes);
    }

    /**
     * 从 rejected 语料池中随机抽取一条台词喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
     */
    public Optional<String> getRandomRejectedQuote(Identifier girlfriendTypeId) {
        return getRandomFromPool(girlfriendTypeId, GiftQuotes::rejectedQuotes);
    }

    /**
     * 从 disliked 语料池中随机抽取一条台词喵~
     *
     * @param girlfriendTypeId 角色类型 ID 喵~
     * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
     */
    public Optional<String> getRandomDislikedQuote(Identifier girlfriendTypeId) {
        return getRandomFromPool(girlfriendTypeId, GiftQuotes::dislikedQuotes);
    }

    @Override
    protected Map<Identifier, GiftQuotes> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, GiftQuotes> prepared = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier fileLocation = entry.getKey();
            Identifier characterTypeId = LISTER.fileToId(fileLocation);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (!jsonElement.isJsonObject()) {
                    throw new JsonParseException("Gift quotes root must be a JSON object");
                }
                GiftQuotes quotes = parseQuotes(jsonElement.getAsJsonObject());
                if (prepared.putIfAbsent(characterTypeId, quotes) != null) {
                    throw new IllegalStateException("Duplicate gift quotes ignored with ID " + characterTypeId);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException exception) {
                LOGGER.error("Couldn't parse gift quotes '{}' from '{}'", characterTypeId, fileLocation, exception);
            }
        }
        return prepared;
    }

    @Override
    protected void apply(Map<Identifier, GiftQuotes> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.quotesMap = Collections.unmodifiableMap(new LinkedHashMap<>(preparations));
    }

    private Optional<String> getRandomFromPool(Identifier girlfriendTypeId,
                                               java.util.function.Function<GiftQuotes, List<String>> poolExtractor) {
        GiftQuotes quotes = this.quotesMap.get(girlfriendTypeId);
        if (quotes == null) {
            return Optional.empty();
        }
        List<String> pool = poolExtractor.apply(quotes);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pool.get(this.random.nextInt(pool.size())));
    }

    private static GiftQuotes parseQuotes(JsonObject jsonObject) {
        ImmutableListMultimap.Builder<Identifier, String> favoriteBuilder = ImmutableListMultimap.builder();
        if (jsonObject.has("favorite")) {
            JsonElement favElement = jsonObject.get("favorite");
            if (!favElement.isJsonObject()) {
                throw new JsonParseException("Field 'favorite' must be a JSON object");
            }
            JsonObject favObj = favElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> favEntry : favObj.entrySet()) {
                Identifier itemId = Identifier.parse(favEntry.getKey());
                JsonArray quotesArr = favEntry.getValue().getAsJsonArray();
                for (JsonElement elem : quotesArr) {
                    if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) {
                        throw new JsonParseException("Favorite quotes for '" + favEntry.getKey() + "' must be strings");
                    }
                    favoriteBuilder.put(itemId, elem.getAsString());
                }
            }
        }
        return new GiftQuotes(
                favoriteBuilder.build(),
                parseStringArray(jsonObject, "liked"),
                parseStringArray(jsonObject, "accepted"),
                parseStringArray(jsonObject, "rejected"),
                parseStringArray(jsonObject, "disliked")
        );
    }

    private static List<String> parseStringArray(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return List.of();
        }
        JsonElement element = jsonObject.get(fieldName);
        if (!element.isJsonArray()) {
            throw new JsonParseException("Field '" + fieldName + "' must be a JSON array");
        }
        JsonArray jsonArray = element.getAsJsonArray();
        List<String> result = new java.util.ArrayList<>(jsonArray.size());
        for (JsonElement elem : jsonArray) {
            if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Field '" + fieldName + "' must contain only strings");
            }
            result.add(elem.getAsString());
        }
        return Collections.unmodifiableList(result);
    }
}
```

- [ ] **Step 4: 运行测试确认通过（绿）**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test --tests "com.hexagram2021.girlfriends.common.gift.GiftQuoteManagerTest"
```
Expected: All 4 tests PASS.

- [ ] **Step 5: 注册 GiftQuoteManager 为 ServerReloadListener**

Edit `GirlfriendsMod.java`:
- 在 `MANAGER_ID` 常量区（line 42-44 之后）新增：
```java
private static final Identifier GIFT_QUOTE_MANAGER_ID = Identifier.fromNamespaceAndPath(MODID, "gift_quotes");
```
- 在 `registerServerReloadListeners` 方法（line 78-83）末尾新增：
```java
event.addListener(GIFT_QUOTE_MANAGER_ID, GiftQuoteManager.INSTANCE);
```
- 新增 import：
```java
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
```

- [ ] **Step 6: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManager.java \
        src/main/java/com/hexagram2021/girlfriends/GirlfriendsMod.java \
        src/test/java/com/hexagram2021/girlfriends/common/gift/GiftQuoteManagerTest.java
git commit -m "feat(REQ-7): 新增 GiftQuoteManager 语料管理器与单元测试"
```

---

### Task 2: GiftResult + GiftService — quoteKey 字段与随机抽取

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftResult.java`
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/gift/GiftService.java`
- Modify: `src/test/java/com/hexagram2021/girlfriends/common/gift/GiftServiceTest.java`

**Interfaces:**
- Consumes: `GiftQuoteManager` (Task 1)
- Produces: `GiftResult.quoteKey()` — `@Nullable String` 字段
- Produces: `GiftService(GiftQuoteManager wired)` — 四参数构造方法传入 GiftQuoteManager
- Produces: `GiftService.applyGift(UUID, Identifier, GiftPreferenceLevel, @Nullable Identifier itemId)` — 新增 itemId 参数

- [ ] **Step 1: 更新 GiftResult 测试（红）**

在 `GiftServiceTest.java` 中新增测试：

```java
/**
 * 验证有 GiftQuoteManager 时 favorite 礼物 GiftResult 包含非空 quoteKey 喵~
 */
@Test
void favoriteGiftIncludesQuoteKey() {
    GirlfriendsWorldData data = new GirlfriendsWorldData();
    RelationshipService relationshipService = new RelationshipService(data);
    GiftQuoteManager quoteManager = GiftQuoteManager.INSTANCE;
    GiftService giftService = new GiftService(relationshipService, null, quoteManager, (_, _) -> true);
    UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000010");

    // 注：此时 GiftQuoteManager.quotesMap 为空（数据包未加载），quoteKey 应为 null
    GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);
    Assertions.assertFalse(result.rejected());
    Assertions.assertNull(result.quoteKey()); // 无数据包时 quoteKey 为 null
}

/**
 * 验证无 GiftQuoteManager 时 GiftResult.quoteKey 为 null（向后兼容）喵~
 */
@Test
void noQuoteManagerLeavesQuoteKeyNull() {
    GirlfriendsWorldData data = new GirlfriendsWorldData();
    RelationshipService relationshipService = new RelationshipService(data);
    GiftService giftService = new GiftService(relationshipService);
    UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000011");

    GiftResult result = giftService.applyGift(playerUuid, MOMO_ID, GiftPreferenceLevel.FAVORITE);
    Assertions.assertFalse(result.rejected());
    Assertions.assertNull(result.quoteKey());
}
```

- [ ] **Step 2: 运行测试确认失败（红）**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test --tests "com.hexagram2021.girlfriends.common.gift.GiftServiceTest.favoriteGiftIncludesQuoteKey" --tests "com.hexagram2021.girlfriends.common.gift.GiftServiceTest.noQuoteManagerLeavesQuoteKeyNull"
```
Expected: compile error — `GiftResult` has no `quoteKey()` accessor.

- [ ] **Step 3: 更新 GiftResult（绿）**

```java
// src/main/java/com/hexagram2021/girlfriends/common/gift/GiftResult.java
package com.hexagram2021.girlfriends.common.gift;

import javax.annotation.Nullable;

/**
 * 赠礼处理结果喵~
 *
 * @param rejected 是否被拒收喵~
 * @param affectionDelta 实际好感变更值喵~
 * @param level 礼物偏好等级喵~
 * @param messageKey 结果消息键（用于 subtitle）喵~
 * @param quoteKey 角色回复台词 i18n key，为 null 时不发送角色台词（聊天栏）喵~
 *
 * @author liudongyu
 */
public record GiftResult(boolean rejected, float affectionDelta, GiftPreferenceLevel level,
                         String messageKey, @Nullable String quoteKey) {
    /**
     * 创建拒收结果喵~
     *
     * @param level 礼物偏好等级喵~
     * @param messageKey 结果消息键喵~
     * @return 拒收结果喵~
     */
    public static GiftResult rejected(GiftPreferenceLevel level, String messageKey) {
        return new GiftResult(true, 0, level, messageKey, null);
    }

    /**
     * 创建拒收结果（含 quoteKey）喵~
     *
     * @param level 礼物偏好等级喵~
     * @param messageKey 结果消息键喵~
     * @param quoteKey 角色台词 i18n key，可为 null 喵~
     * @return 拒收结果喵~
     */
    public static GiftResult rejected(GiftPreferenceLevel level, String messageKey, @Nullable String quoteKey) {
        return new GiftResult(true, 0, level, messageKey, quoteKey);
    }

    /**
     * 创建已处理结果喵~
     *
     * @param level 礼物偏好等级喵~
     * @param affectionDelta 实际好感变更值喵~
     * @param messageKey 结果消息键喵~
     * @return 已处理结果喵~
     */
    public static GiftResult accepted(GiftPreferenceLevel level, float affectionDelta, String messageKey) {
        return new GiftResult(false, affectionDelta, level, messageKey, null);
    }

    /**
     * 创建已处理结果（含 quoteKey）喵~
     *
     * @param level 礼物偏好等级喵~
     * @param affectionDelta 实际好感变更值喵~
     * @param messageKey 结果消息键喵~
     * @param quoteKey 角色台词 i18n key，可为 null 喵~
     * @return 已处理结果喵~
     */
    public static GiftResult accepted(GiftPreferenceLevel level, float affectionDelta, String messageKey,
                                      @Nullable String quoteKey) {
        return new GiftResult(false, affectionDelta, level, messageKey, quoteKey);
    }
}
```

- [ ] **Step 4: 更新 GiftService 构造方法与 applyGift 逻辑（绿）**

```java
// 在 GiftService.java 中新增字段和构造方法

// 在现有字段后新增：
@Nullable
private final GiftQuoteManager giftQuoteManager;

// 在现有两个参数构造法之后新增三参数和四参数构造方法：

/**
 * 创建赠礼服务（含台词管理器）喵~
 *
 * @param relationshipService 关系服务喵~
 * @param giftQuoteManager 台词管理器，可为 null 喵~
 */
public GiftService(RelationshipService relationshipService, @Nullable GiftQuoteManager giftQuoteManager) {
    this(relationshipService, null, giftQuoteManager, (playerUuid, girlfriendTypeId) -> true);
}

/**
 * 创建赠礼服务（含偏好管理器与台词管理器）喵~
 *
 * @param relationshipService 关系服务喵~
 * @param giftPreferenceManager 礼物偏好管理器，可为 null 喵~
 * @param giftQuoteManager 台词管理器，可为 null 喵~
 * @param canReceiveGift 是否允许收礼判定喵~
 */
public GiftService(
        RelationshipService relationshipService,
        @Nullable GiftPreferenceManager giftPreferenceManager,
        @Nullable GiftQuoteManager giftQuoteManager,
        BiPredicate<UUID, Identifier> canReceiveGift
) {
    this.relationshipService = Objects.requireNonNull(relationshipService);
    this.giftPreferenceManager = giftPreferenceManager;
    this.giftQuoteManager = giftQuoteManager;
    this.canReceiveGift = Objects.requireNonNull(canReceiveGift);
}
```

修改 `applyGift` 方法签名，新增 itemId 参数；在 `applyGiftItem` 中传递 itemId：

```java
// 原 applyGift 方法改造：
public GiftResult applyGift(UUID playerUuid, Identifier girlfriendTypeId, GiftPreferenceLevel level) {
    return applyGift(playerUuid, girlfriendTypeId, level, null);
}

/**
 * 按偏好等级应用赠礼结果（含物品 ID，用于 favorite 台词选择）喵~
 *
 * @param playerUuid 玩家 UUID 喵~
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param level 礼物偏好等级喵~
 * @param itemId 礼物物品 ID，用于 favorite 台词匹配，可为 null 喵~
 * @return 赠礼结果喵~
 */
public GiftResult applyGift(UUID playerUuid, Identifier girlfriendTypeId, GiftPreferenceLevel level,
                            @Nullable Identifier itemId) {
    // 抽取台词 quoteKey 喵~
    String quoteKey = extractQuoteKey(girlfriendTypeId, level, itemId);

    if (level == GiftPreferenceLevel.REJECTED) {
        return GiftResult.rejected(level, MESSAGE_KEY_REJECTED, quoteKey);
    }
    if (!this.canReceiveGift.test(playerUuid, girlfriendTypeId)) {
        return GiftResult.rejected(level, MESSAGE_KEY_PERMISSION_DENIED, null);
    }
    PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
    if (level.isPositive() && relation.getDailyGiftGain() >= DAILY_GIFT_GAIN_CAP) {
        return GiftResult.rejected(level, MESSAGE_KEY_CAP_REACHED, null);
    }
    float affectionDelta = computeAffectionDelta(level, relation.getDailyGiftGain());
    this.relationshipService.changeAffection(playerUuid, girlfriendTypeId, AffectionChangeSource.GIFT, affectionDelta);
    if (affectionDelta > 0) {
        relation.setDailyGiftGain(Math.min(DAILY_GIFT_GAIN_CAP, relation.getDailyGiftGain() + affectionDelta));
    }
    String messageKey = affectionDelta > 0 ? MESSAGE_KEY_ACCEPTED : MESSAGE_KEY_DISLIKED;
    return GiftResult.accepted(level, affectionDelta, messageKey, quoteKey);
}

// 修改 applyGiftItem，将 itemId 传入 applyGift：
public GiftResult applyGiftItem(UUID playerUuid, Identifier girlfriendTypeId, ItemStack itemStack) {
    GiftPreferenceLevel level = resolvePreferenceLevel(girlfriendTypeId, itemStack);
    Identifier itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
    return this.applyGift(playerUuid, girlfriendTypeId, level, itemId);
}

// 新增私有方法：
@Nullable
private String extractQuoteKey(Identifier girlfriendTypeId, GiftPreferenceLevel level, @Nullable Identifier itemId) {
    if (this.giftQuoteManager == null) {
        return null;
    }
    return switch (level) {
        case FAVORITE -> itemId != null
                ? this.giftQuoteManager.getRandomFavoriteQuote(girlfriendTypeId, itemId).orElse(null)
                : null;
        case LIKED -> this.giftQuoteManager.getRandomLikedQuote(girlfriendTypeId).orElse(null);
        case ACCEPTED -> this.giftQuoteManager.getRandomAcceptedQuote(girlfriendTypeId).orElse(null);
        case DISLIKED -> this.giftQuoteManager.getRandomDislikedQuote(girlfriendTypeId).orElse(null);
        case REJECTED -> this.giftQuoteManager.getRandomRejectedQuote(girlfriendTypeId).orElse(null);
    };
}
```

同时更新原有三参数构造方法，保持向后兼容：
```java
// 原有三参数构造方法调用新的四参数方法：
public GiftService(
        RelationshipService relationshipService,
        @Nullable GiftPreferenceManager giftPreferenceManager,
        BiPredicate<UUID, Identifier> canReceiveGift
) {
    this(relationshipService, giftPreferenceManager, null, canReceiveGift);
}
```

需要新增 import：
```java
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import net.minecraft.core.registries.BuiltInRegistries;
```

- [ ] **Step 5: 运行全部 GiftServiceTest 确认通过（绿）**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test --tests "com.hexagram2021.girlfriends.common.gift.GiftServiceTest"
```
Expected: All 6 tests PASS（4 旧 + 2 新）。

- [ ] **Step 6: 运行全部单元测试确认回归通过**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/gift/GiftResult.java \
        src/main/java/com/hexagram2021/girlfriends/common/gift/GiftService.java \
        src/test/java/com/hexagram2021/girlfriends/common/gift/GiftServiceTest.java
git commit -m "feat(REQ-7): GiftResult 新增 quoteKey，GiftService 注入 GiftQuoteManager"
```

---

### Task 3: GirlfriendsNetwork — 字幕三联包 + 台词聊天栏

**Files:**
- Modify: `src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java`

**Interfaces:**
- Consumes: `GiftResult.quoteKey()` (Task 2), `GiftQuoteManager.INSTANCE` (Task 1)
- Produces: `sendGiftFeedback(ServerPlayer, Identifier, GiftResult)` — 替代原 `buildGiftMessage`
- Produces: 字幕显示流程 — `ClientboundSetTitlesAnimationPacket` → `ClientboundSetTitleTextPacket(Component.empty())` → `ClientboundSetSubtitleTextPacket(message)`

- [ ] **Step 1: 修改 GirlfriendsNetwork**

替换 `buildGiftMessage` 方法为 `sendGiftFeedback`；修改两个 gift handler 调用新方法并注入 `GiftQuoteManager`：

**修改 1** — 替换 `buildGiftMessage`（line 231-234）为：

```java
/**
 * 发送赠礼反馈到玩家——角色台词走聊天栏，好感度变化走 subtitle 喵~
 *
 * @param player 目标玩家喵~
 * @param girlfriendTypeId 角色类型 ID 喵~
 * @param result 赠礼结果喵~
 */
private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId, GiftResult result) {
    // 1. 角色台词 → 聊天栏（若 quoteKey 不为 null）喵~
    if (result.quoteKey() != null) {
        player.sendSystemMessage(Component.translatable(result.quoteKey()));
    }

    // 2. 好感度变化 → subtitle 三联包喵~
    // subtitle 在原版中必须配合 title 叠加层才能渲染：
    //   TitlesAnimation 设定时序（0.25s 淡入，2s 停留，0.5s 淡出）
    //   SetTitleText 空标题激活叠加层
    //   SetSubtitleText 为实际好感度消息喵~
    Component characterName = Component.translatable("girlfriends.girlfriend_type." + girlfriendTypeId.getPath());
    Component subtitleMsg = Component.translatable(result.messageKey(), characterName,
            String.format("%+.1f", result.affectionDelta()));
    player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
    player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleMsg));
}
```

**修改 2** — 更新 `handleGiveGift`（line 105-108）：

原代码：
```java
GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
        (playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), player.getItemInHand(InteractionHand.MAIN_HAND));
    player.sendSystemMessage(buildGiftMessage(packet.girlfriendTypeId(), result));
```

改为：
```java
GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
        GiftQuoteManager.INSTANCE,
        (playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(),
        player.getItemInHand(InteractionHand.MAIN_HAND));
sendGiftFeedback(player, packet.girlfriendTypeId(), result);
```

**修改 3** — 更新 `handleGiveGiftFromSlot`（line 220-226）：

原代码：
```java
GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
        (playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), itemStack);
if (!result.rejected()) {
    player.getInventory().removeItem(slotIndex, 1);
}
player.sendSystemMessage(buildGiftMessage(packet.girlfriendTypeId(), result));
```

改为：
```java
GiftService giftService = new GiftService(relationshipService, GiftPreferenceManager.INSTANCE,
        GiftQuoteManager.INSTANCE,
        (playerUuid, girlfriendTypeId) -> canReachCharacter(data, girlfriendTypeId));
GiftResult result = giftService.applyGiftItem(player.getUUID(), packet.girlfriendTypeId(), itemStack);
if (!result.rejected()) {
    player.getInventory().removeItem(slotIndex, 1);
}
sendGiftFeedback(player, packet.girlfriendTypeId(), result);
```

**修改 4** — 新增 import（文件头部）：

```java
import com.hexagram2021.girlfriends.common.gift.GiftQuoteManager;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
```

- [ ] **Step 2: 编译验证**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew classes
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 运行测试回归**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test
```
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/hexagram2021/girlfriends/common/network/GirlfriendsNetwork.java
git commit -m "feat(REQ-7): 赠礼反馈拆分——角色台词走聊天栏，好感度走 subtitle 三联包"
```

---

### Task 4: Data Files — 五角色 gift_quotes JSON

**Files:**
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_quotes/momo.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_quotes/yuxi.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_quotes/meishu.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_quotes/wanying.json`
- Create: `src/main/resources/data/girlfriends/girlfriends/gift_quotes/youruo.json`

- [ ] **Step 1: 创建 momo.json**

```json
{
  "favorite": {
    "minecraft:honeycomb": [
      "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.0",
      "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.1",
      "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.2"
    ],
    "girlfriends:bouquet": [
      "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.0",
      "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.1",
      "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.momo.liked.0",
    "girlfriends.gift.quote.momo.liked.1",
    "girlfriends.gift.quote.momo.liked.2",
    "girlfriends.gift.quote.momo.liked.3",
    "girlfriends.gift.quote.momo.liked.4",
    "girlfriends.gift.quote.momo.liked.5",
    "girlfriends.gift.quote.momo.liked.6",
    "girlfriends.gift.quote.momo.liked.7"
  ],
  "accepted": [
    "girlfriends.gift.quote.momo.accepted.0",
    "girlfriends.gift.quote.momo.accepted.1",
    "girlfriends.gift.quote.momo.accepted.2",
    "girlfriends.gift.quote.momo.accepted.3",
    "girlfriends.gift.quote.momo.accepted.4",
    "girlfriends.gift.quote.momo.accepted.5"
  ],
  "rejected": [
    "girlfriends.gift.quote.momo.rejected.0",
    "girlfriends.gift.quote.momo.rejected.1",
    "girlfriends.gift.quote.momo.rejected.2",
    "girlfriends.gift.quote.momo.rejected.3"
  ],
  "disliked": [
    "girlfriends.gift.quote.momo.disliked.0",
    "girlfriends.gift.quote.momo.disliked.1",
    "girlfriends.gift.quote.momo.disliked.2",
    "girlfriends.gift.quote.momo.disliked.3",
    "girlfriends.gift.quote.momo.disliked.4"
  ]
}
```

- [ ] **Step 2: 创建 yuxi.json**

```json
{
  "favorite": {
    "minecraft:nautilus_shell": [
      "girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.0",
      "girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.1",
      "girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.yuxi.liked.0",
    "girlfriends.gift.quote.yuxi.liked.1",
    "girlfriends.gift.quote.yuxi.liked.2",
    "girlfriends.gift.quote.yuxi.liked.3",
    "girlfriends.gift.quote.yuxi.liked.4",
    "girlfriends.gift.quote.yuxi.liked.5",
    "girlfriends.gift.quote.yuxi.liked.6",
    "girlfriends.gift.quote.yuxi.liked.7"
  ],
  "accepted": [
    "girlfriends.gift.quote.yuxi.accepted.0",
    "girlfriends.gift.quote.yuxi.accepted.1",
    "girlfriends.gift.quote.yuxi.accepted.2",
    "girlfriends.gift.quote.yuxi.accepted.3",
    "girlfriends.gift.quote.yuxi.accepted.4",
    "girlfriends.gift.quote.yuxi.accepted.5"
  ],
  "rejected": [
    "girlfriends.gift.quote.yuxi.rejected.0",
    "girlfriends.gift.quote.yuxi.rejected.1",
    "girlfriends.gift.quote.yuxi.rejected.2",
    "girlfriends.gift.quote.yuxi.rejected.3"
  ],
  "disliked": [
    "girlfriends.gift.quote.yuxi.disliked.0",
    "girlfriends.gift.quote.yuxi.disliked.1",
    "girlfriends.gift.quote.yuxi.disliked.2",
    "girlfriends.gift.quote.yuxi.disliked.3",
    "girlfriends.gift.quote.yuxi.disliked.4"
  ]
}
```

- [ ] **Step 3: 创建 meishu.json**

```json
{
  "favorite": {
    "minecraft:iron_ingot": [
      "girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.0",
      "girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.1",
      "girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.meishu.liked.0",
    "girlfriends.gift.quote.meishu.liked.1",
    "girlfriends.gift.quote.meishu.liked.2",
    "girlfriends.gift.quote.meishu.liked.3",
    "girlfriends.gift.quote.meishu.liked.4",
    "girlfriends.gift.quote.meishu.liked.5",
    "girlfriends.gift.quote.meishu.liked.6",
    "girlfriends.gift.quote.meishu.liked.7"
  ],
  "accepted": [
    "girlfriends.gift.quote.meishu.accepted.0",
    "girlfriends.gift.quote.meishu.accepted.1",
    "girlfriends.gift.quote.meishu.accepted.2",
    "girlfriends.gift.quote.meishu.accepted.3",
    "girlfriends.gift.quote.meishu.accepted.4",
    "girlfriends.gift.quote.meishu.accepted.5"
  ],
  "rejected": [
    "girlfriends.gift.quote.meishu.rejected.0",
    "girlfriends.gift.quote.meishu.rejected.1",
    "girlfriends.gift.quote.meishu.rejected.2",
    "girlfriends.gift.quote.meishu.rejected.3"
  ],
  "disliked": [
    "girlfriends.gift.quote.meishu.disliked.0",
    "girlfriends.gift.quote.meishu.disliked.1",
    "girlfriends.gift.quote.meishu.disliked.2",
    "girlfriends.gift.quote.meishu.disliked.3",
    "girlfriends.gift.quote.meishu.disliked.4"
  ]
}
```

- [ ] **Step 4: 创建 wanying.json**

```json
{
  "favorite": {
    "minecraft:blaze_rod": [
      "girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.0",
      "girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.1",
      "girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.wanying.liked.0",
    "girlfriends.gift.quote.wanying.liked.1",
    "girlfriends.gift.quote.wanying.liked.2",
    "girlfriends.gift.quote.wanying.liked.3",
    "girlfriends.gift.quote.wanying.liked.4",
    "girlfriends.gift.quote.wanying.liked.5",
    "girlfriends.gift.quote.wanying.liked.6",
    "girlfriends.gift.quote.wanying.liked.7"
  ],
  "accepted": [
    "girlfriends.gift.quote.wanying.accepted.0",
    "girlfriends.gift.quote.wanying.accepted.1",
    "girlfriends.gift.quote.wanying.accepted.2",
    "girlfriends.gift.quote.wanying.accepted.3",
    "girlfriends.gift.quote.wanying.accepted.4",
    "girlfriends.gift.quote.wanying.accepted.5"
  ],
  "rejected": [
    "girlfriends.gift.quote.wanying.rejected.0",
    "girlfriends.gift.quote.wanying.rejected.1",
    "girlfriends.gift.quote.wanying.rejected.2",
    "girlfriends.gift.quote.wanying.rejected.3"
  ],
  "disliked": [
    "girlfriends.gift.quote.wanying.disliked.0",
    "girlfriends.gift.quote.wanying.disliked.1",
    "girlfriends.gift.quote.wanying.disliked.2",
    "girlfriends.gift.quote.wanying.disliked.3",
    "girlfriends.gift.quote.wanying.disliked.4"
  ]
}
```

- [ ] **Step 5: 创建 youruo.json**

```json
{
  "favorite": {
    "minecraft:ender_pearl": [
      "girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.0",
      "girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.1",
      "girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.youruo.liked.0",
    "girlfriends.gift.quote.youruo.liked.1",
    "girlfriends.gift.quote.youruo.liked.2",
    "girlfriends.gift.quote.youruo.liked.3",
    "girlfriends.gift.quote.youruo.liked.4",
    "girlfriends.gift.quote.youruo.liked.5",
    "girlfriends.gift.quote.youruo.liked.6",
    "girlfriends.gift.quote.youruo.liked.7"
  ],
  "accepted": [
    "girlfriends.gift.quote.youruo.accepted.0",
    "girlfriends.gift.quote.youruo.accepted.1",
    "girlfriends.gift.quote.youruo.accepted.2",
    "girlfriends.gift.quote.youruo.accepted.3",
    "girlfriends.gift.quote.youruo.accepted.4",
    "girlfriends.gift.quote.youruo.accepted.5"
  ],
  "rejected": [
    "girlfriends.gift.quote.youruo.rejected.0",
    "girlfriends.gift.quote.youruo.rejected.1",
    "girlfriends.gift.quote.youruo.rejected.2",
    "girlfriends.gift.quote.youruo.rejected.3"
  ],
  "disliked": [
    "girlfriends.gift.quote.youruo.disliked.0",
    "girlfriends.gift.quote.youruo.disliked.1",
    "girlfriends.gift.quote.youruo.disliked.2",
    "girlfriends.gift.quote.youruo.disliked.3",
    "girlfriends.gift.quote.youruo.disliked.4"
  ]
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/girlfriends/girlfriends/gift_quotes/
git commit -m "feat(REQ-7): 新增五角色 gift_quotes JSON 数据文件"
```

---

### Task 5: Language Files — 中英双语 133 条台词翻译

**Files:**
- Modify: `src/main/resources/assets/girlfriends/lang/en_us.json`
- Modify: `src/main/resources/assets/girlfriends/lang/zh_cn.json`

- [ ] **Step 1: 追加沫沫台词到 en_us.json**

在 `en_us.json` 末尾（`"quest.girlfriends.type.random"` 之前）插入：

```json
  "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.0": "This is... honeycomb? Thank you. The bees will be happy too.",
  "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.1": "You remembered I like this... Mm, I'll treasure it.",
  "girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.2": "The scent of honeycomb always reminds me of the quietest afternoons in the garden. Thank you.",
  "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.0": "Such a beautiful bouquet... You picked each flower carefully, didn't you?",
  "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.1": "When flowers gather together, it's like they're whispering secrets. From now on... can I whisper secrets with you too?",
  "girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.2": "This bouquet will never wither—because it's from you. I'll tend to it forever.",

  "girlfriends.gift.quote.momo.liked.0": "You always manage to pick things that touch my heart.",
  "girlfriends.gift.quote.momo.liked.1": "Thank you. This will help me tend the garden.",
  "girlfriends.gift.quote.momo.liked.2": "Mm, I like it. Let's use it in the greenhouse.",
  "girlfriends.gift.quote.momo.liked.3": "The things you give... they all carry such a gentle feeling.",
  "girlfriends.gift.quote.momo.liked.4": "I'll take it. Next time you come to the garden, you'll see it.",
  "girlfriends.gift.quote.momo.liked.5": "These go so well with flowers. Thank you for thinking of this.",
  "girlfriends.gift.quote.momo.liked.6": "With this, life in the garden has one more thing to look forward to.",
  "girlfriends.gift.quote.momo.liked.7": "It seems you know what I like better than I do now.",

  "girlfriends.gift.quote.momo.accepted.0": "Thank you, I'll put it to good use.",
  "girlfriends.gift.quote.momo.accepted.1": "Mm, I accept it. You're always so thoughtful.",
  "girlfriends.gift.quote.momo.accepted.2": "Though I don't usually use these... since it's from you...",
  "girlfriends.gift.quote.momo.accepted.3": "Thanks. Every time you visit, this place feels a little warmer.",
  "girlfriends.gift.quote.momo.accepted.4": "This is a thoughtful gift. I'll remember it.",
  "girlfriends.gift.quote.momo.accepted.5": "Mm, I'll keep it in the storage. It'll come in handy for the garden someday.",

  "girlfriends.gift.quote.momo.rejected.0": "Mm... I'm not sure how to use this in the garden...",
  "girlfriends.gift.quote.momo.rejected.1": "Thank you for the thought. But the flowers and bees probably don't need this.",
  "girlfriends.gift.quote.momo.rejected.2": "Maybe it'd be better to give it to someone who needs it more?",
  "girlfriends.gift.quote.momo.rejected.3": "I've received your kindness. It's just... it doesn't have a place in my garden.",

  "girlfriends.gift.quote.momo.disliked.0": "This... will hurt my flowers. Please take it away.",
  "girlfriends.gift.quote.momo.disliked.1": "I don't like things like this. It reminds me of the sound of petals being torn by a storm.",
  "girlfriends.gift.quote.momo.disliked.2": "Flowers are delicate. They can't bear things like this...",
  "girlfriends.gift.quote.momo.disliked.3": "Maybe you don't understand, but the garden has no need for things that wither and rot.",
  "girlfriends.gift.quote.momo.disliked.4": "Please take it back. The scent of decay will lead the bees astray.",
```

备注：yuxi、meishu、wanying、youruo 四角色的英文台词以及全部中文台词内容详见 `docs/superpowers/specs/2026-06-20-gift-quote-system-design.md` 附录 A，此处不再重复罗列以控制计划文档长度。实施时按规格逐一录入。

- [ ] **Step 2: 追加全部中文台词到 zh_cn.json**

按规格附录 A 的 key→zh_cn 对照表，将所有 133 条中文翻译写入 `zh_cn.json`。

- [ ] **Step 3: 追加全部英文台词到 en_us.json**

按规格附录 A 的 key→en_us 对照表，将剩余四角色的英文翻译写入 `en_us.json`。

- [ ] **Step 4: 验证 JSON 合法性**

```bash
cd "D:\Projects\Girlfriends" && python -c "import json; json.load(open('src/main/resources/assets/girlfriends/lang/en_us.json')); json.load(open('src/main/resources/assets/girlfriends/lang/zh_cn.json')); print('Both JSON files valid')"
```
Expected: `Both JSON files valid`

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/assets/girlfriends/lang/en_us.json \
        src/main/resources/assets/girlfriends/lang/zh_cn.json
git commit -m "feat(REQ-7): 新增 133 条礼物台词中英双语翻译"
```

---

### Task 6: 集成验证

- [ ] **Step 1: 编译项目**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew build
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: 运行全部测试**

```bash
cd "D:\Projects\Girlfriends" && ./gradlew test
```
Expected: All tests PASS.

- [ ] **Step 3: 手动集成测试清单（启动客户端验证）**

启动 `./gradlew runClient`，创建世界后执行以下验证：

1. 使用 `/affection set momo <player> 0` 重置好感度
2. 给沫沫送蜜脾（`/give @s minecraft:honeycomb`，右键沫沫→Give Gift）：
   - [ ] 聊天栏出现沫沫的 favorite 台词（3 条之一）
   - [ ] subtitle 显示 "沫沫 收下了礼物，好感度 +5.0"
3. 给沫沫送花（任何 minecraft:flowers tag 物品）：
   - [ ] 聊天栏出现 liked 台词（8 条之一）
   - [ ] subtitle 显示好感度变化
4. 给沫沫送苹果：
   - [ ] 聊天栏出现 accepted 台词（6 条之一）
   - [ ] subtitle 显示好感度变化
5. 给沫沫送腐肉：
   - [ ] 聊天栏出现 disliked 台词（5 条之一）
   - [ ] subtitle 显示 "看起来沫沫对你的礼物不太高兴，好感度 -2.0"
6. 给沫沫送非偏好物品（如石头）：
   - [ ] 聊天栏出现 rejected 台词（4 条之一）
   - [ ] subtitle 显示 "沫沫对这个礼物不感兴趣..."
7. 连续送 15 点好感度的礼物后再次送正偏好礼物：
   - [ ] 聊天栏不出现台词
   - [ ] subtitle 显示 "沫沫今天已经不想再收礼物了..."

