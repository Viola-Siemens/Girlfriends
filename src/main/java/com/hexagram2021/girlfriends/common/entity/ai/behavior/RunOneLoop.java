package com.hexagram2021.girlfriends.common.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运行多个行为，并循环运行
 * @param <E> 实体类型
 */
public class RunOneLoop<E extends LivingEntity> implements BehaviorControl<E> {
	private final List<BehaviorControl<E>> behaviors;
	private final int interval;
	private final int randomInterval;
	private int index = 0;
	private long nextTick = 0;
	private Behavior.Status status = Behavior.Status.STOPPED;

	/**
	 * 创建循环行为
	 * @param behaviors 需要运行的行为
	 * @param interval 运行间隔
	 * @param randomInterval 随机间隔
	 */
	public RunOneLoop(List<BehaviorControl<E>> behaviors, int interval, int randomInterval) {
		if(behaviors.isEmpty()) {
			throw new IllegalArgumentException("RunOneLoop requires at least one behavior");
		}
		this.behaviors = behaviors;
		this.interval = interval;
		this.randomInterval = randomInterval;
	}

	@Override
	public Behavior.Status getStatus() {
		return this.status;
	}

	@Override
	public Set<MemoryModuleType<?>> getRequiredMemories() {
		return this.behaviors.stream()
				.flatMap(behavior -> behavior.getRequiredMemories().stream())
				.collect(Collectors.toSet());
	}

	@Override
	public boolean tryStart(ServerLevel serverLevel, E e, long l) {
		this.status = Behavior.Status.RUNNING;
		this.behaviors.get(this.index).tryStart(serverLevel, e, l);
		return true;
	}

	@Override
	public void tickOrStop(ServerLevel serverLevel, E e, long l) {
		if(this.nextTick < e.level().getGameTime()) {
			this.nextTick = e.level().getGameTime() + this.interval + e.level().getRandom().nextInt(this.randomInterval);
			this.behaviors.get(this.index).doStop(serverLevel, e, l);
			this.index = (this.index + 1) % this.behaviors.size();
		}
		this.behaviors.get(this.index).tickOrStop(serverLevel, e, l);
		if(this.behaviors.get(this.index).getStatus() == Behavior.Status.STOPPED) {
			this.doStop(serverLevel, e, l);
		}
	}

	@Override
	public void doStop(ServerLevel serverLevel, E e, long l) {
		this.status = Behavior.Status.STOPPED;
		this.behaviors.get(this.index).doStop(serverLevel, e, l);
	}

	@Override
	public String debugString() {
		return this.getClass().getSimpleName() + ": " + this.behaviors.stream()
				.filter(goal -> goal.getStatus() == Behavior.Status.RUNNING)
				.map(b -> b.getClass().getSimpleName())
				.collect(Collectors.toSet());
	}
}
