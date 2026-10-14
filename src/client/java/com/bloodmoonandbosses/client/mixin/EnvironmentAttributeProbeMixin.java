package com.bloodmoonandbosses.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.level.Level;

/**
 * La cámara lee de acá el color del cielo, la niebla, las nubes y la luz del cielo (SkyRenderer,
 * AtmosphericFogEnvironment, LightmapRenderStateExtractor): un solo lugar para teñirlos a todos.
 */
@Mixin(EnvironmentAttributeProbe.class)
public abstract class EnvironmentAttributeProbeMixin {
	@Shadow
	private Level level;

	@ModifyReturnValue(method = "getValue", at = @At("RETURN"))
	private Object bloodmoonandbosses$colorear(Object valor, EnvironmentAttribute<?> atributo, float parcial) {
		// El servidor integrado del singleplayer corre en el mismo proceso: sus probes no se tocan
		if (level == null || !level.isClientSide() || level.dimension() != Level.OVERWORLD) {
			return valor;
		}
		float intensidad = LunaDeSangreCliente.intensidad(parcial);
		return intensidad <= 0 ? valor : LunaDeSangreCliente.colorear(atributo, valor, intensidad);
	}
}
