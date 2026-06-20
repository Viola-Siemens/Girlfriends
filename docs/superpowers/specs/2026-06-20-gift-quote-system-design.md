# 礼物回复台词系统设计规格

> 日期: 2026-06-20 | 状态: 设计完成 | 关联需求: REQ-7

## 1. 概述

为《Girlfriends》模组的礼物系统增加角色回复台词功能。角色收到礼物后，根据礼物档位从对应的语料池中随机选取一条台词发送到聊天栏；好感度变化信息则从聊天栏移至 subtitle（屏幕副标题）显示。

五位角色各有符合其性格（参考 `docs/v0.1.0/GDD/`）的独白台词，支持中英双语。

## 2. 核心数据

### 2.1 礼物档位与语料数量

| 档位 | 代码枚举 | 每角色语料数 | 是否区分物品 |
|------|----------|-------------|-------------|
| Favorite（最爱） | `FAVORITE` | 3 条/物品 | 是，每个物品独立语料池 |
| Liked（喜欢） | `LIKED` | 8 条 | 否，全档位共享 |
| Accepted（接受） | `ACCEPTED` | 6 条 | 否 |
| Rejected（不感兴趣） | `REJECTED` | 4 条 | 否 |
| Disliked（厌恶） | `DISLIKED` | 5 条 | 否 |

**特殊处理**——REJECTED 档位在代码中涵盖三种场景，分开处理：
- 物品不在任何偏好列表中（"不感兴趣"）：使用 4 条 rejected 语料池，台词发送到聊天栏
- 每日赠礼好感已达上限（`daily_cap_reached`）：**不发送台词**，仅 subtitle 显示提示
- 权限校验失败（`permission_denied`）：**不发送台词**，仅 subtitle 显示提示

### 2.2 各角色 Favorite 物品

| 角色 | Favorite 物品 | 语料条数 |
|------|--------------|---------|
| 沫沫 (momo) | `minecraft:honeycomb`、`girlfriends:bouquet` | 3×2=6 |
| 渔溪 (yuxi) | `minecraft:nautilus_shell` | 3×1=3 |
| 梅疏 (meishu) | `minecraft:iron_ingot` | 3×1=3 |
| 晚萤 (wanying) | `minecraft:blaze_rod` | 3×1=3 |
| 幽若 (youruo) | `minecraft:ender_pearl` | 3×1=3 |

### 2.3 各角色语料总条目

| 角色 | Favorite | Liked | Accepted | Rejected | Disliked | 合计 |
|------|----------|-------|----------|----------|----------|------|
| 沫沫 | 6 | 8 | 6 | 4 | 5 | 29 |
| 渔溪 | 3 | 8 | 6 | 4 | 5 | 26 |
| 梅疏 | 3 | 8 | 6 | 4 | 5 | 26 |
| 晚萤 | 3 | 8 | 6 | 4 | 5 | 26 |
| 幽若 | 3 | 8 | 6 | 4 | 5 | 26 |
| **总计** | | | | | | **133** |

## 3. 架构设计

### 3.1 新增类型

```
src/main/java/com/hexagram2021/girlfriends/common/gift/
└── GiftQuoteManager.java     (数据包加载 + 随机抽取接口)
```

### 3.2 修改类型

| 文件 | 改动 |
|------|------|
| `GiftResult.java` | 新增 `@Nullable String quoteKey` 字段 |
| `GiftService.java` | 新增 `GiftQuoteManager` 依赖注入，`applyGift` 中执行随机抽取 |
| `GirlfriendsNetwork.java` | 拆分 `buildGiftMessage` 为 `sendGiftFeedback`：角色台词→聊天栏，好感度变化→subtitle |
| `GiftServiceTest.java` | 补充 quoteKey 相关测试用例 |

### 3.3 数据文件（新增）

```
src/main/resources/data/girlfriends/girlfriends/gift_quotes/
├── momo.json
├── yuxi.json
├── meishu.json
├── wanying.json
└── youruo.json
```

### 3.4 语言文件（修改）

```
src/main/resources/assets/girlfriends/lang/
├── en_us.json    (新增 133 条 quote 翻译)
└── zh_cn.json    (新增 133 条 quote 翻译)
```

## 4. 详细设计

### 4.1 GiftQuoteManager

**职责**：从 `data/<ns>/girlfriends/gift_quotes/*.json` 加载语料 i18n key，提供按档位+物品的随机抽取接口，使用线程安全的 `java.util.random.RandomGenerator`。

**类结构**：

