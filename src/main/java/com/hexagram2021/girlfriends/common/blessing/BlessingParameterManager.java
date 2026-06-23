package com.hexagram2021.girlfriends.common.blessing;

import com.google.common.collect.Maps;
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

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;

/**
 * 祝福参数重载管理器喵~
 *
 * @author liudongyu
 */
public class BlessingParameterManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
	public static final BlessingParameterManager INSTANCE = new BlessingParameterManager();

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("girlfriends/blessing_parameters");

	private Map<Identifier, JsonElement> parameters = Map.of();

	/**
	 * 创建祝福参数重载管理器喵~
	 */
	public BlessingParameterManager() {
	}

	/**
	 * 创建使用固定参数的祝福参数管理器喵~
	 *
	 * @param parameters 祝福参数喵~
	 */
	public BlessingParameterManager(Map<Identifier, JsonElement> parameters) {
		this.parameters = Collections.unmodifiableMap(Maps.newLinkedHashMap(parameters));
	}

	/**
	 * 获取祝福参数 JSON 喵~
	 *
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @return 祝福参数 JSON 喵~
	 */
	public JsonElement getParameter(Identifier blessingTypeId) {
		return this.parameters.get(blessingTypeId);
	}

	@Override
	protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<Identifier, JsonElement> prepared = Maps.newLinkedHashMap();
		for(Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
			Identifier fileLocation = entry.getKey();
			Identifier blessingTypeId = LISTER.fileToId(fileLocation);
			try(Reader reader = entry.getValue().openAsReader()) {
				JsonElement jsonElement = JsonParser.parseReader(reader);
				if(!jsonElement.isJsonObject()) {
					throw new JsonParseException("Blessing parameter root must be a JSON object");
				}
				prepared.put(blessingTypeId, jsonElement);
			} catch(IllegalArgumentException | IOException | JsonParseException exception) {
				LOGGER.error("Couldn't parse blessing parameter '{}' from '{}'", blessingTypeId, fileLocation, exception);
			}
		}
		return prepared;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> preparations, ResourceManager manager, ProfilerFiller profiler) {
		this.parameters = Collections.unmodifiableMap(Maps.newLinkedHashMap(preparations));
	}
}
