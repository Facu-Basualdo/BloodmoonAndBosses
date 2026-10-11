package com.mobspoderosos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootTable;

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

	/** Las tablas raras que le tocan a un mob de ese nivel: cada una se sortea por separado. */
	public static List<Botin> botinesRaros(int nivel) {
		List<Botin> botines = new ArrayList<>();
		if (nivel == 0) {
			return botines;
		}
		for (Botin botin : Botin.values()) {
			if (nivel >= botin.rareza().nivelMinimo()) {
				botines.add(botin);
			}
		}
		return botines;
	}

	/** Aplica también a los mobs de sin_tiradas_extra: estas tablas son del mod, no multiplican la del mob. */
	public static void soltarBotinRaro(Mob mob, ServerLevel level, DamageSource source) {
		for (Botin botin : botinesRaros(NivelDeMob.nivel(mob))) {
			if (mob.getRandom().nextDouble() < botin.rareza().chance()) {
				mob.dropFromLootTable(level, source, true, botin.tabla());
			}
		}
	}

	/** La tabla más alta que le corresponde a ese nivel, y como mínimo la dada: el premio de los eventos. */
	public static Botin mejorBotin(int nivel, Botin minimo) {
		Botin mejor = minimo;
		for (Botin botin : botinesRaros(nivel)) {
			if (botin.ordinal() > mejor.ordinal()) {
				mejor = botin;
			}
		}
		return mejor;
	}

	/** Un jefe de zona siempre suelta la mejor tabla que le toca, como mínimo la épica. Además de su botín raro común. */
	public static void soltarBotinDeJefe(Mob mob, ServerLevel level, DamageSource source) {
		if (JefeDeZona.esJefe(mob)) {
			mob.dropFromLootTable(level, source, true, mejorBotin(NivelDeMob.nivel(mob), Botin.EPICO).tabla());
		}
	}

	public static int experiencia(int base, int nivel) {
		if (nivel == 0) {
			return base;
		}
		return (int) Math.round(base * (1 + nivel * MobsPoderosos.config().experienciaPorNivel()));
	}

	/** Las tablas de data/mobspoderosos/loot_table/botin_por_nivel: se pueden pisar con un datapack. */
	public enum Botin {
		RARO("raro", Configuracion::botinRaro),
		EPICO("epico", Configuracion::botinEpico),
		LEGENDARIO("legendario", Configuracion::botinLegendario);

		private final ResourceKey<LootTable> tabla;
		private final Function<Configuracion, Configuracion.Rareza> rareza;

		Botin(String nombre, Function<Configuracion, Configuracion.Rareza> rareza) {
			this.tabla = ResourceKey.create(Registries.LOOT_TABLE, MobsPoderosos.id("botin_por_nivel/" + nombre));
			this.rareza = rareza;
		}

		public ResourceKey<LootTable> tabla() {
			return tabla;
		}

		public Configuracion.Rareza rareza() {
			return rareza.apply(MobsPoderosos.config());
		}
	}
}
