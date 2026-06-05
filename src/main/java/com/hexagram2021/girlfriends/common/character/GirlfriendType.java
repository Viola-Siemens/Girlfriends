package com.hexagram2021.girlfriends.common.character;

import net.minecraft.resources.Identifier;

/**
 * 角色类型定义喵~
 *
 * @author liudongyu
 */
public class GirlfriendType {
	private final String descriptionId;
	private final DimensionPolicy dimensionPolicy;
	private final Identifier favoriteGiftItem;
	private final Identifier blessingTypeId;
	private final Identifier shelterStructureKey;

	/**
	 * 创建角色类型喵~
	 *
	 * @param descriptionId 描述键喵~
	 * @param dimensionPolicy 维度策略喵~
	 * @param favoriteGiftItem 偏好礼物物品 ID 喵~
	 * @param blessingTypeId 祝福类型 ID 喵~
	 * @param shelterStructureKey 庇护所结构 ID 喵~
	 */
	public GirlfriendType(String descriptionId, DimensionPolicy dimensionPolicy, Identifier favoriteGiftItem,
			Identifier blessingTypeId, Identifier shelterStructureKey) {
		this.descriptionId = descriptionId;
		this.dimensionPolicy = dimensionPolicy;
		this.favoriteGiftItem = favoriteGiftItem;
		this.blessingTypeId = blessingTypeId;
		this.shelterStructureKey = shelterStructureKey;
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
	 * 获取维度策略喵~
	 *
	 * @return 维度策略喵~
	 */
	public DimensionPolicy getDimensionPolicy() {
		return this.dimensionPolicy;
	}

	/**
	 * 获取偏好礼物物品 ID 喵~
	 *
	 * @return 偏好礼物物品 ID 喵~
	 */
	public Identifier getFavoriteGiftItem() {
		return this.favoriteGiftItem;
	}

	/**
	 * 获取祝福类型 ID 喵~
	 *
	 * @return 祝福类型 ID 喵~
	 */
	public Identifier getBlessingTypeId() {
		return this.blessingTypeId;
	}

	/**
	 * 获取庇护所结构 ID 喵~
	 *
	 * @return 庇护所结构 ID 喵~
	 */
	public Identifier getShelterStructureKey() {
		return this.shelterStructureKey;
	}
}
