package com.hexagram2021.girlfriends.common.relationship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家与角色关系状态喵~
 *
 * @author liudongyu
 */
public class PlayerCharacterRelation {
	public static final int DATA_VERSION = 1;

	@Nullable
	private UUID playerUuid;
	@Nullable
	private Identifier characterId;
	private float affection;
	private boolean confirmedIntimacy;
	private final Set<Integer> completedFixedQuests = new LinkedHashSet<>();
	private boolean claimedFinalReward;
	private boolean homePartner;
	private float dailyGiftGain;
	private boolean dailyHomeGainClaimed;
	private boolean dailyConflictTriggered;
	private final Set<String> knownGiftPreferences = new LinkedHashSet<>();
	private long lastDailyResetDay;

	/**
	 * 创建空关系状态喵~
	 */
	public PlayerCharacterRelation() {
	}

	/**
	 * 创建带键的关系状态喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 * @param characterId 角色 ID 喵~
	 */
	public PlayerCharacterRelation(UUID playerUuid, Identifier characterId) {
		this.playerUuid = playerUuid;
		this.characterId = characterId;
	}

	/**
	 * 获取玩家 UUID 喵~
	 *
	 * @return 玩家 UUID 喵~
	 */
	@Nullable
	public UUID getPlayerUuid() {
		return this.playerUuid;
	}

	/**
	 * 设置玩家 UUID 喵~
	 *
	 * @param playerUuid 玩家 UUID 喵~
	 */
	public void setPlayerUuid(@Nullable UUID playerUuid) {
		this.playerUuid = playerUuid;
	}

	/**
	 * 获取角色 ID 喵~
	 *
	 * @return 角色 ID 喵~
	 */
	@Nullable
	public Identifier getCharacterId() {
		return this.characterId;
	}

	/**
	 * 设置角色 ID 喵~
	 *
	 * @param characterId 角色 ID 喵~
	 */
	public void setCharacterId(@Nullable Identifier characterId) {
		this.characterId = characterId;
	}

	/**
	 * 获取好感度喵~
	 *
	 * @return 好感度喵~
	 */
	public float getAffection() {
		return this.affection;
	}

	/**
	 * 设置好感度喵~
	 *
	 * @param affection 好感度喵~
	 */
	public void setAffection(float affection) {
		this.affection = affection;
	}

	/**
	 * 判断是否已确认亲密关系喵~
	 *
	 * @return 是否已确认喵~
	 */
	public boolean isConfirmedIntimacy() {
		return this.confirmedIntimacy;
	}

	/**
	 * 设置是否已确认亲密关系喵~
	 *
	 * @param confirmedIntimacy 是否已确认喵~
	 */
	public void setConfirmedIntimacy(boolean confirmedIntimacy) {
		this.confirmedIntimacy = confirmedIntimacy;
	}

	/**
	 * 获取已完成固定委托集合喵~
	 *
	 * @return 已完成固定委托集合喵~
	 */
	public Set<Integer> getCompletedFixedQuests() {
		return this.completedFixedQuests;
	}

	/**
	 * 判断是否已领取终章奖励喵~
	 *
	 * @return 是否已领取喵~
	 */
	public boolean isClaimedFinalReward() {
		return this.claimedFinalReward;
	}

	/**
	 * 设置是否已领取终章奖励喵~
	 *
	 * @param claimedFinalReward 是否已领取喵~
	 */
	public void setClaimedFinalReward(boolean claimedFinalReward) {
		this.claimedFinalReward = claimedFinalReward;
	}

	/**
	 * 判断是否已成为家园伴侣喵~
	 *
	 * @return 是否已成为家园伴侣喵~
	 */
	public boolean isHomePartner() {
		return this.homePartner;
	}

	/**
	 * 设置是否已成为家园伴侣喵~
	 *
	 * @param homePartner 是否已成为家园伴侣喵~
	 */
	public void setHomePartner(boolean homePartner) {
		this.homePartner = homePartner;
	}

	/**
	 * 获取当日礼物收益喵~
	 *
	 * @return 当日礼物收益喵~
	 */
	public float getDailyGiftGain() {
		return this.dailyGiftGain;
	}

	/**
	 * 设置当日礼物收益喵~
	 *
	 * @param dailyGiftGain 当日礼物收益喵~
	 */
	public void setDailyGiftGain(float dailyGiftGain) {
		this.dailyGiftGain = dailyGiftGain;
	}

	/**
	 * 判断是否已领取家园日收益喵~
	 *
	 * @return 是否已领取喵~
	 */
	public boolean isDailyHomeGainClaimed() {
		return this.dailyHomeGainClaimed;
	}

	/**
	 * 设置是否已领取家园日收益喵~
	 *
	 * @param dailyHomeGainClaimed 是否已领取喵~
	 */
	public void setDailyHomeGainClaimed(boolean dailyHomeGainClaimed) {
		this.dailyHomeGainClaimed = dailyHomeGainClaimed;
	}

	/**
	 * 判断是否已触发当日争执喵~
	 *
	 * @return 是否已触发喵~
	 */
	public boolean isDailyConflictTriggered() {
		return this.dailyConflictTriggered;
	}

