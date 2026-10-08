package com.mobspoderosos.test;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import com.mobspoderosos.BotinPorNivel;
import com.mobspoderosos.Configuracion;
import com.mobspoderosos.MobsPoderosos;
import com.mobspoderosos.NivelDeMob;
import com.mobspoderosos.NivelDeZona;
import com.mobspoderosos.mixin.CreeperAccessor;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public class MobsPoderososGameTest {
	private static final BlockPos DENTRO = new BlockPos(2, 1, 2);

	@GameTest
	public void nivelPorDistancia(GameTestHelper helper) {
		Configuracion fija = MobsPoderosos.config().conVariacion(0);
		RandomSource random = RandomSource.create(42);

		helper.assertValueEqual(1, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, BlockPos.ZERO, fija, random), "Nivel en el spawn");
		helper.assertValueEqual(11, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(1500, 70, 0), fija, random), "Nivel a 1500 bloques");
		helper.assertValueEqual(50, NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(0, 70, 7500), fija, random), "Nivel a 7500 bloques (tope)");
		helper.assertValueEqual(11, NivelDeZona.calcular(Level.OVERWORLD, new BlockPos(1000, 64, 0), new BlockPos(2500, 64, 0), fija, random), "La distancia se mide desde el spawn, no desde (0, 0)");
		// 100 bloques del Nether son 800 del overworld: 1 + 5 + 10
		helper.assertValueEqual(16, NivelDeZona.calcular(Level.NETHER, BlockPos.ZERO, new BlockPos(100, 64, 0), fija, random), "El Nether escala ×8 y suma su extra");
		// En el End el spawn del overworld no cuenta: 1 + 6 + 20
		helper.assertValueEqual(27, NivelDeZona.calcular(Level.END, new BlockPos(5000, 64, 0), new BlockPos(1000, 64, 0), fija, random), "El End mide desde (0, 0)");

		Configuracion variable = MobsPoderosos.config().conVariacion(1);
		for (int i = 0; i < 50; i++) {
			int nivel = NivelDeZona.calcular(Level.OVERWORLD, BlockPos.ZERO, new BlockPos(1500, 70, 0), variable, random);
			helper.assertTrue(nivel >= 10 && nivel <= 12, Component.literal("Con variación el nivel queda en 11 ± 1 (dio " + nivel + ")"));
		}
		helper.succeed();
	}

	@GameTest
	public void aplicarNivel(GameTestHelper helper) {
		Zombie zombie = spawn(helper, EntityTypes.ZOMBIE, EntitySpawnReason.COMMAND);
		helper.assertFalse(zombie.hasAttached(NivelDeMob.NIVEL), Component.literal("COMMAND no escala solo"));

		NivelDeMob.aplicar(zombie, 20);
		NivelDeMob.aplicar(zombie, 20);
		assertCerca(helper, 60, zombie.getMaxHealth(), "Vida máxima de un zombi de nivel 20 (20 × 3)");
		assertCerca(helper, 60, zombie.getHealth(), "Aparece con la vida llena");
		helper.assertValueEqual(1, zombie.getAttribute(Attributes.MAX_HEALTH).getModifiers().size(), "Aplicar dos veces no apila modificadores");
		helper.assertValueEqual(20, NivelDeMob.nivel(zombie), "Nivel guardado en el attachment");
		helper.assertTrue(zombie.getCustomName().getString().endsWith("[Nv 20]"), Component.literal("El nombre lleva el nivel: " + zombie.getCustomName().getString()));
		helper.assertFalse(zombie.isCustomNameVisible(), Component.literal("Por defecto el nombre sólo se ve al mirar el mob"));
		helper.assertTrue(NivelDeMob.esNombreDelMod(zombie), Component.literal("El nombre es el del mod"));
		helper.assertFalse(zombie.isPersistenceRequired(), Component.literal("El nombre puesto por el mod no evita el despawn"));

		NivelDeMob.aplicar(zombie, 50);
		assertCerca(helper, 100, zombie.getMaxHealth(), "La vida topea en ×5");
		AttributeModifier danio = zombie.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(MobsPoderosos.id("nivel"));
		assertCerca(helper, 1.0, danio.amount(), "El daño topea en +100 %");
		AttributeModifier armadura = zombie.getAttribute(Attributes.ARMOR).getModifier(MobsPoderosos.id("nivel"));
		assertCerca(helper, 8, armadura.amount(), "La armadura topea en +8");

		zombie.setCustomName(Component.literal("Pepe"));
		helper.assertFalse(NivelDeMob.esNombreDelMod(zombie), Component.literal("Con un name tag el nombre deja de ser del mod"));
		helper.succeed();
	}

	@GameTest
	public void creeper(GameTestHelper helper) {
		Creeper debil = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		Creeper fuerte = spawn(helper, EntityTypes.CREEPER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(debil, 29);
		NivelDeMob.aplicar(fuerte, 30);
		helper.assertValueEqual(3, ((CreeperAccessor) debil).mobspoderosos$getExplosionRadius(), "Radio de un creeper de nivel 29");
		helper.assertValueEqual(4, ((CreeperAccessor) fuerte).mobspoderosos$getExplosionRadius(), "Radio de un creeper de nivel 30");
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
		assertCerca(helper, 20 * 4.3, recargado.getMaxHealth(), "La vida máxima de nivel 33 se conserva");
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
		assertCerca(helper, 34, ahogado.getMaxHealth(), "Con las estadísticas de nivel 7");

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
		helper.assertValueEqual(0, BotinPorNivel.tiradasExtra(7), "Tiradas extra en nivel 7");
		helper.assertValueEqual(1, BotinPorNivel.tiradasExtra(8), "Tiradas extra en nivel 8");
		helper.assertValueEqual(5, BotinPorNivel.tiradasExtra(47), "Tiradas extra en nivel 47");
		helper.assertValueEqual(6, BotinPorNivel.tiradasExtra(48), "Tiradas extra en nivel 48");
		helper.assertValueEqual(6, BotinPorNivel.tiradasExtra(200), "Las tiradas extra topean en 6");

		helper.assertValueEqual(1.0, chanceConNivel(helper, EntityTypes.ZOMBIE, 25), "Las tiradas extra de un zombi son seguras");
		helper.assertValueEqual(0.0, chanceConNivel(helper, EntityTypes.WITHER_SKELETON, 25), "El esqueleto del Wither (sin_tiradas_extra) no tiene");
		assertCerca(helper, 0.1, chanceConNivel(helper, EntityTypes.EVOKER, 10), "Chance de cada tirada extra de un evocador de nivel 10");
		assertCerca(helper, 0.5, chanceConNivel(helper, EntityTypes.EVOKER, 50), "La chance del evocador topea en 50 %");
		helper.succeed();
	}

	@GameTest
	public void botinAlMorir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = joinSurvivalPlayer(helper, "cazador");

		// El devastador siempre suelta una montura: cada tirada extra es una montura más
		Mob devastador = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(devastador, 30);
		helper.assertValueEqual(4, matarYContar(helper, devastador, level.damageSources().playerAttack(jugador), Items.SADDLE), "Un devastador de nivel 30 muerto por un jugador suelta 1 + 3 monturas");

		Mob lejano = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(lejano, 50);
		helper.assertValueEqual(7, matarYContar(helper, lejano, level.damageSources().playerAttack(jugador), Items.SADDLE), "Un devastador de nivel 50 suelta 1 + 6 monturas");

		Mob sinJugador = spawn(helper, EntityTypes.RAVAGER, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(sinJugador, 30);
		helper.assertValueEqual(1, matarYContar(helper, sinJugador, level.damageSources().genericKill(), Items.SADDLE), "Sin jugador no hay tiradas extra");

		// 20 evocadores de nivel 50: 120 tiradas extra al 50 %. Que salgan todas o ninguna tiene probabilidad 2^-120
		int totems = 0;
		for (int i = 0; i < 20; i++) {
			Mob evocador = spawn(helper, EntityTypes.EVOKER, EntitySpawnReason.COMMAND);
			NivelDeMob.aplicar(evocador, 50);
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
		helper.assertValueEqual(15, conNivel.getExperienceReward(level, null), "Experiencia de un creeper de nivel 20 (5 × 3)");
		helper.succeed();
	}

	private static double chanceConNivel(GameTestHelper helper, EntityType<? extends Mob> tipo, int nivel) {
		Mob mob = spawn(helper, tipo, EntitySpawnReason.COMMAND);
		NivelDeMob.aplicar(mob, nivel);
		double chance = BotinPorNivel.chanceTiradaExtra(mob);
		mob.discard();
		return chance;
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

	private static void assertCerca(GameTestHelper helper, double esperado, double real, String que) {
		helper.assertTrue(Math.abs(esperado - real) < 0.01, Component.literal(que + ": se esperaba " + esperado + " y dio " + real));
	}
}
