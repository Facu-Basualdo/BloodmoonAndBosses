package com.bloodmoonandbosses.client.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

/** La niebla roja. En 1.21.1 su color queda en tres campos estáticos, justo antes de limpiar la pantalla con él. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Shadow
	private static float fogRed;
	@Shadow
	private static float fogGreen;
	@Shadow
	private static float fogBlue;

	@Inject(method = "setupColor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
	private static void bloodmoonandbosses$niebla(Camera camara, float parcial, ClientLevel level, int distancia, float oscuridad, CallbackInfo ci) {
		// Bajo el agua o en lava la niebla es otra: no es la del cielo
		if (level.dimension() != Level.OVERWORLD || camara.getFluidInCamera() != FogType.NONE) {
			return;
		}
		float intensidad = LunaDeSangreCliente.intensidad(parcial);
		if (intensidad > 0) {
			Vector3f color = LunaDeSangreCliente.niebla(fogRed, fogGreen, fogBlue, intensidad);
			fogRed = color.x();
			fogGreen = color.y();
			fogBlue = color.z();
		}
	}
}
