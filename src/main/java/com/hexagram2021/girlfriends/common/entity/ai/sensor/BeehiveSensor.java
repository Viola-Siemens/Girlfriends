package com.hexagram2021.girlfriends.common.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsMemoryTypes;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 蜂箱传感器 — 检测 32 格内所有蜂箱方块
 *
 * @author liudongyu
 */
public class BeehiveSensor extends Sensor<GirlfriendEntity> {
	private final Long2LongMap batchCache = new Long2LongOpenHashMap();
	private int triedCount;
	private long lastUpdate;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return ImmutableSet.of(GirlfriendsMemoryTypes.NEAREST_BEEHIVE.get());
	}

	@Override
	protected void doTick(ServerLevel level, GirlfriendEntity body) {
		this.triedCount = 0;
		this.lastUpdate = level.getGameTime() + level.getRandom().nextInt(20);
		PoiManager poiManager = level.getPoiManager();
		Predicate<BlockPos> cacheTest = pos -> {
			long key = pos.asLong();
			if (this.batchCache.containsKey(key)) {
				return false;
			} else if (++this.triedCount >= 5) {
				return false;
			} else {
				this.batchCache.put(key, this.lastUpdate + 40L);
				return true;
			}
		};
		Set<Pair<Holder<PoiType>, BlockPos>> pois = poiManager.findAllWithType(
				e -> e.is(PoiTypes.BEEHIVE),
				cacheTest,
				body.blockPosition(),
				32,
				PoiManager.Occupancy.ANY
		).collect(Collectors.toSet());
		Path path = AcquirePoi.findPathToPois(body, pois);
		if (path != null && path.canReach()) {
			BlockPos targetPos = path.getTarget();
			Optional<Holder<PoiType>> type = poiManager.getType(targetPos);
			if (type.isPresent()) {
				body.getBrain().setMemory(GirlfriendsMemoryTypes.NEAREST_BEEHIVE.get(), targetPos);
			}
		} else if (this.triedCount < 5) {
			this.batchCache.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < this.lastUpdate);
		}
	}
}
