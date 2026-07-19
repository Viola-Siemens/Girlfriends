package com.hexagram2021.girlfriends.common.entity;

import com.google.common.collect.ImmutableList;
import com.hexagram2021.girlfriends.common.blessing.GirlfriendsMobEffects;
import com.hexagram2021.girlfriends.common.character.GirlfriendType;
import com.hexagram2021.girlfriends.common.character.GirlfriendTypes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsActivities;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsEnvironmentAttributes;
import com.hexagram2021.girlfriends.common.entity.ai.GirlfriendsSensorTypes;
import com.hexagram2021.girlfriends.common.entity.ai.behavior.*;
import com.hexagram2021.girlfriends.common.item.GirlfriendsItemTags;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 晚萤实体喵~
 * <p>
 * 热烈直率、勇敢无畏的战士，生成于下界喵~
 * 日程：清晨巡逻 → 上午战斗训练 → 下午清剿敌对生物 → 傍晚守望 → 夜晚保养武器喵~
 * <p>
 * 与其他角色不同，晚萤对敌对生物采取主动攻击策略，而非逃跑喵~
 *
 * @author liudongyu
 */
public class WanyingEntity extends GirlfriendEntity {
	/**
	 * 构造晚萤实体
	 * @param entityType 实体类型
	 * @param level 世界
	 */
	public WanyingEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public Identifier getGirlfriendTypeId() {
		return GirlfriendTypes.WANYING_ID;
	}

	@Override
	public Holder<GirlfriendType> getGirlfriendType() {
		return GirlfriendTypes.WANYING;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
		return itemStack.is(GirlfriendsItemTags.WANYING_PICKS_UP);
	}

	@Override
	public Holder<MobEffect> getBlessingEffect() {
		return GirlfriendsMobEffects.FLAME_GUARDIAN;
	}

