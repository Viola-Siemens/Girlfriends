package com.hexagram2021.girlfriends.common.voice;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
