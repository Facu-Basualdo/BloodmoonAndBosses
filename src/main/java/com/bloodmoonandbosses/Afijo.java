package com.bloodmoonandbosses;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** El rasgo de un campeón: se ve en el nombre ("Zombi [Nv 27] [Vampiro]") y viaja con el mob en las conversiones. */
public enum Afijo implements StringRepresentable {
	// Cada uno con su partícula, para reconocerlo de lejos o en una cueva, antes de leer el nombre
	VELOZ("veloz", ChatFormatting.AQUA, ParticleTypes.CLOUD),
	VAMPIRO("vampiro", ChatFormatting.DARK_RED, ParticleTypes.CRIMSON_SPORE),
	EXPLOSIVO("explosivo", ChatFormatting.RED, ParticleTypes.SMOKE),
	INVOCADOR("invocador", ChatFormatting.LIGHT_PURPLE, ParticleTypes.WITCH),
	BLINDADO("blindado", ChatFormatting.GRAY, ParticleTypes.ELECTRIC_SPARK),
	VENENOSO("venenoso", ChatFormatting.DARK_GREEN, ParticleTypes.ITEM_SLIME),
	CONGELANTE("congelante", ChatFormatting.BLUE, ParticleTypes.SNOWFLAKE),
	TELETRANSPORTADOR("teletransportador", ChatFormatting.DARK_AQUA, ParticleTypes.PORTAL);

