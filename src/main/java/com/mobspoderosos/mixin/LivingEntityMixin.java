package com.mobspoderosos.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.mobspoderosos.BotinPorNivel;
import com.mobspoderosos.NivelDeMob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	// Vanilla loguea la muerte de todo mob con nombre: con niveles, cada mob muerto sería una línea en la consola
	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private boolean mobspoderosos$sinLogDeMuerte(Logger logger, String mensaje, Object entidad, Object causa) {
		return !((Object) this instanceof Mob mob && NivelDeMob.esNombreDelMod(mob));
	}

	// Tiradas extra y botín raro. Acá y no en AFTER_DEATH: esta llamada ya pasó por shouldDropLoot (gamerule mobDrops, crías), y
	// matoUnJugador es el mismo criterio que usa vanilla para el loot de "killed_by_player"
	@WrapOperation(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
	private void mobspoderosos$tiradasExtra(LivingEntity entidad, ServerLevel level, DamageSource source, boolean matoUnJugador, Operation<Void> original) {
		// Antes que la de vanilla: Mob.dropFromLootTable borra la tabla propia (DeathLootTable) después de usarla
		if (matoUnJugador && entidad instanceof Mob mob) {
			BotinPorNivel.soltarTiradasExtra(mob, level, source);
			BotinPorNivel.soltarBotinRaro(mob, level, source);
		}
		original.call(entidad, level, source, matoUnJugador);
	}

	// getExperienceReward es final: cubre todas las subclases
	@ModifyReturnValue(method = "getExperienceReward", at = @At("RETURN"))
	private int mobspoderosos$experienciaPorNivel(int experiencia) {
		return BotinPorNivel.experiencia(experiencia, NivelDeMob.nivel((LivingEntity) (Object) this));
	}
}
