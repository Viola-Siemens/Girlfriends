package com.hexagram2021.girlfriends.common.binding;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 角色绑定状态喵~
 *
 * @author liudongyu
 */
public class CharacterBindingState {
	public static final int DATA_VERSION = 1;

	@Nullable
	private UUID boundPlayerUuid;
	private boolean lockedByIntimacy;
	@Nullable
	private UUID challengerPlayerUuid;
	private long waveringStartDay;
	private boolean warnedBoundPlayer;

	/**
	 * 获取绑定玩家 UUID 喵~
	 *
	 * @return 绑定玩家 UUID 喵~
	 */
	@Nullable
	public UUID getBoundPlayerUuid() {
		return this.boundPlayerUuid;
	}

	/**
	 * 设置绑定玩家 UUID 喵~
	 *
	 * @param boundPlayerUuid 绑定玩家 UUID 喵~
	 */
	public void setBoundPlayerUuid(@Nullable UUID boundPlayerUuid) {
		this.boundPlayerUuid = boundPlayerUuid;
	}

	/**
	 * 判断是否因亲密关系锁定喵~
	 *
	 * @return 是否锁定喵~
	 */
	public boolean isLockedByIntimacy() {
		return this.lockedByIntimacy;
	}

	/**
	 * 设置是否因亲密关系锁定喵~
	 *
	 * @param lockedByIntimacy 是否锁定喵~
	 */
	public void setLockedByIntimacy(boolean lockedByIntimacy) {
		this.lockedByIntimacy = lockedByIntimacy;
	}

	/**
	 * 获取挑战者玩家 UUID 喵~
	 *
	 * @return 挑战者玩家 UUID 喵~
	 */
	@Nullable
	public UUID getChallengerPlayerUuid() {
		return this.challengerPlayerUuid;
	}

	/**
	 * 设置挑战者玩家 UUID 喵~
	 *
	 * @param challengerPlayerUuid 挑战者玩家 UUID 喵~
	 */
	public void setChallengerPlayerUuid(@Nullable UUID challengerPlayerUuid) {
		this.challengerPlayerUuid = challengerPlayerUuid;
	}

	/**
	 * 获取动摇期开始游戏日喵~
	 *
	 * @return 动摇期开始游戏日喵~
	 */
	public long getWaveringStartDay() {
		return this.waveringStartDay;
	}

	/**
	 * 设置动摇期开始游戏日喵~
	 *
	 * @param waveringStartDay 动摇期开始游戏日喵~
	 */
	public void setWaveringStartDay(long waveringStartDay) {
		this.waveringStartDay = waveringStartDay;
	}

	/**
	 * 判断是否已预警原绑定玩家喵~
	 *
	 * @return 是否已预警喵~
	 */
	public boolean isWarnedBoundPlayer() {
		return this.warnedBoundPlayer;
	}

	/**
	 * 设置是否已预警原绑定玩家喵~
	 *
	 * @param warnedBoundPlayer 是否已预警喵~
	 */
	public void setWarnedBoundPlayer(boolean warnedBoundPlayer) {
		this.warnedBoundPlayer = warnedBoundPlayer;
	}

	/**
	 * 序列化绑定状态喵~
	 *
	 * @return NBT 数据喵~
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("data_version", DATA_VERSION);
		if(this.boundPlayerUuid != null) {
			tag.putString("bound_player_uuid", this.boundPlayerUuid.toString());
		}
		tag.putBoolean("locked_by_intimacy", this.lockedByIntimacy);
		if(this.challengerPlayerUuid != null) {
			tag.putString("challenger_player_uuid", this.challengerPlayerUuid.toString());
		}
		tag.putLong("wavering_start_day", this.waveringStartDay);
		tag.putBoolean("warned_bound_player", this.warnedBoundPlayer);
		return tag;
	}

	/**
	 * 反序列化绑定状态喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 绑定状态喵~
	 */
	public static CharacterBindingState deserializeNBT(CompoundTag tag) {
		CharacterBindingState state = new CharacterBindingState();
		tag.getString("bound_player_uuid").ifPresent(value -> state.boundPlayerUuid = UUID.fromString(value));
		state.lockedByIntimacy = tag.getBoolean("locked_by_intimacy").orElse(false);
		tag.getString("challenger_player_uuid").ifPresent(value -> state.challengerPlayerUuid = UUID.fromString(value));
		state.waveringStartDay = tag.getLong("wavering_start_day").orElse(0L);
		state.warnedBoundPlayer = tag.getBoolean("warned_bound_player").orElse(false);
		return state;
	}
}
