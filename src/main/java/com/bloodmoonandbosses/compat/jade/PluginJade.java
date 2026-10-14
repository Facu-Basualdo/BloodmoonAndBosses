package com.bloodmoonandbosses.compat.jade;

import com.bloodmoonandbosses.BloodmoonAndBosses;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.ConfigIcon;
import snownee.jade.api.config.IWailaConfig;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.HitResult;

/**
 * Lo carga Jade por el entrypoint {@code jade}: sin Jade esta clase nunca se toca. Sólo hace algo en un cliente con
 * el mod; la vida que muestra Jade es la sincronizada de vanilla, así que no necesita nada del servidor.
 */
@WailaPlugin(BloodmoonAndBosses.MOD_ID)
public class PluginJade implements IWailaPlugin {
	public static final Identifier ALCANCE = BloodmoonAndBosses.id("alcance");
	public static final float ALCANCE_POR_DEFECTO = 48;
	public static final float ALCANCE_MAXIMO = 128;

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addConfig(ALCANCE, ConfigIcon.none(), ALCANCE_POR_DEFECTO, 0, ALCANCE_MAXIMO, true);
		registration.addRayTraceCallback(PluginJade::sinOtrasEntidadesLejos);
	}

	/** El de la config de Jade, o el suyo si es mayor: con 0 queda como sin el mod. Lo usa {@code RayTracingMixin}. */
	public static double alcanceContraEntidades(double alcanceDeJade) {
		return Math.max(alcanceDeJade, IWailaConfig.get().plugin().getFloat(ALCANCE));
	}

	/**
	 * El alcance extra es para ver la vida de los mobs: un ítem tirado o un marco a 40 bloques no le sirve a nadie.
	 * Más allá del alcance normal de Jade, cualquier otra entidad se descarta.
	 */
	private static Accessor<?> sinOtrasEntidadesLejos(HitResult hit, Accessor<?> accessor, Accessor<?> original) {
		if (!(accessor instanceof EntityAccessor entidad) || entidad.getEntity() instanceof Mob) {
			return accessor;
		}
		double alcanceNormal = accessor.getPlayer().entityInteractionRange() + IWailaConfig.get().general().getExtendedReach();
		double distancia = accessor.getPlayer().getEyePosition().distanceTo(entidad.getHitResult().getLocation());
		return distancia > alcanceNormal ? null : accessor;
	}
}
