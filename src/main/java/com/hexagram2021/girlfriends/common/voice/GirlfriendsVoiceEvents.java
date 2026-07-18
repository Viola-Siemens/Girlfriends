package com.hexagram2021.girlfriends.common.voice;

import com.google.common.collect.Lists;
import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 角色语音 SoundEvent 注册表 + locale → voiceKey → Holder 两级索引喵~
 *
 * @author liudongyu
 */
public final class GirlfriendsVoiceEvents {
	public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GirlfriendsMod.MODID);

	private static final List<VoiceRegistryRecord> GIRLFRIENDS_VOICES = Lists.newArrayList();

	private static final String ZH_CN = "zh_cn";

	// registryPath 格式: gift.quote.{locale}.{voiceKey}，确保多语言 key 不冲突喵~

	// region 沫沫 (momo) — favorite 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_0 =
			registerGiftQuote(ZH_CN, "momo.favorite_girlfriends_bouquet_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_1 =
			registerGiftQuote(ZH_CN, "momo.favorite_girlfriends_bouquet_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_GIRLFRIENDS_BOUQUET_2 =
			registerGiftQuote(ZH_CN, "momo.favorite_girlfriends_bouquet_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_MINECRAFT_HONEYCOMB_0 =
			registerGiftQuote(ZH_CN, "momo.favorite_minecraft_honeycomb_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_MINECRAFT_HONEYCOMB_1 =
			registerGiftQuote(ZH_CN, "momo.favorite_minecraft_honeycomb_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_FAVORITE_MINECRAFT_HONEYCOMB_2 =
			registerGiftQuote(ZH_CN, "momo.favorite_minecraft_honeycomb_2");
	// endregion

	// region 沫沫 (momo) — liked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_0 =
			registerGiftQuote(ZH_CN, "momo.liked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_1 =
			registerGiftQuote(ZH_CN, "momo.liked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_2 =
			registerGiftQuote(ZH_CN, "momo.liked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_3 =
			registerGiftQuote(ZH_CN, "momo.liked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_4 =
			registerGiftQuote(ZH_CN, "momo.liked_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_5 =
			registerGiftQuote(ZH_CN, "momo.liked_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_6 =
			registerGiftQuote(ZH_CN, "momo.liked_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_LIKED_7 =
			registerGiftQuote(ZH_CN, "momo.liked_7");
	// endregion

	// region 沫沫 (momo) — accepted 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_0 =
			registerGiftQuote(ZH_CN, "momo.accepted_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_1 =
			registerGiftQuote(ZH_CN, "momo.accepted_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_2 =
			registerGiftQuote(ZH_CN, "momo.accepted_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_3 =
			registerGiftQuote(ZH_CN, "momo.accepted_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_4 =
			registerGiftQuote(ZH_CN, "momo.accepted_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_ACCEPTED_5 =
			registerGiftQuote(ZH_CN, "momo.accepted_5");
	// endregion

	// region 沫沫 (momo) — rejected 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_REJECTED_0 =
			registerGiftQuote(ZH_CN, "momo.rejected_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_REJECTED_1 =
			registerGiftQuote(ZH_CN, "momo.rejected_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_REJECTED_2 =
			registerGiftQuote(ZH_CN, "momo.rejected_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_REJECTED_3 =
			registerGiftQuote(ZH_CN, "momo.rejected_3");
	// endregion

	// region 沫沫 (momo) — disliked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_DISLIKED_0 =
			registerGiftQuote(ZH_CN, "momo.disliked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_DISLIKED_1 =
			registerGiftQuote(ZH_CN, "momo.disliked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_DISLIKED_2 =
			registerGiftQuote(ZH_CN, "momo.disliked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_DISLIKED_3 =
			registerGiftQuote(ZH_CN, "momo.disliked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOMO_DISLIKED_4 =
			registerGiftQuote(ZH_CN, "momo.disliked_4");
	// endregion

	// region 渔溪 (yuxi) — favorite 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_NAUTILUS_SHELL_0 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_nautilus_shell_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_NAUTILUS_SHELL_1 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_nautilus_shell_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_NAUTILUS_SHELL_2 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_nautilus_shell_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_HEART_OF_THE_SEA_0 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_heart_of_the_sea_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_HEART_OF_THE_SEA_1 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_heart_of_the_sea_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_FAVORITE_MINECRAFT_HEART_OF_THE_SEA_2 =
			registerGiftQuote(ZH_CN, "yuxi.favorite_minecraft_heart_of_the_sea_2");
	// endregion

	// region 渔溪 (yuxi) — liked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_0 =
			registerGiftQuote(ZH_CN, "yuxi.liked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_1 =
			registerGiftQuote(ZH_CN, "yuxi.liked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_2 =
			registerGiftQuote(ZH_CN, "yuxi.liked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_3 =
			registerGiftQuote(ZH_CN, "yuxi.liked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_4 =
			registerGiftQuote(ZH_CN, "yuxi.liked_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_5 =
			registerGiftQuote(ZH_CN, "yuxi.liked_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_6 =
			registerGiftQuote(ZH_CN, "yuxi.liked_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_LIKED_7 =
			registerGiftQuote(ZH_CN, "yuxi.liked_7");
	// endregion

	// region 渔溪 (yuxi) — accepted 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_0 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_1 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_2 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_3 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_4 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_ACCEPTED_5 =
			registerGiftQuote(ZH_CN, "yuxi.accepted_5");
	// endregion

	// region 渔溪 (yuxi) — rejected 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_REJECTED_0 =
			registerGiftQuote(ZH_CN, "yuxi.rejected_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_REJECTED_1 =
			registerGiftQuote(ZH_CN, "yuxi.rejected_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_REJECTED_2 =
			registerGiftQuote(ZH_CN, "yuxi.rejected_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_REJECTED_3 =
			registerGiftQuote(ZH_CN, "yuxi.rejected_3");
	// endregion

	// region 渔溪 (yuxi) — disliked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_DISLIKED_0 =
			registerGiftQuote(ZH_CN, "yuxi.disliked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_DISLIKED_1 =
			registerGiftQuote(ZH_CN, "yuxi.disliked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_DISLIKED_2 =
			registerGiftQuote(ZH_CN, "yuxi.disliked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_DISLIKED_3 =
			registerGiftQuote(ZH_CN, "yuxi.disliked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YUXI_DISLIKED_4 =
			registerGiftQuote(ZH_CN, "yuxi.disliked_4");
	// endregion

	// region 梅疏 (meishu) — favorite 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_IRON_INGOT_0 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_iron_ingot_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_IRON_INGOT_1 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_iron_ingot_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_IRON_INGOT_2 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_iron_ingot_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_NETHERITE_INGOT_0 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_netherite_ingot_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_NETHERITE_INGOT_1 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_netherite_ingot_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_FAVORITE_MINECRAFT_NETHERITE_INGOT_2 =
			registerGiftQuote(ZH_CN, "meishu.favorite_minecraft_netherite_ingot_2");
	// endregion

	// region 梅疏 (meishu) — liked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_0 =
			registerGiftQuote(ZH_CN, "meishu.liked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_1 =
			registerGiftQuote(ZH_CN, "meishu.liked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_2 =
			registerGiftQuote(ZH_CN, "meishu.liked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_3 =
			registerGiftQuote(ZH_CN, "meishu.liked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_4 =
			registerGiftQuote(ZH_CN, "meishu.liked_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_5 =
			registerGiftQuote(ZH_CN, "meishu.liked_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_6 =
			registerGiftQuote(ZH_CN, "meishu.liked_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_LIKED_7 =
			registerGiftQuote(ZH_CN, "meishu.liked_7");
	// endregion

	// region 梅疏 (meishu) — accepted 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_0 =
			registerGiftQuote(ZH_CN, "meishu.accepted_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_1 =
			registerGiftQuote(ZH_CN, "meishu.accepted_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_2 =
			registerGiftQuote(ZH_CN, "meishu.accepted_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_3 =
			registerGiftQuote(ZH_CN, "meishu.accepted_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_4 =
			registerGiftQuote(ZH_CN, "meishu.accepted_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_ACCEPTED_5 =
			registerGiftQuote(ZH_CN, "meishu.accepted_5");
	// endregion

	// region 梅疏 (meishu) — rejected 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_REJECTED_0 =
			registerGiftQuote(ZH_CN, "meishu.rejected_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_REJECTED_1 =
			registerGiftQuote(ZH_CN, "meishu.rejected_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_REJECTED_2 =
			registerGiftQuote(ZH_CN, "meishu.rejected_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_REJECTED_3 =
			registerGiftQuote(ZH_CN, "meishu.rejected_3");
	// endregion

	// region 梅疏 (meishu) — disliked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_DISLIKED_0 =
			registerGiftQuote(ZH_CN, "meishu.disliked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_DISLIKED_1 =
			registerGiftQuote(ZH_CN, "meishu.disliked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_DISLIKED_2 =
			registerGiftQuote(ZH_CN, "meishu.disliked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_DISLIKED_3 =
			registerGiftQuote(ZH_CN, "meishu.disliked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> MEISHU_DISLIKED_4 =
			registerGiftQuote(ZH_CN, "meishu.disliked_4");
	// endregion

	// region 晚萤 (wanying) — favorite 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BLAZE_ROD_0 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_blaze_rod_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BLAZE_ROD_1 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_blaze_rod_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BLAZE_ROD_2 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_blaze_rod_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BREEZE_ROD_0 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_breeze_rod_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BREEZE_ROD_1 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_breeze_rod_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_FAVORITE_MINECRAFT_BREEZE_ROD_2 =
			registerGiftQuote(ZH_CN, "wanying.favorite_minecraft_breeze_rod_2");
	// endregion

	// region 晚萤 (wanying) — liked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_0 =
			registerGiftQuote(ZH_CN, "wanying.liked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_1 =
			registerGiftQuote(ZH_CN, "wanying.liked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_2 =
			registerGiftQuote(ZH_CN, "wanying.liked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_3 =
			registerGiftQuote(ZH_CN, "wanying.liked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_4 =
			registerGiftQuote(ZH_CN, "wanying.liked_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_5 =
			registerGiftQuote(ZH_CN, "wanying.liked_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_6 =
			registerGiftQuote(ZH_CN, "wanying.liked_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_LIKED_7 =
			registerGiftQuote(ZH_CN, "wanying.liked_7");
	// endregion

	// region 晚萤 (wanying) — accepted 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_0 =
			registerGiftQuote(ZH_CN, "wanying.accepted_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_1 =
			registerGiftQuote(ZH_CN, "wanying.accepted_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_2 =
			registerGiftQuote(ZH_CN, "wanying.accepted_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_3 =
			registerGiftQuote(ZH_CN, "wanying.accepted_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_4 =
			registerGiftQuote(ZH_CN, "wanying.accepted_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_ACCEPTED_5 =
			registerGiftQuote(ZH_CN, "wanying.accepted_5");
	// endregion

	// region 晚萤 (wanying) — rejected 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_REJECTED_0 =
			registerGiftQuote(ZH_CN, "wanying.rejected_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_REJECTED_1 =
			registerGiftQuote(ZH_CN, "wanying.rejected_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_REJECTED_2 =
			registerGiftQuote(ZH_CN, "wanying.rejected_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_REJECTED_3 =
			registerGiftQuote(ZH_CN, "wanying.rejected_3");
	// endregion

	// region 晚萤 (wanying) — disliked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_DISLIKED_0 =
			registerGiftQuote(ZH_CN, "wanying.disliked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_DISLIKED_1 =
			registerGiftQuote(ZH_CN, "wanying.disliked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_DISLIKED_2 =
			registerGiftQuote(ZH_CN, "wanying.disliked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_DISLIKED_3 =
			registerGiftQuote(ZH_CN, "wanying.disliked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WANYING_DISLIKED_4 =
			registerGiftQuote(ZH_CN, "wanying.disliked_4");
	// endregion

	// region 幽若 (youruo) — favorite 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_PEARL_0 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_pearl_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_PEARL_1 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_pearl_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_PEARL_2 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_pearl_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_EYE_0 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_eye_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_EYE_1 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_eye_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_FAVORITE_MINECRAFT_ENDER_EYE_2 =
			registerGiftQuote(ZH_CN, "youruo.favorite_minecraft_ender_eye_2");
	// endregion

	// region 幽若 (youruo) — liked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_0 =
			registerGiftQuote(ZH_CN, "youruo.liked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_1 =
			registerGiftQuote(ZH_CN, "youruo.liked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_2 =
			registerGiftQuote(ZH_CN, "youruo.liked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_3 =
			registerGiftQuote(ZH_CN, "youruo.liked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_4 =
			registerGiftQuote(ZH_CN, "youruo.liked_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_5 =
			registerGiftQuote(ZH_CN, "youruo.liked_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_6 =
			registerGiftQuote(ZH_CN, "youruo.liked_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_LIKED_7 =
			registerGiftQuote(ZH_CN, "youruo.liked_7");
	// endregion

	// region 幽若 (youruo) — accepted 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_0 =
			registerGiftQuote(ZH_CN, "youruo.accepted_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_1 =
			registerGiftQuote(ZH_CN, "youruo.accepted_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_2 =
			registerGiftQuote(ZH_CN, "youruo.accepted_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_3 =
			registerGiftQuote(ZH_CN, "youruo.accepted_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_4 =
			registerGiftQuote(ZH_CN, "youruo.accepted_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_ACCEPTED_5 =
			registerGiftQuote(ZH_CN, "youruo.accepted_5");
	// endregion

	// region 幽若 (youruo) — rejected 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_REJECTED_0 =
			registerGiftQuote(ZH_CN, "youruo.rejected_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_REJECTED_1 =
			registerGiftQuote(ZH_CN, "youruo.rejected_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_REJECTED_2 =
			registerGiftQuote(ZH_CN, "youruo.rejected_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_REJECTED_3 =
			registerGiftQuote(ZH_CN, "youruo.rejected_3");
	// endregion

	// region 幽若 (youruo) — disliked 喵~
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_DISLIKED_0 =
			registerGiftQuote(ZH_CN, "youruo.disliked_0");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_DISLIKED_1 =
			registerGiftQuote(ZH_CN, "youruo.disliked_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_DISLIKED_2 =
			registerGiftQuote(ZH_CN, "youruo.disliked_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_DISLIKED_3 =
			registerGiftQuote(ZH_CN, "youruo.disliked_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> YOURUO_DISLIKED_4 =
			registerGiftQuote(ZH_CN, "youruo.disliked_4");
	// endregion

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
	private static DeferredHolder<SoundEvent, SoundEvent> registerGiftQuote(String locale, String voiceKey) {
		String registryPath = "gift.quote." + locale + "." + voiceKey;
		DeferredHolder<SoundEvent, SoundEvent> holder = REGISTER.register(
				registryPath,
				() -> SoundEvent.createVariableRangeEvent(
						Identifier.fromNamespaceAndPath(GirlfriendsMod.MODID, registryPath)
				)
		);
		// 类初始化所有字段完成注册，保证会在同一个线程中执行
		GIRLFRIENDS_VOICES.add(new VoiceRegistryRecord(locale, voiceKey, holder));
		return holder;
	}

	private record VoiceRegistryRecord(String locale, String voiceKey, DeferredHolder<SoundEvent, SoundEvent> holder) {
	}

	/**
	 * 注册本模组所有语音。注册后清空，以保证幂等性。<br/>
	 * 请仅在主线程访问该方法！
	 */
	@ApiStatus.Internal
	public static void registerVoices() {
		// 跨模组注册到汇总 Map 中，需保证所有模组在主线程操作
		GIRLFRIENDS_VOICES.forEach(rec -> GirlfriendsVoiceManager.registerVoice(
				rec.locale, rec.voiceKey, rec.holder
		));
		GIRLFRIENDS_VOICES.clear();
	}
}
