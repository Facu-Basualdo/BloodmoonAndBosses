package com.bloodmoonandbosses;

import java.util.UUID;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;

/**
 * Algunas noches de luna llena los mobs que aparecen tienen más nivel y vienen de a dos. Sólo en el overworld, que
 * es donde hay luna.
 */
public final class LunaDeSangre {
	// En el ServerLevel y persistentes: si el servidor se reinicia a mitad de la noche, la luna sigue y no se vuelve a sortear
	public static final AttachmentType<Long> NOCHE_ACTIVA = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("luna_de_sangre"), Codec.LONG);
	public static final AttachmentType<Long> NOCHE_SORTEADA = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("noche_sorteada"), Codec.LONG);

	private static @Nullable ServerBossEvent barra;

	private LunaDeSangre() {
	}

	public static boolean activa(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && level.hasAttached(NOCHE_ACTIVA) && BloodmoonAndBosses.config().lunaDeSangre().activa();
	}

	public static int nivelesExtra(ServerLevel level) {
		return activa(level) ? BloodmoonAndBosses.config().lunaDeSangre().nivelesExtra() : 0;
	}

	public static boolean esLunaLlena(ServerLevel level) {
		return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE) == MoonPhase.FULL_MOON;
	}

	/** Cada segundo. La noche se sortea una sola vez, al empezar: así dormir o reconectarse no da otra tirada. */
	public static void actualizar(ServerLevel overworld) {
		Configuracion.LunaDeSangre config = BloodmoonAndBosses.config().lunaDeSangre();
		boolean noche = NivelDeZona.esDeNoche(overworld);
		long dia = NivelDeZona.dias(overworld);

		@Nullable Long activa = overworld.getAttached(NOCHE_ACTIVA);
		if (activa != null && (!noche || activa != dia || !config.activa())) {
			terminar(overworld);
		}
		if (noche && overworld.getAttachedOrElse(NOCHE_SORTEADA, -1L) != dia) {
			overworld.setAttached(NOCHE_SORTEADA, dia);
			if (config.activa() && esLunaLlena(overworld) && overworld.getRandom().nextDouble() < config.chance()) {
				empezar(overworld);
			}
		}
		if (activa(overworld)) {
			mostrarBarra(overworld);
		}
	}

	/** También para /bloodmoon evento: sólo de noche, porque al amanecer se termina sola. */
	public static boolean empezar(ServerLevel overworld) {
		if (!NivelDeZona.esDeNoche(overworld) || overworld.dimension() != Level.OVERWORLD) {
			return false;
		}
		long dia = NivelDeZona.dias(overworld);
		overworld.setAttached(NOCHE_ACTIVA, dia);
		overworld.setAttached(NOCHE_SORTEADA, dia);
		for (ServerPlayer jugador : overworld.players()) {
			Eventos.anunciar(jugador, Idioma.tr(jugador, "bloodmoonandbosses.luna.titulo").withStyle(ChatFormatting.DARK_RED),
					Idioma.tr(jugador, "bloodmoonandbosses.luna.subtitulo").withStyle(ChatFormatting.RED));
			overworld.playSound(null, jugador.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.6f);
		}
		mostrarBarra(overworld);
		return true;
	}

	public static void terminar(ServerLevel overworld) {
		overworld.removeAttached(NOCHE_ACTIVA);
		if (barra != null) {
			barra.removeAllPlayers();
			barra = null;
		}
		for (ServerPlayer jugador : overworld.players()) {
			jugador.sendSystemMessage(Idioma.tr(jugador, "bloodmoonandbosses.luna.fin").withStyle(ChatFormatting.GRAY), true);
		}
	}

	private static void mostrarBarra(ServerLevel overworld) {
		if (barra == null) {
			// Una barra para todos: traducible, con respaldo en el idioma de la config
			barra = new ServerBossEvent(UUID.randomUUID(), Idioma.guardado("bloodmoonandbosses.luna.titulo").withStyle(ChatFormatting.DARK_RED),
					BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
		}
		barra.setProgress(1 - NivelDeZona.avanceDeLaNoche(overworld));
		// Sólo los del overworld: en el Nether no hay luna
		for (ServerPlayer jugador : barra.getPlayers().toArray(ServerPlayer[]::new)) {
			if (jugador.level() != overworld || jugador.isRemoved()) {
				barra.removePlayer(jugador);
			}
		}
		for (ServerPlayer jugador : overworld.players()) {
			barra.addPlayer(jugador);
		}
	}

	/**
	 * finalizeSpawn de un mob NATURAL: con chanceDeCompania, un tick después le aparece al lado otro igual, con el
	 * mismo nivel. Un tick después porque recién ahí está en el mundo y tiene nivel; con motivo EVENT para que el
	 * compañero no traiga otro.
	 */
	public static void alAparecerNatural(Mob mob, ServerLevel level) {
		if (!activa(level) || mob.getRandom().nextDouble() >= BloodmoonAndBosses.config().lunaDeSangre().chanceDeCompania()) {
			return;
		}
		Eventos.programar(level, 1, () -> {
			int nivel = NivelDeMob.nivel(mob);
			if (!mob.isAlive() || mob.level() != level || nivel == 0) {
				return;
			}
			BlockPos pos = Eventos.lugarCerca(level, mob.blockPosition(), 1, 3, null, mob.getType(), false, mob.getRandom());
			if (pos != null) {
				NivelDeMob.invocarConNivel(mob.getType(), level, pos, EntitySpawnReason.EVENT, nivel, false);
			}
		});
	}

	public static void limpiar() {
		barra = null;
	}
}