```java
public final class GiftQuoteManager extends SimplePreparableReloadListener<GiftQuoteManager.QuotesMap> {

    public static final GiftQuoteManager INSTANCE = new GiftQuoteManager();

    private GiftQuoteManager() { /* singleton, registered as ServerReloadListener */ }

    /**
     * 内部数据记录——单个角色的全部语料池
     */
    public record GiftQuotes(
        ListMultimap<Identifier, String> favoriteQuotes,  // ImmutableListMultimap，key 为物品 ID，value 为 i18n key 列表
        List<String> likedQuotes,
        List<String> acceptedQuotes,
        List<String> rejectedQuotes,
        List<String> dislikedQuotes
    ) {}

    // 公开接口（均返回 Optional，未配置时返回 Optional.empty()）

    /** 从指定物品的 favorite 语料池随机抽取一条 */
    public Optional<String> getRandomFavoriteQuote(Identifier girlfriendTypeId, Identifier itemId)

    /** 从 liked/accepted/rejected/disliked 语料池各随机抽取一条 */
    public Optional<String> getRandomLikedQuote(Identifier girlfriendTypeId)
    public Optional<String> getRandomAcceptedQuote(Identifier girlfriendTypeId)
    public Optional<String> getRandomRejectedQuote(Identifier girlfriendTypeId)
    public Optional<String> getRandomDislikedQuote(Identifier girlfriendTypeId)
}
```

**加载细节**：

- 扫描目录：通过 `FileToIdConverter.json("girlfriends/gift_quotes")` 实现
- 文件名映射：`momo.json` → `girlfriends:momo`（`Identifier`）
- JSON 解析：`favorite` 为 Object (key→String[])，其余为 String[]
- `apply()` 时将结果存入不可变 Map
- 空白数组或缺失字段视为不存在（`getRandom*` 返回 `Optional.empty()`）

### 4.2 JSON Schema

```json
{
  "favorite": {
    "modid:item_id": [
      "girlfriends.gift.quote.<角色名>.favorite.modid.item_path.0",
      "girlfriends.gift.quote.<角色名>.favorite.modid.item_path.1",
      "girlfriends.gift.quote.<角色名>.favorite.modid.item_path.2"
    ]
  },
  "liked": [
    "girlfriends.gift.quote.<角色名>.liked.0",
    "... 共 8 条"
  ],
  "accepted": ["... 共 6 条"],
  "rejected": ["... 共 4 条"],
  "disliked": ["... 共 5 条"]
}
```

- 所有顶层字段均为可选，`favorite` 中的每个物品条目也为可选
- 若某档位未配置或数组为空，则该档位不发送台词
- 若 favorite 中未找到对应物品 ID，回退为不发送 favorite 台词（不 fallback 到 liked）

### 4.3 注册位置

在 `GirlfriendsNetwork` 中使用 `registrar.registerServerReloadListener(GiftQuoteManager.INSTANCE)` 注册，与 `GiftPreferenceManager` 并列。

### 4.4 GiftResult 改动

```java
// 改造前
public record GiftResult(boolean rejected, float affectionDelta,
                         GiftPreferenceLevel level, String messageKey)

// 改造后
public record GiftResult(boolean rejected, float affectionDelta,
                         GiftPreferenceLevel level, String messageKey,
                         @Nullable String quoteKey)
```

`quoteKey` 为 null 时表示不发送角色台词。

### 4.5 GiftService 改动

**构造方法注入**：新增可选 `@Nullable GiftQuoteManager` 参数：

```java
public GiftService(RelationshipService relationshipService,
                   @Nullable GiftPreferenceManager giftPreferenceManager,
                   @Nullable GiftQuoteManager giftQuoteManager,
                   BiPredicate<UUID, Identifier> canReceiveGift)
```

**applyGift 逻辑调整**：在判定档位后，从 `GiftQuoteManager` 抽取 quoteKey：

- `FAVORITE` → `giftQuoteManager.getRandomFavoriteQuote(gfId, itemId)`（需 itemId）
- `LIKED` → `giftQuoteManager.getRandomLikedQuote(gfId)`
- `ACCEPTED` → `giftQuoteManager.getRandomAcceptedQuote(gfId)`
- `DISLIKED` → `giftQuoteManager.getRandomDislikedQuote(gfId)`
- `REJECTED`（不感兴趣）→ `giftQuoteManager.getRandomRejectedQuote(gfId)`
- REJECTED（每日上限/权限拒绝）→ `quoteKey = null`

由于 favorite 需要知道具体物品 ID，`applyGift` 方法需新增 `@Nullable Identifier itemId` 参数。

### 4.6 网络层改动

**位置**：`GirlfriendsNetwork.java`

**拆分 `buildGiftMessage`** 为 `sendGiftFeedback` 方法：

