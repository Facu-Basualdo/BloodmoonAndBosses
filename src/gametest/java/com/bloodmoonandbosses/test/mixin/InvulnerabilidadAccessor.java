package com.bloodmoonandbosses.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.level.ServerPlayer;

/**
 * Sólo en los tests. En 1.21.1 un jugador recién entrado no recibe daño durante 3 segundos (spawnInvulnerableTime); en
 * 26.x eso se reemplazó por esperar a que el cliente cargue el mundo. Los tests de daño lo ponen en 0.
 */
@Mixin(ServerPlayer.class)
public interface InvulnerabilidadAccessor {
	@Accessor("spawnInvulnerableTime")
	void bloodmoonandbosses$setInvulnerabilidad(int ticks);
}
