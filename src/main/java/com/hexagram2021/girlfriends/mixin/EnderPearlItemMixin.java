package com.hexagram2021.girlfriends.mixin;

import com.hexagram2021.girlfriends.common.blessing.GirlfriendsMobEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 末影珍珠物品注入类，实现虚空回响效果
 *
 * @author liudongyu
 */
@Mixin(EnderpearlItem.class)
public class EnderPearlItemMixin {
	@WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
	private void girlfriends$wrapConsume(ItemStack instance, int amount, LivingEntity owner, Operation<Void> original) {
		if(!owner.hasEffect(GirlfriendsMobEffects.VOID_ECHO) || owner.getRandom().nextInt(4) != 0) {
			original.call(instance, amount, owner);
		}
	}
}
