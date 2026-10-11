package com.mobspoderosos;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Una horda de mobs de la zona que carga contra un jugador, de noche y al aire libre. Si los jugadores matan al menos
 * la mitad, se lleva una tirada de botín raro. Vive en memoria: si el servidor se reinicia, los mobs quedan como mobs
 * comunes.
 */
public final class Estampida {
	public static final TagKey<EntityType<?>> TIPOS = TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("estampida"));
	// En el jugador, persistente y copiado al morir: si no, morirse o reconectarse saltearía la espera
	public static final AttachmentType<Long> ULTIMA = AttachmentRegistry.create(MobsPoderosos.id("ultima_estampida"),
			builder -> builder.persistent(Codec.LONG).copyOnDeath());

	// Transitorios: no se guardan con el mob, así que si el servidor se reinicia vuelve a su velocidad normal
	private static final Identifier MODIFICADOR = MobsPoderosos.id("estampida");
	private static final Map<UUID, Estampida> ACTIVAS = new HashMap<>();

	private final ServerPlayer jugador;
	private final ServerLevel level;
	private final List<Mob> mobs;
	private final int total;
	private final int nivel;
	private final long fin;
	private final ServerBossEvent barra;
	// Los que mató un jugador
	private int muertos;

	private Estampida(ServerPlayer jugador, ServerLevel level, List<Mob> mobs, int nivel, long fin) {
		this.jugador = jugador;
		this.level = level;
		this.mobs = mobs;
		this.total = mobs.size();
		this.nivel = nivel;
		this.fin = fin;
		this.barra = new ServerBossEvent(UUID.randomUUID(), Component.literal("Estampida").withStyle(ChatFormatting.RED),
				BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		barra.addPlayer(jugador);
	}

	public static boolean activa(ServerPlayer jugador) {
		return ACTIVAS.containsKey(jugador.getUUID());
	}

	public static List<Mob> mobs(ServerPlayer jugador) {
		Estampida estampida = ACTIVAS.get(jugador.getUUID());
		return estampida == null ? List.of() : List.copyOf(estampida.mobs);
	}

	public static int cuantos(int nivelZona, Configuracion.Estampida config) {
		return Math.max(1, Math.min(config.mobsBase() + nivelZona / config.nivelesPorMobExtra(), config.mobsMaximos()));
	}

	/** Cada minuto, por jugador. */
	public static void sortear(ServerPlayer jugador) {
		Configuracion.Estampida config = MobsPoderosos.config().estampida();
		if (!config.activa() || activa(jugador) || !Eventos.puedeTenerEventos(jugador)
				|| !(jugador.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
			return;
		}
		if (!NivelDeZona.esDeNoche(level) || !level.canSeeSky(jugador.blockPosition())) {
			return;
		}
		long ultima = jugador.getAttachedOrElse(ULTIMA, Long.MIN_VALUE / 2);
		if (level.getGameTime() - ultima < config.diasEntreEstampidas() * 24_000L) {
			return;
		}
		if (NivelDeZona.calcular(level, jugador.blockPosition(), level.getRandom()) < config.nivelMinimo()) {
			return;
		}
		if (level.getRandom().nextDouble() < config.chancePorMinuto()) {
			empezar(jugador, config);
		}
	}

	/**
	 * Sin sorteo ni espera (es lo que usa /mobspoderosos evento). Devuelve el motivo si no pudo empezar. Al aire
	 * libre aparecen sobre la superficie; bajo techo, a la altura del jugador.
	 */
	public static @Nullable String empezar(ServerPlayer jugador, Configuracion.Estampida config) {
		if (activa(jugador)) {
			return "ya hay una estampida contra " + jugador.getName().getString();
		}
		if (!(jugador.level() instanceof ServerLevel level)) {
			return "el jugador no está en un mundo del servidor";
		}
		Optional<HolderSet.Named<EntityType<?>>> tipos = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).get(TIPOS);
		if (tipos.isEmpty() || tipos.get().size() == 0) {
			return "el tag " + TIPOS.location() + " está vacío";
		}

		RandomSource random = level.getRandom();
		BlockPos centro = jugador.blockPosition();
		boolean superficie = level.canSeeSky(centro);
		int nivelZona = NivelDeZona.calcular(level, centro, random);
		int cantidad = cuantos(nivelZona, config);
		double angulo = random.nextDouble() * Math.PI * 2;
		List<Mob> mobs = new ArrayList<>();
		for (int i = 0; i < cantidad; i++) {
			EntityType<?> tipo = tipos.get().getRandomElement(random).map(Holder::value).orElseThrow();
			BlockPos pos = Eventos.lugarCerca(level, centro, config.distanciaMinima(), config.distanciaMaxima(), angulo, tipo, superficie, random);
			if (pos == null) {
				continue;
			}
			// El primero es el líder: un campeón con afijo
			boolean lider = mobs.isEmpty();
			int nivel = NivelDeZona.calcular(level, pos, random) + (lider ? MobsPoderosos.config().nivelesDeCampeon() : 0);
			Mob mob = NivelDeMob.invocarConNivel(tipo, level, pos, EntitySpawnReason.EVENT, nivel, lider);
			if (mob != null) {
				cargar(mob, jugador, config);
				mobs.add(mob);
			}
		}
		// Con la mitad de la horda ya no es una estampida, y al lado de un precipicio o en el agua puede no entrar
		if (mobs.size() * 2 < cantidad) {
			mobs.forEach(Entity::discard);
			return "no hay lugar alrededor de " + jugador.getName().getString() + " para la estampida";
		}

		Estampida estampida = new Estampida(jugador, level, mobs, nivelZona, level.getGameTime() + config.segundosMaximos() * 20L);
		ACTIVAS.put(jugador.getUUID(), estampida);
		jugador.setAttached(ULTIMA, level.getGameTime());
		// Desde donde vienen: el cuerno se oye de lejos y marca la dirección
		level.playSound(null, mobs.getFirst().blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0f, 1.0f);
		Eventos.anunciar(jugador, Component.literal("¡Estampida!").withStyle(ChatFormatting.RED),
				Component.literal(mobs.size() + " mobs vienen hacia vos").withStyle(ChatFormatting.GOLD));
		return null;
	}

	private static void cargar(Mob mob, ServerPlayer jugador, Configuracion.Estampida config) {
		mob.setTarget(jugador);
		modificar(mob.getAttribute(Attributes.MOVEMENT_SPEED), config.velocidadExtra(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// Un esqueleto sigue a 16 bloques: sin esto, perdería al jugador antes de llegar
		modificar(mob.getAttribute(Attributes.FOLLOW_RANGE), config.distanciaMaxima() * 2.0, AttributeModifier.Operation.ADD_VALUE);
	}

	private static void modificar(@Nullable AttributeInstance atributo, double valor, AttributeModifier.Operation operacion) {
		if (atributo != null && valor != 0) {
			atributo.addOrUpdateTransientModifier(new AttributeModifier(MODIFICADOR, valor, operacion));
		}
	}

	private static void calmar(Mob mob) {
		for (var atributo : List.of(Attributes.MOVEMENT_SPEED, Attributes.FOLLOW_RANGE)) {
			AttributeInstance instancia = mob.getAttribute(atributo);
			if (instancia != null) {
				instancia.removeModifier(MODIFICADOR);
			}
		}
	}

	/** Cada segundo. */
	public static void actualizarTodas() {
		for (Estampida estampida : List.copyOf(ACTIVAS.values())) {
			estampida.actualizar();
		}
	}

	private void actualizar() {
		mobs.removeIf(mob -> {
			if (!mob.isRemoved()) {
				return false;
			}
			// Sólo los que mató un jugador: al amanecer los zombis y esqueletos se queman solos
			if (mob.getRemovalReason() == Entity.RemovalReason.KILLED && mob.getLastHurtByPlayer() != null) {
				muertos++;
			}
			return true;
		});

		boolean sigueAhi = jugador.isAlive() && !jugador.isRemoved() && jugador.level() == level
				&& level.getServer().getPlayerList().getPlayer(jugador.getUUID()) == jugador;
		if (!sigueAhi) {
			terminar(jugador.isDeadOrDying() ? "La estampida te pasó por encima" : null);
			return;
		}
		if (mobs.isEmpty()) {
			// Si la mitad se fue (se quemó, desapareció, se descargó el chunk) no cuenta como haberla frenado
			if (muertos * 2 >= total) {
				premiar();
			} else {
				terminar("La estampida se dispersó");
			}
			return;
		}
		if (level.getGameTime() >= fin) {
			terminar("La estampida se dispersó");
			return;
		}
		for (Mob mob : mobs) {
			if (mob.getTarget() == null && mob.isAlive()) {
				mob.setTarget(jugador);
			}
		}
		barra.setProgress(mobs.size() / (float) total);
	}

	private void premiar() {
		BotinPorNivel.Botin botin = BotinPorNivel.mejorBotin(nivel, BotinPorNivel.Botin.RARO);
		// El jugador como origen: las tablas son de tipo entity y no tienen condiciones sobre quién murió
		jugador.dropFromLootTable(level, level.damageSources().generic(), true, botin.tabla(), item -> Eventos.darAlJugador(jugador, item));
		level.playSound(null, jugador.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		Eventos.anunciar(jugador, Component.literal("¡Frenaste la estampida!").withStyle(ChatFormatting.GOLD),
				Component.literal("Te llevás un premio").withStyle(ChatFormatting.YELLOW));
		terminar(null);
	}

	private void terminar(@Nullable String mensaje) {
		ACTIVAS.remove(jugador.getUUID());
		barra.removeAllPlayers();
		for (Mob mob : mobs) {
			calmar(mob);
		}
		if (mensaje != null && !jugador.isRemoved()) {
			jugador.sendSystemMessage(Component.literal(mensaje).withStyle(ChatFormatting.GRAY), true);
		}
	}

	public static void limpiar() {
		ACTIVAS.clear();
	}
}
