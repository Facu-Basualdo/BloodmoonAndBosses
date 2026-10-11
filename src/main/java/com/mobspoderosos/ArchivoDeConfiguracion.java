package com.mobspoderosos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/** config/mobspoderosos.json: los valores de Configuracion, editables sin recompilar. */
public final class ArchivoDeConfiguracion {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ArchivoDeConfiguracion() {
	}

	public static Path ruta() {
		return FabricLoader.getInstance().getConfigDir().resolve(MobsPoderosos.MOD_ID + ".json");
	}

	/**
	 * Lee el archivo sobre los valores por defecto (lo que falte queda por defecto) y lo reescribe completo.
	 * Si no existe, lo crea. Si tiene un error, tira la excepción sin tocar el archivo.
	 */
	public static Configuracion leer(Path ruta) throws IOException {
		JsonObject completo = GSON.toJsonTree(Configuracion.POR_DEFECTO).getAsJsonObject();
		if (Files.exists(ruta)) {
			JsonElement leido = JsonParser.parseString(Files.readString(ruta));
			if (!leido.isJsonObject()) {
				throw new JsonParseException("el archivo tiene que ser un objeto JSON ({ ... })");
			}
			mezclar(completo, leido.getAsJsonObject(), "");
		}
		Configuracion config = GSON.fromJson(completo, Configuracion.class);
		config.validar();

		// Reescrito con todas las claves: así aparecen las que sumó una versión nueva del mod
		Files.createDirectories(ruta.getParent());
		Files.writeString(ruta, GSON.toJson(completo));
		return config;
	}

	private static void mezclar(JsonObject destino, JsonObject origen, String prefijo) {
		for (Map.Entry<String, JsonElement> entrada : origen.entrySet()) {
			String clave = entrada.getKey();
			JsonElement porDefecto = destino.get(clave);
			if (porDefecto == null) {
				// Un nombre mal escrito quedaría ignorado en silencio: mejor avisar
				MobsPoderosos.LOGGER.warn("Configuración: no existe la clave \"{}{}\", se ignora", prefijo, clave);
			} else if (porDefecto.isJsonObject() && entrada.getValue().isJsonObject()) {
				mezclar(porDefecto.getAsJsonObject(), entrada.getValue().getAsJsonObject(), prefijo + clave + ".");
			} else {
				destino.add(clave, entrada.getValue());
			}
		}
	}
}