```java
private static void sendGiftFeedback(ServerPlayer player, Identifier girlfriendTypeId,
                                     GiftResult result) {
    Component characterName = Component.translatable(
        "girlfriends.girlfriend_type." + girlfriendTypeId.getPath());
    String deltaStr = String.format("%+.1f", result.affectionDelta());

    // 1. 角色台词 → 聊天栏（若 quoteKey 不为 null）
    if (result.quoteKey() != null) {
        player.sendSystemMessage(Component.translatable(result.quoteKey()));
    }

    // 2. 好感度变化 → subtitle（原版 title/subtitle 叠加层）
    // 注：原版 subtitle 必须配合 title 叠加层才能渲染，需发送三个包：
    //   - TitlesAnimation 设定显示时序
    //   - SetTitleText（空标题）激活叠加层
    //   - SetSubtitleText 才是实际看到的好感度消息
    Component subtitleMsg = Component.translatable(result.messageKey(), characterName, deltaStr);
    player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10)); // 0.25s 淡入，2s 停留，0.5s 淡出
    player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleMsg));
}
```

**两个 handler 的改动**：

- `handleGiveGift`（ServerboundGiveGiftPacket）：将 `buildGiftMessage` 调用替换为 `sendGiftFeedback`
- `handleGiveGiftFromSlot`（ServerboundGiveGiftFromSlotPacket）：同上

**引入 import**：需新增 `ClientboundSetTitlesAnimationPacket`、`ClientboundSetTitleTextPacket`、`ClientboundSetSubtitleTextPacket` 三个原版包的 import。均为 Mojang 游戏协议包（非 NeoForge 自定义 payload），通过 `player.connection.send()` 直接发送。

### 4.7 i18n Key 命名约定

**现有 5 个礼物结果 key**（保持不变）：`girlfriends.gift.accepted`、`girlfriends.gift.disliked`、`girlfriends.gift.rejected`、`girlfriends.gift.daily_cap_reached`、`girlfriends.gift.permission_denied`

**新增台词 key 模式**：

- 非 favorite 档位：`girlfriends.gift.quote.<角色名>.<档位>.<序号>`
- favorite 档位：`girlfriends.gift.quote.<角色名>.favorite.<命名空间>.<物品路径>.<序号>`

示例——沫沫·蜜脾 favorite：
```
girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.0
girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.1
girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.2
```

示例——沫沫·liked：
```
girlfriends.gift.quote.momo.liked.0
...
girlfriends.gift.quote.momo.liked.7
```

## 5. 角色台词

完整台词内容参见附录 A。

## 6. 测试策略

### 6.1 GiftQuoteManager 单元测试

- 测试正常 JSON 加载：包含所有五个档位的完整 JSON，验证各 `getRandom*` 方法返回非空结果
- 测试部分 JSON 加载：仅配置 favorite 和 liked，验证其它档位返回 `Optional.empty()`
- 测试空 JSON（`{}`）：所有 `getRandom*` 均返回 `Optional.empty()`
- 测试随机性：对同一角色同一档位连续抽取 100 次，验证覆盖所有配置条目
- 测试不存在的角色 ID：返回 `Optional.empty()`

### 6.2 GiftService 改动回归测试

- 现有 `GiftServiceTest` 所有测试用例保持通过（使用无 GiftQuoteManager 的构造方法）
- 新增测试：使用模拟 GiftQuoteManager 验证 GiftResult.quoteKey 正确填充
- 新增测试：验证每日上限/权限拒绝场景下 quoteKey 为 null

### 6.3 集成验证

- 使用 `/affection set` 重置好感度后手动赠礼，验证聊天栏出现角色台词、subtitle 显示好感度变化
- 验证不同档位的台词不重复（通过多次赠礼观察）
- 验证 favorite 的同一物品每次随机选取不同台词

## 7. i18n Key 清单

### 7.1 墓碑表 (Tombstone Keys)

由于 133 条台词需要在两个语言文件中各定义对应翻译，以下列出每条 key 在 `zh_cn.json` 和 `en_us.json` 中的值。

## 附录 A：角色台词完整明细

### A.1 沫沫 (Momo) —— 29 条

#### Favorite: minecraft:honeycomb

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.0` | 这是……蜜脾？谢谢你，蜜蜂们也会高兴的。 | This is... honeycomb? Thank you. The bees will be happy too. |
| `girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.1` | 你记得我喜欢这个呀……嗯，我会好好珍藏的。 | You remembered I like this... Mm, I'll treasure it. |
| `girlfriends.gift.quote.momo.favorite.minecraft.honeycomb.2` | 蜜脾的味道，总让我想起花园里最安静的午后。谢谢你。 | The scent of honeycomb always reminds me of the quietest afternoons in the garden. Thank you. |

#### Favorite: girlfriends:bouquet

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.0` | 好漂亮的花束……每一朵你都用心挑了吧？ | Such a beautiful bouquet... You picked each flower carefully, didn't you? |
| `girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.1` | 花儿聚在一起的时候，就像在说悄悄话一样。以后，我也能和你说悄悄话吗？ | When flowers gather together, it's like they're whispering secrets. From now on... can I whisper secrets with you too? |
| `girlfriends.gift.quote.momo.favorite.girlfriends.bouquet.2` | 这束花不会凋谢的——因为是你送的，我会一直养着它。 | This bouquet will never wither—because it's from you. I'll tend to it forever. |

