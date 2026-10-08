package com.mobspoderosos;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class BotinPorNivel {
	// Mobs cuya tabla tiene ítems que no deberían multiplicarse: calaveras de wither, caparazones de shulker
	public static final TagKey<EntityType<?>> SIN_TIRADAS_EXTRA = TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("sin_tiradas_extra"));
	// Cada tirada extra es una chance que sube con el nivel: con tiradas seguras, cada evocador lejano daría varios tótems
	public static final TagKey<EntityType<?>> TIRADAS_EXTRA_POR_CHANCE = TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("tiradas_extra_por_chance"));

	private BotinPorNivel() {
	}

	public static int tiradasExtra(int nivel) {
		Configuracion config = MobsPoderosos.config();
		return Math.min(nivel / config.nivelesPorTiradaExtra(), config.tiradasExtraMaximas());
	}

	/** Probabilidad de que salga cada tirada extra: 1 para la mayoría, 0 para sin_tiradas_extra. */
	public static double chanceTiradaExtra(Mob mob) {
		if (mob.is(SIN_TIRADAS_EXTRA)) {
			return 0;
		}
		if (mob.is(TIRADAS_EXTRA_POR_CHANCE)) {
			return MobsPoderosos.config().chanceTiradaExtra().en(NivelDeMob.nivel(mob));
		}
		return 1;
	}

	/** Sólo se llama si lo mató un jugador: si no, una granja automática multiplicaría el loot sin riesgo. */
	public static void soltarTiradasExtra(Mob mob, ServerLevel level, DamageSource source) {
		int tiradas = tiradasExtra(NivelDeMob.nivel(mob));
		double chance = chanceTiradaExtra(mob);
		if (tiradas == 0 || chance <= 0) {
			return;
		}
		mob.getLootTable().ifPresent(tabla -> {
			for (int i = 0; i < tiradas; i++) {
				if (chance >= 1 || mob.getRandom().nextDouble() < chance) {
					mob.dropFromLootTable(level, source, true, tabla);
				}
			}
		});
	}

	public static int experiencia(int base, int nivel) {
		if (nivel == 0) {
			return base;
		}
		return (int) Math.round(base * (1 + nivel * MobsPoderosos.config().experienciaPorNivel()));
	}
}
