package com.bloodmoonandbosses.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bloodmoonandbosses.client.AspectoDeMobs;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Jefes y campeones teñidos. En 1.21.1 no hay estado de render: el tinte se calcula al dibujar el modelo, con la entidad
 * a mano, y se multiplica por el color que ya le pasa vanilla (el semitransparente de los invisibles).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
			index = 4)
	private int bloodmoonandbosses$tinte(int color, @Local(argsOnly = true) LivingEntity entidad, @Local(argsOnly = true, ordinal = 1) float parcial) {
		Integer nuestro = AspectoDeMobs.tinte(entidad, entidad.tickCount + parcial);
		return nuestro == null ? color : FastColor.ARGB32.multiply(color, nuestro);
	}
}