#### Liked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.liked.0` | 你总是能挑到我心坎里的东西呢。 | You always manage to pick things that touch my heart. |
| `girlfriends.gift.quote.momo.liked.1` | 谢谢。这个，正好能帮我照料花园。 | Thank you. This will help me tend the garden. |
| `girlfriends.gift.quote.momo.liked.2` | 嗯，我很喜欢。放在温室里用吧。 | Mm, I like it. Let's use it in the greenhouse. |
| `girlfriends.gift.quote.momo.liked.3` | 你送的东西，都带着一种很温柔的感觉。 | The things you give... they all carry such a gentle feeling. |
| `girlfriends.gift.quote.momo.liked.4` | 收下啦。下次来花园的时候，你就能看到它了。 | I'll take it. Next time you come to the garden, you'll see it. |
| `girlfriends.gift.quote.momo.liked.5` | 这些和花儿很配呢。谢谢你想到这个。 | These go so well with flowers. Thank you for thinking of this. |
| `girlfriends.gift.quote.momo.liked.6` | 有了它，花园里的日子又多了点期待。 | With this, life in the garden has one more thing to look forward to. |
| `girlfriends.gift.quote.momo.liked.7` | 你好像比我自己还了解我喜欢什么了。 | It seems you know what I like better than I do now. |

#### Accepted

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.accepted.0` | 谢谢，我会好好使用的。 | Thank you, I'll put it to good use. |
| `girlfriends.gift.quote.momo.accepted.1` | 嗯，收下了。你总是这么细心。 | Mm, I accept it. You're always so thoughtful. |
| `girlfriends.gift.quote.momo.accepted.2` | 虽然平时不怎么用得到，但既然是你给的…… | Though I don't usually use these... since it's from you... |
| `girlfriends.gift.quote.momo.accepted.3` | 谢谢。你每次来，都让这里更温暖了一点点。 | Thanks. Every time you visit, this place feels a little warmer. |
| `girlfriends.gift.quote.momo.accepted.4` | 这是份心意，我会记住的。 | This is a thoughtful gift. I'll remember it. |
| `girlfriends.gift.quote.momo.accepted.5` | 嗯，放在储物间里吧。哪天花园需要的时候就能用上了。 | Mm, I'll keep it in the storage. It'll come in handy for the garden someday. |

#### Rejected

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.rejected.0` | 嗯……这个，我不知道该怎么用在花园里呢…… | Mm... I'm not sure how to use this in the garden... |
| `girlfriends.gift.quote.momo.rejected.1` | 谢谢你的好意。不过花和蜜蜂大概不需要这个吧。 | Thank you for the thought. But the flowers and bees probably don't need this. |
| `girlfriends.gift.quote.momo.rejected.2` | 也许送给更需要它的人会更好？ | Maybe it'd be better to give it to someone who needs it more? |
| `girlfriends.gift.quote.momo.rejected.3` | 你的心意我收到了。只是……它在我的花园里找不到位置呢。 | I've received your kindness. It's just... it doesn't have a place in my garden. |

#### Disliked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.momo.disliked.0` | 这个……会伤害到我的花儿的。请拿走吧。 | This... will hurt my flowers. Please take it away. |
| `girlfriends.gift.quote.momo.disliked.1` | 我不喜欢这种东西。它让我想起暴风雨打落花瓣的声音。 | I don't like things like this. It reminds me of the sound of petals being torn by a storm. |
| `girlfriends.gift.quote.momo.disliked.2` | 花儿很娇嫩，经不起这个的…… | Flowers are delicate. They can't bear things like this... |
| `girlfriends.gift.quote.momo.disliked.3` | 也许你不了解，但花园不需要枯萎和腐烂的东西。 | Maybe you don't understand, but the garden has no need for things that wither and rot. |
| `girlfriends.gift.quote.momo.disliked.4` | 请收回去。腐烂的气息会让蜜蜂迷路的。 | Please take it back. The scent of decay will lead the bees astray. |

### A.2 渔溪 (Yuxi) —— 26 条

