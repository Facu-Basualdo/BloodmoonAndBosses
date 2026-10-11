package com.mobspoderosos;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Un mob mucho más fuerte que aparece cerca de un jugador, con escoltas y barra de jefe. No depende de memoria: el
 * attachment JEFE lo marca, y al cargarse (reinicio, chunk que vuelve) se lo vuelve a seguir.
 */
public final class JefeDeZona {
	public static final AttachmentType<Boolean> JEFE = AttachmentRegistry.createPersistent(MobsPoderosos.id("jefe"), Codec.BOOL);
	public static final AttachmentType<Long> ULTIMO = AttachmentRegistry.create(MobsPoderosos.id("ultimo_jefe"),
			builder -> builder.persistent(Codec.LONG).copyOnDeath());
	public static final TagKey<EntityType<?>> TIPOS_OVERWORLD = tipos("overworld");
	public static final TagKey<EntityType<?>> TIPOS_NETHER = tipos("nether");
	public static final TagKey<EntityType<?>> TIPOS_END = tipos("end");

	private static final Identifier MODIFICADOR = MobsPoderosos.id("jefe");
	// Si ya hay un jefe a esta distancia, no aparece otro: dos barras a la vez son una pelea imposible
	private static final int DISTANCIA_ENTRE_JEFES = 128;
	private static final int DISTANCIA_MINIMA = 16;
	private static final int DISTANCIA_MAXIMA = 24;

	private static final Map<Mob, Seguido> SEGUIDOS = new IdentityHashMap<>();

	private JefeDeZona() {
	}

