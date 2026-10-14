package com.bloodmoonandbosses.test;

import com.bloodmoonandbosses.client.LunaDeSangreCliente;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * En un cliente de verdad: la luna de sangre le llega al cliente y el cielo se tiñe, y al amanecer vuelve. Saca capturas
 * (build/run/clientGameTest/screenshots) para mirar los colores a ojo: el test no puede saber si "se ve bien".
 */
public class LunaDeSangreClienteTest implements FabricClientGameTest {
	// El aviso sale con el reloj de eventos (cada segundo) y el color tarda 60 ticks en entrar
	private static final int ESPERA = 20 + 60 + 10;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext mundo = context.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			mundo.getServer().runCommand("weather clear");
			// Medianoche (la luna está arriba de todo) del día 1: el mundo nuevo arranca con luna llena, y el sorteo de la
			// noche podría darle una luna de sangre antes de la foto de la noche común
			mundo.getServer().runCommand("time set 42000");
			mundo.getServer().runCommand("tp @a ~ ~ ~ 0 -70");
			context.waitTicks(20);
			context.takeScreenshot("bloodmoonandbosses_noche_comun");

			mundo.getServer().runCommand("bloodmoon evento luna_de_sangre");
			context.waitTicks(ESPERA);
			float intensidad = context.computeOnClient(cliente -> LunaDeSangreCliente.intensidad(1));
			if (intensidad < 1) {
				throw new AssertionError("Con luna de sangre el cielo tiene que estar rojo del todo, y está en " + intensidad);
			}
			context.takeScreenshot("bloodmoonandbosses_luna_de_sangre");
			mundo.getServer().runCommand("tp @a ~ ~ ~ 0 0");
			context.waitTicks(5);
			context.takeScreenshot("bloodmoonandbosses_luna_de_sangre_horizonte");

			mundo.getServer().runCommand("time set 49000");
			context.waitTicks(ESPERA);
			float despues = context.computeOnClient(cliente -> LunaDeSangreCliente.intensidad(1));
			if (despues > 0) {
				throw new AssertionError("Al amanecer el cielo vuelve a la normalidad, y quedó en " + despues);
			}
		}
	}
}
