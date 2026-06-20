package com.hexagram2021.girlfriends.common.blessing;

/**
 * 祝福类型定义喵~
 *
 * @author liudongyu
 */
public class BlessingType {
	private final String descriptionId;
	private final String behaviorKey;

	/**
	 * 创建祝福类型喵~
	 *
	 * @param descriptionId 描述键喵~
	 * @param behaviorKey 行为键喵~
	 */
	public BlessingType(String descriptionId, String behaviorKey) {
		this.descriptionId = descriptionId;
		this.behaviorKey = behaviorKey;
	}

	/**
	 * 获取描述键喵~
	 *
	 * @return 描述键喵~
	 */
	public String getDescriptionId() {
		return this.descriptionId;
	}

	/**
	 * 获取行为键喵~
	 *
	 * @return 行为键喵~
	 */
	public String getBehaviorKey() {
		return this.behaviorKey;
	}
}
