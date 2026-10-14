package com.bloodmoonandbosses.test;

import java.util.List;

import com.bloodmoonandbosses.Afijo;
import com.bloodmoonandbosses.BloodmoonAndBosses;
import com.bloodmoonandbosses.JefeDeZona;
import com.bloodmoonandbosses.NivelDeMob;
import com.bloodmoonandbosses.client.AspectoDeMobs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Vindicator;

/**
 * En un cliente de verdad: los attachments de jefe, furia y afijo llegan al cliente y cambian el tinte. De izquierda
 * a derecha en la captura: común, campeón vampiro, campeón veloz, jefe, jefe furioso. Vindicadores: no se queman de día.
 */
public class AspectoDeMobsClienteTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext mundo = context.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			mundo.getServer().runCommand("difficulty normal");
			mundo.getServer().runCommand("time set 6000");
			mundo.getServer().runCommand("weather clear");
			mundo.getServer().runCommand("tp @a ~ ~ ~ 0 5");
			mundo.getServer().runOnServer(servidor -> {
				ServerPlayer jugador = servidor.getPlayerList().getPlayers().getFirst();
				ServerLevel level = jugador.level();
				BlockPos frente = jugador.blockPosition().south(9);
				List<Mob> mobs = List.of(
						poner(level, frente.east(6)),
						poner(level, frente.east(3)),
						poner(level, frente),
						poner(level, frente.west(3)),
						poner(level, frente.west(7)));
				campeon(mobs.get(1), Afijo.VAMPIRO);
				campeon(mobs.get(2), Afijo.VELOZ);
				JefeDeZona.convertir(mobs.get(3), BloodmoonAndBosses.config().jefeDeZona());
				JefeDeZona.convertir(mobs.get(4), BloodmoonAndBosses.config().jefeDeZona());
				mobs.get(4).setAttached(JefeDeZona.FURIA, true);
			});

			context.waitFor(cliente -> cuantos(cliente.level.getEntitiesOfClass(Vindicator.class, cliente.player.getBoundingBox().inflate(20))) == 4);
			context.waitTicks(20);
			context.runOnClient(cliente -> {
				List<Vindicator> vindicadores = cliente.level.getEntitiesOfClass(Vindicator.class, cliente.player.getBoundingBox().inflate(20));
				if (vindicadores.size() != 5) {
					throw new AssertionError("Tendría que ver 5 vindicadores, ve " + vindicadores.size());
				}
				for (Vindicator vindicador : vindicadores) {
					boolean especial = JefeDeZona.esJefe(vindicador) || vindicador.hasAttached(Afijo.AFIJO);
					boolean conTinte = AspectoDeMobs.tinte(vindicador, vindicador.tickCount) != null;
					if (especial != conTinte) {
						throw new AssertionError("Sólo jefes y campeones van conTintes: " + vindicador.getName().getString());
					}
				}
			});
			context.takeScreenshot("bloodmoonandbosses_aspecto_de_mobs");
		}
	}

	/** Los jefes y los campeones, contados del lado del cliente: si son 4, la sincronización llegó. */
	private static int cuantos(List<Vindicator> vindicadores) {
		int especiales = 0;
		for (Vindicator vindicador : vindicadores) {
			if (JefeDeZona.esJefe(vindicador) || vindicador.hasAttached(Afijo.AFIJO)) {
				especiales++;
			}
		}
		return especiales;
	}

	private static Mob poner(ServerLevel level, BlockPos pos) {
		Mob mob = NivelDeMob.invocarConNivel(EntityTypes.VINDICATOR, level, pos, EntitySpawnReason.EVENT, 20, false);
		mob.setNoAi(true);
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180, 0);
		mob.setYHeadRot(180);
		mob.setYBodyRot(180);
		return mob;
	}

	private static void campeon(Mob mob, Afijo afijo) {
		mob.setAttached(Afijo.AFIJO, afijo);
		NivelDeMob.aplicar(mob, 30);
	}
}
