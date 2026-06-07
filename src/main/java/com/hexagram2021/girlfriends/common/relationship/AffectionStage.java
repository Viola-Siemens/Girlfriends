package com.hexagram2021.girlfriends.common.relationship;

/**
 * 好感阶段定义喵~
 *
 * @author liudongyu
 */
public enum AffectionStage {
	STRANGER(0.0F, 100.0F),
	FAMILIAR(100.0F, 200.0F),
	TRUST(200.0F, 500.0F),
	AFFECTION(500.0F, 700.0F),
	INTIMATE(700.0F, 900.0F),
	HOME_PARTNER(900.0F, 1000.0F);

	private final float minAffection;
	private final float maxAffection;

	AffectionStage(float minAffection, float maxAffection) {
		this.minAffection = minAffection;
		this.maxAffection = maxAffection;
	}

	/**
	 * 获取最小好感度喵~
	 *
	 * @return 最小好感度喵~
	 */
	public float getMinAffection() {
		return this.minAffection;
	}

	/**
	 * 获取最大好感度（不含）喵~
	 *
	 * @return 最大好感度喵~
	 */
	public float getMaxAffection() {
		return this.maxAffection;
	}
}
