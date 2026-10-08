package com.mobspoderosos;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import com.mobspoderosos.mixin.CreeperAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;

public final class NivelDeMob {
	public static final AttachmentType<Integer> NIVEL = AttachmentRegistry.createPersistent(MobsPoderosos.id("nivel"), Codec.INT);
	// Persistente y no un campo: los mobs de estructuras se guardan a NBT en el ProtoChunk y se recrean al completarse el chunk
	public static final AttachmentType<Boolean> PENDIENTE = AttachmentRegistry.createPersistent(MobsPoderosos.id("pendiente"), Codec.BOOL);
	public static final TagKey<EntityType<?>> SIN_NIVEL = TagKey.create(Registries.ENTITY_TYPE, MobsPoderosos.id("sin_nivel"));

	private static final Identifier MODIFICADOR = MobsPoderosos.id("nivel");
	private static final int RADIO_CREEPER_VANILLA = 3;

	private NivelDeMob() {
	}

	/** Los attachments persistentes tienen que estar registrados antes de que se cargue el primer chunk. */
	public static void inicializar() {
	}

	public static boolean escala(EntitySpawnReason motivo) {
		return switch (motivo) {
			case NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL, EVENT, REINFORCEMENT, JOCKEY, MOB_SUMMONED, TRIGGERED -> true;
			case SPAWNER, TRIAL_SPAWNER -> MobsPoderosos.config().escalarSpawners();
			default -> false;
		};
	}

	public static boolean puedeTenerNivel(Mob mob) {
		return mob instanceof Enemy && !mob.is(SIN_NIVEL);
	}

	public static int nivel(Entity entity) {
		return entity.getAttachedOrElse(NIVEL, 0);
	}

	/** ENTITY_LOAD: el mob ya tiene el equipo de vanilla, que cada subclase pone después de super.finalizeSpawn. */
	public static void alCargar(Entity entity, ServerLevel level) {
		if (!(entity instanceof Mob mob) || mob.removeAttached(PENDIENTE) == null) {
			return;
		}
		// Una conversión pudo haberle copiado el nivel antes de entrar al mundo
		if (mob.hasAttached(NIVEL) || !puedeTenerNivel(mob)) {
			return;
		}
		aplicar(mob, NivelDeZona.calcular(level, mob.blockPosition(), mob.getRandom()));
	}

	/** MOB_CONVERSION corre antes de addFreshEntity, así que le gana a alCargar. */
	public static void alConvertir(Mob viejo, Mob nuevo) {
		Integer nivel = viejo.getAttached(NIVEL);
		if (nivel == null) {
			return;
		}
		// ConversionType copia el nombre: si sigue siendo el nuestro, hay que rehacerlo con el tipo nuevo
		boolean nombreDelMod = esNombreDelMod(viejo);
		if (puedeTenerNivel(nuevo)) {
			aplicarEstadisticas(nuevo, nivel);
			if (nombreDelMod) {
				ponerNombre(nuevo, nivel);
			}
		} else if (nombreDelMod) {
			nuevo.setCustomName(null);
			nuevo.setCustomNameVisible(false);
		}
	}

	public static void aplicar(Mob mob, int nivel) {
		aplicarEstadisticas(mob, nivel);
		ponerNombre(mob, nivel);
	}

	/** Si un jugador le puso un name tag, el nombre ya no es nuestro: se loguea su muerte y no se toca en las conversiones. */
	public static boolean esNombreDelMod(Mob mob) {
		Integer nivel = mob.getAttached(NIVEL);
		Component nombre = mob.getCustomName();
		return nivel != null && nombre != null && nombre.equals(nombre(mob.getType(), nivel));
	}

	public static Component nombre(EntityType<?> tipo, int nivel) {
		return tipo.getDescription().copy().append(" [Nv " + nivel + "]").withStyle(color(nivel));
	}

	private static ChatFormatting color(int nivel) {
		if (nivel >= 40) {
			return ChatFormatting.GOLD;
		} else if (nivel >= 30) {
			return ChatFormatting.DARK_PURPLE;
		} else if (nivel >= 20) {
			return ChatFormatting.BLUE;
		} else if (nivel >= 10) {
			return ChatFormatting.GREEN;
		}
		return ChatFormatting.GRAY;
	}

	private static void aplicarEstadisticas(Mob mob, int nivel) {
		Configuracion config = MobsPoderosos.config();
		mob.setAttached(NIVEL, nivel);
		modificar(mob, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.vida().en(nivel));
		modificar(mob, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.danio().en(nivel));
		modificar(mob, Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, config.armadura().en(nivel));
		modificar(mob, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.velocidad().en(nivel));
		modificar(mob, Attributes.FOLLOW_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.rangoDeSeguimiento().en(nivel));
		modificar(mob, Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, config.resistenciaAlEmpuje().en(nivel));

		// La explosión del creeper no usa ATTACK_DAMAGE: sin esto sólo se vuelve más resistente
		if (mob instanceof Creeper creeper && nivel >= config.nivelRadioCreeper()) {
			((CreeperAccessor) creeper).mobspoderosos$setExplosionRadius(RADIO_CREEPER_VANILLA + config.radioCreeperExtra());
		}

		// Si no, aparece con la vida base aunque el máximo haya subido
		mob.setHealth(mob.getMaxHealth());
	}

	private static void ponerNombre(Mob mob, int nivel) {
		mob.setCustomName(nombre(mob.getType(), nivel));
		mob.setCustomNameVisible(MobsPoderosos.config().nombreSiempreVisible());
	}

	private static void modificar(Mob mob, Holder<Attribute> atributo, AttributeModifier.Operation operacion, double valor) {
		@Nullable AttributeInstance instancia = mob.getAttribute(atributo);
		if (instancia == null) {
			return;
		}
		if (valor == 0) {
			instancia.removeModifier(MODIFICADOR);
		} else {
			// Mismo Identifier siempre: aplicar dos veces reemplaza, no apila
			instancia.addOrReplacePermanentModifier(new AttributeModifier(MODIFICADOR, valor, operacion));
		}
	}
}
