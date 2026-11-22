package com.bloodmoonandbosses.client.mixin;

import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.client.renderer.SkyRenderer;

/** La luna roja y más grande. renderMoon sólo corre en el overworld, el único con luna. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	// En 1.21.11 el tamaño de la luna es el scale(20, 1, 20) de la matriz: se agrandan el ancho y el largo
	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"), index = 0)
	private float bloodmoonandbosses$anchoDeLaLuna(float tamanio) {
		return LunaDeSangreCliente.tamanioDeLaLuna(tamanio, LunaDeSangreCliente.intensidad(1));
	}

	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"), index = 2)
	private float bloodmoonandbosses$largoDeLaLuna(float tamanio) {
		return LunaDeSangreCliente.tamanioDeLaLuna(tamanio, LunaDeSangreCliente.intensidad(1));
	}

	// El color es un Vector4f(1, 1, 1, alfa) recién creado: se puede cambiar sin copiarlo
	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
			index = 1)
	private Vector4fc bloodmoonandbosses$colorDeLaLuna(Vector4fc color) {
		LunaDeSangreCliente.colorearLuna((Vector4f) color, LunaDeSangreCliente.intensidad(1));
		return color;
	}
}
