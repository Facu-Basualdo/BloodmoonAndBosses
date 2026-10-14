package com.bloodmoonandbosses.mixin.jade;

import com.bloodmoonandbosses.compat.jade.PluginJade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Jade apunta sólo hasta el alcance de interacción más su "Reach distance" (tope +20), y si a esa distancia no le pega a
 * nada no llama a los callbacks de los plugins: un {@code addRayTraceCallback} no alcanza para ver un mob a 40 bloques.
 * Se estira sólo el alcance contra entidades; el de bloques queda como lo configuró el jugador.
 */
@Pseudo
@Mixin(targets = "snownee.jade.overlay.RayTracing")
public class RayTracingMixin {
	@ModifyArg(method = "fire", at = @At(value = "INVOKE",
			target = "Lsnownee/jade/overlay/RayTracing;rayTrace(Lnet/minecraft/world/entity/Entity;DD)V"), index = 2)
	private double bloodmoonandbosses$alcanceContraEntidades(double alcanceDeJade) {
		return PluginJade.alcanceContraEntidades(alcanceDeJade);
	}
}
