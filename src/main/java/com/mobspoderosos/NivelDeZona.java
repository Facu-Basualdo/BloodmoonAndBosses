package com.mobspoderosos;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class NivelDeZona {
	private NivelDeZona() {
	}

	public static int calcular(ServerLevel level, BlockPos pos, RandomSource random) {
		BlockPos spawn = level.getServer().getRespawnData().pos();
		return calcular(level.dimension(), spawn, pos, MobsPoderosos.config(), random);
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
