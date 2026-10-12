package com.bloodmoonandbosses;

import java.util.ArrayList;
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
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

/**
 * Un mob mucho más fuerte que aparece cerca de un jugador, con escoltas y barra de jefe. No depende de memoria: el
 * attachment JEFE lo marca, y al cargarse (reinicio, chunk que vuelve) se lo vuelve a seguir.
 *
 * <p>Además de pegar como su especie, pelea como jefe: a media vida se enfurece (más rápido, más fuerte y con otra
 * tanda de escoltas) y cada tanto usa una habilidad que avisa un segundo antes: una onda de choque contra los que
 * están cerca o un tirón que trae a los que pelean de lejos.
 */
public final class JefeDeZona {
	public static final AttachmentType<Boolean> JEFE = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("jefe"), Codec.BOOL);
	// Persistente: un jefe que se enfureció y vuelve con su chunk no se enfurece de nuevo (ni llama otra tanda de escoltas)
	public static final AttachmentType<Boolean> FURIA = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("furia"), Codec.BOOL);
	public static final AttachmentType<Long> ULTIMO = AttachmentRegistry.create(BloodmoonAndBosses.id("ultimo_jefe"),
			builder -> builder.persistent(Codec.LONG).copyOnDeath());
	public static final TagKey<EntityType<?>> TIPOS_OVERWORLD = tipos("overworld");
	public static final TagKey<EntityType<?>> TIPOS_NETHER = tipos("nether");
	public static final TagKey<EntityType<?>> TIPOS_END = tipos("end");

	private static final Identifier MODIFICADOR = BloodmoonAndBosses.id("jefe");
	private static final Identifier MODIFICADOR_FURIA = BloodmoonAndBosses.id("furia");
	// El aviso de una habilidad: lo justo para alejarse dos o tres bloques, o cubrirse
	private static final int TICKS_DE_AVISO = 20;
	// Cuánto se queda lejos del jefe el que trae el tirón: si llegara encima, el siguiente golpe sería seguro
	private static final double DISTANCIA_DEL_TIRON = 3;
	private static final double VELOCIDAD_MAXIMA_DEL_TIRON = 2.5;
	// Si ya hay un jefe a esta distancia, no aparece otro: dos barras a la vez son una pelea imposible
	private static final int DISTANCIA_ENTRE_JEFES = 128;
	private static final int DISTANCIA_MINIMA = 16;
	private static final int DISTANCIA_MAXIMA = 24;
	// El color del contorno sale del equipo del scoreboard: uno por color, el mismo que la barra
	public static final String EQUIPO = "bloodmoon.jefe";
	public static final String EQUIPO_FURIA = "bloodmoon.furia";

	private static final Map<Mob, Seguido> SEGUIDOS = new IdentityHashMap<>();

	private JefeDeZona() {
	}

	private static TagKey<EntityType<?>> tipos(String dimension) {
		return TagKey.create(Registries.ENTITY_TYPE, BloodmoonAndBosses.id("jefe_de_zona/" + dimension));
	}

	public static boolean esJefe(Entity entity) {
		return entity.getAttachedOrElse(JEFE, false);
	}

	public static boolean estaFurioso(Entity entity) {
		return entity.getAttachedOrElse(FURIA, false);
	}

	public static @Nullable ServerBossEvent barra(Mob jefe) {
		Seguido seguido = SEGUIDOS.get(jefe);
		return seguido == null ? null : seguido.barra;
	}

	/** Cada minuto, por jugador. */
	public static void sortear(ServerPlayer jugador) {
		Configuracion.JefeDeZona config = BloodmoonAndBosses.config().jefeDeZona();
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

	/** Sin sorteo ni espera (es lo que usa /bloodmoon evento). Devuelve el jefe, o null si no encontró lugar. */
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
		ServerBossEvent barra = new ServerBossEvent(UUID.randomUUID(), nombreDeLaBarra(jefe), colorDeLaBarra(jefe), BossEvent.BossBarOverlay.NOTCHED_10);
		long ahora = level.getGameTime();
		SEGUIDOS.put(jefe, new Seguido(barra, ahora, ahora + BloodmoonAndBosses.config().jefeDeZona().segundosEntreHabilidades() * 20L));
		resaltar(jefe, BloodmoonAndBosses.config().jefeDeZona().contorno());
	}

	/**
	 * El contorno brillante de la flecha espectral, que el cliente vanilla dibuja con el color del equipo del mob:
	 * violeta, y rojo con furia, como la barra. Se ve a través de las paredes, igual que la barra avisa que está cerca.
	 * Al morir o desaparecer, vanilla lo saca del equipo solo ({@code Scoreboard.entityRemoved}).
	 */
	public static void resaltar(Mob jefe, boolean contorno) {
		Scoreboard scoreboard = jefe.level().getScoreboard();
		String nombre = jefe.getScoreboardName();
		PlayerTeam actual = scoreboard.getPlayersTeam(nombre);
		jefe.setGlowingTag(contorno);
		if (!contorno) {
			if (actual != null && (actual.getName().equals(EQUIPO) || actual.getName().equals(EQUIPO_FURIA))) {
				scoreboard.removePlayerFromTeam(nombre, actual);
			}
			return;
		}
		PlayerTeam equipo = estaFurioso(jefe) ? equipo(scoreboard, EQUIPO_FURIA, TeamColor.RED) : equipo(scoreboard, EQUIPO, TeamColor.DARK_PURPLE);
		// Sólo si cambia: cada alta en un equipo se les manda a todos los jugadores
		if (actual != equipo) {
			scoreboard.addPlayerToTeam(nombre, equipo);
		}
	}

	private static PlayerTeam equipo(Scoreboard scoreboard, String nombre, TeamColor color) {
		PlayerTeam equipo = scoreboard.getPlayerTeam(nombre);
		if (equipo == null) {
			equipo = scoreboard.addPlayerTeam(nombre);
			equipo.setColor(Optional.of(color));
		}
		return equipo;
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
		int radio = BloodmoonAndBosses.config().jefeDeZona().radioDeBarra();
		for (Player jugador : level.getEntitiesOfClass(Player.class, muerto.getBoundingBox().inflate(radio), Entity::isAlive)) {
			jugador.sendSystemMessage(mensaje);
		}
	}

	/** Cada segundo: la barra la ven los que están cerca, y un jefe sin nadie cerca durante un rato se va. */
	public static void actualizarTodos() {
		Configuracion.JefeDeZona config = BloodmoonAndBosses.config().jefeDeZona();
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
			if (!estaFurioso(jefe) && jefe.isAlive() && jefe.getHealth() <= jefe.getMaxHealth() * config.vidaFuria()) {
				enfurecer(jefe, config);
			}
			seguido.barra.setName(nombreDeLaBarra(jefe));
			seguido.barra.setColor(colorDeLaBarra(jefe));
			// Cada segundo: así sigue a la furia y a un /bloodmoon recargar que lo apague o lo prenda
			resaltar(jefe, config.contorno());
			seguido.barra.setProgress(Math.clamp(jefe.getHealth() / jefe.getMaxHealth(), 0f, 1f));

			long ahora = level.getGameTime();
			if (alguienCerca) {
				seguido.ultimoJugadorCerca = ahora;
				if (ahora >= seguido.proximaHabilidad && jefe.isAlive()) {
					seguido.proximaHabilidad = ahora + config.segundosEntreHabilidades() * 20L;
					usarHabilidad(jefe, level, config);
				}
			} else if (ahora - seguido.ultimoJugadorCerca > config.segundosAbandonado() * 20L) {
				jefe.discard();
			}
		}
	}

	private static Component nombreDeLaBarra(Mob jefe) {
		Component nombre = NivelDeMob.nombreSinVida(jefe);
		return estaFurioso(jefe) ? nombre.copy().append(Component.literal(" · Furia").withStyle(ChatFormatting.RED)) : nombre;
	}

	private static BossEvent.BossBarColor colorDeLaBarra(Mob jefe) {
		return estaFurioso(jefe) ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE;
	}

	/**
	 * Una sola vez por jefe (FURIA viaja en el NBT). Los modificadores son permanentes con id fijo, como los del nivel:
	 * se guardan con el mob y aplicar dos veces no apila.
	 */
	public static void enfurecer(Mob jefe, Configuracion.JefeDeZona config) {
		if (!(jefe.level() instanceof ServerLevel level) || estaFurioso(jefe)) {
			return;
		}
		jefe.setAttached(FURIA, true);
		modificar(jefe, Attributes.MOVEMENT_SPEED, config.velocidadFuria());
		// MULTIPLIED_TOTAL: sobre todo lo que ya suma el nivel y el arma
		modificar(jefe, Attributes.ATTACK_DAMAGE, config.danioFuria());

		RandomSource random = jefe.getRandom();
		LivingEntity objetivo = jefe.getTarget();
		for (int i = 0; i < config.escoltasFuria(); i++) {
			BlockPos lugar = Eventos.lugarCerca(level, jefe.blockPosition(), 1, 4, null, jefe.getType(), false, random);
			if (lugar != null) {
				Mob escolta = NivelDeMob.invocarConNivel(jefe.getType(), level, lugar, EntitySpawnReason.EVENT, NivelDeZona.calcular(level, lugar, random), false);
				if (escolta != null && objetivo != null) {
					escolta.setTarget(objetivo);
				}
			}
		}
		level.playSound(null, jefe.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0f, 0.8f);
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, jefe.getX(), jefe.getY(1) + 0.3, jefe.getZ(), 10, 0.6, 0.4, 0.6, 0);
		avisar(jefe, level, config, Component.literal("¡El jefe se enfurece!").withStyle(ChatFormatting.RED));
	}

	private static void modificar(Mob jefe, Holder<Attribute> atributo, double valor) {
		AttributeInstance instancia = jefe.getAttribute(atributo);
		if (instancia != null && valor != 0) {
			instancia.addOrReplacePermanentModifier(new AttributeModifier(MODIFICADOR_FURIA, valor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** Lo que hace el jefe cada segundosEntreHabilidades. */
	public enum Habilidad {
		/** Empuja y lastima a los que están a menos de radioOnda: castiga quedarse pegado al jefe. */
		ONDA,
		/** Trae a los que están entre radioOnda y radioTiron: castiga pelear de lejos sin acercarse nunca. */
		TIRON
	}

	/** La que corresponde según dónde están los jugadores, o null si no hay nadie a tiro. */
	public static @Nullable Habilidad elegirHabilidad(Mob jefe, Configuracion.JefeDeZona config, RandomSource random) {
		boolean alguienCerca = !jugadoresEntre(jefe, 0, config.radioOnda()).isEmpty();
		boolean alguienLejos = !jugadoresEntre(jefe, config.radioOnda(), Math.min(config.radioTiron(), config.radioDeBarra())).isEmpty();
		if (alguienCerca && alguienLejos) {
			return random.nextBoolean() ? Habilidad.ONDA : Habilidad.TIRON;
		}
		return alguienCerca ? Habilidad.ONDA : alguienLejos ? Habilidad.TIRON : null;
	}

	/** Avisa ahora y la hace un segundo después, con los jugadores que estén a tiro en ese momento. */
	private static void usarHabilidad(Mob jefe, ServerLevel level, Configuracion.JefeDeZona config) {
		Habilidad habilidad = elegirHabilidad(jefe, config, jefe.getRandom());
		if (habilidad == null) {
			return;
		}
		if (habilidad == Habilidad.ONDA) {
			level.playSound(null, jefe.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 1.2f);
			level.sendParticles(ParticleTypes.CLOUD, jefe.getX(), jefe.getY() + 0.2, jefe.getZ(), 30, config.radioOnda() / 3.0, 0.1, config.radioOnda() / 3.0, 0.02);
			avisar(jefe, level, config, Component.literal("El jefe prepara una onda de choque: ¡alejate!").withStyle(ChatFormatting.GOLD));
		} else {
			level.playSound(null, jefe.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.HOSTILE, 3.0f, 0.6f);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, jefe.getX(), jefe.getY(0.5), jefe.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
			avisar(jefe, level, config, Component.literal("El jefe te va a atraer: ¡agarrate!").withStyle(ChatFormatting.GOLD));
		}
		Eventos.programar(level, TICKS_DE_AVISO, () -> {
			if (jefe.isAlive() && !jefe.isRemoved() && jefe.level() == level) {
				ejecutar(habilidad, jefe, level, config);
			}
		});
	}

	/** Público para los tests: sin el segundo de aviso. */
	public static void ejecutar(Habilidad habilidad, Mob jefe, ServerLevel level, Configuracion.JefeDeZona config) {
		if (habilidad == Habilidad.ONDA) {
			ondaDeChoque(jefe, level, config);
		} else {
			tiron(jefe, level, config);
		}
	}

	private static void ondaDeChoque(Mob jefe, ServerLevel level, Configuracion.JefeDeZona config) {
		AttributeInstance ataque = jefe.getAttribute(Attributes.ATTACK_DAMAGE);
		float danio = (float) ((ataque == null ? 0 : ataque.getValue()) * config.danioOnda());
		for (ServerPlayer jugador : jugadoresEntre(jefe, 0, config.radioOnda())) {
			if (danio > 0) {
				jugador.hurtServer(level, jefe.damageSources().mobAttack(jefe), danio);
			}
			Vec3 afuera = horizontal(jugador.position().subtract(jefe.position()));
			jugador.push(afuera.x * config.fuerzaOnda(), 0.5, afuera.z * config.fuerzaOnda());
			// El cliente es dueño de su movimiento: sin esto el empujón sólo pasa en el servidor y el jugador "rebota" atrás
			jugador.needsSync = true;
		}
		level.playSound(null, jefe.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.7f);
		for (int i = 0; i < 16; i++) {
			double angulo = Math.PI * 2 * i / 16;
			level.sendParticles(ParticleTypes.EXPLOSION, jefe.getX() + Math.cos(angulo) * config.radioOnda() * 0.6, jefe.getY() + 0.3,
					jefe.getZ() + Math.sin(angulo) * config.radioOnda() * 0.6, 1, 0, 0, 0, 0);
		}
	}

	private static void tiron(Mob jefe, ServerLevel level, Configuracion.JefeDeZona config) {
		for (ServerPlayer jugador : jugadoresEntre(jefe, config.radioOnda(), Math.min(config.radioTiron(), config.radioDeBarra()))) {
			Vec3 hacia = jefe.position().subtract(jugador.position());
			double distancia = Math.sqrt(hacia.x * hacia.x + hacia.z * hacia.z);
			// En el aire el roce frena un 9 % por tick: una velocidad v recorre unas 11 v. Así llega a unos bloques del jefe
			double velocidad = Math.min(Math.max(distancia - DISTANCIA_DEL_TIRON, 0) / 11, VELOCIDAD_MAXIMA_DEL_TIRON);
			Vec3 direccion = horizontal(hacia);
			jugador.push(direccion.x * velocidad, 0.6, direccion.z * velocidad);
			jugador.needsSync = true;
			jugador.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), jefe);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, jugador.getX(), jugador.getY(0.5), jugador.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
		}
		level.playSound(null, jefe.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 3.0f, 0.5f);
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 plano = new Vec3(vector.x, 0, vector.z);
		return plano.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : plano.normalize();
	}

	/** Los que pelean: vivos, en supervivencia o aventura, en la misma dimensión, a una distancia en (desde, hasta]. */
	private static List<ServerPlayer> jugadoresEntre(Mob jefe, double desde, double hasta) {
		if (!(jefe.level() instanceof ServerLevel level)) {
			return List.of();
		}
		List<ServerPlayer> jugadores = new ArrayList<>();
		for (ServerPlayer jugador : level.players()) {
			double distancia = jugador.distanceToSqr(jefe);
			if (jugador.isAlive() && !jugador.isSpectator() && !jugador.isCreative() && distancia <= hasta * hasta
					&& (desde == 0 || distancia > desde * desde)) {
				jugadores.add(jugador);
			}
		}
		return jugadores;
	}

	/** En la barra de acción de los que ven la barra del jefe: el título taparía la pelea. */
	private static void avisar(Mob jefe, ServerLevel level, Configuracion.JefeDeZona config, Component mensaje) {
		for (ServerPlayer jugador : jugadoresEntre(jefe, 0, config.radioDeBarra())) {
			jugador.sendSystemMessage(mensaje, true);
		}
	}

	public static void limpiar() {
		SEGUIDOS.clear();
	}

	private static final class Seguido {
		private final ServerBossEvent barra;
		private long ultimoJugadorCerca;
		// En memoria: después de un reinicio el jefe espera una vuelta entera antes de la primera habilidad
		private long proximaHabilidad;

		private Seguido(ServerBossEvent barra, long ultimoJugadorCerca, long proximaHabilidad) {
			this.barra = barra;
			this.ultimoJugadorCerca = ultimoJugadorCerca;
			this.proximaHabilidad = proximaHabilidad;
		}
	}
}