#### Favorite: minecraft:nautilus_shell

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.0` | 又一个鹦鹉螺壳……它听过的那片海，和我听过的，是不是同一片？ | Another nautilus shell... The sea it has heard—is it the same one I've listened to? |
| `girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.1` | 谢谢你。我会把它放在灯塔的窗台上，让它继续听潮声。 | Thank you. I'll place it on the lighthouse windowsill, so it can keep listening to the tides. |
| `girlfriends.gift.quote.yuxi.favorite.minecraft.nautilus_shell.2` | 这个壳上的纹路很完整。你是在很远的地方找到的吗？ | The spirals on this shell are perfectly intact. Did you find it somewhere far away? |

#### Liked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.yuxi.liked.0` | 嗯，这个很好。谢谢你。 | Mm, this is good. Thank you. |
| `girlfriends.gift.quote.yuxi.liked.1` | 你知道我喜欢什么。这很难得。 | You know what I like. That's rare. |
| `girlfriends.gift.quote.yuxi.liked.2` | 在海上的话，这种最实用了。收下了。 | Out on the sea, this is the most practical. I'll take it. |
| `girlfriends.gift.quote.yuxi.liked.3` | 潮水退下去的时候，会在沙滩上留下好东西。你挑的这个也是。 | When the tide recedes, it leaves good things on the sand. This you picked is one of them. |
| `girlfriends.gift.quote.yuxi.liked.4` | 谢谢。下次出海的时候带上吧。 | Thanks. Let's bring it along next time we set sail. |
| `girlfriends.gift.quote.yuxi.liked.5` | 不用很多话。你给的，我收下就是了。 | No need for many words. You give it, I take it. |
| `girlfriends.gift.quote.yuxi.liked.6` | 嗯，放进船舱里了。 | Mm, it's stowed in the cabin. |
| `girlfriends.gift.quote.yuxi.liked.7` | 有些东西不用挑很久，一眼就知道是好东西。就像这个。 | Some things don't need long choosing. One glance, and you know they're good. Like this. |

#### Accepted

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.yuxi.accepted.0` | 谢谢。我收下了。 | Thanks. I'll take it. |
| `girlfriends.gift.quote.yuxi.accepted.1` | 嗯，也许航行的时候用得着。 | Mm, it might come in handy during the voyage. |
| `girlfriends.gift.quote.yuxi.accepted.2` | 行。放在码头的箱子里了。 | Alright. It's in the dock chest. |
| `girlfriends.gift.quote.yuxi.accepted.3` | 你的心意，潮水会帮我记住的。 | Your kindness—the tides will help me remember it. |
| `girlfriends.gift.quote.yuxi.accepted.4` | 谢谢你。岸上的东西，有时候也和海里的差不多有趣。 | Thank you. Sometimes things from the shore are almost as interesting as things from the sea. |
| `girlfriends.gift.quote.yuxi.accepted.5` | 收下了。虽然我不太爱说话，但你的心意我都明白。 | I'll take it. I may not talk much, but I understand your feelings. |

#### Rejected

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.yuxi.rejected.0` | 这个……放在船上不知道能做什么。 | This... I don't know what it could do on the boat. |
| `girlfriends.gift.quote.yuxi.rejected.1` | 海浪会把不需要的东西冲走的。这个也是。 | The waves wash away what isn't needed. This, too. |
| `girlfriends.gift.quote.yuxi.rejected.2` | 也许它不是不好，只是和海没有缘分吧。 | Perhaps it's not that it's bad. It just has no fate with the sea. |
| `girlfriends.gift.quote.yuxi.rejected.3` | 谢谢你。但船上的空间很小，每一寸都有用。 | Thank you. But space on the boat is small—every inch has its purpose. |

#### Disliked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.yuxi.disliked.0` | 收回去。这种东西不该出现在海边。 | Take it back. Things like this shouldn't appear by the sea. |
| `girlfriends.gift.quote.yuxi.disliked.1` | 海风告诉我，这不是什么好东西。 | The sea wind tells me this is nothing good. |
| `girlfriends.gift.quote.yuxi.disliked.2` | 它会污染岸边的。请拿走。 | It will pollute the shore. Please take it away. |
| `girlfriends.gift.quote.yuxi.disliked.3` | 我不喜欢。海里的东西比表面看起来更有直觉——它们也讨厌这个。 | I don't like it. Things from the sea have keener instincts than they appear—they hate this too. |
| `girlfriends.gift.quote.yuxi.disliked.4` | 这种东西……会让鱼群绕道而行的。 | Things like this... will make the schools of fish swim the other way. |

### A.3 梅疏 (Meishu) —— 26 条

