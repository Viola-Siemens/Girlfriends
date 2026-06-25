package com.hexagram2021.girlfriends.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组通用配置文件
 *
 * @author liudongyu
 */
public final class GirlfriendsCommonConfig {
	public static final ModConfigSpec CONFIG;
	public static final ModConfigSpec.BooleanValue ENABLE_RELATION_BIND;
	public static final ModConfigSpec.BooleanValue ENABLE_QUESTS;

	private GirlfriendsCommonConfig() {
	}

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.push("girlfriends-common");
		ENABLE_RELATION_BIND = builder.comment("Set to false to disable relation binding.").define("ENABLE_RELATION_BIND", true);
		ENABLE_QUESTS = builder.comment("Set to false to disable quests.").define("ENABLE_QUESTS", true);
		builder.pop();
		CONFIG = builder.build();
	}
}
