package com.bloodmoonandbosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bloodmoonandbosses.NivelDeMob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;

/**
 * En 1.21.1 el slime arma a sus hijos a mano al morir, sin el evento de conversión de Fabric (en 26.x la división es una
 * conversión): el nivel se pasa acá, antes de addFreshEntity, como hace alConvertir.
 */
@Mixin(Slime.class)
abstract class SlimeMixin {
	@ModifyArg(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private Entity bloodmoonandbosses$heredarNivel(Entity hijo) {
		if (hijo instanceof Mob mob) {
			NivelDeMob.alConvertir((Mob) (Object) this, mob);
		}
		return hijo;
	}
}
