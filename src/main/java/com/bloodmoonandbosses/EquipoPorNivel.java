package com.bloodmoonandbosses;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class EquipoPorNivel {
	// Sólo los que dibujan la armadura: a un illager se la pondría invisible
	public static final TagKey<EntityType<?>> USAN_EQUIPO = TagKey.create(Registries.ENTITY_TYPE, BloodmoonAndBosses.id("usan_equipo"));
	// Con la chance de soltar subida, en nivel 40+ la netherita encantada saldría gratis
	public static final TagKey<Item> EQUIPO_VALIOSO = TagKey.create(Registries.ITEM, BloodmoonAndBosses.id("equipo_valioso"));

	private static final List<EquipmentSlot> ARMADURA = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<EquipmentSlot> CON_EQUIPO = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private static final Material CUERO = new Material(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
	private static final Material CADENA = new Material(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
	private static final Material ORO = new Material(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
	private static final Material HIERRO = new Material(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
	private static final Material DIAMANTE = new Material(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
	private static final Material NETHERITA = new Material(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
	// Un rango cada nivelesPorMaterialDeArmadura niveles; si hay varias opciones, se elige una por mob
	private static final List<List<Material>> POR_RANGO = List.of(
			List.of(CUERO),
			List.of(CADENA, ORO),
			List.of(HIERRO),
			List.of(DIAMANTE),
			List.of(NETHERITA));

	private EquipoPorNivel() {
	}

	/** Sólo al aparecer: en una conversión el mob nuevo ya trae el equipo del viejo. */
	public static void equipar(Mob mob, int nivel) {
		Configuracion config = BloodmoonAndBosses.config();
		RandomSource random = mob.getRandom();
		if (mob.is(USAN_EQUIPO)) {
			ponerArmadura(mob, nivel, config, random);
		}
		encantar(mob, nivel, config, random);
		subirChancesDeSoltar(mob, nivel, config);
	}

	public static Material material(int nivel, RandomSource random) {
		int rango = Math.min(nivel / BloodmoonAndBosses.config().nivelesPorMaterialDeArmadura(), POR_RANGO.size() - 1);
		List<Material> opciones = POR_RANGO.get(rango);
		return opciones.get(random.nextInt(opciones.size()));
	}

	private static void ponerArmadura(Mob mob, int nivel, Configuracion config, RandomSource random) {
		Material material = material(nivel, random);
		double chance = config.chanceArmaduraExtra().en(nivel);
		boolean sinCasco = mob.is(EntityTypeTags.BURN_IN_DAYLIGHT) && !config.cascoParaLosQueSeQueman();
		for (EquipmentSlot slot : ARMADURA) {
			// Lo que puso vanilla no se toca: tampoco el casco de un zombi
			if ((slot == EquipmentSlot.HEAD && sinCasco) || !mob.getItemBySlot(slot).isEmpty()) {
				continue;
			}
			if (random.nextDouble() < chance) {
				mob.setItemSlot(slot, new ItemStack(material.pieza(slot)));
			}
		}
	}

	private static void encantar(Mob mob, int nivel, Configuracion config, RandomSource random) {
		int poder = (int) config.poderDeEncantamiento().en(nivel);
		if (poder < 1) {
			return;
		}
		RegistryAccess registros = mob.registryAccess();
		// Los mismos que usa vanilla para el equipo de los mobs: sin Reparación ni maldiciones
		Optional<HolderSet.Named<Enchantment>> opciones = registros.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT);
		for (EquipmentSlot slot : CON_EQUIPO) {
			ItemStack item = mob.getItemBySlot(slot);
			// selectEnchantment no mira lo que el ítem ya tiene: sobre uno encantado por vanilla podría sumar uno incompatible
			if (item.isEmpty() || item.isEnchanted()) {
				continue;
			}
			EnchantmentHelper.enchantItem(random, item, poder, registros, opciones);
			mob.setItemSlot(slot, item);
		}
	}

	private static void subirChancesDeSoltar(Mob mob, int nivel, Configuracion config) {
		for (EquipmentSlot slot : CON_EQUIPO) {
			ItemStack item = mob.getItemBySlot(slot);
			if (item.isEmpty() || item.is(EQUIPO_VALIOSO)) {
				continue;
			}
			float actual = mob.getDropChances().byEquipment(slot);
			// En 0 vanilla no lo suelta nunca a propósito (la espada del vex): subirlo regalaría ese ítem
			if (actual <= 0) {
				continue;
			}
			float nueva = (float) config.chanceSoltarEquipo().sobre(actual, nivel);
			// Nunca baja: lo que vanilla marca para soltar siempre (lo que el mob levantó del piso) queda así
			if (nueva > actual) {
				mob.setDropChance(slot, nueva);
			}
		}
	}

	public record Material(Item casco, Item pechera, Item pantalones, Item botas) {
		public Item pieza(EquipmentSlot slot) {
			return switch (slot) {
				case HEAD -> casco;
				case CHEST -> pechera;
				case LEGS -> pantalones;
				case FEET -> botas;
				default -> throw new IllegalArgumentException("No es una ranura de armadura: " + slot);
			};
		}
	}
}