#### Favorite: minecraft:iron_ingot

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.0` | 铁锭。成分很纯。你懂的不少。 | Iron ingot. Pure composition. You know your stuff. |
| `girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.1` | 好铁。是好的开始。收下了。 | Good iron. A good start. I'll take it. |
| `girlfriends.gift.quote.meishu.favorite.minecraft.iron_ingot.2` | 这是我今天收到的最有价值的东西。包括我挖到的在内。 | This is the most valuable thing I've received today. Including what I mined myself. |

#### Liked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.meishu.liked.0` | 不错。能用。 | Not bad. Usable. |
| `girlfriends.gift.quote.meishu.liked.1` | 收下了。下次开炉的时候可以用。 | Taken. Can be used next time the furnace is lit. |
| `girlfriends.gift.quote.meishu.liked.2` | 嗯。硬度够。 | Mm. Hard enough. |
| `girlfriends.gift.quote.meishu.liked.3` | 你很会挑材料。 | You're good at picking materials. |
| `girlfriends.gift.quote.meishu.liked.4` | 这是正经东西。比你嘴上说的话有用。 | This is serious stuff. More useful than the words you say. |
| `girlfriends.gift.quote.meishu.liked.5` | 行。放进材料箱了。 | Fine. In the materials chest. |
| `girlfriends.gift.quote.meishu.liked.6` | 这种东西用得上。不像那些华而不实的装饰品。 | This kind of thing is useful. Unlike those flashy but useless decorations. |
| `girlfriends.gift.quote.meishu.liked.7` | 又一种好材料。你的眼光比大多数人靠得住。 | Another good material. Your judgment is more reliable than most. |

#### Accepted

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.meishu.accepted.0` | 嗯。收下了。 | Mm. Taken. |
| `girlfriends.gift.quote.meishu.accepted.1` | 还行。能用得上。 | Alright. Can find a use for it. |
| `girlfriends.gift.quote.meishu.accepted.2` | 不是最好的材料，但也不是没用的。谢谢你。 | Not the best material, but not useless either. Thank you. |
| `girlfriends.gift.quote.meishu.accepted.3` | 放那儿吧。以后再说。 | Leave it there. We'll see later. |
| `girlfriends.gift.quote.meishu.accepted.4` | 还行吧。你至少不是空手来的。 | Fine enough. At least you didn't come empty-handed. |
| `girlfriends.gift.quote.meishu.accepted.5` | 也许哪天能派上用场。材料就是这样——现在没用，将来不一定。 | Might come in handy someday. That's how materials are—useless now, not necessarily later. |

#### Rejected

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.meishu.rejected.0` | 这东西没有硬度。我用不上。 | This thing has no hardness. I have no use for it. |
| `girlfriends.gift.quote.meishu.rejected.1` | 材质太差了。你留着吧。 | Material quality is too poor. You keep it. |
| `girlfriends.gift.quote.meishu.rejected.2` | 不能入炉。不能锻造。对我来说没用。 | Can't go in the furnace. Can't be forged. No use to me. |
| `girlfriends.gift.quote.meishu.rejected.3` | 你也许不知道什么是好材料。下次我告诉你。 | Maybe you don't know what good material is. Next time I'll tell you. |

#### Disliked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.meishu.disliked.0` | 拿走。这种东西不配进我的箱子。 | Take it away. This doesn't deserve to be in my chest. |
| `girlfriends.gift.quote.meishu.disliked.1` | 我用了一辈子火焰和铁砧——这种东西连废料都算不上。 | I've spent my life with flames and anvils—this isn't even scrap. |
| `girlfriends.gift.quote.meishu.disliked.2` | 华而不实。我讨厌没用又抢眼的东西。 | Flashy and useless. I hate things that catch the eye but do nothing. |
| `girlfriends.gift.quote.meishu.disliked.3` | 别把这东西放在铁砧旁边。会玷污了它。 | Don't put this near the anvil. It will taint it. |
| `girlfriends.gift.quote.meishu.disliked.4` | 我宁可要一块碎石，也不要这个。 | I'd rather have a piece of gravel than this. |

### A.4 晚萤 (Wanying) —— 26 条

