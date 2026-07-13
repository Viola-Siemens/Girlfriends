package com.hexagram2021.girlfriends.mixin;

import com.hexagram2021.girlfriends.common.blessing.GirlfriendsMobEffects;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 船实体注入类，实现潮汐同行效果
 *
 * @author liudongyu
 */
@Mixin(AbstractBoat.class)
public class AbstractBoatMixin {
	@Inject(method = "controlBoat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.BEFORE))
	private void girlfriends$wrapAcceleration(CallbackInfo ci, @Local(name = "acceleration") LocalFloatRef acceleration) {
		AbstractBoat self = (AbstractBoat)(Object)this;
		LivingEntity livingEntity = self.getControllingPassenger();
		if(livingEntity != null && livingEntity.hasEffect(GirlfriendsMobEffects.TIDE_COMPANION)) {
			acceleration.set(acceleration.get() * 1.5F);
		}
	}
}
