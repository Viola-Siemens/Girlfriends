package com.hexagram2021.girlfriends.common.gift;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * 角色礼物偏好定义喵~
 *
 * @author liudongyu
 */
public record GiftPreference(Identifier characterTypeId,Set<Identifier> favoriteItems, Set<Identifier> likedItems,
							 Set<Identifier> likedTags, Set<Identifier> acceptedItems, Set<Identifier> acceptedTags,
							 Set<Identifier> dislikedItems, Set<Identifier> dislikedTags) {
	/**
	 * 创建角色礼物偏好喵~
	 *
	 * @param characterTypeId 角色类型 ID 喵~
	 * @param favoriteItems   最喜欢物品集合喵~
	 * @param likedItems      喜欢物品集合喵~
	 * @param likedTags       喜欢标签集合喵~
	 * @param acceptedItems   可接受物品集合喵~
	 * @param acceptedTags    可接受标签集合喵~
	 * @param dislikedItems   不喜欢物品集合喵~
	 * @param dislikedTags    不喜欢标签集合喵~
	 */
	public GiftPreference(
			Identifier characterTypeId,
			Set<Identifier> favoriteItems,
			Set<Identifier> likedItems,
			Set<Identifier> likedTags,
			Set<Identifier> acceptedItems,
			Set<Identifier> acceptedTags,
			Set<Identifier> dislikedItems,
			Set<Identifier> dislikedTags
	) {
		this.characterTypeId = characterTypeId;
		this.favoriteItems = ImmutableSet.copyOf(favoriteItems);
		this.likedItems = ImmutableSet.copyOf(likedItems);
		this.likedTags = ImmutableSet.copyOf(likedTags);
		this.acceptedItems = ImmutableSet.copyOf(acceptedItems);
		this.acceptedTags = ImmutableSet.copyOf(acceptedTags);
		this.dislikedItems = ImmutableSet.copyOf(dislikedItems);
		this.dislikedTags = ImmutableSet.copyOf(dislikedTags);
	}
}