	private static TagKey<EntityType<?>> tipos(String dimension) {
		return TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("jefe_de_zona/" + dimension));
	}

	public static boolean esJefe(Entity entity) {
		return entity.getAttachedOrElse(JEFE, false);
	}

	public static @Nullable ServerBossEvent barra(Mob jefe) {
		Seguido seguido = SEGUIDOS.get(jefe);
		return seguido == null ? null : seguido.barra;
	}

	/** Cada minuto, por jugador. */
	public static void sortear(ServerPlayer jugador) {
		Configuracion.JefeDeZona config = MobsPoderosos.config().jefeDeZona();
		if (!config.activo() || !Eventos.puedeTenerEventos(jugador) || !(jugador.level() instanceof ServerLevel level)) {
			return;
		}
		long ultimo = jugador.getAttachedOrElse(ULTIMO, Long.MIN_VALUE / 2);
		if (level.getGameTime() - ultimo < config.diasEntreJefes() * 24_000L || hayUnJefeCerca(jugador)) {
			return;
		}
		if (NivelDeZona.calcular(level, jugador.blockPosition(), level.getRandom()) < config.nivelMinimo()) {
			return;
		}
		if (level.getRandom().nextDouble() < config.chancePorMinuto()) {
			aparecer(jugador, config, DISTANCIA_MINIMA, DISTANCIA_MAXIMA);
		}
	}

	private static boolean hayUnJefeCerca(ServerPlayer jugador) {
		for (Mob jefe : SEGUIDOS.keySet()) {
			if (jefe.level() == jugador.level() && jefe.distanceToSqr(jugador) < DISTANCIA_ENTRE_JEFES * DISTANCIA_ENTRE_JEFES) {
				return true;
			}
		}
		return false;
	}

	/** Sin sorteo ni espera (es lo que usa /mobspoderosos evento). Devuelve el jefe, o null si no encontró lugar. */
	public static @Nullable Mob aparecer(ServerPlayer jugador, Configuracion.JefeDeZona config, int distanciaMinima, int distanciaMaxima) {
		if (!(jugador.level() instanceof ServerLevel level)) {
			return null;
		}
		Optional<HolderSet.Named<EntityType<?>>> tipos = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).get(tiposDe(level));
		RandomSource random = level.getRandom();
		if (tipos.isEmpty() || tipos.get().size() == 0) {
			return null;
		}
		EntityType<?> tipo = tipos.get().getRandomElement(random).map(Holder::value).orElseThrow();
		// A la altura del jugador y no en la superficie: en una mina tiene que aparecer en la mina
		BlockPos pos = Eventos.lugarCerca(level, jugador.blockPosition(), distanciaMinima, distanciaMaxima, null, tipo, false, random);
		if (pos == null) {
			return null;
		}

		int nivelZona = NivelDeZona.calcular(level, pos, random);
		Mob jefe = NivelDeMob.invocarConNivel(tipo, level, pos, EntitySpawnReason.EVENT, nivelZona + config.nivelesExtra(), true);
		if (jefe == null) {
			return null;
		}
		convertir(jefe, config);
		jefe.setTarget(jugador);
		for (int i = 0; i < config.escoltas(); i++) {
			BlockPos lugar = Eventos.lugarCerca(level, pos, 1, 4, null, tipo, false, random);
			if (lugar != null) {
				Mob escolta = NivelDeMob.invocarConNivel(tipo, level, lugar, EntitySpawnReason.EVENT, NivelDeZona.calcular(level, lugar, random), false);
				if (escolta != null) {
					escolta.setTarget(jugador);
				}
			}
		}

		jugador.setAttached(ULTIMO, level.getGameTime());
		level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 4.0f, 0.6f);
		Eventos.anunciar(jugador, Component.literal("Jefe de zona").withStyle(ChatFormatting.DARK_PURPLE),
				Component.literal("Apareció cerca: ").append(NivelDeMob.nombreSinVida(jefe)));
		return jefe;
	}

	private static TagKey<EntityType<?>> tiposDe(ServerLevel level) {
		if (level.dimension() == Level.NETHER) {
			return TIPOS_NETHER;
		} else if (level.dimension() == Level.END) {
			return TIPOS_END;
		}
		return TIPOS_OVERWORLD;
	}

	/** Un mob con nivel pasa a ser jefe: más vida, persistente y con barra. */
	public static void convertir(Mob jefe, Configuracion.JefeDeZona config) {
		jefe.setAttached(JEFE, true);
		AttributeInstance vida = jefe.getAttribute(Attributes.MAX_HEALTH);
		if (vida != null && config.multiplicadorDeVida() > 1) {
			// MULTIPLIED_TOTAL: multiplica también lo que ya sumó el nivel
			vida.addOrReplacePermanentModifier(new AttributeModifier(MODIFICADOR, config.multiplicadorDeVida() - 1,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		jefe.setHealth(jefe.getMaxHealth());
		// Si no, a más de 128 bloques de un jugador desaparece al instante y la barra queda colgada
		jefe.setPersistenceRequired();
		seguir(jefe);
	}

	private static void seguir(Mob jefe) {
		if (SEGUIDOS.containsKey(jefe) || !(jefe.level() instanceof ServerLevel level)) {
			return;
		}
		ServerBossEvent barra = new ServerBossEvent(UUID.randomUUID(), NivelDeMob.nombreSinVida(jefe), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
		SEGUIDOS.put(jefe, new Seguido(barra, level.getGameTime()));
	}

	/** ENTITY_LOAD: un jefe que vuelve con su chunk o después de un reinicio. */
	public static void alCargar(Entity entity, ServerLevel level) {
		if (entity instanceof Mob jefe && esJefe(jefe)) {
			seguir(jefe);
		}
	}

	/** ENTITY_UNLOAD: también corre al morir o al desaparecer. */
	public static void alDescargar(Entity entity, ServerLevel level) {
		if (entity instanceof Mob jefe) {
			Seguido seguido = SEGUIDOS.remove(jefe);
			if (seguido != null) {
				seguido.barra.removeAllPlayers();
			}
		}
	}

	public static void alMorir(LivingEntity muerto, DamageSource source) {
		if (!esJefe(muerto) || !(muerto.level() instanceof ServerLevel level)) {
			return;
		}
		Component mensaje = Component.literal("Cayó el jefe de zona: ").append(NivelDeMob.nombreSinVida((Mob) muerto)).withStyle(ChatFormatting.GOLD);
		int radio = MobsPoderosos.config().jefeDeZona().radioDeBarra();
		for (Player jugador : level.getEntitiesOfClass(Player.class, muerto.getBoundingBox().inflate(radio), Entity::isAlive)) {
			jugador.sendSystemMessage(mensaje);
		}
	}

	/** Cada segundo: la barra la ven los que están cerca, y un jefe sin nadie cerca durante un rato se va. */
	public static void actualizarTodos() {
		Configuracion.JefeDeZona config = MobsPoderosos.config().jefeDeZona();
		for (Map.Entry<Mob, Seguido> entrada : List.copyOf(SEGUIDOS.entrySet())) {
			Mob jefe = entrada.getKey();
			Seguido seguido = entrada.getValue();
			if (jefe.isRemoved() || !(jefe.level() instanceof ServerLevel level)) {
				alDescargar(jefe, null);
				continue;
			}
			double radio = config.radioDeBarra();
			boolean alguienCerca = false;
			for (ServerPlayer jugador : level.players()) {
				boolean cerca = jugador.isAlive() && !jugador.isSpectator() && jugador.distanceToSqr(jefe) <= radio * radio;
				if (cerca) {
					seguido.barra.addPlayer(jugador);
					alguienCerca = true;
				} else {
					seguido.barra.removePlayer(jugador);
				}
			}
			// Los que se fueron a otra dimensión o se desconectaron no están en level.players()
			for (ServerPlayer jugador : seguido.barra.getPlayers().toArray(ServerPlayer[]::new)) {
				if (jugador.level() != level || jugador.isRemoved()) {
					seguido.barra.removePlayer(jugador);
				}
			}
			seguido.barra.setName(NivelDeMob.nombreSinVida(jefe));
			seguido.barra.setProgress(Math.clamp(jefe.getHealth() / jefe.getMaxHealth(), 0f, 1f));

			if (alguienCerca) {
				seguido.ultimoJugadorCerca = level.getGameTime();
			} else if (level.getGameTime() - seguido.ultimoJugadorCerca > config.segundosAbandonado() * 20L) {
				jefe.discard();
			}
		}
	}

	public static void limpiar() {
		SEGUIDOS.clear();
	}

	private static final class Seguido {
		private final ServerBossEvent barra;
		private long ultimoJugadorCerca;

		private Seguido(ServerBossEvent barra, long ultimoJugadorCerca) {
			this.barra = barra;
			this.ultimoJugadorCerca = ultimoJugadorCerca;
		}
	}
}
