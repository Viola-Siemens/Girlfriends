package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

/**
 * 女友角色切换活动行为
 *
 * @author liudongyu
 */
public final class GirlfriendUpdateActivityFromSchedule {
	/**
	 * 创建切换活动行为
	 * @return 切换活动行为
	 */
	public static BehaviorControl<GirlfriendEntity> create() {
		return BehaviorBuilder.create(i -> i.point((level, body, _) -> {
			if(body.getFollowMode().isStayOrFollow()) {
				return false;
			}
			body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
			return true;
		}));
	}

	private GirlfriendUpdateActivityFromSchedule() {
	}
}
