package com.hexagram2021.girlfriends.mixin;

import com.hexagram2021.girlfriends.common.blessing.GirlfriendsMobEffects;
import com.hexagram2021.girlfriends.common.entity.GirlfriendEntityTags;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 生命实体注入类，实现自然宽恕效果
 *
 * @author liudongyu
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyReturnValue(method = "canAttack", at = @At(value = "RETURN"))
	private boolean girlfriends$tryIgnorePlayerWithEffect(boolean original, @Local(argsOnly = true) LivingEntity target) {
		LivingEntity self = (LivingEntity)(Object)this;
		if(self.typeHolder().is(GirlfriendEntityTags.NATURE_FORGIVING_MOBS) &&
				target instanceof Player player && player.hasEffect(GirlfriendsMobEffects.FORGIVENESS_OF_NATURE)) {
			return false;
		}
		return original;
	}
}
