package com.hexagram2021.girlfriends.common.home;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 家园状态喵~
 *
 * @author liudongyu
 */
public class HomeState {
	public static final int DATA_VERSION = 1;

	@Nullable
	private UUID playerUuid;
	@Nullable
	private Identifier characterId;
	@Nullable
	private GlobalPos bedPos;
	@Nullable
	private HomeAnchor homeAnchor;
	private boolean active;

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
	 * 获取双人床位置喵~
	 *
	 * @return 双人床位置喵~
	 */
	@Nullable
	public GlobalPos getBedPos() {
		return this.bedPos;
	}

	/**
	 * 设置双人床位置喵~
	 *
	 * @param bedPos 双人床位置喵~
	 */
	public void setBedPos(@Nullable GlobalPos bedPos) {
		this.bedPos = bedPos;
	}

	/**
	 * 获取家园床位锚点喵~
	 *
	 * @return 家园床位锚点喵~
	 */
	@Nullable
	public HomeAnchor getHomeAnchor() {
		return this.homeAnchor;
	}

	/**
	 * 设置家园床位锚点喵~
	 *
	 * @param homeAnchor 家园床位锚点喵~
	 */
	public void setHomeAnchor(@Nullable HomeAnchor homeAnchor) {
		this.homeAnchor = homeAnchor;
	}

	/**
	 * 判断家园绑定是否有效喵~
	 *
	 * @return 是否有效喵~
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * 设置家园绑定是否有效喵~
	 *
	 * @param active 是否有效喵~
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * 序列化家园状态喵~
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
		if(this.bedPos != null) {
			GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.bedPos).result().ifPresent(value -> tag.put("bed_pos", value));
		}
		if(this.homeAnchor != null) {
			CompoundTag homeAnchorTag = new CompoundTag();
			homeAnchorTag.putString("dimension_id", this.homeAnchor.dimension().toString());
			homeAnchorTag.putInt("x", this.homeAnchor.x());
			homeAnchorTag.putInt("y", this.homeAnchor.y());
			homeAnchorTag.putInt("z", this.homeAnchor.z());
			tag.put("home_anchor", homeAnchorTag);
		}
		tag.putBoolean("active", this.active);
		return tag;
	}

	/**
	 * 反序列化家园状态喵~
	 *
	 * @param tag NBT 数据喵~
	 * @return 家园状态喵~
	 */
	public static HomeState deserializeNBT(CompoundTag tag) {
		HomeState state = new HomeState();
		tag.getString("player_uuid").ifPresent(value -> state.playerUuid = UUID.fromString(value));
		tag.getString("character_id").ifPresent(value -> state.characterId = Identifier.parse(value));
		Tag bedPosTag = tag.get("bed_pos");
		if(bedPosTag != null) {
			GlobalPos.CODEC.parse(NbtOps.INSTANCE, bedPosTag).result().ifPresent(pos -> state.bedPos = pos);
		}
		CompoundTag homeAnchorTag = tag.getCompoundOrEmpty("home_anchor");
		String dimensionId = homeAnchorTag.getString("dimension_id").orElse("");
		if(!dimensionId.isEmpty()) {
			state.homeAnchor = new HomeAnchor(
					Identifier.parse(dimensionId),
					homeAnchorTag.getInt("x").orElse(0),
					homeAnchorTag.getInt("y").orElse(0),
					homeAnchorTag.getInt("z").orElse(0)
			);
		}
		state.active = tag.getBoolean("active").orElse(false);
		return state;
	}
}
