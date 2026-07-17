package com.hexagram2021.girlfriends.common.entity;

import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 可攻略角色接口
 *
 * @author liudongyu
 */
public interface IRomanceable {
	/**
	 * 获取该实体的角色类型 ID 喵~
	 *
	 * @return 角色类型注册表 key 喵~
	 */
	Identifier getGirlfriendTypeId();

	/**
	 * 解析角色类型对象喵~
	 *
	 * @return 角色类型，若注册表缺失则返回 null 喵~
	 */
	Holder<GirlfriendType> getGirlfriendType();

	/**
	 * 获取当前跟随模式喵~
	 *
	 * @return 跟随模式喵~
	 */
	FollowMode getFollowMode();

	/**
	 * 设置跟随模式喵~
	 *
	 * @param mode 新的跟随模式喵~
	 */
	void setFollowMode(FollowMode mode);

	/**
	 * 获取跟随玩家<br/>
	 * 角色只有可能跟随爱慕的玩家
	 *
	 * @return 跟随玩家，非跟随状态、非玩家、不同维度时返回 null
	 */
	@Nullable
	Player getFollowedPlayer();

	/**
	 * 判断是否对指定玩家感兴趣<br/>
	 * 仅当与该玩家确认关系，或未与任何玩家确认关系时，返回 true
	 *
	 * @param player 玩家
	 * @return 是否对指定玩家感兴趣
	 */
	boolean isInterestedIn(Player player);

	/**
	 * 设置当前爱慕玩家 UUID 喵~
	 *
	 * @param playerUuid 玩家 UUID，null 表示清除喵~
	 */
	void setLikedPlayerUuid(@Nullable UUID playerUuid);

	/**
	 * 将实体运行时状态同步到持久化世界数据喵~
	 * <br/>
	 * 将 entityUuid、followMode、followTargetUuid 写入 CharacterWorldState 喵~
	 *
	 * @param data 世界数据喵~
	 */
	void syncToWorldState(GirlfriendsWorldData data);

	/**
	 * 从持久化世界数据同步到实体运行时状态喵~
	 * <p>
	 * 读取 CharacterWorldState 的 followMode、followTargetUuid 并应用到实体喵~
	 *
	 * @param data 世界数据喵~
	 */
	void syncFromWorldState(GirlfriendsWorldData data);

	/**
	 * 跟随时为玩家提供的状态效果
	 *
	 * @return 祝佑状态效果
	 */
	Holder<MobEffect> getBlessingEffect();
}
