package com.bloodmoonandbosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;

// En 1.21.1 la chance de soltar cada pieza es protegida (en 26.x, getDropChances() es público)
@Mixin(Mob.class)
public interface ChanceDeSoltarInvoker {
	@Invoker("getEquipmentDropChance")
	float bloodmoonandbosses$chanceDeSoltar(EquipmentSlot slot);
}
