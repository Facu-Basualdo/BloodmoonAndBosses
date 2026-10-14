package com.bloodmoonandbosses.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.bloodmoonandbosses.ArchivoDeConfiguracion;
import com.bloodmoonandbosses.BloodmoonAndBosses;
import com.bloodmoonandbosses.client.PantallaDeConfiguracionDeBloodmoon;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * La pantalla de configuración de Mod Menu, abierta directamente (Mod Menu no está en el entorno de prueba): guardar
 * cambia el archivo y la config del servidor integrado, y un valor que {@code validar()} rechaza deja el archivo como
 * estaba. Deja capturas en {@code build/run/clientGameTest/screenshots}.
 */
public class ConfiguracionClienteTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Los campos se buscan por su nombre en español: el cliente de prueba arranca en inglés
		context.runOnClient(cliente -> cliente.getLanguageManager().setSelected("es_ar"));
		try (TestSingleplayerContext mundo = context.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			context.getInput().setCursorPos(0, 0);

			context.setScreen(() -> new PantallaDeConfiguracionDeBloodmoon(null));
			context.waitForScreen(PantallaDeConfiguracionDeBloodmoon.class);
			context.waitTicks(2);
			context.takeScreenshot("configuracion");
			context.runOnClient(cliente -> campo(cliente, "Bloques por nivel").setValue("80"));
			context.clickScreenButton("Guardar");
			context.waitFor(cliente -> cliente.gui.screen() == null);
			int bloques = mundo.getServer().computeOnServer(servidor -> BloodmoonAndBosses.config().bloquesPorNivel());
			if (bloques != 80) {
				throw new AssertionError("Guardar tenía que dejar bloquesPorNivel en 80, quedó " + bloques);
			}

			// validar() exige de 0 a 1: no se guarda, y el archivo y la config siguen como antes
			context.setScreen(() -> new PantallaDeConfiguracionDeBloodmoon(null));
			context.waitForScreen(PantallaDeConfiguracionDeBloodmoon.class);
			context.runOnClient(cliente -> campo(cliente, "Probabilidad de campeón").setValue("5"));
			context.clickScreenButton("Guardar");
			context.waitTicks(10);
			if (!context.computeOnClient(cliente -> cliente.gui.screen() instanceof PantallaDeConfiguracionDeBloodmoon)) {
				throw new AssertionError("Con un valor rechazado la pantalla tenía que quedar abierta, con el aviso");
			}
			context.takeScreenshot("configuracion-rechazada");
			String archivo = leer();
			if (!archivo.contains("\"chanceDeCampeon\": 0.05") || !archivo.contains("\"bloquesPorNivel\": 80")) {
				throw new AssertionError("El archivo tenía que quedar como estaba antes del valor rechazado:\n" + archivo);
			}
			double chance = mundo.getServer().computeOnServer(servidor -> BloodmoonAndBosses.config().chanceDeCampeon());
			if (chance != 0.05) {
				throw new AssertionError("La config tenía que seguir con chanceDeCampeon 0.05, quedó " + chance);
			}
			context.setScreen(() -> null);
		}
	}

	private static String leer() {
		try {
			return Files.readString(ArchivoDeConfiguracion.ruta(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Los de la lista están adentro del contenedor con scroll, no en la pantalla directamente. */
	private static EditBox campo(Minecraft cliente, String nombre) {
		List<EditBox> cajas = new ArrayList<>();
		for (GuiEventListener hijo : cliente.gui.screen().children()) {
			recorrer(hijo, cajas);
		}
		return cajas.stream()
				.filter(caja -> caja.getMessage().getString().equals(nombre))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No está el campo " + nombre));
	}

	private static void recorrer(GuiEventListener elemento, List<EditBox> cajas) {
		if (elemento instanceof AbstractWidget && elemento instanceof EditBox caja) {
			cajas.add(caja);
		}
		if (elemento instanceof ContainerEventHandler contenedor) {
			for (GuiEventListener hijo : contenedor.children()) {
				recorrer(hijo, cajas);
			}
		}
	}
}