	/**
	 * 设置是否已触发当日争执喵~
	 *
	 * @param dailyConflictTriggered 是否已触发喵~
	 */
	public void setDailyConflictTriggered(boolean dailyConflictTriggered) {
		this.dailyConflictTriggered = dailyConflictTriggered;
	}

	/**
	 * 获取已知礼物偏好集合喵~
	 *
	 * @return 已知礼物偏好集合喵~
	 */
	public Set<String> getKnownGiftPreferences() {
		return this.knownGiftPreferences;
	}

	/**
	 * 获取上次每日重置游戏日喵~
	 *
	 * @return 上次每日重置游戏日喵~
	 */
	public long getLastDailyResetDay() {
		return this.lastDailyResetDay;
	}

	/**
	 * 设置上次每日重置游戏日喵~
	 *
	 * @param lastDailyResetDay 上次每日重置游戏日喵~
	 */
	public void setLastDailyResetDay(long lastDailyResetDay) {
		this.lastDailyResetDay = lastDailyResetDay;
	}

	/**
	 * 重置关系进度并保留终章奖励归属喵~
	 *
	 * @param claimedFinalReward 是否保留终章奖励归属喵~
	 */
	public void resetProgressPreservingFinalReward(boolean claimedFinalReward) {
		this.affection = 0;
		this.confirmedIntimacy = false;
		this.completedFixedQuests.clear();
		this.claimedFinalReward = claimedFinalReward;
		this.homePartner = false;
		this.dailyGiftGain = 0;
		this.dailyHomeGainClaimed = false;
		this.dailyConflictTriggered = false;
		this.knownGiftPreferences.clear();
		this.lastDailyResetDay = 0L;
	}

	/**
	 * 序列化关系状态喵~
	 *
	 * @return NBT 数据喵~
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		if(this.playerUuid != null) {
			tag.putString("player_uuid", this.playerUuid.toString());
		}
		if(this.characterId != null) {
			tag.putString("character_id", this.characterId.toString());
		}
		tag.putFloat("affection", this.affection);
		tag.putBoolean("confirmed_intimacy", this.confirmedIntimacy);
		ListTag completedFixedQuestsTag = new ListTag();
		for(Integer fixedQuest : this.completedFixedQuests) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("value", fixedQuest);
			completedFixedQuestsTag.add(entry);
		}
		tag.put("completed_fixed_quests", completedFixedQuestsTag);
		tag.putBoolean("claimed_final_reward", this.claimedFinalReward);
		tag.putBoolean("home_partner", this.homePartner);
		tag.putFloat("daily_gift_gain", this.dailyGiftGain);
		tag.putBoolean("daily_home_gain_claimed", this.dailyHomeGainClaimed);
		tag.putBoolean("daily_conflict_triggered", this.dailyConflictTriggered);
		ListTag knownGiftPreferencesTag = new ListTag();
		for(String preference : this.knownGiftPreferences) {
			knownGiftPreferencesTag.add(StringTag.valueOf(preference));
		}
		tag.put("known_gift_preferences", knownGiftPreferencesTag);
		tag.putLong("last_daily_reset_day", this.lastDailyResetDay);
		return tag;
	}

	/**
	 * 反序列化关系状态喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 关系状态喵~
	 */
	public static PlayerCharacterRelation deserializeNBT(CompoundTag tag) {
		PlayerCharacterRelation relation = new PlayerCharacterRelation();
		tag.getString("player_uuid").ifPresent(value -> relation.playerUuid = UUID.fromString(value));
		tag.getString("character_id").ifPresent(value -> relation.characterId = Identifier.parse(value));
		relation.affection = tag.getInt("affection").orElse(0);
		relation.confirmedIntimacy = tag.getBoolean("confirmed_intimacy").orElse(false);
		ListTag completedFixedQuestsTag = tag.getListOrEmpty("completed_fixed_quests");
		for (Tag value : completedFixedQuestsTag) {
			if (value instanceof CompoundTag entry) {
				relation.completedFixedQuests.add(entry.getInt("value").orElse(0));
			}
		}
		relation.claimedFinalReward = tag.getBoolean("claimed_final_reward").orElse(false);
		relation.homePartner = tag.getBoolean("home_partner").orElse(false);
		relation.dailyGiftGain = tag.getInt("daily_gift_gain").orElse(0);
		relation.dailyHomeGainClaimed = tag.getBoolean("daily_home_gain_claimed").orElse(false);
		relation.dailyConflictTriggered = tag.getBoolean("daily_conflict_triggered").orElse(false);
		ListTag knownGiftPreferencesTag = tag.getListOrEmpty("known_gift_preferences");
		for(int i = 0; i < knownGiftPreferencesTag.size(); i++) {
			if(knownGiftPreferencesTag.get(i).getId() == Tag.TAG_STRING) {
				relation.knownGiftPreferences.add(knownGiftPreferencesTag.getStringOr(i, ""));
			}
		}
		relation.lastDailyResetDay = tag.getLong("last_daily_reset_day").orElse(0L);
		return relation;
	}
}