#### Favorite: minecraft:blaze_rod

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.0` | 烈焰棒！好家伙，你是从烈焰人手里抢来的？漂亮。 | Blaze rod! Whoa, did you snatch this from a blaze? Nice. |
| `girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.1` | 够热、够亮。这就是我喜欢它的原因——和你很像。 | Hot enough, bright enough. That's why I like it—just like you. |
| `girlfriends.gift.quote.wanying.favorite.minecraft.blaze_rod.2` | 多一根烈焰棒，我的剑就能多一分烈火的锐气。谢了，搭档。 | One more blaze rod, and my blade gains another edge of flame. Thanks, partner. |

#### Liked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.wanying.liked.0` | 好东西！战场上的硬通货。 | Good stuff! Hard currency on the battlefield. |
| `girlfriends.gift.quote.wanying.liked.1` | 行，我就喜欢实用的东西。收下了。 | Alright, I like practical things. I'll take it. |
| `girlfriends.gift.quote.wanying.liked.2` | 哈，你眼光不错。这种东西在战斗里能保命。 | Ha, good eye. This kind of thing can save your life in a fight. |
| `girlfriends.gift.quote.wanying.liked.3` | 痛快。不像那些磨磨唧唧的东西。 | Straightforward. Not like those roundabout things. |
| `girlfriends.gift.quote.wanying.liked.4` | 有这种装备，下次和我一起冲在前面。站旁边，别站后面。 | With equipment like this, charge up front with me next time. Beside me, not behind me. |
| `girlfriends.gift.quote.wanying.liked.5` | 送得正好。上次那场仗打完，正缺这个。 | Perfect timing. Just ran short after the last battle. |
| `girlfriends.gift.quote.wanying.liked.6` | 战斗不是一个人的事——有好装备更要紧。谢了，伙伴。 | Fighting isn't a solo game—good gear matters more. Thanks, mate. |
| `girlfriends.gift.quote.wanying.liked.7` | 嗯。我觉得你比我还会挑武器。下次一起去挑。 | Huh. I think you pick weapons better than I do. Let's go pick some together next time. |

#### Accepted

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.wanying.accepted.0` | 还行。收下啦。 | Not bad. I'll take it. |
| `girlfriends.gift.quote.wanying.accepted.1` | 虽然不是战备物资，但日常用得着。 | Not combat supplies, but useful for daily life. |
| `girlfriends.gift.quote.wanying.accepted.2` | 行，放补给包里了。 | Alright, in the supply bag it goes. |
| `girlfriends.gift.quote.wanying.accepted.3` | 你给的东西，我不会扔的。放心吧。 | Something from you—I won't throw it away. Rest easy. |
| `girlfriends.gift.quote.wanying.accepted.4` | 谢啦。虽然我更想看到你在战场上还活着。 | Thanks. Though I'd rather see you alive on the battlefield. |
| `girlfriends.gift.quote.wanying.accepted.5` | 不打仗的日子，也会需要这些。我懂的。 | Days without battle need these too. I get it. |

#### Rejected

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.wanying.rejected.0` | 这东西在战场上帮不上忙。你留着吧。 | This won't help on the battlefield. You keep it. |
| `girlfriends.gift.quote.wanying.rejected.1` | 不够硬。不够利。不够快。不是我的风格。 | Not hard enough. Not sharp enough. Not fast enough. Not my style. |
| `girlfriends.gift.quote.wanying.rejected.2` | 也许你搞错了——我可不是那种会喜欢这种小玩意儿的人。 | Maybe you've got the wrong idea—I'm not the type who likes little trinkets like this. |
| `girlfriends.gift.quote.wanying.rejected.3` | 如果你是想让我高兴，那方法错了。下次直接约我打架。 | If you're trying to make me happy, wrong approach. Next time just challenge me to a fight. |

#### Disliked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.wanying.disliked.0` | 冷的、软的、没有锋芒的东西——我不需要。拿走。 | Cold, soft, edgeless things—I don't need them. Take them. |
| `girlfriends.gift.quote.wanying.disliked.1` | 这种东西让我想起放弃战斗的人。别让我看到它。 | Things like this remind me of those who gave up the fight. Don't let me see it. |
| `girlfriends.gift.quote.wanying.disliked.2` | 我宁可面对十个烈焰人，也不想碰这个。 | I'd rather face ten blazes than touch this. |
| `girlfriends.gift.quote.wanying.disliked.3` | 你以为我需要这个？不。我的体温是自己挣来的。 | You thought I needed this? No. I earn my own warmth. |
| `girlfriends.gift.quote.wanying.disliked.4` | 战斗教会我一件事：最危险的不是刀剑，是让人变得软弱的东西。收回去。 | Combat taught me one thing: the most dangerous thing isn't blades—it's things that make people soft. Take it back. |

### A.5 幽若 (Youruo) —— 26 条

