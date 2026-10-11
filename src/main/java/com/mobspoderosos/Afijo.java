package com.mobspoderosos;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** El rasgo de un campeón: se ve en el nombre ("Zombi [Nv 27] [Vampiro]") y viaja con el mob en las conversiones. */
public enum Afijo implements StringRepresentable {
	VELOZ("veloz", "Veloz", ChatFormatting.AQUA),
	VAMPIRO("vampiro", "Vampiro", ChatFormatting.DARK_RED),
	EXPLOSIVO("explosivo", "Explosivo", ChatFormatting.RED),
	INVOCADOR("invocador", "Invocador", ChatFormatting.LIGHT_PURPLE);

	public static final Codec<Afijo> CODEC = StringRepresentable.fromEnum(Afijo::values);
	public static final AttachmentType<Afijo> AFIJO = AttachmentRegistry.createPersistent(MobsPoderosos.id("afijo"), CODEC);
	// Persistente: si no, guardar y cargar el chunk le devolvería todas las invocaciones
	public static final AttachmentType<Integer> INVOCACIONES = AttachmentRegistry.createPersistent(MobsPoderosos.id("invocaciones"), Codec.INT);
	public static final AttachmentType<Long> ULTIMA_INVOCACION = AttachmentRegistry.create(MobsPoderosos.id("ultima_invocacion"));
	// Mobs que no pueden llamar copias de sí mismos: un vex sin evocador no desaparece nunca
	public static final TagKey<EntityType<?>> NO_INVOCAN = TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("no_invocan"));

	private static final Identifier MODIFICADOR = MobsPoderosos.id("afijo");

	private final String id;
	private final String nombre;
	private final ChatFormatting color;

	Afijo(String id, String nombre, ChatFormatting color) {
		this.id = id;
		this.nombre = nombre;
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	/** Entre corchetes y no como adjetivo: "Bruja Vampiro" no concuerda, "Bruja [Nv 4] [Vampiro]" sí. */
	public Component etiqueta() {
		return Component.literal(" [" + nombre + "]").withStyle(color);
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
		double chance = MobsPoderosos.config().afijos().chance();
		if (chance <= 0 || random.nextDouble() >= chance) {
			return null;
		}
		return alAzar(mob, random);
	}

	/** Dentro de aplicarEstadisticas: así una conversión, que copia el afijo, también rehace el modificador. */
	public static void aplicarModificadores(Mob mob) {
		AttributeInstance velocidad = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (velocidad == null) {
			return;
		}
		if (de(mob) == VELOZ) {
			velocidad.addOrReplacePermanentModifier(new AttributeModifier(MODIFICADOR, MobsPoderosos.config().afijos().velocidadVeloz(),
					AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		} else {
			velocidad.removeModifier(MODIFICADOR);
		}
	}

	/** AFTER_DAMAGE: el vampiro se cura con lo que pega y el invocador llama refuerzos cuando lo lastiman. */
	public static void alRecibirDanio(LivingEntity victima, DamageSource source, float danioBase, float danioRecibido, boolean bloqueado) {
		if (danioRecibido <= 0 || !(victima.level() instanceof ServerLevel level)) {
			return;
		}
		// getEntity y no getDirectEntity: en una flecha es el esqueleto que la tiró
		if (source.getEntity() instanceof Mob atacante && de(atacante) == VAMPIRO && atacante.isAlive()) {
			atacante.heal(danioRecibido * (float) MobsPoderosos.config().afijos().curacionVampiro());
			level.sendParticles(ParticleTypes.HEART, atacante.getX(), atacante.getEyeY() + 0.5, atacante.getZ(), 2, 0.3, 0.2, 0.3, 0);
		}
		if (victima instanceof Mob invocador && de(invocador) == INVOCADOR && invocador.isAlive()) {
			invocar(invocador, level, source.getEntity() instanceof LivingEntity enemigo ? enemigo : null);
		}
	}

	private static void invocar(Mob invocador, ServerLevel level, @Nullable LivingEntity enemigo) {
		Configuracion.Afijos config = MobsPoderosos.config().afijos();
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

	/** AFTER_DEATH: la explosión llega un rato después, con el sonido de la mecha, para que se pueda escapar. */
	public static void alMorir(LivingEntity muerto, DamageSource source) {
		if (!(muerto instanceof Mob mob) || de(mob) != EXPLOSIVO || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		Configuracion.Afijos config = MobsPoderosos.config().afijos();
		Vec3 pos = mob.position();
		level.playSound(null, mob.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0f, 0.5f);
		Eventos.programar(level, config.ticksDeMecha(), () -> {
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
			// NONE: un campeón explosivo que muere al lado de una base no la rompe
			level.explode(null, pos.x, pos.y, pos.z, config.potenciaExplosivo(), Level.ExplosionInteraction.NONE);
		});
	}
}
