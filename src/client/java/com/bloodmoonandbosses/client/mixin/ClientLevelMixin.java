package com.bloodmoonandbosses.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** El cielo y las nubes rojos. En 1.21.1 cada color sale de su método (en 26.x, de los atributos de ambiente). */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@ModifyReturnValue(method = "getSkyColor", at = @At("RETURN"))
	private Vec3 bloodmoonandbosses$cielo(Vec3 color, Vec3 posicion, float parcial) {
		float intensidad = bloodmoonandbosses$intensidad(parcial);
		return intensidad <= 0 ? color : LunaDeSangreCliente.cielo(color, intensidad);
	}

	@ModifyReturnValue(method = "getCloudColor", at = @At("RETURN"))
	private Vec3 bloodmoonandbosses$nubes(Vec3 color, float parcial) {
		float intensidad = bloodmoonandbosses$intensidad(parcial);
		return intensidad <= 0 ? color : LunaDeSangreCliente.nubes(color, intensidad);
	}

	private float bloodmoonandbosses$intensidad(float parcial) {
		return ((ClientLevel) (Object) this).dimension() == Level.OVERWORLD ? LunaDeSangreCliente.intensidad(parcial) : 0;
	}
}
