package com.hexagram2021.girlfriends.common.gift;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 礼物偏好资源管理器喵~
 *
 * @author liudongyu
 */
public class GiftPreferenceManager extends SimplePreparableReloadListener<Map<Identifier, GiftPreference>> {
	public static final GiftPreferenceManager INSTANCE = new GiftPreferenceManager();

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/gift_preferences");

	private Map<Identifier, GiftPreference> preferences = Map.of();

	/**
	 * 获取指定角色礼物偏好喵~
	 *
	 * @param characterTypeId 角色类型 ID 喵~
	 * @return 角色礼物偏好喵~
	 */
	public Optional<GiftPreference> getPreference(Identifier characterTypeId) {
		return Optional.ofNullable(this.preferences.get(characterTypeId));
	}

	/**
	 * 获取全部礼物偏好喵~
	 *
	 * @return 全部礼物偏好映射喵~
	 */
	public Map<Identifier, GiftPreference> getPreferences() {
		return this.preferences;
	}

	@Override
	protected Map<Identifier, GiftPreference> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<Identifier, GiftPreference> prepared = new LinkedHashMap<>();
		for(Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
			Identifier fileLocation = entry.getKey();
			Identifier characterTypeId = LISTER.fileToId(fileLocation);
			try(Reader reader = entry.getValue().openAsReader()) {
				JsonElement jsonElement = JsonParser.parseReader(reader);
				if(!jsonElement.isJsonObject()) {
					throw new JsonParseException("Gift preference root must be a JSON object");
				}
				GiftPreference preference = parsePreference(characterTypeId, jsonElement.getAsJsonObject());
				if(prepared.putIfAbsent(characterTypeId, preference) != null) {
					throw new IllegalStateException("Duplicate gift preference ignored with ID " + characterTypeId);
				}
			} catch(IllegalArgumentException | IOException | JsonParseException exception) {
				LOGGER.error("Couldn't parse gift preference '{}' from '{}'", characterTypeId, fileLocation, exception);
			}
		}
		return prepared;
	}

	@Override
	protected void apply(Map<Identifier, GiftPreference> preparations, ResourceManager manager, ProfilerFiller profiler) {
		this.preferences = Collections.unmodifiableMap(new LinkedHashMap<>(preparations));
	}

	private static GiftPreference parsePreference(Identifier characterTypeId, JsonObject jsonObject) {
		return new GiftPreference(
				characterTypeId,
				parseIdentifierSet(jsonObject, "favorite"),
				parseIdentifierSet(jsonObject, "liked_items"),
				parseIdentifierSet(jsonObject, "liked_tags"),
				parseIdentifierSet(jsonObject, "accepted_items"),
				parseIdentifierSet(jsonObject, "accepted_tags"),
				parseIdentifierSet(jsonObject, "disliked_items"),
				parseIdentifierSet(jsonObject, "disliked_tags")
		);
	}

	private static Set<Identifier> parseIdentifierSet(JsonObject jsonObject, String fieldName) {
		if(!jsonObject.has(fieldName)) {
			return Set.of();
		}
		JsonElement jsonElement = jsonObject.get(fieldName);
		if(!jsonElement.isJsonArray()) {
			throw new JsonParseException("Field '" + fieldName + "' must be an array");
		}
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		Set<Identifier> identifiers = new LinkedHashSet<>();
		for(JsonElement element : jsonArray) {
			if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw new JsonParseException("Field '" + fieldName + "' must contain only strings");
			}
			identifiers.add(Identifier.parse(element.getAsString()));
		}
		return identifiers;
	}
}
