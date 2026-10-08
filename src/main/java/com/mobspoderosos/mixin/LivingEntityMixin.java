package com.mobspoderosos.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.mobspoderosos.NivelDeMob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	// Vanilla loguea la muerte de todo mob con nombre: con niveles, cada mob muerto sería una línea en la consola
	@WrapWithCondition(method = "die", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private boolean mobspoderosos$sinLogDeMuerte(Logger logger, String mensaje, Object entidad, Object causa) {
		return !((Object) this instanceof Mob mob && NivelDeMob.esNombreDelMod(mob));
	}
}
