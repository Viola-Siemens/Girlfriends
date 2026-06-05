package com.hexagram2021.girlfriends.common.relationship;

/**
 * 好感阶段定义喵~
 *
 * @author liudongyu
 */
public enum AffectionStage {
	STRANGER(0, 99),
	FAMILIAR(100, 199),
	TRUST(200, 499),
	AFFECTION(500, 899),
	INTIMATE(700, 899),
	HOME_PARTNER(900, 1000);

	private final int minAffection;
	private final int maxAffection;

	AffectionStage(int minAffection, int maxAffection) {
		this.minAffection = minAffection;
		this.maxAffection = maxAffection;
	}

	/**
	 * 获取最小好感度喵~
	 *
	 * @return 最小好感度喵~
	 */
	public int getMinAffection() {
		return this.minAffection;
	}

	/**
	 * 获取最大好感度喵~
	 *
	 * @return 最大好感度喵~
	 */
	public int getMaxAffection() {
		return this.maxAffection;
	}
}