	@Override
	public boolean isWithinMeleeAttackRange(LivingEntity target) {
		AttackRange attackRange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
		double maxRange;
		double minRange;
		if (attackRange == null) {
			maxRange = 1.05D;
			minRange = 0.0D;
		} else {
			maxRange = attackRange.effectiveMaxRange(this) * 1.25D;
			minRange = attackRange.effectiveMinRange(this);
		}

		AABB hitbox = target.getHitbox();
		return this.getAttackBoundingBox(maxRange).intersects(hitbox) && (minRange <= 0.0D || !this.getAttackBoundingBox(minRange).intersects(hitbox));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Brain.Provider<GirlfriendEntity> getBrainProvider() {
		return Brain.provider(
				List.of(
						SensorType.HURT_BY,
						SensorType.NEAREST_LIVING_ENTITIES,
						GirlfriendsSensorTypes.SHELTER_SENSOR.get(),
						GirlfriendsSensorTypes.HOSTILE_SENSOR.get()
				), girlfriend -> {
					ImmutableList.Builder<ActivityData<GirlfriendEntity>> activities = ImmutableList.builder();

					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> core = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> panic = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> morning = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> dayWork = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> afternoon = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> sunset = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> nightRest = ImmutableList.builder();
					ImmutableList.Builder<Pair<Integer, BehaviorControl<GirlfriendEntity>>> follow = ImmutableList.builder();

					// 核心行为：晚萤只对受伤恐慌，对敌对生物采取攻击策略喵~
					core.add(
							Pair.of(0, new Swim<>(0.8F)),
							Pair.of(0, (BehaviorControl<GirlfriendEntity>)(Object) InteractWithDoor.create()),
							// 战斗触发器：受伤恐慌，发现敌对生物则攻击喵~
							Pair.of(0, (BehaviorControl<GirlfriendEntity>)(Object) BehaviorBuilder.create(i -> i.group(
									i.present(MemoryModuleType.HURT_BY)
							).apply(i, hurtBy -> (_, body, _) -> {
								DamageSource hurtByDamageSource = i.get(hurtBy);
								// 反击
								if(hurtByDamageSource.getEntity() instanceof LivingEntity attacker &&
										attacker.is(GirlfriendEntityTags.WANYING_ATTACKS)) {
									body.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
									return true;
								}
								// 受伤 → 触发恐慌
								body.getBrain().setActiveActivityIfPossible(Activity.PANIC);
								return true;
							}))),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) new LookAtTargetSink(45, 90)),
							Pair.of(1, BackToShelter.create(16, 48, 0.4F)),
							Pair.of(2, (BehaviorControl<GirlfriendEntity>)(Object) SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F)),
							Pair.of(2, (BehaviorControl<GirlfriendEntity>)(Object) MeleeAttack.create(20)),
							Pair.of(3, (BehaviorControl<GirlfriendEntity>)(Object) new MoveToTargetSink(80, 120)),
							Pair.of(6, new RunOne<>(List.of(
									Pair.of(SetEntityLookTarget.create(EntityType.BLAZE, 8.0F), 2),
									Pair.of(SetEntityLookTarget.create(EntityType.WITHER_SKELETON, 8.0F), 2),
									Pair.of(SetEntityLookTarget.create(mob -> mob.is(GirlfriendEntityTags.GIRLFRIENDS), 8.0F), 4),
									Pair.of(SetEntityLookTarget.create(mob -> mob instanceof Player player && girlfriend.isInterestedIn(player), 8.0F), 4)
							))),
							Pair.of(7, (BehaviorControl<GirlfriendEntity>)(Object) new DoNothing(30, 60))
					);

					// 恐慌行为：只对受伤逃跑喵~
					panic.add(
							Pair.of(0, GirlfriendCalmDown.createWanying()),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, 0.8F, 6, false)),
							Pair.of(2, ShelterBoundRandomStroll.create(0.8F))
					);

					// 清晨：巡逻喵~
					morning.add(
							Pair.of(3, ShelterBoundRandomStroll.create(0.5F)),
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					// 上午：战斗训练喵~
					dayWork.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) MeleeAttack.create(20)),
							Pair.of(3, ShelterBoundRandomStroll.create(0.5F)),
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					// 下午：清剿敌对生物喵~
					afternoon.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) MeleeAttack.create(20)),
							Pair.of(3, ShelterBoundRandomStroll.create(0.6F)),
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					// 傍晚：守望喵~
					sunset.add(
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) new RandomLookAround(UniformInt.of(150, 250), 30.0F, -10.0F, 0.0F)),
							Pair.of(3, BackToShelter.create(2, 48, 0.4F)),
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					// 夜晚：保养武器喵~
					nightRest.add(
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					// 跟随行为
					follow.add(
							Pair.of(1, StayCloseToIntimatePlayer.create(3, 16, 0.8F)),
							Pair.of(1, (BehaviorControl<GirlfriendEntity>)(Object) MeleeAttack.create(20)),
							Pair.of(6, (BehaviorControl<GirlfriendEntity>)(Object) RandomStroll.stroll(0.4F)),
							Pair.of(49, GirlfriendUpdateActivityFromSchedule.create())
					);

					activities.add(ActivityData.create(Activity.CORE, core.build()));
					activities.add(ActivityData.create(Activity.PANIC, panic.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.MORNING.get(), morning.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.DAY_WORK.get(), dayWork.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.AFTERNOON.get(), afternoon.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.SUNSET.get(), sunset.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.NIGHT_REST.get(), nightRest.build()));
					activities.add(ActivityData.create(GirlfriendsActivities.FOLLOW.get(), follow.build()));

					return activities.build();
				}
		);
	}

	@Override
	protected void registerBrainGoals(Brain<?> brain) {
		brain.setSchedule(GirlfriendsEnvironmentAttributes.WANYING_ACTIVITY.get());

		brain.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
	}

	@Override
	protected int getNextHealCooldown() {
		// CD 是默认值的一半
		return super.getNextHealCooldown() / 2;
	}
}
