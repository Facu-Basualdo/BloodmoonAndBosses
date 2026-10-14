package com.bloodmoonandbosses.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bloodmoonandbosses.client.AspectoDeMobs;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

/**
 * Jefes y campeones teñidos. El estado de dibujo no tiene la entidad: el tinte se calcula al extraerlo y viaja como
 * dato de Fabric. Siempre se pone (también null): el estado se reusa entre entidades.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void bloodmoonandbosses$extraerTinte(LivingEntity entidad, LivingEntityRenderState estado, float parcial, CallbackInfo ci) {
		estado.setData(AspectoDeMobs.TINTE, AspectoDeMobs.tinte(entidad, estado.ageInTicks));
	}

	// Multiplicado, por si vanilla u otro mod ya lo tiñen
	@ModifyReturnValue(method = "getModelTint", at = @At("RETURN"))
	private int bloodmoonandbosses$aplicarTinte(int tinte, LivingEntityRenderState estado) {
		Integer nuestro = estado.getData(AspectoDeMobs.TINTE);
		return nuestro == null ? tinte : ARGB.multiply(tinte, nuestro);
	}
}
