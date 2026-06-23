package com.hexagram2021.girlfriends.common.gift;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * 礼物回复台词管理器——从数据包加载各角色语料 i18n key，提供按档位随机抽取接口喵~
 *
 * @author liudongyu
 */
public final class GiftQuoteManager extends SimplePreparableReloadListener<Map<Identifier, GiftQuoteManager.GiftQuotes>> {
	public static final GiftQuoteManager INSTANCE = new GiftQuoteManager();

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/gift_quotes");

	/**
	 * 台词 i18n key 的前缀，JSON 中仅存储后缀部分，由 Java 代码统一拼接喵~
	 */
	public static final String QUOTE_KEY_PREFIX = "girlfriends.gift.quote.";

	private Map<Identifier, GiftQuotes> quotesMap = Map.of();
	private final RandomGenerator random = RandomGenerator.getDefault();

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
		return Optional.of(QUOTE_KEY_PREFIX + pool.get(this.random.nextInt(pool.size())));
	}

	/**
	 * 从 liked 语料池中随机抽取一条台词喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
	 */
	public Optional<String> getRandomLikedQuote(Identifier girlfriendTypeId) {
		return this.getRandomFromPool(girlfriendTypeId, GiftQuotes::likedQuotes);
	}

	/**
	 * 从 accepted 语料池中随机抽取一条台词喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
	 */
	public Optional<String> getRandomAcceptedQuote(Identifier girlfriendTypeId) {
		return this.getRandomFromPool(girlfriendTypeId, GiftQuotes::acceptedQuotes);
	}

	/**
	 * 从 rejected 语料池中随机抽取一条台词喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
	 */
	public Optional<String> getRandomRejectedQuote(Identifier girlfriendTypeId) {
		return this.getRandomFromPool(girlfriendTypeId, GiftQuotes::rejectedQuotes);
	}

	/**
	 * 从 disliked 语料池中随机抽取一条台词喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @return 随机台词 i18n key，未配置时返回 {@link Optional#empty()} 喵~
	 */
	public Optional<String> getRandomDislikedQuote(Identifier girlfriendTypeId) {
		return this.getRandomFromPool(girlfriendTypeId, GiftQuotes::dislikedQuotes);
	}

	@Override
	protected Map<Identifier, GiftQuotes> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<Identifier, GiftQuotes> prepared = Maps.newLinkedHashMap();
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
		this.quotesMap = Collections.unmodifiableMap(Maps.newLinkedHashMap(preparations));
	}

	private Optional<String> getRandomFromPool(Identifier girlfriendTypeId,
											   Function<GiftQuotes, List<String>> poolExtractor) {
		GiftQuotes quotes = this.quotesMap.get(girlfriendTypeId);
		if (quotes == null) {
			return Optional.empty();
		}
		List<String> pool = poolExtractor.apply(quotes);
		if (pool.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(QUOTE_KEY_PREFIX + pool.get(this.random.nextInt(pool.size())));
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
		List<String> result = Lists.newArrayListWithCapacity(jsonArray.size());
		for (JsonElement elem : jsonArray) {
			if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) {
				throw new JsonParseException("Field '" + fieldName + "' must contain only strings");
			}
			result.add(elem.getAsString());
		}
		return Collections.unmodifiableList(result);
	}
}
