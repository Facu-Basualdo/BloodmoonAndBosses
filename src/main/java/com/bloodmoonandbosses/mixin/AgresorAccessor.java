package com.bloodmoonandbosses.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// En 1.21.1 LivingEntity no tiene getLastHurtByPlayer(): es el mismo campo que vanilla usa para el loot "killed_by_player"
@Mixin(LivingEntity.class)
public interface AgresorAccessor {
	@Accessor("lastHurtByPlayer")
	@Nullable Player bloodmoonandbosses$ultimoJugador();
}
