package com.hexagram2021.girlfriends.common.quest;

import com.google.gson.JsonElement;
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

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 随机委托模板资源管理器喵~
 *
 * @author liudongyu
 */
public class RandomQuestTemplateManager extends SimplePreparableReloadListener<Map<String, QuestDefinition>> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/random_quest_templates");

	/** 随机委托模板管理器单例喵~ */
	public static final RandomQuestTemplateManager INSTANCE = new RandomQuestTemplateManager();

	private Map<String, QuestDefinition> definitions = Map.of();

	/**
	 * 获取随机委托模板喵~
	 *
	 * @param questId 委托定义 ID 喵~
	 * @return 委托定义喵~
	 */
	public Optional<QuestDefinition> getDefinition(String questId) {
		return Optional.ofNullable(this.definitions.get(questId));
	}

	/**
	 * 获取全部随机委托模板喵~
	 *
	 * @return 委托定义映射喵~
	 */
	public Map<String, QuestDefinition> getDefinitions() {
		return this.definitions;
	}

	/**
	 * 按角色类型随机获取一个委托模板喵~
	 *
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param random 随机数生成器喵~
	 * @return 随机委托定义，无匹配时返回 null 喵~
	 */
	@Nullable
	public QuestDefinition getRandomDefinitionForType(Identifier girlfriendTypeId, RandomSource random) {
		List<QuestDefinition> candidates = this.definitions.values().stream()
				.filter(d -> d.girlfriendTypeId().equals(girlfriendTypeId))
				.toList();
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(random.nextInt(candidates.size()));
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
					throw new JsonParseException("Random quest template root must be a JSON object");
				}
				QuestDefinition definition = FixedQuestDefinitionManager.parseDefinition(questLocation, jsonElement.getAsJsonObject(), QuestType.RANDOM);
				if(prepared.putIfAbsent(definition.questId(), definition) != null) {
					throw new IllegalStateException("Duplicate random quest template with ID " + definition.questId());
				}
			} catch(IllegalArgumentException | IOException | JsonParseException exception) {
				LOGGER.error("Couldn't parse random quest template '{}' from '{}'", questLocation, fileLocation, exception);
			}
		}
		return prepared;
	}

	@Override
	protected void apply(Map<String, QuestDefinition> preparations, ResourceManager manager, ProfilerFiller profiler) {
		this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(preparations));
	}
}
