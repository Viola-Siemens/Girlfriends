package com.hexagram2021.girlfriends.common.character;

import net.minecraft.resources.Identifier;

/**
 * 角色类型定义喵~
 *
 * @param descriptionId       描述键喵~
 * @param dimensionPolicy     维度策略喵~
 * @param favoriteGiftItem    偏好礼物物品 ID 喵~
 * @param blessingTypeId      祝福类型 ID 喵~
 * @param shelterStructureKey 庇护所结构 ID 喵~
 *
 * @author liudongyu
 */
public record GirlfriendType(String descriptionId, DimensionPolicy dimensionPolicy, Identifier favoriteGiftItem,
							 Identifier blessingTypeId, Identifier shelterStructureKey) {
}