	public static final Codec<Afijo> CODEC = StringRepresentable.fromEnum(Afijo::values);
	public static final AttachmentType<Afijo> AFIJO = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("afijo"), CODEC);
	// Persistente: si no, guardar y cargar el chunk le devolvería todas las invocaciones
	public static final AttachmentType<Integer> INVOCACIONES = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("invocaciones"), Codec.INT);
	public static final AttachmentType<Long> ULTIMA_INVOCACION = AttachmentRegistry.create(BloodmoonAndBosses.id("ultima_invocacion"));
	public static final AttachmentType<Long> ULTIMO_TELETRANSPORTE = AttachmentRegistry.create(BloodmoonAndBosses.id("ultimo_teletransporte"));
	// Mobs que no pueden llamar copias de sí mismos: un vex sin evocador no desaparece nunca
	public static final TagKey<EntityType<?>> NO_INVOCAN = TagKey.create(Registries.ENTITY_TYPE, BloodmoonAndBosses.id("no_invocan"));

	private static final Identifier MODIFICADOR = BloodmoonAndBosses.id("afijo");
	// Más lejos, vanilla no le manda las partículas al cliente igual (sendParticles sin forzar llega a 32 bloques)
	private static final double RADIO_DE_PARTICULAS = 32;

	private final String id;
	private final ChatFormatting color;
	private final SimpleParticleType particula;

	Afijo(String id, ChatFormatting color, SimpleParticleType particula) {
		this.id = id;
		this.color = color;
		this.particula = particula;
	}

	public SimpleParticleType particula() {
		return particula;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	/** Entre corchetes y no como adjetivo: "Bruja Vampiro" no concuerda, "Bruja [Nv 4] [Vampiro]" sí. */
	public Component etiqueta() {
		return etiqueta(BloodmoonAndBosses.config().idioma());
	}

	/** Con el respaldo en ese idioma: para reconocer el nombre de un mob que apareció con otra config. */
	public Component etiqueta(String idioma) {
		return Component.literal(" [").append(Idioma.guardado(idioma, "bloodmoonandbosses.afijo." + id)).append("]").withStyle(color);
	}

	/** Como era hasta la 0.13, texto fijo en español: para reconocer el nombre de los mobs que ya estaban. */
	public Component etiquetaVieja() {
		return Component.literal(" [" + Idioma.plantilla(Idioma.ESPANOL, "bloodmoonandbosses.afijo." + id) + "]").withStyle(color);
	}

	public static @Nullable Afijo de(Mob mob) {
		return mob.getAttached(AFIJO);
	}

	public static @Nullable Afijo porId(String id) {
		for (Afijo afijo : values()) {
			if (afijo.id.equals(id)) {
				return afijo;
			}
		}
		return null;
	}

	/** Los que tienen sentido para ese mob: a un creeper, que muere al atacar, no le sirve curarse. */
	public static List<Afijo> posibles(Mob mob) {
		List<Afijo> posibles = new ArrayList<>();
		for (Afijo afijo : values()) {
			if (afijo == VAMPIRO && mob instanceof Creeper) {
				continue;
			}
			if (afijo == INVOCADOR && mob.is(NO_INVOCAN)) {
				continue;
			}
			posibles.add(afijo);
		}
		return posibles;
	}

	public static Afijo alAzar(Mob mob, RandomSource random) {
		List<Afijo> posibles = posibles(mob);
		return posibles.get(random.nextInt(posibles.size()));
	}

	/** Para un campeón que aparece solo: la chance de la config decide si tiene afijo. */
	public static @Nullable Afijo paraCampeon(Mob mob, RandomSource random) {
		double chance = BloodmoonAndBosses.config().afijos().chance();
		if (chance <= 0 || random.nextDouble() >= chance) {
			return null;
		}
		return alAzar(mob, random);
	}

	/** Dentro de aplicarEstadisticas: así una conversión, que copia el afijo, también rehace el modificador. */
	public static void aplicarModificadores(Mob mob) {
		Configuracion.Afijos config = BloodmoonAndBosses.config().afijos();
		Afijo afijo = de(mob);
		modificar(mob, Attributes.MOVEMENT_SPEED, afijo == VELOZ ? config.velocidadVeloz() : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modificar(mob, Attributes.ARMOR, afijo == BLINDADO ? config.armaduraBlindado() : 0, AttributeModifier.Operation.ADD_VALUE);
		modificar(mob, Attributes.KNOCKBACK_RESISTANCE, afijo == BLINDADO ? config.empujeBlindado() : 0, AttributeModifier.Operation.ADD_VALUE);
		modificar(mob, Attributes.SCALE, afijo != null ? config.escala() : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	/** Con 0 lo saca: un mob que cambió de afijo (o lo perdió) no se queda con el modificador del anterior. */
	private static void modificar(Mob mob, Holder<Attribute> atributo, double valor, AttributeModifier.Operation operacion) {
		AttributeInstance instancia = mob.getAttribute(atributo);
		if (instancia == null) {
			return;
		}
		if (valor == 0) {
			instancia.removeModifier(MODIFICADOR);
		} else {
			instancia.addOrReplacePermanentModifier(new AttributeModifier(MODIFICADOR, valor, operacion));
		}
	}

	/**
	 * AFTER_DAMAGE: lo que hace el que pega (el vampiro se cura, el venenoso envenena, el congelante frena) y lo que hace
	 * el que recibe (el invocador llama refuerzos, el teletransportador aparece al lado del que le pegó).
	 */
	public static void alRecibirDanio(LivingEntity victima, DamageSource source, float danioBase, float danioRecibido, boolean bloqueado) {
		if (danioRecibido <= 0 || !(victima.level() instanceof ServerLevel level)) {
			return;
		}
		Configuracion.Afijos config = BloodmoonAndBosses.config().afijos();
		// getEntity y no getDirectEntity: en una flecha es el esqueleto que la tiró, y en una explosión el creeper
		if (source.getEntity() instanceof Mob atacante && atacante != victima) {
			Afijo afijo = de(atacante);
			if (afijo == VAMPIRO && atacante.isAlive()) {
				atacante.heal(danioRecibido * (float) config.curacionVampiro());
				level.sendParticles(ParticleTypes.HEART, atacante.getX(), atacante.getEyeY() + 0.5, atacante.getZ(), 2, 0.3, 0.2, 0.3, 0);
			} else if (afijo == VENENOSO) {
				victima.addEffect(new MobEffectInstance(MobEffects.POISON, config.segundosVeneno() * 20, config.nivelVeneno() - 1), atacante);
			} else if (afijo == CONGELANTE) {
				victima.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.segundosCongelante() * 20, config.nivelCongelante() - 1), atacante);
				// La escarcha de la nieve polvo, sólo para que se vea: fuera de la nieve baja 2 por tick, y por debajo del
				// máximo de vanilla no lastima
				int escarcha = Math.min(victima.getTicksRequiredToFreeze() - 1, config.segundosCongelante() * 40);
				victima.setTicksFrozen(Math.max(victima.getTicksFrozen(), escarcha));
				level.sendParticles(ParticleTypes.SNOWFLAKE, victima.getX(), victima.getY(1), victima.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
			}
		}
		if (victima instanceof Mob afectado && afectado.isAlive()) {
			Afijo afijo = de(afectado);
			LivingEntity enemigo = source.getEntity() instanceof LivingEntity viva && viva != afectado ? viva : null;
			if (afijo == INVOCADOR) {
				invocar(afectado, level, enemigo);
			} else if (afijo == TELETRANSPORTADOR && enemigo != null) {
				teletransportar(afectado, level, enemigo);
			}
		}
	}

	/** Como un enderman, pero hacia el que le pegó: aparece a su lado, así que alejarse o tirarle flechas no alcanza. */
	private static void teletransportar(Mob mob, ServerLevel level, LivingEntity enemigo) {
		Configuracion.Afijos config = BloodmoonAndBosses.config().afijos();
		long ahora = level.getGameTime();
		Long ultimo = mob.getAttached(ULTIMO_TELETRANSPORTE);
		RandomSource random = mob.getRandom();
		if ((ultimo != null && ahora - ultimo < config.segundosEntreTeletransportes() * 20L) || random.nextDouble() >= config.chanceTeletransporte()) {
			return;
		}
		if (enemigo.level() != level) {
			return;
		}
		// Con su tamaño: un jefe teletransportador no puede caer en un túnel donde no entra
		BlockPos destino = Eventos.lugarCerca(level, enemigo.blockPosition(), 2, 4, null, mob.getType(), mob.getScale(), false, random);
		if (destino == null) {
			return;
		}
		mob.setAttached(ULTIMO_TELETRANSPORTE, ahora);
		level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY(0.5), mob.getZ(), 24, 0.3, 0.6, 0.3, 0.3);
		mob.teleportTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5);
		mob.setTarget(enemigo);
		level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY(0.5), mob.getZ(), 24, 0.3, 0.6, 0.3, 0.3);
		level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
	}

	private static void invocar(Mob invocador, ServerLevel level, @Nullable LivingEntity enemigo) {
		Configuracion.Afijos config = BloodmoonAndBosses.config().afijos();
		int hechas = invocador.getAttachedOrElse(INVOCACIONES, 0);
		long ahora = level.getGameTime();
		Long ultima = invocador.getAttached(ULTIMA_INVOCACION);
		if (hechas >= config.invocacionesMaximas() || (ultima != null && ahora - ultima < config.segundosEntreInvocaciones() * 20L)) {
			return;
		}
		invocador.setAttached(INVOCACIONES, hechas + 1);
		invocador.setAttached(ULTIMA_INVOCACION, ahora);

		RandomSource random = invocador.getRandom();
		for (int i = 0; i < config.refuerzosInvocador(); i++) {
			BlockPos pos = Eventos.lugarCerca(level, invocador.blockPosition(), 1, 3, null, invocador.getType(), false, random);
			if (pos == null) {
				continue;
			}
			// Con el nivel de la zona y no el del invocador: si no, cada campeón sería un criadero de campeones
			Mob refuerzo = NivelDeMob.invocarConNivel(invocador.getType(), level, pos, EntitySpawnReason.MOB_SUMMONED,
					NivelDeZona.calcular(level, pos, random), false);
			if (refuerzo != null && enemigo != null) {
				refuerzo.setTarget(enemigo);
			}
		}
		level.playSound(null, invocador.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0f, 1.0f);
	}

	/**
	 * Los campeones a los que les tocan partículas: vivos, visibles y a menos de RADIO_DE_PARTICULAS de algún jugador.
	 * Se buscan alrededor de los jugadores y no en todo el mundo: lejos de todos nadie las vería, y recorrer cada mob
	 * cargado cada medio segundo no vale la pena. Un Set, porque dos jugadores juntos verían al mismo mob dos veces.
	 */
	public static Set<Mob> conParticulas(MinecraftServer servidor) {
		Set<Mob> mobs = new LinkedHashSet<>();
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			mobs.addAll(jugador.level().getEntitiesOfClass(Mob.class, jugador.getBoundingBox().inflate(RADIO_DE_PARTICULAS),
					mob -> mob.isAlive() && !mob.isInvisible() && de(mob) != null));
		}
		return mobs;
	}

	/** Cada ticksEntreParticulas, desde el reloj de Eventos. Son partículas de vanilla: las ve cualquier cliente. */
	public static void mostrarParticulas(MinecraftServer servidor) {
		if (!particulasActivas()) {
			return;
		}
		for (Mob mob : conParticulas(servidor)) {
			Afijo afijo = de(mob);
			if (afijo == null || !(mob.level() instanceof ServerLevel level)) {
				continue;
			}
			double ancho = mob.getBbWidth() * 0.4;
			if (afijo == VELOZ) {
				// A los pies, como el polvo de correr
				level.sendParticles(afijo.particula, mob.getX(), mob.getY() + 0.1, mob.getZ(), 2, ancho, 0.05, ancho, 0.01);
			} else if (afijo == EXPLOSIVO) {
				// Una mecha: humo y alguna chispa arriba de la cabeza
				level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY(1) + 0.2, mob.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
				level.sendParticles(ParticleTypes.SMALL_FLAME, mob.getX(), mob.getY(1) + 0.2, mob.getZ(), 1, 0.1, 0.05, 0.1, 0.005);
			} else {
				level.sendParticles(afijo.particula, mob.getX(), mob.getY(0.5), mob.getZ(), 2, ancho, mob.getBbHeight() * 0.3, ancho, 0.02);
			}
		}
	}

	private static boolean particulasActivas() {
		return BloodmoonAndBosses.config().afijos().particulas();
	}

	/** AFTER_DEATH: la explosión llega un rato después, con el sonido de la mecha, para que se pueda escapar. */
	public static void alMorir(LivingEntity muerto, DamageSource source) {
		if (!(muerto instanceof Mob mob) || de(mob) != EXPLOSIVO || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		Configuracion.Afijos config = BloodmoonAndBosses.config().afijos();
		Vec3 pos = mob.position();
		level.playSound(null, mob.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0f, 0.5f);
		Eventos.programar(level, config.ticksDeMecha(), () -> {
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
			// NONE: un campeón explosivo que muere al lado de una base no la rompe
			level.explode(null, pos.x, pos.y, pos.z, config.potenciaExplosivo(), Level.ExplosionInteraction.NONE);
		});
	}
}
