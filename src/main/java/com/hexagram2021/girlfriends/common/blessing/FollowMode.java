package com.hexagram2021.girlfriends.common.blessing;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 角色跟随模式喵~
 *
 * @author liudongyu
 */
public enum FollowMode implements StringRepresentable {
	/** 非跟随 */
	NONE,
	/** 停留 */
	STAY,
	/** 跟随 */
	FOLLOW,
	/** 家园 */
	HOME;

	public static final Codec<FollowMode> CODEC = StringRepresentable.fromEnum(FollowMode::values);

	/**
	 * 根据序号获取跟随模式喵~
	 *
	 * @param id 序号喵~
	 * @return 对应的跟随模式，越界则返回 STAY 喵~
	 */
	public static FollowMode fromId(int id) {
		FollowMode[] values = values();
		if (id < 0 || id >= values.length) {
			return STAY;
		}
		return values[id];
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * STAY 或 FOLLOW 状态下无需回家
	 * @return 是否不需要寻路回家
	 */
	public boolean shouldIgnoreHome() {
		return this == STAY || this == FOLLOW;
	}
}
