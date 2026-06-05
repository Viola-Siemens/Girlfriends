package com.hexagram2021.girlfriends.common.gift;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色礼物偏好定义喵~
 *
 * @author liudongyu
 */
public final class GiftPreference {
	private final Identifier characterTypeId;
	private final Set<Identifier> favoriteItems;
	private final Set<Identifier> likedItems;
	private final Set<Identifier> likedTags;
	private final Set<Identifier> acceptedItems;
	private final Set<Identifier> acceptedTags;
	private final Set<Identifier> dislikedItems;
	private final Set<Identifier> dislikedTags;

	/**
	 * 创建角色礼物偏好喵~
	 *
	 * @param characterTypeId 角色类型 ID 喵~
	 * @param favoriteItems 最喜欢物品集合喵~
	 * @param likedItems 喜欢物品集合喵~
	 * @param likedTags 喜欢标签集合喵~
	 * @param acceptedItems 可接受物品集合喵~
	 * @param acceptedTags 可接受标签集合喵~
	 * @param dislikedItems 不喜欢物品集合喵~
	 * @param dislikedTags 不喜欢标签集合喵~
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
		this.favoriteItems = immutableCopy(favoriteItems);
		this.likedItems = immutableCopy(likedItems);
		this.likedTags = immutableCopy(likedTags);
		this.acceptedItems = immutableCopy(acceptedItems);
		this.acceptedTags = immutableCopy(acceptedTags);
		this.dislikedItems = immutableCopy(dislikedItems);
		this.dislikedTags = immutableCopy(dislikedTags);
	}

	/**
	 * 获取角色类型 ID 喵~
	 *
	 * @return 角色类型 ID 喵~
	 */
	public Identifier getCharacterTypeId() {
		return this.characterTypeId;
	}

	/**
	 * 获取最喜欢物品集合喵~
	 *
	 * @return 最喜欢物品集合喵~
	 */
	public Set<Identifier> getFavoriteItems() {
		return this.favoriteItems;
	}

	/**
	 * 获取喜欢物品集合喵~
	 *
	 * @return 喜欢物品集合喵~
	 */
	public Set<Identifier> getLikedItems() {
		return this.likedItems;
	}

	/**
	 * 获取喜欢标签集合喵~
	 *
	 * @return 喜欢标签集合喵~
	 */
	public Set<Identifier> getLikedTags() {
		return this.likedTags;
	}

	/**
	 * 获取可接受物品集合喵~
	 *
	 * @return 可接受物品集合喵~
	 */
	public Set<Identifier> getAcceptedItems() {
		return this.acceptedItems;
	}

	/**
	 * 获取可接受标签集合喵~
	 *
	 * @return 可接受标签集合喵~
	 */
	public Set<Identifier> getAcceptedTags() {
		return this.acceptedTags;
	}

	/**
	 * 获取不喜欢物品集合喵~
	 *
	 * @return 不喜欢物品集合喵~
	 */
	public Set<Identifier> getDislikedItems() {
		return this.dislikedItems;
	}

	/**
	 * 获取不喜欢标签集合喵~
	 *
	 * @return 不喜欢标签集合喵~
	 */
	public Set<Identifier> getDislikedTags() {
		return this.dislikedTags;
	}

	private static Set<Identifier> immutableCopy(Set<Identifier> source) {
		return Collections.unmodifiableSet(new LinkedHashSet<>(source));
	}
}
