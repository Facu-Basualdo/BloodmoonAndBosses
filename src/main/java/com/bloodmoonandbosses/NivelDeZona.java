package com.bloodmoonandbosses;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class NivelDeZona {
	// Las horas del reloj en que vanilla dibuja la noche (de 19 a 5)
	private static final int EMPIEZA_LA_NOCHE = 13_000;
	private static final int TERMINA_LA_NOCHE = 23_000;

	private NivelDeZona() {
	}

	/**
	 * El nivel con el que aparece un mob en ese lugar: el de la zona, más el del tiempo, más el de la luna de sangre.
	 * El del campeón se suma aparte (NivelDeMob.alCargar), porque los mobs de los eventos no tiran campeón.
	 */
	public static int calcular(ServerLevel level, BlockPos pos, RandomSource random) {
		Configuracion config = BloodmoonAndBosses.config();
		BlockPos spawn = level.getServer().getRespawnData().pos();
		int nivel = calcular(level.dimension(), spawn, pos, config, random) + porTiempo(dias(level), config) + LunaDeSangre.nivelesExtra(level);
		return Mth.clamp(nivel, 1, config.nivelMaximo());
	}

	/**
	 * Los días del reloj del overworld, el mismo que usa vanilla para la dificultad regional y el que ve el
	 * jugador en F3: avanza al dormir, y {@code /time set} lo puede atrasar.
	 */
	public static long dias(ServerLevel level) {
		return level.getOverworldClockTime() / SharedConstants.TICKS_PER_GAME_DAY;
	}

	/** Con el reloj y no con isDarkOutside, que también da true en una tormenta de día. */
	public static boolean esDeNoche(ServerLevel level) {
		return esDeNoche(level.getOverworldClockTime());
	}

	public static boolean esDeNoche(long reloj) {
		long hora = reloj % SharedConstants.TICKS_PER_GAME_DAY;
		return hora >= EMPIEZA_LA_NOCHE && hora < TERMINA_LA_NOCHE;
	}

	/** Cuánto de la noche ya pasó, de 0 a 1: la barra de la luna de sangre se vacía con esto. */
	public static float avanceDeLaNoche(ServerLevel level) {
		long hora = level.getOverworldClockTime() % SharedConstants.TICKS_PER_GAME_DAY;
		return Mth.clamp((hora - EMPIEZA_LA_NOCHE) / (float) (TERMINA_LA_NOCHE - EMPIEZA_LA_NOCHE), 0, 1);
	}

	public static int porTiempo(long dias, Configuracion config) {
		if (config.diasPorNivelExtra() <= 0) {
			return 0;
		}
		return (int) Math.min(dias / config.diasPorNivelExtra(), config.nivelesPorTiempoMaximos());
	}

	public static boolean esCampeon(Configuracion config, RandomSource random) {
		return config.chanceDeCampeon() > 0 && random.nextDouble() < config.chanceDeCampeon();
	}

	public static int calcular(ResourceKey<Level> dimension, BlockPos spawn, BlockPos pos, Configuracion config, RandomSource random) {
		double dx;
		double dz;
		int extra;
		if (dimension == Level.NETHER) {
			// Un bloque del Nether son 8 del overworld: así cruzar un portal no cambia de golpe el nivel
			dx = pos.getX() * 8.0 - spawn.getX();
			dz = pos.getZ() * 8.0 - spawn.getZ();
			extra = config.extraNether();
		} else if (dimension == Level.END) {
			// El spawn del overworld no significa nada en el End: las islas se ordenan desde el centro
			dx = pos.getX();
			dz = pos.getZ();
			extra = config.extraEnd();
		} else {
			dx = pos.getX() - spawn.getX();
			dz = pos.getZ() - spawn.getZ();
			extra = 0;
		}

		int nivel = 1 + (int) Math.floor(Math.sqrt(dx * dx + dz * dz) / config.bloquesPorNivel()) + extra;
		if (config.variacion() > 0) {
			nivel += random.nextInt(config.variacion() * 2 + 1) - config.variacion();
		}
		return Mth.clamp(nivel, 1, config.nivelMaximo());
	}
}
