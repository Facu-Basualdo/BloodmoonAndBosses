package com.mobspoderosos;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/** El reloj de los eventos: estampidas, jefes de zona, luna de sangre y las tareas que esperan unos ticks. */
public final class Eventos {
	private static final int TICKS_POR_SEGUNDO = 20;
	private static final int TICKS_POR_MINUTO = 1200;
	// Cuánto se busca hacia arriba y hacia abajo un piso a la altura del jugador (en una cueva, en un edificio)
	private static final int ALTO_DE_BUSQUEDA = 8;
	private static final int INTENTOS = 12;
	// Un ángulo de 60°: los mobs de una estampida llegan todos del mismo lado
	private static final double APERTURA = Math.PI / 3;

	private static final List<Tarea> TAREAS = new ArrayList<>();

	private Eventos() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(Eventos::alTerminarTick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Afijo::alRecibirDanio);
		ServerLivingEntityEvents.AFTER_DEATH.register(Afijo::alMorir);
		ServerLivingEntityEvents.AFTER_DEATH.register(JefeDeZona::alMorir);
		ServerEntityEvents.ENTITY_LOAD.register(JefeDeZona::alCargar);
		ServerEntityEvents.ENTITY_UNLOAD.register(JefeDeZona::alDescargar);
		// En singleplayer el mismo proceso abre otro mundo: lo que quedó en memoria es del anterior
		ServerLifecycleEvents.SERVER_STOPPED.register(servidor -> limpiar());
	}

	public static void limpiar() {
		TAREAS.clear();
		Estampida.limpiar();
		JefeDeZona.limpiar();
		LunaDeSangre.limpiar();
	}

	/** Corre la acción dentro de tantos ticks, si el mundo sigue cargado. */
	public static void programar(ServerLevel level, int ticks, Runnable accion) {
		TAREAS.add(new Tarea(level, level.getServer().getTickCount() + Math.max(ticks, 1), accion));
	}

	private static void alTerminarTick(MinecraftServer servidor) {
		int ahora = servidor.getTickCount();
		if (!TAREAS.isEmpty()) {
			// Copia: una tarea puede programar otra
			List<Tarea> listas = new ArrayList<>();
			TAREAS.removeIf(tarea -> {
				if (tarea.cuando() <= ahora) {
					listas.add(tarea);
					return true;
				}
				return false;
			});
			for (Tarea tarea : listas) {
				if (servidor.getLevel(tarea.level().dimension()) == tarea.level()) {
					tarea.accion().run();
				}
			}
		}

		if (ahora % TICKS_POR_SEGUNDO == 0) {
			LunaDeSangre.actualizar(servidor.overworld());
			Estampida.actualizarTodas();
			JefeDeZona.actualizarTodos();
		}
		if (ahora % TICKS_POR_MINUTO == 0) {
			for (ServerPlayer jugador : List.copyOf(servidor.getPlayerList().getPlayers())) {
				Estampida.sortear(jugador);
				JefeDeZona.sortear(jugador);
			}
		}
	}

	/** Los jugadores a los que les puede tocar un evento: vivos, en supervivencia o aventura, y con mobs hostiles en el mundo. */
	public static boolean puedeTenerEventos(ServerPlayer jugador) {
		return jugador.isAlive() && !jugador.isSpectator() && !jugador.isCreative()
				&& jugador.level().getDifficulty() != Difficulty.PEACEFUL;
	}

	/**
	 * Un lugar donde un mob de ese tipo puede pararse, a entre min y max bloques del centro. Con angulo, cerca de esa
	 * dirección. En la superficie usa el heightmap; si no, busca un piso a la altura del centro (para cuevas). null si
	 * no encontró ninguno en un chunk cargado.
	 */
	public static @Nullable BlockPos lugarCerca(ServerLevel level, BlockPos centro, int min, int max, @Nullable Double angulo,
			EntityType<?> tipo, boolean superficie, RandomSource random) {
		for (int i = 0; i < INTENTOS; i++) {
			double a = angulo != null ? angulo + (random.nextDouble() - 0.5) * APERTURA : random.nextDouble() * Math.PI * 2;
			double distancia = min + random.nextDouble() * (max - min);
			int x = centro.getX() + (int) Math.round(Math.cos(a) * distancia);
			int z = centro.getZ() + (int) Math.round(Math.sin(a) * distancia);
			if (!level.isLoaded(new BlockPos(x, centro.getY(), z))) {
				continue;
			}
			if (superficie) {
				BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
				if (sirve(level, pos, tipo)) {
					return pos;
				}
				continue;
			}
			for (int dy = 0; dy <= ALTO_DE_BUSQUEDA * 2; dy++) {
				// 0, +1, -1, +2, -2…: primero lo más parecido a la altura del centro
				int y = centro.getY() + (dy % 2 == 0 ? dy / 2 : -(dy + 1) / 2);
				BlockPos pos = new BlockPos(x, y, z);
				if (sirve(level, pos, tipo)) {
					return pos;
				}
			}
		}
		return null;
	}

	private static boolean sirve(ServerLevel level, BlockPos pos, EntityType<?> tipo) {
		return SpawnPlacements.isSpawnPositionOk(tipo, level, pos)
				&& level.noCollision(tipo.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}

	/** Un título en el centro de la pantalla: lo muestra cualquier cliente, con o sin el mod. */
	public static void anunciar(ServerPlayer jugador, Component titulo, Component subtitulo) {
		jugador.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
		jugador.connection.send(new ClientboundSetSubtitleTextPacket(subtitulo));
		jugador.connection.send(new ClientboundSetTitleTextPacket(titulo));
	}

	/** Al inventario, y lo que no entra, al piso a los pies del jugador. */
	public static void darAlJugador(ServerPlayer jugador, ItemStack item) {
		jugador.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
	}

	private record Tarea(ServerLevel level, int cuando, Runnable accion) {
	}
}
