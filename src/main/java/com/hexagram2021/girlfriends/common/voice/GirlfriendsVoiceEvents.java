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
	// TODO 后续新增语音文件时同步添加喵~

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
