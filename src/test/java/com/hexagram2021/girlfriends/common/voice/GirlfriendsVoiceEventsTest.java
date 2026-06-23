package com.hexagram2021.girlfriends.common.voice;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GirlfriendsVoiceEvents 注册与 VOICE_MAP 一致性测试喵~
 *
 * @author liudongyu
 */
class GirlfriendsVoiceEventsTest {
	@Test
	void voiceMapContainsAllFiveEntries() {
		Map<String, Map<String, DeferredHolder<SoundEvent, SoundEvent>>> voiceMap =
				GirlfriendsVoiceEvents.VOICE_MAP;

		assertTrue(voiceMap.containsKey("zh_cn"), "VOICE_MAP should contain zh_cn locale");
		Map<String, DeferredHolder<SoundEvent, SoundEvent>> zhCnMap = voiceMap.get("zh_cn");
		assertNotNull(zhCnMap);
		assertEquals(5, zhCnMap.size(), "zh_cn should have 5 voice entries");

		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_0"));
		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_1"));
		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_2"));
		assertTrue(zhCnMap.containsKey("momo.liked_0"));
		assertTrue(zhCnMap.containsKey("momo.liked_1"));
	}

	@Test
	void holderRegistryNameMatchesLocaleAndVoiceKey() {
		DeferredHolder<SoundEvent, SoundEvent> holder =
				GirlfriendsVoiceEvents.MOMO_LIKED_0;

		Identifier expectedId = Identifier.fromNamespaceAndPath(
				GirlfriendsMod.MODID, "gift.quote.zh_cn.momo.liked_0");
		assertEquals(expectedId, holder.getId());
	}

	@Test
	void voiceMapReferencesSameHolderAsStaticField() {
		DeferredHolder<SoundEvent, SoundEvent> staticHolder =
				GirlfriendsVoiceEvents.MOMO_LIKED_0;
		DeferredHolder<SoundEvent, SoundEvent> mapHolder =
				GirlfriendsVoiceEvents.VOICE_MAP.get("zh_cn").get("momo.liked_0");

		assertSame(staticHolder, mapHolder,
				"Static field and VOICE_MAP entry should reference the same DeferredHolder");
	}

	@Test
	void registryKeyIsSoundEvent() {
		assertEquals(BuiltInRegistries.SOUND_EVENT.key(),
				GirlfriendsVoiceEvents.REGISTER.getRegistryKey());
	}
}
