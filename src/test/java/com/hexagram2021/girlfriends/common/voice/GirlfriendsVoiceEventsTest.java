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
	void voiceMapContainsAllZhCnEntries() {
		Map<String, Map<String, DeferredHolder<SoundEvent, SoundEvent>>> voiceMap =
				GirlfriendsVoiceManager.VOICE_MAP;

		assertTrue(voiceMap.containsKey("zh_cn"), "VOICE_MAP should contain zh_cn locale");
		Map<String, DeferredHolder<SoundEvent, SoundEvent>> zhCnMap = voiceMap.get("zh_cn");
		assertNotNull(zhCnMap);
		assertEquals(145, zhCnMap.size(), "zh_cn should have 145 voice entries (5 chars × 5 categories)");

		// 验证五位角色各档位均有点位存在喵~
		assertTrue(zhCnMap.containsKey("momo.favorite_girlfriends_bouquet_0"));
		assertTrue(zhCnMap.containsKey("momo.favorite_minecraft_honeycomb_2"));
		assertTrue(zhCnMap.containsKey("momo.liked_7"));
		assertTrue(zhCnMap.containsKey("momo.accepted_0"));
		assertTrue(zhCnMap.containsKey("momo.rejected_3"));
		assertTrue(zhCnMap.containsKey("momo.disliked_4"));

		assertTrue(zhCnMap.containsKey("yuxi.favorite_minecraft_nautilus_shell_0"));
		assertTrue(zhCnMap.containsKey("yuxi.liked_7"));
		assertTrue(zhCnMap.containsKey("yuxi.accepted_0"));
		assertTrue(zhCnMap.containsKey("yuxi.rejected_3"));
		assertTrue(zhCnMap.containsKey("yuxi.disliked_4"));

		assertTrue(zhCnMap.containsKey("meishu.favorite_minecraft_iron_ingot_0"));
		assertTrue(zhCnMap.containsKey("meishu.liked_7"));
		assertTrue(zhCnMap.containsKey("meishu.accepted_0"));
		assertTrue(zhCnMap.containsKey("meishu.rejected_3"));
		assertTrue(zhCnMap.containsKey("meishu.disliked_4"));

		assertTrue(zhCnMap.containsKey("wanying.favorite_minecraft_blaze_rod_0"));
		assertTrue(zhCnMap.containsKey("wanying.liked_7"));
		assertTrue(zhCnMap.containsKey("wanying.accepted_0"));
		assertTrue(zhCnMap.containsKey("wanying.rejected_3"));
		assertTrue(zhCnMap.containsKey("wanying.disliked_4"));

		assertTrue(zhCnMap.containsKey("youruo.favorite_minecraft_ender_pearl_0"));
		assertTrue(zhCnMap.containsKey("youruo.liked_7"));
		assertTrue(zhCnMap.containsKey("youruo.accepted_0"));
		assertTrue(zhCnMap.containsKey("youruo.rejected_3"));
		assertTrue(zhCnMap.containsKey("youruo.disliked_4"));
	}

	@Test
	void holderRegistryNameMatchesLocaleAndVoiceKey() {
		DeferredHolder<SoundEvent, SoundEvent> holder = GirlfriendsVoiceEvents.MOMO_LIKED_0;

		Identifier expectedId = Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, "gift.quote.zh_cn.momo.liked_0");
		assertEquals(expectedId, holder.getId());
	}

	@Test
	void voiceMapReferencesSameHolderAsStaticField() {
		DeferredHolder<SoundEvent, SoundEvent> staticHolder = GirlfriendsVoiceEvents.MOMO_LIKED_0;
		DeferredHolder<SoundEvent, SoundEvent> mapHolder =
				GirlfriendsVoiceManager.VOICE_MAP.get("zh_cn").get("momo.liked_0");

		assertSame(staticHolder, mapHolder, "Static field and VOICE_MAP entry should reference the same DeferredHolder");
	}

	@Test
	void registryKeyIsSoundEvent() {
		assertEquals(BuiltInRegistries.SOUND_EVENT.key(), GirlfriendsVoiceEvents.REGISTER.getRegistryKey());
	}
}
