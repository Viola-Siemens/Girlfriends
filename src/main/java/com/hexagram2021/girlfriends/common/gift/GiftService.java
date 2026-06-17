package com.hexagram2021.girlfriends.common.gift;

import com.hexagram2021.girlfriends.common.relationship.AffectionChangeSource;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * 赠礼服务喵~
 *
 * @author liudongyu
 */
public class GiftService {
	private static final float DAILY_GIFT_GAIN_CAP = 15.0F;
	private static final String MESSAGE_KEY_ACCEPTED = "girlfriends.gift.accepted";
	private static final String MESSAGE_KEY_DISLIKED = "girlfriends.gift.disliked";
	private static final String MESSAGE_KEY_REJECTED = "girlfriends.gift.rejected";
	private static final String MESSAGE_KEY_CAP_REACHED = "girlfriends.gift.daily_cap_reached";
	private static final String MESSAGE_KEY_PERMISSION_DENIED = "girlfriends.gift.permission_denied";

	private final RelationshipService relationshipService;
	@Nullable
	private final GiftPreferenceManager giftPreferenceManager;
	private final BiPredicate<UUID, Identifier> canReceiveGift;

	/**
	 * 创建仅带收礼判定的赠礼服务喵~
	 *
	 * @param relationshipService 关系服务喵~
	 * @param canReceiveGift 是否允许收礼判定喵~
	 */
	public GiftService(RelationshipService relationshipService, BiPredicate<UUID, Identifier> canReceiveGift) {
		this(relationshipService, null, canReceiveGift);
	}

	/**
	 * 创建赠礼服务喵~
	 *
	 * @param relationshipService 关系服务喵~
	 */
	public GiftService(RelationshipService relationshipService) {
		this(relationshipService, null, (playerUuid, girlfriendTypeId) -> true);
	}

	/**
	 * 创建赠礼服务喵~
	 *
	 * @param relationshipService 关系服务喵~
	 * @param giftPreferenceManager 礼物偏好管理器喵~
	 */
	public GiftService(RelationshipService relationshipService, @Nullable GiftPreferenceManager giftPreferenceManager) {
		this(relationshipService, giftPreferenceManager, (playerUuid, girlfriendTypeId) -> true);
	}

	/**
	 * 创建赠礼服务喵~
	 *
	 * @param relationshipService 关系服务喵~
	 * @param giftPreferenceManager 礼物偏好管理器喵~
	 * @param canReceiveGift 是否允许收礼判定喵~
	 */
	public GiftService(
			RelationshipService relationshipService,
			@Nullable GiftPreferenceManager giftPreferenceManager,
			BiPredicate<UUID, Identifier> canReceiveGift
	) {
		this.relationshipService = Objects.requireNonNull(relationshipService);
		this.giftPreferenceManager = giftPreferenceManager;
		this.canReceiveGift = Objects.requireNonNull(canReceiveGift);
	}

	/**
	 * 按偏好等级应用赠礼结果喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param level 礼物偏好等级喵~
	 * @return 赠礼结果喵~
	 */
	public GiftResult applyGift(UUID playerUuid, Identifier girlfriendTypeId, GiftPreferenceLevel level) {
		if(level == GiftPreferenceLevel.REJECTED) {
			return GiftResult.rejected(level, MESSAGE_KEY_REJECTED);
		}
		if(!this.canReceiveGift.test(playerUuid, girlfriendTypeId)) {
			return GiftResult.rejected(level, MESSAGE_KEY_PERMISSION_DENIED);
		}
		PlayerCharacterRelation relation = this.relationshipService.getRelation(playerUuid, girlfriendTypeId);
		if(level.isPositive() && relation.getDailyGiftGain() >= DAILY_GIFT_GAIN_CAP) {
			return GiftResult.rejected(level, MESSAGE_KEY_CAP_REACHED);
		}
		float affectionDelta = computeAffectionDelta(level, relation.getDailyGiftGain());
		this.relationshipService.changeAffection(playerUuid, girlfriendTypeId, AffectionChangeSource.GIFT, affectionDelta);
		if(affectionDelta > 0) {
			relation.setDailyGiftGain(Math.min(DAILY_GIFT_GAIN_CAP, relation.getDailyGiftGain() + affectionDelta));
		}
		return GiftResult.accepted(level, affectionDelta, affectionDelta > 0 ? MESSAGE_KEY_ACCEPTED : MESSAGE_KEY_DISLIKED);
	}

	/**
	 * 按物品判定并应用赠礼结果喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param girlfriendTypeId 角色类型 ID 喵~
	 * @param itemStack 礼物物品栈喵~
	 * @return 赠礼结果喵~
	 */
	public GiftResult applyGiftItem(UUID playerUuid, Identifier girlfriendTypeId, ItemStack itemStack) {
		GiftPreferenceLevel level = resolvePreferenceLevel(girlfriendTypeId, itemStack);
		return this.applyGift(playerUuid, girlfriendTypeId, level);
	}

	private GiftPreferenceLevel resolvePreferenceLevel(Identifier girlfriendTypeId, ItemStack itemStack) {
		if(this.giftPreferenceManager == null) {
			return GiftPreferenceLevel.REJECTED;
		}
		return this.giftPreferenceManager.getPreference(girlfriendTypeId)
				.map(preference -> this.resolvePreferenceLevel(preference, itemStack))
				.orElse(GiftPreferenceLevel.REJECTED);
	}

	private GiftPreferenceLevel resolvePreferenceLevel(GiftPreference preference, ItemStack itemStack) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		if(preference.getFavoriteItems().contains(itemId)) {
			return GiftPreferenceLevel.FAVORITE;
		}
		if(preference.getLikedItems().contains(itemId) || matchesAnyTag(itemStack, preference.getLikedTags())) {
			return GiftPreferenceLevel.LIKED;
		}
		if(preference.getAcceptedItems().contains(itemId) || matchesAnyTag(itemStack, preference.getAcceptedTags())) {
			return GiftPreferenceLevel.ACCEPTED;
		}
		if(preference.getDislikedItems().contains(itemId) || matchesAnyTag(itemStack, preference.getDislikedTags())) {
			return GiftPreferenceLevel.DISLIKED;
		}
		return GiftPreferenceLevel.REJECTED;
	}

	private static boolean matchesAnyTag(ItemStack itemStack, Iterable<Identifier> tagIds) {
		for(Identifier tagId : tagIds) {
			if(itemStack.is(holder -> holder.is(ItemTags.create(tagId)))) {
				return true;
			}
		}
		return false;
	}

	private static float computeAffectionDelta(GiftPreferenceLevel level, float dailyGiftGain) {
		float factor = Mth.sqrt((16.0F - Math.min(dailyGiftGain, DAILY_GIFT_GAIN_CAP)) / 16.0F);
		float rawDelta = level.getBaseDelta() * factor;
		if(level.isPositive()) {
			return Math.max(1, rawDelta);
		}
		if(level.isNegative()) {
			return Math.min(-1, rawDelta);
		}
		return 0;
	}
}
