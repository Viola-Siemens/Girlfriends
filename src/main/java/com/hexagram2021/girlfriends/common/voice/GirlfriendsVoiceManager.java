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
