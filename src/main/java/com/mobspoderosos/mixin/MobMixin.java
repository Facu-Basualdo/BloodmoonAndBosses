package com.mobspoderosos.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mobspoderosos.LunaDeSangre;
import com.mobspoderosos.NivelDeMob;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;

@Mixin(Mob.class)
abstract class MobMixin {
	// Es el único lugar que conoce el motivo del spawn. Sólo marca: el nivel se aplica en ENTITY_LOAD,
	// cuando la subclase ya terminó de ponerle el equipo (Zombie lo hace después de llamar a super).
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void mobspoderosos$marcarPendiente(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason motivo,
			@Nullable SpawnGroupData datos, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (NivelDeMob.escala(motivo)) {
			((Mob) (Object) this).setAttached(NivelDeMob.PENDIENTE, true);
		}
		// Sólo NATURAL: en una estructura o una patrulla, duplicar los mobs cambiaría la estructura
		if (motivo == EntitySpawnReason.NATURAL) {
			LunaDeSangre.alAparecerNatural((Mob) (Object) this, level.getLevel());
		}
	}
}
