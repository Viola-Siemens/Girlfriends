package com.hexagram2021.girlfriends.common.quest;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 固定委托定义资源管理器喵~
 *
 * @author liudongyu
 */
public class FixedQuestDefinitionManager extends SimplePreparableReloadListener<Map<String, QuestDefinition>> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/fixed_quests");

	/** 固定委托管理器单例喵~ */
	public static final FixedQuestDefinitionManager INSTANCE = new FixedQuestDefinitionManager();

	private Map<String, QuestDefinition> definitions = Map.of();

	/**
	 * 获取固定委托定义喵~
	 *
	 * @param questId 委托定义 ID 喵~
	 * @return 委托定义喵~
	 */
	public Optional<QuestDefinition> getDefinition(String questId) {
		return Optional.ofNullable(this.definitions.get(questId));
	}

	/**
	 * 获取全部固定委托定义喵~
	 *
	 * @return 委托定义映射喵~
	 */
	public Map<String, QuestDefinition> getDefinitions() {
		return this.definitions;
	}

	@Override
	protected Map<String, QuestDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<String, QuestDefinition> prepared = new LinkedHashMap<>();
		for(Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
			Identifier fileLocation = entry.getKey();
			Identifier questLocation = LISTER.fileToId(fileLocation);
			try(Reader reader = entry.getValue().openAsReader()) {
				JsonElement jsonElement = JsonParser.parseReader(reader);
				if(!jsonElement.isJsonObject()) {
					throw new JsonParseException("Fixed quest root must be a JSON object");
				}
				QuestDefinition definition = parseDefinition(questLocation, jsonElement.getAsJsonObject(), QuestType.FIXED);
				if(prepared.putIfAbsent(definition.questId(), definition) != null) {
					throw new IllegalStateException("Duplicate fixed quest definition with ID " + definition.questId());
				}
			} catch(IllegalArgumentException | IOException | JsonParseException exception) {
				LOGGER.error("Couldn't parse fixed quest '{}' from '{}'", questLocation, fileLocation, exception);
			}
		}
		return prepared;
	}

	@Override
	protected void apply(Map<String, QuestDefinition> preparations, ResourceManager manager, ProfilerFiller profiler) {
		this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(preparations));
	}

	static QuestDefinition parseDefinition(Identifier fallbackId, JsonObject jsonObject, QuestType defaultQuestType) {
		try {
			String questId = GsonHelper.getAsString(jsonObject, "quest_id", fallbackId.toString());
			QuestType questType = QuestType.valueOf(GsonHelper.getAsString(jsonObject, "quest_type", defaultQuestType.name()).toUpperCase());
			Identifier girlfriendTypeId = Identifier.parse(GsonHelper.getAsString(
					jsonObject, "girlfriend_type_id", GsonHelper.getAsString(
							jsonObject, "character_id",
							fallbackId.withPath(path -> path.contains("/") ? path.substring(0, path.indexOf('/')) : path).toString()
					)
			));
			int fixedIndex = getInt(jsonObject, "fixed_index").orElse(0);
			AffectionStage requiredStage = AffectionStage.valueOf(GsonHelper.getAsString(jsonObject, "required_stage", "STRANGER").toUpperCase());
			QuestObjectiveGroup objectives = parseObjectives(jsonObject.getAsJsonArray("objectives"));
			return new QuestDefinition(questId, questType, girlfriendTypeId, fixedIndex, requiredStage, objectives);
		} catch(Exception e) {
			throw new IllegalStateException("Failed to parse " + fallbackId, e);
		}
	}

	static QuestObjectiveGroup parseObjectives(@Nullable JsonArray objectivesArray) {
		if(objectivesArray == null) {
			return QuestObjectiveGroup.empty();
		}
		List<QuestObjectiveHandler> objectives = Lists.newArrayList();
		for(JsonElement element : objectivesArray) {
			if(!element.isJsonObject()) {
				throw new JsonParseException("Objective entry must be a JSON object");
			}
			JsonObject objectiveObject = element.getAsJsonObject();
			String type = GsonHelper.getAsString(objectiveObject, "type", "noop");
			switch(type) {
				case "item_delivery" -> objectives.add(new ItemDeliveryObjectiveHandler(
						ItemDeliveryObjectiveHandler.ItemDeliveryRecord.LIST_CODEC.parse(JsonOps.INSTANCE, objectiveObject.get("items")).getOrThrow()
				));
				case "structure_visit" -> objectives.add(new StructureVisitObjectiveHandler(
						GsonHelper.getAsString(objectiveObject, "structures", "")
				));
				case "block_stay" -> objectives.add(new BlockStayObjectiveHandler(
						Identifier.parse(GsonHelper.getAsString(objectiveObject, "block_id", "minecraft:air")),
						getInt(objectiveObject, "required_ticks").orElse(20)
				));
				case "build" -> objectives.add(new BuildObjectiveHandler());
				case "accompany" -> objectives.add(new AccompanyObjectiveHandler());
				case "fight" -> objectives.add(new FightObjectiveHandler());
				case "noop" -> {
					// no-op
				}
				default -> throw new JsonParseException("Unsupported objective type: " + type);
			}
		}
		return objectives.isEmpty() ? QuestObjectiveGroup.empty() : new QuestObjectiveGroup(objectives);
	}

	private static Optional<Integer> getInt(JsonObject jsonObject, String fieldName) {
		if(!jsonObject.has(fieldName)) {
			return Optional.empty();
		}
		JsonElement element = jsonObject.get(fieldName);
		if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new JsonParseException("Field '" + fieldName + "' must be a number");
		}
		return Optional.of(element.getAsInt());
	}
}
