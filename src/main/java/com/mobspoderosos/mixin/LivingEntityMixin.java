package com.mobspoderosos.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mobspoderosos.BotinPorNivel;
import com.mobspoderosos.NivelDeMob;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	// Vanilla loguea la muerte de todo mob con nombre: con niveles, cada mob muerto sería una línea en la consola
	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private boolean mobspoderosos$sinLogDeMuerte(Logger logger, String mensaje, Object entidad, Object causa) {
		return !((Object) this instanceof Mob mob && NivelDeMob.esNombreDelMod(mob));
	}

	// Daño, curación, regeneración y vida máxima que baja pasan todos por acá. En el cliente no hace nada: NIVEL no se sincroniza
	@Inject(method = "setHealth", at = @At("TAIL"))
	private void mobspoderosos$vidaEnElNombre(float vida, CallbackInfo ci) {
		if ((Object) this instanceof Mob mob) {
			NivelDeMob.actualizarVida(mob);
		}
	}

	// Si la vida máxima sube (un efecto, la furia de un jefe, un comando), vanilla no llama a setHealth: sin esto el nombre
	// seguiría mostrando el máximo viejo hasta el próximo golpe. Cuando baja, vanilla ya llama a setHealth
	@Inject(method = "onAttributeUpdated", at = @At("TAIL"))
	private void mobspoderosos$vidaMaximaEnElNombre(Holder<Attribute> atributo, CallbackInfo ci) {
		if (atributo.is(Attributes.MAX_HEALTH) && (Object) this instanceof Mob mob) {
			NivelDeMob.actualizarVida(mob);
		}
	}

	// Tiradas extra y botín raro. Acá y no en AFTER_DEATH: esta llamada ya pasó por shouldDropLoot (gamerule mobDrops, crías), y
	// matoUnJugador es el mismo criterio que usa vanilla para el loot de "killed_by_player"
	@WrapOperation(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
	private void mobspoderosos$tiradasExtra(LivingEntity entidad, ServerLevel level, DamageSource source, boolean matoUnJugador, Operation<Void> original) {
		// Antes que la de vanilla: Mob.dropFromLootTable borra la tabla propia (DeathLootTable) después de usarla
		if (matoUnJugador && entidad instanceof Mob mob) {
			BotinPorNivel.soltarTiradasExtra(mob, level, source);
			BotinPorNivel.soltarBotinRaro(mob, level, source);
			BotinPorNivel.soltarBotinDeJefe(mob, level, source);
		}
		original.call(entidad, level, source, matoUnJugador);
	}

	// getExperienceReward es final: cubre todas las subclases
	@ModifyReturnValue(method = "getExperienceReward", at = @At("RETURN"))
	private int mobspoderosos$experienciaPorNivel(int experiencia) {
		return BotinPorNivel.experiencia(experiencia, NivelDeMob.nivel((LivingEntity) (Object) this));
	}
}
