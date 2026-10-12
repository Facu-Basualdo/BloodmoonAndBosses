package com.bloodmoonandbosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.monster.Creeper;

@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("explosionRadius")
	int bloodmoonandbosses$getExplosionRadius();

	@Accessor("explosionRadius")
	void bloodmoonandbosses$setExplosionRadius(int radio);
}
