package com.hexagram2021.girlfriends.common.gift;

/**
 * 礼物偏好等级定义喵~
 *
 * @author liudongyu
 */
public enum GiftPreferenceLevel {
	FAVORITE(5),
	LIKED(4),
	ACCEPTED(2),
	DISLIKED(-2),
	REJECTED(0);

	private final int baseDelta;

	GiftPreferenceLevel(int baseDelta) {
		this.baseDelta = baseDelta;
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
}