#### Favorite: minecraft:ender_pearl

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.0` | 末影珍珠。观察：完整度极高。结论：你采集的时候很小心。我很满意。 | Ender pearl. Observation: extremely high integrity. Conclusion: you were very careful during collection. I'm very satisfied. |
| `girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.1` | 每一颗末影珍珠都包含一个非欧几里得空间的坐标信息。这颗的数据很清晰。谢谢你。 | Every ender pearl contains the coordinate data of a non-Euclidean space. This one's data is very clear. Thank you. |
| `girlfriends.gift.quote.youruo.favorite.minecraft.ender_pearl.2` | 假说：你选这颗的时候，已经知道我会喜欢了。验证方式——我已经笑了。你没看到？ | Hypothesis: when you picked this, you already knew I'd like it. Verification—I've already smiled. You didn't see? |

#### Liked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.youruo.liked.0` | 观察完毕：这是一个高质量样本。收下了。 | Observation complete: this is a high-quality sample. Accepted. |
| `girlfriends.gift.quote.youruo.liked.1` | 有趣。和我正在做的研究有关联。谢谢。 | Interesting. It has relevance to my current research. Thank you. |
| `girlfriends.gift.quote.youruo.liked.2` | 推论：你知道我需要这个。数据点匹配。 | Inference: you knew I needed this. Data point matches. |
| `girlfriends.gift.quote.youruo.liked.3` | 实验材料 +1。你对我的研究课题越来越了解了。 | Plus one experimental material. Your understanding of my research topic is improving. |
| `girlfriends.gift.quote.youruo.liked.4` | 结论：可以参考使用。我会记录在实验日志里。 | Conclusion: can be used for reference. I'll record it in the experiment log. |
| `girlfriends.gift.quote.youruo.liked.5` | 假说成立：和你相处的时间越长，收到的样本质量越高。 | Hypothesis confirmed: the longer I spend with you, the higher the quality of samples received. |
| `girlfriends.gift.quote.youruo.liked.6` | 这个能帮我推进空间跃迁的第 3 个阶段。你大概不知道，但这很重要。 | This can advance phase 3 of my spatial leap research. You probably don't know that, but it's very important. |
| `girlfriends.gift.quote.youruo.liked.7` | 研究室的架子上又多了一样。那些没有用的空架子……你差不多帮我填满一半了。 | Another one on the lab shelf. Those empty shelves that had no use... you've helped me fill almost half of them now. |

#### Accepted

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.youruo.accepted.0` | 确认收到。已记录到实验日志。 | Receipt confirmed. Logged in the experiment record. |
| `girlfriends.gift.quote.youruo.accepted.1` | 虽然不是研究必需品，但日常实验可以消耗。谢谢。 | Not essential for research, but consumable in daily experiments. Thank you. |
| `girlfriends.gift.quote.youruo.accepted.2` | 观察结果：还有优化的空间。不过能用。 | Observation result: there's room for optimization. But it's usable. |
| `girlfriends.gift.quote.youruo.accepted.3` | 收到了。每个实验都需要一些辅助材料——你就是我的辅助材料供应商了？ | Received. Every experiment needs auxiliary materials—are you my auxiliary material supplier now? |
| `girlfriends.gift.quote.youruo.accepted.4` | 也许未来某次实验会用到。先归档。 | Perhaps a future experiment will use it. Archiving for now. |
| `girlfriends.gift.quote.youruo.accepted.5` | 在数据没有验证之前，我不会做太多评价。但……谢谢。 | Before data is verified, I won't make too many judgments. But... thank you. |

#### Rejected

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.youruo.rejected.0` | 这个和当前研究方向没有相关性。 | This has no correlation with the current research direction. |
| `girlfriends.gift.quote.youruo.rejected.1` | 暂时没有我能派上用场的场景。建议你另外分配。 | Temporarily no scenario where I can use it. Suggest you reallocate. |
| `girlfriends.gift.quote.youruo.rejected.2` | 数据不足……无法判断用途。不过目前来看，实验不需要它。 | Insufficient data... unable to determine use. But currently, the experiments don't need it. |
| `girlfriends.gift.quote.youruo.rejected.3` | 也许不是你的问题——只是它的物理特性对我的研究没有参考价值。 | Perhaps it's not your fault—its physical properties simply have no reference value for my research. |

#### Disliked

| Key | zh_cn | en_us |
|-----|-------|-------|
| `girlfriends.gift.quote.youruo.disliked.0` | 结论：高熵值物质。会污染实验环境的。请移除。 | Conclusion: high-entropy substance. It will contaminate the experimental environment. Please remove it. |
| `girlfriends.gift.quote.youruo.disliked.1` | 混沌和无序的东西对精密实验是毁灭性的。你应该能理解吧？ | Chaos and disorder are catastrophic for precision experiments. You should be able to understand that, right? |
| `girlfriends.gift.quote.youruo.disliked.2` | 不要把这个放在研究台附近。它会让数据产生不可逆的噪声。 | Do not place this near the research bench. It will introduce irreversible noise into the data. |
| `girlfriends.gift.quote.youruo.disliked.3` | 也许在别的地方它有价值——但在我的实验室里，它是干扰项。 | Perhaps it has value elsewhere—but in my laboratory, it is a confounding variable. |
| `girlfriends.gift.quote.youruo.disliked.4` | 你的好意和这个物品本身的属性是两回事。好意我收到了；物品请拿走。 | Your kindness and this item's inherent properties are two separate things. I've received the kindness; please take the item away. |
