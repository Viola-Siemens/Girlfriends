package com.hexagram2021.girlfriends.common.gift;

import net.minecraft.ChatFormatting;

/**
 * 礼物偏好等级定义喵~
 *
 * @author liudongyu
 */
public enum GiftPreferenceLevel {
	FAVORITE(5, ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
	LIKED(4, ChatFormatting.GOLD),
	ACCEPTED(2, ChatFormatting.GREEN),
	DISLIKED(-2, ChatFormatting.DARK_RED),
	REJECTED(0, ChatFormatting.GRAY);

	private final int baseDelta;
	private final ChatFormatting[] styles;

	GiftPreferenceLevel(int baseDelta, ChatFormatting... styles) {
		this.baseDelta = baseDelta;
		this.styles = styles;
	}

	/**
	 * 获取基础好感变更值喵~
	 *
	 * @return 基础好感变更值喵~
	 */
	public int getBaseDelta() {
		return this.baseDelta;
	}

	/**
	 * 判断是否为正向礼物喵~
	 *
	 * @return 是否为正向礼物喵~
	 */
	public boolean isPositive() {
		return this.baseDelta > 0;
	}

	/**
	 * 判断是否为负向礼物喵~
	 *
	 * @return 是否为负向礼物喵~
	 */
	public boolean isNegative() {
		return this.baseDelta < 0;
	}

	public ChatFormatting[] getStyles() {
		return this.styles;
	}
}
