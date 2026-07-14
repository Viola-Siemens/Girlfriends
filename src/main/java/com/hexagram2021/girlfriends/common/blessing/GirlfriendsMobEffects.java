package com.hexagram2021.girlfriends.common.blessing;

import com.hexagram2021.girlfriends.GirlfriendsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组状态效果注册表，实现女友跟随时的祝佑效果
 *
 * @author liudongyu
 */
public final class GirlfriendsMobEffects {
	public static final DeferredRegister<MobEffect> REGISTER =
			DeferredRegister.create(Registries.MOB_EFFECT, GirlfriendsMod.MODID);

	/** 自然宽恕：沫沫专属祝佑效果 */
	public static final DeferredHolder<MobEffect, GirlfriendsBlessingEffect> FORGIVENESS_OF_NATURE = REGISTER.register(
			"forgiveness_of_nature", () -> new GirlfriendsBlessingEffect(MobEffectCategory.BENEFICIAL, 0xf9fbf8)
	);
	/** 潮汐同行：渔溪专属祝佑效果 */
	public static final DeferredHolder<MobEffect, GirlfriendsBlessingEffect> TIDE_COMPANION = REGISTER.register(
			"tide_companion", () -> new GirlfriendsBlessingEffect(MobEffectCategory.BENEFICIAL, 0x2653cd)
	);
	/** 大地馈赠：梅疏专属祝佑效果 */
	public static final DeferredHolder<MobEffect, GirlfriendsBlessingEffect> BOUNTY_OF_EARTH = REGISTER.register(
			"bounty_of_earth", () -> new GirlfriendsBlessingEffect(MobEffectCategory.BENEFICIAL, 0x262423)
	);
	/** 烈焰守护：晚萤专属祝佑效果 */
	public static final DeferredHolder<MobEffect, GirlfriendsBlessingEffect> FLAME_GUARDIAN = REGISTER.register(
			"flame_guardian", () -> (GirlfriendsBlessingEffect) new GirlfriendsBlessingEffect(
					MobEffectCategory.BENEFICIAL,
					0xCC2502
			).addAttributeModifier(
					Attributes.ATTACK_DAMAGE,
					Identifier.withDefaultNamespace("effect.girlfriends.flame_guardian"),
					0.4D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			)
	);
	/** 虚空回响：幽若专属祝佑效果 */
	public static final DeferredHolder<MobEffect, GirlfriendsBlessingEffect> VOID_ECHO = REGISTER.register(
			"void_echo", () -> new GirlfriendsBlessingEffect(MobEffectCategory.BENEFICIAL, 0xCC2502)
	);

	private GirlfriendsMobEffects() {
	}
}
