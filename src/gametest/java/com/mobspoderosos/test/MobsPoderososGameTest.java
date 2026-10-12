package com.mobspoderosos.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import com.mobspoderosos.Afijo;
import com.mobspoderosos.ArchivoDeConfiguracion;
import com.mobspoderosos.BotinPorNivel;
import com.mobspoderosos.Configuracion;
import com.mobspoderosos.EquipoPorNivel;
import com.mobspoderosos.Estampida;
import com.mobspoderosos.JefeDeZona;
import com.mobspoderosos.LunaDeSangre;
import com.mobspoderosos.MobsPoderosos;
import com.mobspoderosos.NivelDeMob;
import com.mobspoderosos.NivelDeZona;
import com.mobspoderosos.mixin.CreeperAccessor;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MobsPoderososGameTest {
	private static final BlockPos DENTRO = new BlockPos(2, 1, 2);
	// Para los eventos, que buscan lugar alrededor: en el medio de la estructura de 8 × 8
	private static final BlockPos CENTRO = new BlockPos(4, 1, 4);

	@GameTest
	public void nivelPorDistancia(GameTestHelper helper) {
		Configuracion fija = MobsPoderosos.config().conVariacion(0);
		RandomSource random = RandomSource.create(42);

		helper.assertValueEqual(1, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, BlockPos.ZERO, fija, random), "Nivel en el spawn");
		helper.assertValueEqual(16, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(1500, 70, 0), fija, random), "Nivel a 1500 bloques");
		helper.assertValueEqual(100, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(0, 70, 10000), fija, random), "Nivel a 10000 bloques (tope)");
		helper.assertValueEqual(16, NivelDeZona.calcular(Level.OVERWORLD, new BlockPos(1000, 64, 0), new BlockPos(2500, 64, 0), fija, random), "La distancia se mide desde el spawn, no desde (0, 0)");
		// 100 bloques del Nether son 800 del overworld: 1 + 8 + 20
		helper.assertValueEqual(29, NivelDeZona.calcular(Level.NETHER, BlockPos.ZERO, new BlockPos(100, 64, 0), fija, random), "El Nether escala ×8 y suma su extra");
		// En el End el spawn del overworld no cuenta: 1 + 10 + 40
		helper.assertValueEqual(51, NivelDeZona.calcular(Level.END, new BlockPos(5000, 64, 0), new BlockPos(1000, 64, 0), fija, random), "El End mide desde (0, 0)");

		Configuracion variable = MobsPoderosos.config().conVariacion(1);
		for (int i = 0; i < 50; i++) {
			int nivel = NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(1500, 70, 0), variable, random);
			helper.assertTrue(nivel >= 15 && nivel <= 17, Component.literal("Con variación el nivel queda en 16 ± 1 (dio " + nivel + ")"));
		}
		helper.succeed();
	}

	@GameTest
	public void nivelPorTiempoYCampeon(GameTestHelper helper) {
		Configuracion config = MobsPoderosos.config().conExtras(5, 20, 0.05, 10);
		helper.assertValueEqual(0, NivelDeZona.porTiempo(4, config), "Antes de los 5 días no suma");
		helper.assertValueEqual(1, NivelDeZona.porTiempo(5, config), "A los 5 días, +1");
		helper.assertValueEqual(19, NivelDeZona.porTiempo(99, config), "A los 99 días, +19");
		helper.assertValueEqual(20, NivelDeZona.porTiempo(100, config), "A los 100 días, +20");
		helper.assertValueEqual(20, NivelDeZona.porTiempo(10_000, config), "Topea en nivelesPorTiempoMaximos");
		helper.assertValueEqual(0, NivelDeZona.porTiempo(10_000, config.conExtras(0, 20, 0.05, 10)), "Con diasPorNivelExtra en 0 no suma nunca");

		// 2000 tiradas al 5 %: en promedio 100, y salir de 50–150 está a más de 5 desvíos
		RandomSource random = RandomSource.create(42);
		int campeones = 0;
		for (int i = 0; i < 2000; i++) {
			if (NivelDeZona.esCampeon(config, random)) {
				campeones++;
			}
		}
		helper.assertTrue(campeones > 50 && campeones < 150, Component.literal("Uno de cada veinte es campeón (salieron " + campeones + " de 2000)"));
		Configuracion sinCampeones = config.conExtras(5, 20, 0, 10);
		for (int i = 0; i < 200; i++) {
			helper.assertFalse(NivelDeZona.esCampeon(sinCampeones, random), Component.literal("Con chanceDeCampeon en 0 no hay campeones"));
		}
		helper.succeed();
	}

	@GameTest
	public void aplicarNivel(GameTestHelper helper) {
		Zombie zombie = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		helper.assertFalse(zombie.hasAttached(NivelDeMob.NIVEL), Component.literal("COMMAND no escala solo"));

		NivelDeMob.aplicar(zombie, 20);
		NivelDeMob.aplicar(zombie, 20);
		assertCerca(helper, 52, zombie.getMaxHealth(), "Vida máxima de un zombi de nivel 20 (20 × 2,6)");
		assertCerca(helper, 52, zombie.getHealth(), "Aparece con la vida llena");
		helper.assertValueEqual(1, zombie.getAttribute(Attributes.MAX_HEALTH).getModifiers().size(), "Aplicar dos veces no apila modificadores");
		helper.assertValueEqual(20, NivelDeMob.nivel(zombie), "Nivel guardado en el attachment");
		helper.assertTrue(zombie.getCustomName().getString().contains("[Nv 20]"), Component.literal("El nombre lleva el nivel: " + zombie.getCustomName().getString()));
		helper.assertFalse(zombie.isCustomNameVisible(), Component.literal("Por defecto el nombre sólo se ve al mirar el mob"));
		helper.assertTrue(NivelDeMob.esNombreDelMod(zombie), Component.literal("El nombre es el del mod"));
		helper.assertFalse(zombie.isPersistenceRequired(), Component.literal("El nombre puesto por el mod no evita el despawn"));

		NivelDeMob.aplicar(zombie, 100);
		assertCerca(helper, 180, zombie.getMaxHealth(), "La vida topea en ×9");
		AttributeModifier danio = zombie.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(MobsPoderosos.id("nivel"));
		assertCerca(helper, 4.0, danio.amount(), "El daño topea en +400 %");
		AttributeModifier armadura = zombie.getAttribute(Attributes.ARMOR).getModifier(MobsPoderosos.id("nivel"));
		assertCerca(helper, 12, armadura.amount(), "La armadura topea en +12");

		zombie.setCustomName(Component.literal("Pepe"));
		helper.assertFalse(NivelDeMob.esNombreDelMod(zombie), Component.literal("Con un name tag el nombre deja de ser del mod"));
		helper.succeed();
	}

	@GameTest
	public void vidaEnElNombre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie zombie = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(zombie, 20);
		assertTerminaEn(helper, zombie, "[Nv 20] ❤ 52/52");
		helper.assertTrue(NivelDeMob.esNombreDelMod(zombie), Component.literal("Con la vida, el nombre sigue siendo del mod"));

		zombie.setHealth(30.4f);
		assertTerminaEn(helper, zombie, "❤ 31/52");
		zombie.setHealth(0.3f);
		assertTerminaEn(helper, zombie, "❤ 1/52");

		Zombie golpeado = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(golpeado, 20);
		golpeado.hurtServer(level, level.damageSources().generic(), 10);
		helper.assertTrue(golpeado.getHealth() < 52, Component.literal("El golpe le sacó vida"));
		assertTerminaEn(helper, golpeado, "❤ " + (int) Math.ceil(golpeado.getHealth()) + "/52");

		// El nombre con la vida tiene que sobrevivir al NBT con la misma forma: si no, esNombreDelMod deja de reconocerlo
		Zombie recargado = recrear(level, zombie, helper.absolutePos(DENTRO));
		helper.assertTrue(NivelDeMob.esNombreDelMod(recargado), Component.literal("Después de guardar y cargar, el nombre sigue siendo del mod"));
		assertTerminaEn(helper, recargado, "❤ 1/52");

		// Un mob de antes de la 0.8.0 tiene el nombre sin la vida: se reconoce y la vida aparece al primer cambio
		recargado.setCustomName(NivelDeMob.nombre(EntityTypes.ZOMBIE, 20));
		helper.assertTrue(NivelDeMob.esNombreDelMod(recargado), Component.literal("El nombre sin la vida también es del mod"));
		recargado.setHealth(10);
		assertTerminaEn(helper, recargado, "[Nv 20] ❤ 10/52");

		recargado.setCustomName(Component.literal("Pepe"));
		recargado.setHealth(5);
		helper.assertValueEqual("Pepe", recargado.getCustomName().getString(), "La vida no pisa un name tag");
		helper.succeed();
	}

	@GameTest
	public void creeper(GameTestHelper helper) {
		Creeper debil = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		Creeper fuerte = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(debil, 59);
		NivelDeMob.aplicar(fuerte, 60);
		helper.assertValueEqual(3, ((CreeperAccessor) debil).mobspoderosos$getExplosionRadius(), "Radio de un creeper de nivel 59");
		helper.assertValueEqual(4, ((CreeperAccessor) fuerte).mobspoderosos$getExplosionRadius(), "Radio de un creeper de nivel 60");
		helper.succeed();
	}

	@GameTest
	public void motivosDeSpawn(GameTestHelper helper) {
		helper.assertTrue(spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.NATURAL).hasAttached(NivelDeMob.NIVEL), Component.literal("NATURAL escala"));
		helper.assertTrue(spawn(helper, EntityTypes.SKELETON, EntitySpawnReason.JOCKEY).hasAttached(NivelDeMob.NIVEL), Component.literal("JOCKEY escala (el jinete de la araña)"));
		helper.assertFalse(spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.SPAWNER).hasAttached(NivelDeMob.NIVEL), Component.literal("SPAWNER no escala"));
		helper.assertFalse(spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.SPAWN_ITEM_USE).hasAttached(NivelDeMob.NIVEL), Component.literal("Los huevos no escalan"));
		helper.assertFalse(spawn(helper, EntityTypes.ELDER_GUARDIAN, EntitySpawnReason.STRUCTURE).hasAttached(NivelDeMob.NIVEL), Component.literal("Los jefes (tag sin_nivel) no escalan"));

		Villager aldeano = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.NATURAL);
		helper.assertFalse(aldeano.hasAttached(NivelDeMob.NIVEL), Component.literal("Los mobs que no son hostiles no escalan"));
		helper.assertFalse(aldeano.hasAttached(NivelDeMob.PENDIENTE), Component.literal("Al cargar se borra PENDIENTE aunque no escale"));
		helper.succeed();
	}

	@GameTest
	public void mobDeEstructura(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(DENTRO);

		// Así pasa un mob de STRUCTURE por el ProtoChunk: finalizeSpawn, a NBT, y recreado desde el NBT
		Zombie enGeneracion = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.STRUCTURE);
		enGeneracion.snapTo(pos, 0, 0);
		enGeneracion.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		helper.assertTrue(enGeneracion.hasAttached(NivelDeMob.PENDIENTE), Component.literal("finalizeSpawn con STRUCTURE marca PENDIENTE"));

		Zombie cargado = recrear(level, enGeneracion, pos);
		helper.assertTrue(cargado.hasAttached(NivelDeMob.NIVEL), Component.literal("Al entrar al mundo desde el NBT recibe nivel"));
		helper.assertFalse(cargado.hasAttached(NivelDeMob.PENDIENTE), Component.literal("Y ya no queda pendiente"));

		// Al recargar un mob con nivel no se le vuelve a aplicar
		NivelDeMob.aplicar(cargado, 33);
		Zombie recargado = recrear(level, cargado, pos);
		helper.assertValueEqual(33, NivelDeMob.nivel(recargado), "El nivel sobrevive a guardar y cargar");
		helper.assertValueEqual(1, recargado.getAttribute(Attributes.MAX_HEALTH).getModifiers().size(), "Los modificadores no se duplican al recargar");
		assertCerca(helper, 20 * 3.64, recargado.getMaxHealth(), "La vida máxima de nivel 33 se conserva");
		helper.succeed();
	}

	@GameTest
	public void conversiones(GameTestHelper helper) {
		Zombie zombie = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(zombie, 7);
		Mob ahogado = zombie.convertTo(EntityTypes.DROWNED, ConversionParams.single(zombie, true, true), m -> {
		});
		helper.assertValueEqual(7, NivelDeMob.nivel(ahogado), "El ahogado conserva el nivel del zombi");
		helper.assertTrue(NivelDeMob.esNombreDelMod(ahogado), Component.literal("Y su nombre es el de ahogado: " + ahogado.getCustomName().getString()));
		assertCerca(helper, 31.2, ahogado.getMaxHealth(), "Con las estadísticas de nivel 7");

		Zombie bautizado = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(bautizado, 9);
		bautizado.setCustomName(Component.literal("Pepe"));
		Mob bautizadoAhogado = bautizado.convertTo(EntityTypes.DROWNED, ConversionParams.single(bautizado, true, true), m -> {
		});
		helper.assertValueEqual("Pepe", bautizadoAhogado.getCustomName().getString(), "No se pisa el name tag de un jugador");
		helper.assertValueEqual(9, NivelDeMob.nivel(bautizadoAhogado), "Pero el nivel sí pasa");

		Mob aldeanoZombi = spawn(helper, EntityTypes.ZOMBIE_VILLAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(aldeanoZombi, 12);
		Mob curado = aldeanoZombi.convertTo(EntityTypes.VILLAGER, ConversionParams.single(aldeanoZombi, true, true), m -> {
		});
		helper.assertFalse(curado.hasAttached(NivelDeMob.NIVEL), Component.literal("El aldeano curado no tiene nivel"));
		helper.assertTrue(curado.getCustomName() == null, Component.literal("Ni el nombre de aldeano zombi"));

		Slime slime = spawn(helper, EntityTypes.SLIME, EntitySpawnReason.COMMAND);
		slime.setSize(2, true);
		NivelDeMob.aplicar(slime, 5);
		slime.setHealth(0);
		slime.remove(Entity.RemovalReason.KILLED);
		List<Slime> hijos = helper.getLevel().getEntitiesOfClass(Slime.class, slime.getBoundingBox().inflate(3), s -> s != slime);
		helper.assertFalse(hijos.isEmpty(), Component.literal("El slime se dividió"));
		for (Slime hijo : hijos) {
			helper.assertValueEqual(5, NivelDeMob.nivel(hijo), "Los hijos del slime heredan el nivel");
		}
		helper.succeed();
	}

	@GameTest
	public void tiradasExtra(GameTestHelper helper) {
		helper.assertValueEqual(0, BotinPorNivel.tiradasExtra(15), "Tiradas extra en nivel 15");
		helper.assertValueEqual(1, BotinPorNivel.tiradasExtra(16), "Tiradas extra en nivel 16");
		helper.assertValueEqual(5, BotinPorNivel.tiradasExtra(95), "Tiradas extra en nivel 95");
		helper.assertValueEqual(6, BotinPorNivel.tiradasExtra(96), "Tiradas extra en nivel 96");
		helper.assertValueEqual(6, BotinPorNivel.tiradasExtra(200), "Las tiradas extra topean en 6");

		helper.assertValueEqual(1.0, chanceConNivel(helper, EntityTypes.ZOMBIE, 25), "Las tiradas extra de un zombi son seguras");
		helper.assertValueEqual(0.0, chanceConNivel(helper, EntityTypes.WITHER_SKELETON, 25), "El esqueleto del Wither (sin_tiradas_extra) no tiene");
		assertCerca(helper, 0.1, chanceConNivel(helper, EntityTypes.EVOKER, 20), "Chance de cada tirada extra de un evocador de nivel 20");
		assertCerca(helper, 0.5, chanceConNivel(helper, EntityTypes.EVOKER, 100), "La chance del evocador topea en 50 %");
		helper.succeed();
	}

	@GameTest
	public void botinAlMorir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "cazador");

		// El devastador siempre suelta una montura: cada tirada extra es una montura más
		Mob devastador = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(devastador, 48);
		helper.assertValueEqual(4, matarYContar(helper, devastador, level.damageSources().playerAttack(jugador), Items.SADDLE), "Un devastador de nivel 48 muerto por un jugador suelta 1 + 3 monturas");

		Mob lejano = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(lejano, 100);
		helper.assertValueEqual(7, matarYContar(helper, lejano, level.damageSources().playerAttack(jugador), Items.SADDLE), "Un devastador de nivel 100 suelta 1 + 6 monturas");

		Mob sinJugador = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(sinJugador, 30);
		helper.assertValueEqual(1, matarYContar(helper, sinJugador, level.damageSources().genericKill(), Items.SADDLE), "Sin jugador no hay tiradas extra");

		// 20 evocadores de nivel 100: 120 tiradas extra al 50 %. Que salgan todas o ninguna tiene probabilidad 2^-120
		int totems = 0;
		for (int i = 0; i < 20; i++) {
			Mob evocador = spawn(helper, EntityTypes.EVOKER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(evocador, 100);
			totems += matarYContar(helper, evocador, level.damageSources().playerAttack(jugador), Items.TOTEM_OF_UNDYING);
		}
		helper.assertTrue(totems > 20 && totems < 140, Component.literal("Los tótems extra del evocador salen por chance: 20 evocadores soltaron " + totems));

		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void experiencia(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// El creeper no tiene equipo: su experiencia base es siempre 5, sin el extra al azar por cada pieza
		Creeper comun = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		Creeper conNivel = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(conNivel, 20);
		helper.assertValueEqual(5, comun.getExperienceReward(level, null), "Experiencia de un creeper sin nivel");
		helper.assertValueEqual(10, conNivel.getExperienceReward(level, null), "Experiencia de un creeper de nivel 20 (5 × 2)");
		helper.succeed();
	}

	@GameTest
	public void materialDeArmadura(GameTestHelper helper) {
		RandomSource random = RandomSource.create(42);
		helper.assertValueEqual(Items.LEATHER_CHESTPLATE, EquipoPorNivel.material(19, random).pechera(), "Material en nivel 19");
		for (int i = 0; i < 20; i++) {
			Item pechera = EquipoPorNivel.material(30, random).pechera();
			helper.assertTrue(pechera == Items.CHAINMAIL_CHESTPLATE || pechera == Items.GOLDEN_CHESTPLATE, Component.literal("En nivel 30 es cadena u oro (dio " + pechera + ")"));
		}
		helper.assertValueEqual(Items.IRON_CHESTPLATE, EquipoPorNivel.material(40, random).pechera(), "Material en nivel 40");
		helper.assertValueEqual(Items.DIAMOND_CHESTPLATE, EquipoPorNivel.material(79, random).pechera(), "Material en nivel 79");
		helper.assertValueEqual(Items.NETHERITE_CHESTPLATE, EquipoPorNivel.material(80, random).pechera(), "Material en nivel 80");
		helper.assertValueEqual(Items.NETHERITE_CHESTPLATE, EquipoPorNivel.material(200, random).pechera(), "El material topea en netherita");
		helper.succeed();
	}

	@GameTest
	public void armaduraPorNivel(GameTestHelper helper) {
		// 20 mobs × 3 o 4 ranuras al 50 %: que no salga ninguna pieza tiene probabilidad 2^-60
		int piezasZombi = 0;
		int cascosPiglin = 0;
		for (int i = 0; i < 20; i++) {
			Zombie zombie = sinArmadura(spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND));
			EquipoPorNivel.equipar(zombie, 100);
			helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), Component.literal("Los que se queman al sol no reciben casco"));
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack pieza = zombie.getItemBySlot(slot);
				if (!pieza.isEmpty()) {
					piezasZombi++;
					helper.assertTrue(pieza.is(EquipoPorNivel.EQUIPO_VALIOSO), Component.literal("En nivel 100 la armadura es de netherita: " + pieza));
					assertCerca(helper, 0.085, zombie.getDropChances().byEquipment(slot), "La netherita se suelta con la chance de vanilla");
				}
			}
			zombie.discard();

			Mob piglin = sinArmadura(spawn(helper, EntityTypes.PIGLIN, EntitySpawnReason.COMMAND));
			EquipoPorNivel.equipar(piglin, 100);
			if (!piglin.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
				cascosPiglin++;
			}
			piglin.discard();
		}
		helper.assertTrue(piezasZombi > 0, Component.literal("Los zombis de nivel 100 reciben armadura"));
		helper.assertTrue(cascosPiglin > 0, Component.literal("Los que no se queman sí pueden recibir casco"));

		Mob vindicador = sinArmadura(spawn(helper, EntityTypes.VINDICATOR, EntitySpawnReason.COMMAND));
		for (int i = 0; i < 10; i++) {
			EquipoPorNivel.equipar(vindicador, 100);
		}
		helper.assertTrue(vindicador.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), Component.literal("Fuera del tag usan_equipo no hay armadura (no se dibujaría)"));
		helper.succeed();
	}

	@GameTest
	public void encantamientosYChanceDeSoltar(GameTestHelper helper) {
		Zombie bajo = conArma(helper, Items.IRON_SWORD);
		EquipoPorNivel.equipar(bajo, 1);
		helper.assertFalse(bajo.getMainHandItem().isEnchanted(), Component.literal("En nivel 1 el poder es 0: no se encanta"));

		Zombie alto = conArma(helper, Items.IRON_SWORD);
		EquipoPorNivel.equipar(alto, 40);
		helper.assertTrue(alto.getMainHandItem().isEnchanted(), Component.literal("En nivel 40 el arma sale encantada"));
		assertCerca(helper, 0.085 + 0.08, alto.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "Chance de soltar el arma en nivel 40");

		Zombie tope = conArma(helper, Items.IRON_SWORD);
		EquipoPorNivel.equipar(tope, 100);
		assertCerca(helper, 0.25, tope.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "La chance de soltar topea en 0,25");

		Zombie diamante = conArma(helper, Items.DIAMOND_SWORD);
		EquipoPorNivel.equipar(diamante, 50);
		assertCerca(helper, 0.085, diamante.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "El equipo valioso se suelta con la chance de vanilla");

		Zombie garantizado = conArma(helper, Items.IRON_SWORD);
		garantizado.setGuaranteedDrop(EquipmentSlot.MAINHAND);
		float antes = garantizado.getDropChances().byEquipment(EquipmentSlot.MAINHAND);
		EquipoPorNivel.equipar(garantizado, 50);
		assertCerca(helper, antes, garantizado.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "Lo que vanilla suelta siempre no baja");

		Zombie yaEncantado = conArma(helper, Items.IRON_SWORD);
		Holder<Enchantment> filo = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		yaEncantado.getMainHandItem().enchant(filo, 1);
		EquipoPorNivel.equipar(yaEncantado, 50);
		helper.assertValueEqual(1, yaEncantado.getMainHandItem().getEnchantments().size(), "Lo que encantó vanilla no se vuelve a encantar");

		Mob vex = spawn(helper, EntityTypes.VEX, EntitySpawnReason.COMMAND);
		EquipoPorNivel.equipar(vex, 50);
		helper.assertValueEqual(0.0f, vex.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "Lo que vanilla no suelta nunca (la espada del vex) sigue sin soltarse");

		// Por el camino real (ENTITY_LOAD): el esqueleto siempre lleva arco. Los tests corren al lado del spawn, así que el nivel es bajo
		Mob esqueleto = spawn(helper, EntityTypes.SKELETON, EntitySpawnReason.NATURAL);
		int nivel = NivelDeMob.nivel(esqueleto);
		helper.assertTrue(nivel >= 1, Component.literal("El esqueleto aparece con nivel"));
		assertCerca(helper, Math.min(0.085 + 0.002 * nivel, 0.25), esqueleto.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "Al aparecer también se le ajusta el equipo");
		helper.succeed();
	}

	@GameTest
	public void botinRaro(GameTestHelper helper) {
		helper.assertValueEqual(List.of(), BotinPorNivel.botinesRaros(0), "Sin nivel no hay botín raro");
		helper.assertValueEqual(List.of(), BotinPorNivel.botinesRaros(19), "Botín raro en nivel 19");
		helper.assertValueEqual(List.of(BotinPorNivel.Botin.RARO), BotinPorNivel.botinesRaros(20), "Botín raro en nivel 20");
		helper.assertValueEqual(List.of(BotinPorNivel.Botin.RARO, BotinPorNivel.Botin.EPICO), BotinPorNivel.botinesRaros(50), "Botín raro en nivel 50");
		helper.assertValueEqual(List.of(BotinPorNivel.Botin.values()), BotinPorNivel.botinesRaros(80), "Botín raro en nivel 80");

		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "saqueador");
		DamageSource golpe = level.damageSources().playerAttack(jugador);

		// Una tabla con un error de formato no carga y queda vacía sin que nada falle: hay que tirarla para saberlo
		for (BotinPorNivel.Botin botin : BotinPorNivel.Botin.values()) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(botin.tabla()) != LootTable.EMPTY, Component.literal("La tabla " + botin.tabla().identifier() + " cargó"));
			for (int i = 0; i < 10; i++) {
				Mob mob = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
				mob.dropFromLootTable(level, golpe, true, botin.tabla());
				helper.assertTrue(contarSoltadosMenos(helper, mob, Items.SADDLE) > 0, Component.literal("La tabla " + botin.tabla().identifier() + " siempre da algo"));
				mob.discard();
			}
		}

		// El devastador sólo suelta monturas: cualquier otra cosa es botín raro
		for (int i = 0; i < 40; i++) {
			Mob bajo = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(bajo, 19);
			bajo.hurtServer(level, golpe, 100_000);
			helper.assertValueEqual(0, contarSoltadosMenos(helper, bajo, Items.SADDLE), "Antes del nivel 20 no hay botín raro");
		}
		// 40 devastadores de nivel 100 con 15 %, 10 % y 5 %: que no salga nada tiene probabilidad ~3 en un millón
		int raros = 0;
		for (int i = 0; i < 40; i++) {
			Mob alto = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(alto, 100);
			alto.hurtServer(level, golpe, 100_000);
			raros += contarSoltadosMenos(helper, alto, Items.SADDLE);
		}
		helper.assertTrue(raros > 0, Component.literal("Los mobs de nivel alto sueltan botín raro"));

		// Sin jugador no hay botín raro, igual que no hay tiradas extra
		for (int i = 0; i < 40; i++) {
			Mob sinJugador = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(sinJugador, 100);
			sinJugador.hurtServer(level, level.damageSources().genericKill(), 100_000);
			helper.assertValueEqual(0, contarSoltadosMenos(helper, sinJugador, Items.SADDLE), "Muerto sin jugador no suelta botín raro");
		}

		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void afijos(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie veloz = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(veloz, 20, Afijo.VELOZ);
		helper.assertTrue(veloz.getCustomName().getString().contains("[Nv 20] [Veloz]"), Component.literal("El afijo va en el nombre: " + veloz.getCustomName().getString()));
		helper.assertTrue(NivelDeMob.esNombreDelMod(veloz), Component.literal("Con el afijo, el nombre sigue siendo del mod"));
		AttributeModifier velocidad = veloz.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MobsPoderosos.id("afijo"));
		helper.assertTrue(velocidad != null, Component.literal("El veloz tiene su modificador de velocidad"));
		assertCerca(helper, 0.3, velocidad.amount(), "El veloz corre un 30 % más");

		Mob ahogado = veloz.convertTo(EntityTypes.DROWNED, ConversionParams.single(veloz, true, true), m -> {
		});
		helper.assertValueEqual(Afijo.VELOZ, Afijo.de(ahogado), "El afijo pasa en la conversión");
		helper.assertTrue(ahogado.getCustomName().getString().contains("[Veloz]"), Component.literal("Y sigue en el nombre: " + ahogado.getCustomName().getString()));
		helper.assertTrue(ahogado.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MobsPoderosos.id("afijo")) != null, Component.literal("Y el modificador también"));

		helper.assertFalse(Afijo.posibles(spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND)).contains(Afijo.VAMPIRO), Component.literal("Un creeper no puede ser vampiro"));
		helper.assertFalse(Afijo.posibles(spawn(helper, EntityTypes.VEX, EntitySpawnReason.COMMAND)).contains(Afijo.INVOCADOR), Component.literal("Un vex (tag no_invocan) no puede ser invocador"));
		helper.assertTrue(Afijo.paraCampeon(ahogado, RandomSource.create(42)) != null, Component.literal("Con chance 1, todo campeón tiene afijo"));

		Zombie vampiro = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(vampiro, 20, Afijo.VAMPIRO);
		vampiro.setHealth(10);
		Mob victima = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		victima.hurtServer(level, level.damageSources().mobAttack(vampiro), 8);
		helper.assertTrue(vampiro.getHealth() > 10, Component.literal("El vampiro se cura con lo que pega (tiene " + vampiro.getHealth() + ")"));

		Zombie comun = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(comun, 20);
		comun.setHealth(10);
		Mob otraVictima = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		otraVictima.hurtServer(level, level.damageSources().mobAttack(comun), 8);
		assertCerca(helper, 10, comun.getHealth(), "Sin el afijo no se cura");
		helper.succeed();
	}

	@GameTest
	public void afijosNuevos(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Configuracion.Afijos config = MobsPoderosos.config().afijos();

		Zombie blindado = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(blindado, 20, Afijo.BLINDADO);
		AttributeModifier armadura = blindado.getAttribute(Attributes.ARMOR).getModifier(MobsPoderosos.id("afijo"));
		helper.assertTrue(armadura != null, Component.literal("El blindado tiene armadura extra"));
		assertCerca(helper, config.armaduraBlindado(), armadura.amount(), "Armadura del blindado");
		assertCerca(helper, config.empujeBlindado(), blindado.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(MobsPoderosos.id("afijo")).amount(),
				"Resistencia al empuje del blindado");
		helper.assertTrue(blindado.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MobsPoderosos.id("afijo")) == null,
				Component.literal("El blindado no corre más: el modificador de velocidad es sólo del veloz"));

		Zombie venenoso = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(venenoso, 20, Afijo.VENENOSO);
		Mob envenenado = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		envenenado.hurtServer(level, level.damageSources().mobAttack(venenoso), 2);
		MobEffectInstance veneno = envenenado.getEffect(MobEffects.POISON);
		helper.assertTrue(veneno != null, Component.literal("El golpe del venenoso envenena"));
		helper.assertValueEqual(config.nivelVeneno() - 1, veneno.getAmplifier(), "Veneno del nivel de la config");

		Zombie congelante = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(congelante, 20, Afijo.CONGELANTE);
		Mob congelado = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		congelado.hurtServer(level, level.damageSources().mobAttack(congelante), 2);
		helper.assertTrue(congelado.hasEffect(MobEffects.SLOWNESS), Component.literal("El golpe del congelante da lentitud"));
		helper.assertTrue(congelado.getTicksFrozen() > 0, Component.literal("Y escarcha, para que se vea"));
		helper.assertTrue(congelado.getTicksFrozen() < congelado.getTicksRequiredToFreeze(), Component.literal("Pero no tanta como para lastimar por frío"));

		Mob sano = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		Zombie comun = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(comun, 20);
		sano.hurtServer(level, level.damageSources().mobAttack(comun), 2);
		helper.assertFalse(sano.hasEffect(MobEffects.POISON) || sano.hasEffect(MobEffects.SLOWNESS), Component.literal("Sin afijo, el golpe no da efectos"));

		for (Afijo afijo : Afijo.values()) {
			helper.assertValueEqual(afijo, Afijo.porId(afijo.getSerializedName()), "El id de " + afijo + " se lee de vuelta");
		}
		helper.succeed();
	}

	@GameTest
	public void afijoTeletransportador(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ponerPiso(helper);
		Zombie teletransportador = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(teletransportador, 20, Afijo.TELETRANSPORTADOR);
		Mob atacante = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		atacante.snapTo(helper.absolutePos(CENTRO), 0, 0);

		// Se llama directo a lo que corre en AFTER_DAMAGE: con hurtServer, después del primer golpe el mob queda
		// invulnerable unos ticks y los demás no cuentan, así que el test tendría una sola chance de 0,5.
		// Que no salte en 20 intentos es 1 en un millón
		DamageSource golpe = level.damageSources().mobAttack(atacante);
		for (int i = 0; i < 20 && !teletransportador.hasAttached(Afijo.ULTIMO_TELETRANSPORTE); i++) {
			Afijo.alRecibirDanio(teletransportador, golpe, 1, 1, false);
		}
		helper.assertTrue(teletransportador.hasAttached(Afijo.ULTIMO_TELETRANSPORTE), Component.literal("Al lastimarlo se teletransporta"));
		double distancia = Math.sqrt(teletransportador.distanceToSqr(atacante));
		helper.assertTrue(distancia <= 5.5, Component.literal("Aparece al lado del que le pegó (a " + distancia + " bloques)"));

		BlockPos donde = teletransportador.blockPosition();
		for (int i = 0; i < 20; i++) {
			Afijo.alRecibirDanio(teletransportador, golpe, 1, 1, false);
		}
		helper.assertValueEqual(donde, teletransportador.blockPosition(), "Durante la espera no vuelve a saltar");
		helper.succeed();
	}

	/** Vanilla no llama a setHealth cuando la vida máxima sube: lo cubre el mixin en onAttributeUpdated, que corre en el tick del mob. */
	@GameTest
	public void vidaMaximaQueSubeEnElNombre(GameTestHelper helper) {
		Zombie zombie = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(zombie, 20);
		helper.runAfterDelay(1, () -> {
			// Sólo el máximo: mientras espera, el zombi se puede quemar al sol y perder vida
			assertTerminaEn(helper, zombie, "/52");
			zombie.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
					new AttributeModifier(MobsPoderosos.id("prueba"), 10, AttributeModifier.Operation.ADD_VALUE));
			helper.runAfterDelay(2, () -> {
				// 78 y no 62: el ADD_MULTIPLIED_BASE del nivel (×2,6) multiplica también los 10 que se suman
				assertTerminaEn(helper, zombie, "/78");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void furiaDelJefe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ponerPiso(helper);
		ServerPlayer jugador = joinSurvivalPlayer(helper, "provocador");
		BlockPos centro = helper.absolutePos(CENTRO);
		jugador.snapTo(centro.getX() + 0.5, centro.getY(), centro.getZ() + 0.5, 0, 0);
		Configuracion.JefeDeZona config = MobsPoderosos.config().jefeDeZona();

		Zombie jefe = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(jefe, 30);
		JefeDeZona.convertir(jefe, config);
		jefe.setTarget(jugador);
		double velocidad = jefe.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double danio = jefe.getAttributeValue(Attributes.ATTACK_DAMAGE);
		AABB alrededor = jefe.getBoundingBox().inflate(6);
		int antes = level.getEntitiesOfClass(Zombie.class, alrededor, z -> z != jefe).size();

		JefeDeZona.actualizarTodos();
		helper.assertFalse(JefeDeZona.estaFurioso(jefe), Component.literal("Con la vida llena no está furioso"));
		helper.assertValueEqual(BossEvent.BossBarColor.PURPLE, JefeDeZona.barra(jefe).getColor(), "La barra arranca violeta");

		jefe.setHealth(jefe.getMaxHealth() * (float) config.vidaFuria() - 1);
		JefeDeZona.actualizarTodos();
		helper.assertTrue(JefeDeZona.estaFurioso(jefe), Component.literal("Por debajo de vidaFuria se enfurece"));
		assertCerca(helper, velocidad * (1 + config.velocidadFuria()), jefe.getAttributeValue(Attributes.MOVEMENT_SPEED), "Corre más");
		assertCerca(helper, danio * (1 + config.danioFuria()), jefe.getAttributeValue(Attributes.ATTACK_DAMAGE), "Pega más");
		int escoltas = level.getEntitiesOfClass(Zombie.class, alrededor, z -> z != jefe).size() - antes;
		helper.assertValueEqual(config.escoltasFuria(), escoltas, "Llama otra tanda de escoltas");
		ServerBossEvent barra = JefeDeZona.barra(jefe);
		helper.assertValueEqual(BossEvent.BossBarColor.RED, barra.getColor(), "La barra se pone roja");
		helper.assertTrue(barra.getName().getString().contains("Furia"), Component.literal("Y lo dice: " + barra.getName().getString()));

		JefeDeZona.actualizarTodos();
		helper.assertValueEqual(escoltas, level.getEntitiesOfClass(Zombie.class, alrededor, z -> z != jefe).size() - antes, "Se enfurece una sola vez");

		jefe.discard();
		JefeDeZona.actualizarTodos();
		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void habilidadesDelJefe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Configuracion.JefeDeZona config = MobsPoderosos.config().jefeDeZona();
		Zombie jefe = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(jefe, 30);
		JefeDeZona.convertir(jefe, config);
		helper.assertTrue(JefeDeZona.elegirHabilidad(jefe, config, RandomSource.create(1)) == null, Component.literal("Sin nadie a tiro no hace nada"));

		ServerPlayer cerca = joinSurvivalPlayer(helper, "pegado");
		// Si no, el jugador de prueba es invulnerable: vanilla espera a que el cliente diga que cargó el mundo
		cerca.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		cerca.snapTo(jefe.getX() + 2, jefe.getY(), jefe.getZ(), 0, 0);
		helper.assertValueEqual(JefeDeZona.Habilidad.ONDA, JefeDeZona.elegirHabilidad(jefe, config, RandomSource.create(1)), "Con alguien pegado, la onda");
		float vida = cerca.getHealth();
		cerca.setDeltaMovement(Vec3.ZERO);
		JefeDeZona.ejecutar(JefeDeZona.Habilidad.ONDA, jefe, level, config);
		helper.assertTrue(cerca.getHealth() < vida, Component.literal("La onda lastima (" + vida + " → " + cerca.getHealth() + ")"));
		helper.assertTrue(cerca.getDeltaMovement().x > 0.5, Component.literal("Y empuja para afuera: " + cerca.getDeltaMovement()));
		helper.assertTrue(cerca.needsSync, Component.literal("Y se le avisa al cliente"));
		level.getServer().getPlayerList().remove(cerca);

		ServerPlayer lejos = joinSurvivalPlayer(helper, "francotirador");
		lejos.snapTo(jefe.getX() - 15, jefe.getY(), jefe.getZ(), 0, 0);
		helper.assertValueEqual(JefeDeZona.Habilidad.TIRON, JefeDeZona.elegirHabilidad(jefe, config, RandomSource.create(1)), "Con alguien lejos, el tirón");
		lejos.setDeltaMovement(Vec3.ZERO);
		JefeDeZona.ejecutar(JefeDeZona.Habilidad.TIRON, jefe, level, config);
		helper.assertTrue(lejos.getDeltaMovement().x > 0.5, Component.literal("El tirón lo trae hacia el jefe: " + lejos.getDeltaMovement()));
		helper.assertTrue(lejos.hasEffect(MobEffects.SLOWNESS), Component.literal("Y lo frena un momento"));

		lejos.snapTo(jefe.getX() - config.radioTiron() - 5, jefe.getY(), jefe.getZ(), 0, 0);
		helper.assertTrue(JefeDeZona.elegirHabilidad(jefe, config, RandomSource.create(1)) == null, Component.literal("Más allá de radioTiron no lo alcanza"));

		level.getServer().getPlayerList().remove(lejos);
		jefe.discard();
		JefeDeZona.actualizarTodos();
		helper.succeed();
	}

	@GameTest
	public void afijoInvocador(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ponerPiso(helper);
		Zombie invocador = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(invocador, 30, Afijo.INVOCADOR);
		invocador.hurtServer(level, level.damageSources().generic(), 1);
		List<Zombie> refuerzos = level.getEntitiesOfClass(Zombie.class, invocador.getBoundingBox().inflate(5), z -> z != invocador);
		helper.assertValueEqual(MobsPoderosos.config().afijos().refuerzosInvocador(), refuerzos.size(), "Al lastimarlo llama refuerzos");
		for (Zombie refuerzo : refuerzos) {
			helper.assertTrue(NivelDeMob.nivel(refuerzo) >= 1, Component.literal("Los refuerzos tienen nivel"));
			helper.assertFalse(refuerzo.hasAttached(NivelDeMob.PENDIENTE), Component.literal("Y no quedan pendientes"));
			helper.assertTrue(Afijo.de(refuerzo) == null, Component.literal("Los refuerzos no son campeones"));
		}
		invocador.hurtServer(level, level.damageSources().generic(), 1);
		helper.assertValueEqual(refuerzos.size(), level.getEntitiesOfClass(Zombie.class, invocador.getBoundingBox().inflate(5), z -> z != invocador).size(),
				"Durante la espera no vuelve a invocar");
		helper.assertValueEqual(1, invocador.getAttached(Afijo.INVOCACIONES), "Cuenta las invocaciones");
		helper.succeed();
	}

	// La mecha dura 30 ticks: con el máximo por defecto (20) el test vencería antes de la explosión
	@GameTest(maxTicks = 60)
	public void afijoExplosivo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie explosivo = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		NivelDeMob.aparecer(explosivo, 20, Afijo.EXPLOSIVO);
		Mob victima = spawn(helper, EntityTypes.VILLAGER, EntitySpawnReason.COMMAND);
		float vida = victima.getHealth();
		explosivo.hurtServer(level, level.damageSources().genericKill(), 100_000);
		assertCerca(helper, vida, victima.getHealth(), "Al morir no explota enseguida");
		helper.runAfterDelay(MobsPoderosos.config().afijos().ticksDeMecha() + 2, () -> {
			helper.assertTrue(victima.getHealth() < vida || victima.isDeadOrDying(), Component.literal("Un rato después explota y lastima a los que están cerca"));
			helper.succeed();
		});
	}

	@GameTest
	public void invocarConNivel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(DENTRO);
		Mob mob = NivelDeMob.invocarConNivel(EntityTypes.ZOMBIE, level, pos, EntitySpawnReason.EVENT, 37, false);
		helper.assertValueEqual(37, NivelDeMob.nivel(mob), "Aparece con el nivel dado, sin el de la zona");
		helper.assertFalse(mob.hasAttached(NivelDeMob.PENDIENTE), Component.literal("No queda pendiente aunque EVENT escale"));
		helper.assertTrue(Afijo.de(mob) == null, Component.literal("Sin afijo"));
		helper.assertTrue(level.getEntity(mob.getUUID()) == mob, Component.literal("Está en el mundo"));

		Mob conAfijo = NivelDeMob.invocarConNivel(EntityTypes.ZOMBIE, level, pos, EntitySpawnReason.EVENT, 500, true);
		helper.assertValueEqual(100, NivelDeMob.nivel(conAfijo), "El nivel se corta en el máximo");
		helper.assertTrue(Afijo.de(conAfijo) != null, Component.literal("Con afijo"));
		helper.assertTrue(NivelDeMob.invocarConNivel(EntityTypes.VILLAGER, level, pos, EntitySpawnReason.EVENT, 10, false) == null,
				Component.literal("Un aldeano no puede tener nivel"));
		helper.succeed();
	}

	// Al aire libre la estampida aparece sobre la superficie: con el techo de barreras del test caería arriba del techo
	@GameTest(skyAccess = true)
	public void estampida(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "corredor");
		ponerPiso(helper);
		BlockPos centro = helper.absolutePos(CENTRO);
		jugador.snapTo(centro.getX() + 0.5, centro.getY(), centro.getZ() + 0.5, 0, 0);
		// A 2–4 bloques para que entre en la estructura del test; nivelesPorMobExtra alto: siempre 4 mobs
		Configuracion.Estampida config = new Configuracion.Estampida(true, 1, 0, 0, 4, 1000, 16, 2, 4, 0.25, 60);
		helper.assertValueEqual(4, Estampida.cuantos(50, config), "Cuántos mobs trae");
		helper.assertValueEqual(16, Estampida.cuantos(50, new Configuracion.Estampida(true, 1, 0, 0, 6, 1, 16, 2, 4, 0.25, 60)), "Topea en mobsMaximos");

		String error = Estampida.empezar(jugador, config);
		helper.assertTrue(error == null, Component.literal("La estampida empieza: " + error));
		helper.assertTrue(Estampida.activa(jugador), Component.literal("Queda activa"));
		helper.assertTrue(Estampida.empezar(jugador, config) != null, Component.literal("No puede haber dos contra el mismo jugador"));
		List<Mob> mobs = Estampida.mobs(jugador);
		helper.assertTrue(mobs.size() >= 2, Component.literal("Aparecen al menos la mitad (" + mobs.size() + ")"));
		helper.assertTrue(Afijo.de(mobs.getFirst()) != null, Component.literal("El primero es el líder, con afijo"));
		for (Mob mob : mobs) {
			helper.assertTrue(mob.getTarget() == jugador, Component.literal("Todos van contra el jugador"));
			helper.assertTrue(NivelDeMob.nivel(mob) >= 1, Component.literal("Todos tienen nivel"));
			helper.assertTrue(mob.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MobsPoderosos.id("estampida")) != null, Component.literal("Y corren más"));
		}

		// remove(KILLED) en vez de esperar la animación de muerte: así nada pasa entre matar y premiar
		for (Mob mob : mobs) {
			mob.hurtServer(level, level.damageSources().playerAttack(jugador), 100_000);
			mob.remove(Entity.RemovalReason.KILLED);
		}
		jugador.getInventory().clearContent();
		Estampida.actualizarTodas();
		helper.assertFalse(Estampida.activa(jugador), Component.literal("Con todos muertos termina"));
		helper.assertFalse(jugador.getInventory().isEmpty(), Component.literal("Y el jugador recibe el premio en el inventario"));

		jugador.getInventory().clearContent();
		String otra = Estampida.empezar(jugador, config);
		helper.assertTrue(otra == null, Component.literal("Otra estampida: " + otra));
		for (Mob mob : Estampida.mobs(jugador)) {
			mob.discard();
		}
		Estampida.actualizarTodas();
		helper.assertFalse(Estampida.activa(jugador), Component.literal("Si desaparecen, termina"));
		helper.assertTrue(jugador.getInventory().isEmpty(), Component.literal("Pero sin premio"));

		// Como los zombis que se queman al amanecer: mueren, pero no los mató un jugador
		helper.assertTrue(Estampida.empezar(jugador, config) == null, Component.literal("Una tercera estampida"));
		for (Mob mob : Estampida.mobs(jugador)) {
			mob.hurtServer(level, level.damageSources().genericKill(), 100_000);
			mob.remove(Entity.RemovalReason.KILLED);
		}
		Estampida.actualizarTodas();
		helper.assertFalse(Estampida.activa(jugador), Component.literal("Si mueren solos, termina"));
		helper.assertTrue(jugador.getInventory().isEmpty(), Component.literal("Pero sin premio"));

		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void jefeDeZona(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "cazajefes");
		ponerPiso(helper);
		BlockPos centro = helper.absolutePos(CENTRO);
		jugador.snapTo(centro.getX() + 0.5, centro.getY(), centro.getZ() + 0.5, 0, 0);
		Configuracion.JefeDeZona config = MobsPoderosos.config().jefeDeZona();

		Mob jefe = JefeDeZona.aparecer(jugador, config, 2, 4);
		helper.assertTrue(jefe != null, Component.literal("Aparece el jefe"));
		helper.assertTrue(JefeDeZona.esJefe(jefe), Component.literal("Marcado como jefe"));
		helper.assertTrue(jefe.isPersistenceRequired(), Component.literal("No desaparece por estar lejos"));
		helper.assertTrue(Afijo.de(jefe) != null, Component.literal("Tiene afijo"));
		helper.assertTrue(jefe.getTarget() == jugador, Component.literal("Va contra el jugador"));
		assertCerca(helper, jefe.getMaxHealth(), jefe.getHealth(), "Aparece con la vida llena");

		Mob igual = NivelDeMob.invocarConNivel(jefe.getType(), level, centro, EntitySpawnReason.EVENT, NivelDeMob.nivel(jefe), false);
		assertCerca(helper, igual.getMaxHealth() * config.multiplicadorDeVida(), jefe.getMaxHealth(), "Tiene el triple de vida que uno común de su nivel");
		igual.discard();

		ServerBossEvent barra = JefeDeZona.barra(jefe);
		helper.assertTrue(barra != null, Component.literal("Tiene barra de jefe"));
		JefeDeZona.actualizarTodos();
		helper.assertTrue(barra.getPlayers().contains(jugador), Component.literal("La ve el jugador que está cerca"));
		jefe.discard();
		JefeDeZona.actualizarTodos();
		helper.assertTrue(JefeDeZona.barra(jefe) == null, Component.literal("Al irse deja de seguirse"));
		helper.assertFalse(barra.getPlayers().contains(jugador), Component.literal("Y la barra se saca"));

		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void botinDeJefe(GameTestHelper helper) {
		helper.assertValueEqual(BotinPorNivel.Botin.RARO, BotinPorNivel.mejorBotin(10, BotinPorNivel.Botin.RARO), "Debajo de todo, el mínimo");
		helper.assertValueEqual(BotinPorNivel.Botin.EPICO, BotinPorNivel.mejorBotin(60, BotinPorNivel.Botin.RARO), "En nivel 60, la épica");
		helper.assertValueEqual(BotinPorNivel.Botin.LEGENDARIO, BotinPorNivel.mejorBotin(90, BotinPorNivel.Botin.EPICO), "En nivel 90, la legendaria");
		helper.assertValueEqual(BotinPorNivel.Botin.EPICO, BotinPorNivel.mejorBotin(10, BotinPorNivel.Botin.EPICO), "Un jefe de nivel bajo igual tira la épica");

		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "verdugo");
		// El devastador sólo suelta monturas: cualquier otra cosa es del jefe. Nivel 19: sin botín raro común
		for (int i = 0; i < 5; i++) {
			Mob jefe = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(jefe, 19);
			JefeDeZona.convertir(jefe, MobsPoderosos.config().jefeDeZona());
			jefe.hurtServer(level, level.damageSources().playerAttack(jugador), 100_000);
			helper.assertTrue(contarSoltadosMenos(helper, jefe, Items.SADDLE) > 0, Component.literal("El jefe siempre suelta la tabla épica"));
		}
		level.getServer().getPlayerList().remove(jugador);
		helper.succeed();
	}

	@GameTest
	public void lunaDeSangre(GameTestHelper helper) {
		helper.assertFalse(NivelDeZona.esDeNoche(12_999), Component.literal("A las 12999 todavía es de día"));
		helper.assertTrue(NivelDeZona.esDeNoche(13_000), Component.literal("A las 13000 es de noche"));
		helper.assertTrue(NivelDeZona.esDeNoche(24_000 * 7 + 22_999), Component.literal("Cualquier día, a las 22999 es de noche"));
		helper.assertFalse(NivelDeZona.esDeNoche(23_000), Component.literal("A las 23000 ya amaneció"));

		ServerLevel level = helper.getLevel();
		ponerPiso(helper);
		helper.assertTrue(level.dimension() == Level.OVERWORLD, Component.literal("Los tests corren en el overworld"));
		helper.assertValueEqual(0, LunaDeSangre.nivelesExtra(level), "Sin luna de sangre no suma");

		// Todo en el mismo tick y se saca antes de volver: los otros tests no llegan a ver la luna
		level.setAttached(LunaDeSangre.NOCHE_ACTIVA, NivelDeZona.dias(level));
		helper.assertValueEqual(MobsPoderosos.config().lunaDeSangre().nivelesExtra(), LunaDeSangre.nivelesExtra(level), "Con luna de sangre suma");
		// 20 spawns NATURAL al 50 %: que no venga ningún compañero tiene probabilidad 2^-20
		for (int i = 0; i < 20; i++) {
			spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.NATURAL);
		}
		level.removeAttached(LunaDeSangre.NOCHE_ACTIVA);
		helper.assertValueEqual(0, LunaDeSangre.nivelesExtra(level), "Al terminar deja de sumar");

		helper.runAfterDelay(3, () -> {
			int zombis = level.getEntitiesOfClass(Zombie.class, new AABB(helper.absolutePos(DENTRO)).inflate(6), z -> true).size();
			helper.assertTrue(zombis > 20, Component.literal("Con luna de sangre los mobs NATURAL traen compañía (hay " + zombis + ")"));
			helper.succeed();
		});
	}

	@GameTest
	public void archivoDeConfiguracion(GameTestHelper helper) {
		try {
			Path carpeta = Files.createTempDirectory("mobspoderosos");

			Path nuevo = carpeta.resolve("nuevo.json");
			helper.assertValueEqual(Configuracion.POR_DEFECTO, ArchivoDeConfiguracion.leer(nuevo), "Sin archivo se usan los valores por defecto");
			helper.assertTrue(Files.readString(nuevo).contains("\"bloquesPorNivel\": 100"), Component.literal("Y se crea el archivo con ellos"));

			Path parcial = carpeta.resolve("parcial.json");
			Files.writeString(parcial, "{\"bloquesPorNivel\": 75, \"vida\": {\"tope\": 2.0}, \"inventada\": 1}");
			Configuracion leida = ArchivoDeConfiguracion.leer(parcial);
			helper.assertValueEqual(75, leida.bloquesPorNivel(), "Se lee lo que está en el archivo");
			assertCerca(helper, 2.0, leida.vida().tope(), "También dentro de un objeto");
			assertCerca(helper, 0.08, leida.vida().porNivel(), "Lo que falta dentro de un objeto queda por defecto");
			helper.assertValueEqual(100, leida.nivelMaximo(), "Lo que falta queda por defecto");
			String reescrito = Files.readString(parcial);
			helper.assertTrue(reescrito.contains("nivelMaximo") && !reescrito.contains("inventada"), Component.literal("Se reescribe completo y sin las claves que no existen"));

			Path invalido = carpeta.resolve("invalido.json");
			String contenido = "{\"bloquesPorNivel\": 0}";
			Files.writeString(invalido, contenido);
			helper.assertTrue(lanza(() -> ArchivoDeConfiguracion.leer(invalido)), Component.literal("bloquesPorNivel 0 se rechaza"));
			helper.assertValueEqual(contenido, Files.readString(invalido), "Un archivo rechazado no se pisa");

			String completo = Files.readString(nuevo);
			helper.assertTrue(completo.contains("\"estampida\"") && completo.contains("\"chancePorMinuto\""), Component.literal("Los eventos también van al archivo"));
			Path estampidaInvalida = carpeta.resolve("estampida.json");
			Files.writeString(estampidaInvalida, "{\"estampida\": {\"distanciaMaxima\": 1}}");
			helper.assertTrue(lanza(() -> ArchivoDeConfiguracion.leer(estampidaInvalida)), Component.literal("Una distanciaMaxima menor que la mínima se rechaza"));

			Path roto = carpeta.resolve("roto.json");
			Files.writeString(roto, "{\"bloquesPorNivel\": ");
			helper.assertTrue(lanza(() -> ArchivoDeConfiguracion.leer(roto)), Component.literal("Un JSON roto se rechaza"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		helper.succeed();
	}

	private static double chanceConNivel(GameTestHelper helper, EntityType<? extends Mob> tipo, int nivel) {
		Mob mob = spawn(helper, tipo, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(mob, nivel);
		double chance = BotinPorNivel.chanceTiradaExtra(mob);
		mob.discard();
		return chance;
	}

	/**
	 * El piso de la estructura de test es de barreras, y vanilla no deja aparecer mobs sobre una barrera
	 * (isSpawnPositionOk): sin esto, lugarCerca no encuentra lugar en ningún test.
	 */
	private static void ponerPiso(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static <T extends Mob> T sinArmadura(T mob) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			mob.setItemSlot(slot, ItemStack.EMPTY);
		}
		return mob;
	}

	/** Un zombi sin armadura y con el arma dada, con la chance de soltarla de vanilla. */
	private static Zombie conArma(GameTestHelper helper, Item arma) {
		Zombie zombie = sinArmadura(spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND));
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(arma));
		zombie.setDropChance(EquipmentSlot.MAINHAND, 0.085f);
		return zombie;
	}

	/** Cuenta lo que soltó el mob salvo el ítem dado, y borra todo para que no lo cuente el próximo. */
	private static int contarSoltadosMenos(GameTestHelper helper, Mob mob, Item ignorado) {
		int cantidad = 0;
		for (ItemEntity soltado : helper.getLevel().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(3), e -> true)) {
			if (!soltado.getItem().is(ignorado)) {
				cantidad += soltado.getItem().getCount();
			}
			soltado.discard();
		}
		return cantidad;
	}

	private static boolean lanza(Lectura lectura) {
		try {
			lectura.leer();
			return false;
		} catch (IOException | RuntimeException e) {
			return true;
		}
	}

	private interface Lectura {
		void leer() throws IOException;
	}

	private static int matarYContar(GameTestHelper helper, Mob mob, DamageSource fuente, Item item) {
		ServerLevel level = helper.getLevel();
		mob.hurtServer(level, fuente, 100_000);
		helper.assertTrue(mob.isDeadOrDying(), Component.literal(mob.getType().getDescription().getString() + " no murió"));
		int cantidad = 0;
		// Se borran para que no los cuente el próximo mob, que muere en el mismo lugar
		for (ItemEntity soltado : level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(3), e -> e.getItem().is(item))) {
			cantidad += soltado.getItem().getCount();
			soltado.discard();
		}
		return cantidad;
	}

	private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> tipo, EntitySpawnReason motivo) {
		return tipo.spawn(helper.getLevel(), helper.absolutePos(DENTRO), motivo);
	}

	private static Zombie recrear(ServerLevel level, Zombie original, BlockPos pos) {
		TagValueOutput salida = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		original.saveWithoutId(salida);
		CompoundTag datos = salida.buildResult();
		// Con el mismo UUID el mundo rechaza la copia
		datos.remove("UUID");
		original.discard();

		Zombie copia = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
		copia.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), datos));
		copia.snapTo(pos, 0, 0);
		level.addFreshEntity(copia);
		return copia;
	}

	/**
	 * Como GameTestHelper.makeMockServerPlayerInLevel(), pero sin su subclase anónima, que está deprecada
	 * y fuerza el modo creativo. Hay que sacarlo con playerList.remove al terminar.
	 */
	private static ServerPlayer joinSurvivalPlayer(GameTestHelper helper, String nombre) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), nombre), false);
		ServerPlayer jugador = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		Connection conexion = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(conexion);
		level.getServer().getPlayerList().placeNewPlayer(conexion, jugador, cookie);
		jugador.setGameMode(GameType.SURVIVAL);
		return jugador;
	}

	private static void assertTerminaEn(GameTestHelper helper, Mob mob, String esperado) {
		String nombre = mob.getCustomName().getString();
		helper.assertTrue(nombre.endsWith(esperado), Component.literal("El nombre tendría que terminar en \"" + esperado + "\" y es \"" + nombre + "\""));
	}

	private static void assertCerca(GameTestHelper helper, double esperado, double real, String que) {
		helper.assertTrue(Math.abs(esperado - real) < 0.01, Component.literal(que + ": se esperaba " + esperado + " y dio " + real));
	}
}
