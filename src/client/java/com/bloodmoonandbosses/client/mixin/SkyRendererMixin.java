package com.bloodmoonandbosses.client.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.client.renderer.SkyRenderer;

/** La luna roja y más grande. renderMoon sólo corre en el overworld, el único con luna. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"),
			index = 2)
	private float bloodmoonandbosses$tamanioDeLaLuna(float tamanio) {
		return LunaDeSangreCliente.tamanioDeLaLuna(tamanio, LunaDeSangreCliente.intensidad(1));
	}

	// El color es un Vector4f(1, 1, 1, alfa) recién creado: se puede cambiar sin copiarlo
	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
			index = 1)
	private Vector4f bloodmoonandbosses$colorDeLaLuna(Vector4f color) {
		LunaDeSangreCliente.colorearLuna(color, LunaDeSangreCliente.intensidad(1));
		return color;
	}
}
