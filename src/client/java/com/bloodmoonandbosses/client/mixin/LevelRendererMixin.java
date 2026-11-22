package com.bloodmoonandbosses.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.client.renderer.LevelRenderer;

/**
 * La luna roja y más grande. En 1.21.1 se dibuja en renderSky: el tamaño es un 20 suelto (el único de ese método) y el
 * color es el del shader, el mismo que usó el sol; las estrellas, que van después, fijan el suyo.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0f))
	private float bloodmoonandbosses$tamanioDeLaLuna(float tamanio) {
		return LunaDeSangreCliente.tamanioDeLaLuna(tamanio, LunaDeSangreCliente.intensidad(1));
	}

	@Inject(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;MOON_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
	private void bloodmoonandbosses$colorDeLaLuna(CallbackInfo ci) {
		float intensidad = LunaDeSangreCliente.intensidad(1);
		if (intensidad <= 0) {
			return;
		}
		float[] actual = RenderSystem.getShaderColor();
		Vector4f color = new Vector4f(actual[0], actual[1], actual[2], actual[3]);
		LunaDeSangreCliente.colorearLuna(color, intensidad);
		RenderSystem.setShaderColor(color.x(), color.y(), color.z(), color.w());
	}
}
