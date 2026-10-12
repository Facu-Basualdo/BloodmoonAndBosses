package com.bloodmoonandbosses;

import java.util.List;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import com.bloodmoonandbosses.mixin.CreeperAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
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
	public static final AttachmentType<Integer> NIVEL = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("nivel"), Codec.INT);
	// Persistente y no un campo: los mobs de estructuras se guardan a NBT en el ProtoChunk y se recrean al completarse el chunk
	public static final AttachmentType<Boolean> PENDIENTE = AttachmentRegistry.createPersistent(BloodmoonAndBosses.id("pendiente"), Codec.BOOL);
	public static final TagKey<EntityType<?>> SIN_NIVEL = TagKey.create(Registries.ENTITY_TYPE, BloodmoonAndBosses.id("sin_nivel"));

	private static final Identifier MODIFICADOR = BloodmoonAndBosses.id("nivel");
	private static final int RADIO_CREEPER_VANILLA = 3;

	private NivelDeMob() {
	}

	/** Los attachments persistentes tienen que estar registrados antes de que se cargue el primer chunk. */
	public static void inicializar() {
		// Cargar las clases registra sus attachments
		List.of(Afijo.AFIJO, Afijo.INVOCACIONES, LunaDeSangre.NOCHE_ACTIVA, Estampida.ULTIMA, JefeDeZona.JEFE);
	}

	public static boolean escala(EntitySpawnReason motivo) {
		return switch (motivo) {
			case NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL, EVENT, REINFORCEMENT, JOCKEY, MOB_SUMMONED, TRIGGERED -> true;
			case SPAWNER, TRIAL_SPAWNER -> BloodmoonAndBosses.config().escalarSpawners();
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
		Configuracion config = BloodmoonAndBosses.config();
		int nivel = NivelDeZona.calcular(level, mob.blockPosition(), mob.getRandom());
		@Nullable Afijo afijo = null;
		if (NivelDeZona.esCampeon(config, mob.getRandom())) {
			nivel = Math.min(nivel + config.nivelesDeCampeon(), config.nivelMaximo());
			afijo = Afijo.paraCampeon(mob, mob.getRandom());
		}
		aparecer(mob, nivel, afijo);
	}

	/** Un mob nuevo: nivel y equipo. Las conversiones usan sólo aplicarEstadisticas, porque el equipo viene del mob viejo. */
	public static void aparecer(Mob mob, int nivel) {
		aparecer(mob, nivel, null);
	}

	public static void aparecer(Mob mob, int nivel, @Nullable Afijo afijo) {
		// Antes de aplicar: el nombre y el modificador de velocidad lo leen del attachment
		if (afijo != null) {
			mob.setAttached(Afijo.AFIJO, afijo);
		}
		aplicar(mob, nivel);
		EquipoPorNivel.equipar(mob, nivel);
	}

	/**
	 * Para los mobs que trae un evento: con el equipo de vanilla (finalizeSpawn) y el nivel dado, sin tirar campeón.
	 * El nivel se pone antes de addFreshEntity, así alCargar sólo le borra PENDIENTE, como en una conversión.
	 */
	public static @Nullable Mob invocarConNivel(EntityType<?> tipo, ServerLevel level, BlockPos pos, EntitySpawnReason motivo, int nivel, boolean conAfijo) {
		if (!(tipo.create(level, motivo) instanceof Mob mob)) {
			return null;
		}
		if (!puedeTenerNivel(mob)) {
			mob.discard();
			return null;
		}
		mob.snapTo(pos, mob.getRandom().nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), motivo, null);
		aparecer(mob, Math.clamp(nivel, 1, BloodmoonAndBosses.config().nivelMaximo()), conAfijo ? Afijo.alAzar(mob, mob.getRandom()) : null);
		level.addFreshEntityWithPassengers(mob);
		return mob;
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
			@Nullable Afijo afijo = Afijo.de(viejo);
			if (afijo != null) {
				nuevo.setAttached(Afijo.AFIJO, afijo);
			}
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
		if (nivel == null || nombre == null) {
			return false;
		}
		// Sin la vida: la que dice el nombre puede ser de antes de un cambio de vidaEnElNombre o de un mob de una versión anterior.
		// En cualquier idioma y con el formato de antes de la 0.14: si no, cambiar el idioma de la config (o actualizar el mod)
		// haría que todos los mobs que ya estaban parezcan bautizados por un jugador, y su nombre dejaría de seguir a la vida
		Component actual = sinVida(nombre);
		Afijo afijo = Afijo.de(mob);
		if (actual.equals(nombreViejo(mob.getType(), nivel, afijo))) {
			return true;
		}
		for (String idioma : Idioma.DISPONIBLES) {
			if (actual.equals(nombre(idioma, mob.getType(), nivel, afijo))) {
				return true;
			}
		}
		return false;
	}

	/** Lo llama cada setHealth: el nombre sigue a la vida, salvo que sea un name tag de un jugador. */
	public static void actualizarVida(Mob mob) {
		if (esNombreDelMod(mob)) {
			mob.setCustomName(nombre(mob, nivel(mob)));
		}
	}

	/** El nombre del mod sin la vida, para la barra de jefe y los mensajes; si tiene un name tag, ése. */
	public static Component nombreSinVida(Mob mob) {
		return esNombreDelMod(mob) ? nombre(mob.getType(), nivel(mob), Afijo.de(mob)) : mob.getName();
	}

	/** "Zombi [Nv 7]", sin la vida. */
	public static Component nombre(EntityType<?> tipo, int nivel) {
		return nombre(tipo, nivel, null);
	}

	/** "Zombi [Nv 7] [Vampiro]" para un campeón con afijo, sin la vida. */
	public static Component nombre(EntityType<?> tipo, int nivel, @Nullable Afijo afijo) {
		return nombre(BloodmoonAndBosses.config().idioma(), tipo, nivel, afijo);
	}

	/**
	 * Con el respaldo en ese idioma. Lo ven todos los que están cerca, así que va traducible: un cliente con el mod lo ve
	 * en su idioma y uno vanilla, en el de la config. El tipo ("Zombi") es de vanilla y lo traduce siempre el cliente.
	 */
	private static Component nombre(String idioma, EntityType<?> tipo, int nivel, @Nullable Afijo afijo) {
		Component nombre = tipo.getDescription().copy().append(" ").append(Idioma.guardado(idioma, "bloodmoonandbosses.nombre.nivel", nivel))
				.withStyle(color(nivel));
		return afijo == null ? nombre : nombre.copy().append(afijo.etiqueta(idioma));
	}

	/** Como era hasta la 0.13, texto fijo en español. */
	private static Component nombreViejo(EntityType<?> tipo, int nivel, @Nullable Afijo afijo) {
		Component nombre = tipo.getDescription().copy().append(" [Nv " + nivel + "]").withStyle(color(nivel));
		return afijo == null ? nombre : nombre.copy().append(afijo.etiquetaVieja());
	}

	/** "Zombi [Nv 7] ❤ 34/40" con vidaEnElNombre: un componente vacío con dos hijos, el del nivel y el de la vida. */
	public static Component nombre(Mob mob, int nivel) {
		Component base = nombre(mob.getType(), nivel, Afijo.de(mob));
		if (!BloodmoonAndBosses.config().vidaEnElNombre()) {
			return base;
		}
		// Para arriba: un mob con 0,3 de vida que dijera 0 parecería muerto
		Component vida = Component.literal(" ").append(Component.literal("❤").withStyle(ChatFormatting.RED))
				.append(" " + Mth.ceil(mob.getHealth()) + "/" + Mth.ceil(mob.getMaxHealth()));
		return Component.empty().append(base).append(vida);
	}

	private static Component sinVida(Component nombre) {
		List<Component> hijos = nombre.getSiblings();
		return nombre.getContents() == PlainTextContents.EMPTY && hijos.size() == 2 ? hijos.get(0) : nombre;
	}

	/** Por quintos del nivel máximo: si se cambia nivelMaximo, los colores se reparten igual. */
	private static ChatFormatting color(int nivel) {
		int quinto = nivel * 5 / BloodmoonAndBosses.config().nivelMaximo();
		if (quinto >= 4) {
			return ChatFormatting.GOLD;
		} else if (quinto == 3) {
			return ChatFormatting.DARK_PURPLE;
		} else if (quinto == 2) {
			return ChatFormatting.BLUE;
		} else if (quinto == 1) {
			return ChatFormatting.GREEN;
		}
		return ChatFormatting.GRAY;
	}

	private static void aplicarEstadisticas(Mob mob, int nivel) {
		Configuracion config = BloodmoonAndBosses.config();
		mob.setAttached(NIVEL, nivel);
		modificar(mob, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.vida().en(nivel));
		modificar(mob, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.danio().en(nivel));
		modificar(mob, Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, config.armadura().en(nivel));
		modificar(mob, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.velocidad().en(nivel));
		modificar(mob, Attributes.FOLLOW_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, config.rangoDeSeguimiento().en(nivel));
		modificar(mob, Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, config.resistenciaAlEmpuje().en(nivel));

		// La explosión del creeper no usa ATTACK_DAMAGE: sin esto sólo se vuelve más resistente
		if (mob instanceof Creeper creeper && nivel >= config.nivelRadioCreeper()) {
			((CreeperAccessor) creeper).bloodmoonandbosses$setExplosionRadius(RADIO_CREEPER_VANILLA + config.radioCreeperExtra());
		}

		Afijo.aplicarModificadores(mob);

		// Si no, aparece con la vida base aunque el máximo haya subido
		mob.setHealth(mob.getMaxHealth());
	}

	private static void ponerNombre(Mob mob, int nivel) {
		mob.setCustomName(nombre(mob, nivel));
		mob.setCustomNameVisible(BloodmoonAndBosses.config().nombreSiempreVisible());
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
