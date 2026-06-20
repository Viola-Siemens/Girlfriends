package com.hexagram2021.girlfriends.common.character;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * 维度策略值对象喵~
 *
 * @author liudongyu
 */
public record DimensionPolicy(Set<Identifier> allowedDimensions) {
	/**
	 * 判断是否允许指定维度喵~
	 *
	 * @param dimensionId 维度 ID 喵~
	 * @return 是否允许喵~
	 */
	public boolean allows(Identifier dimensionId) {
		return this.allowedDimensions.contains(dimensionId);
	}
}
