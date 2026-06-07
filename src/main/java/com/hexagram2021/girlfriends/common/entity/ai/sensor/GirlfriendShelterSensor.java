package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Set;

/**
 * 庇护所位置传感器喵~
 * <p>
 * 将角色当前站立位置作为庇护所参考点写入 MEETING_POINT 记忆喵~
 * 后续 Story 16 集成 KD Tree 后改为查找最近可用庇护所喵~
 *
 * @author liudongyu
 */
public class GirlfriendShelterSensor extends Sensor<GirlfriendEntity> {
	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(GirlfriendsMemoryTypes.MEETING_POINT.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity entity) {
		entity.getBrain().setMemory(GirlfriendsMemoryTypes.MEETING_POINT.get(),
				GlobalPos.of(level.dimension(), entity.blockPosition()));
	}
}
