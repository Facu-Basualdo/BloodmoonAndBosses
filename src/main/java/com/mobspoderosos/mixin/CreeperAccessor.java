package com.mobspoderosos.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.monster.Creeper;

@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("explosionRadius")
	int mobspoderosos$getExplosionRadius();

	@Accessor("explosionRadius")
	void mobspoderosos$setExplosionRadius(int radio);
}
