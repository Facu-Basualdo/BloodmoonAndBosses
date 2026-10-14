package com.bloodmoonandbosses.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.bloodmoonandbosses.ArchivoDeConfiguracion;
import com.bloodmoonandbosses.BloodmoonAndBosses;
import com.bloodmoonandbosses.Idioma;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/**
 * {@code config/bloodmoonandbosses.json} desde Mod Menu. Con un mundo abierto en esta instancia se aplica como
 * {@code /bloodmoon recargar}: los mobs que ya están no cambian, los eventos y el botín sí.
 */
public final class PantallaDeConfiguracionDeBloodmoon extends PantallaDeConfiguracion {
	public PantallaDeConfiguracionDeBloodmoon(@Nullable Screen anterior) {
		super(Idioma.tr(idioma(), "bloodmoonandbosses.config.pantalla.titulo"), anterior, ArchivoDeConfiguracion.ruta());
	}

	private static String idioma() {
		return Idioma.resolver(Minecraft.getInstance().getLanguageManager().getSelected());
	}

	@Override
	protected @Nullable MutableComponent traducir(String clave, Object... args) {
		String idioma = idioma();
		return Idioma.claves(idioma).contains(clave) ? Idioma.tr(idioma, clave, args) : null;
	}

	@Override
	protected String prefijo() {
		return "bloodmoonandbosses.config";
	}

	@Override
	protected List<Component> recargar(@Nullable MinecraftServer servidor) {
		String error = BloodmoonAndBosses.cargarConfiguracion();
		return error == null ? List.of() : List.of(Component.literal(error));
	}

	@Override
	protected List<String> opciones(String ruta) {
		return ruta.equals("idioma") ? Idioma.DISPONIBLES : List.of();
	}
}
