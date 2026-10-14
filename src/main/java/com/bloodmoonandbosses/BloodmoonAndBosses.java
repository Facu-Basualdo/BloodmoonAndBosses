package com.bloodmoonandbosses;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.resources.Identifier;

public class BloodmoonAndBosses implements ModInitializer {
	public static final String MOD_ID = "bloodmoonandbosses";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		// Antes que todo: hasta la 0.10.0 el id era mobspoderosos (ver MigracionDeId). También registra los attachments viejos
		MigracionDeId.migrarConfig();
		cargarConfiguracion();
		NivelDeMob.inicializar();

		// El primero de ENTITY_LOAD: los de abajo tienen que ver el nivel y la marca de jefe ya en el id nuevo
		ServerEntityEvents.ENTITY_LOAD.register(MigracionDeId::migrarEntidad);
		ServerLifecycleEvents.SERVER_STARTED.register(MigracionDeId::migrarMundos);
		ServerEntityEvents.ENTITY_LOAD.register(NivelDeMob::alCargar);
		ServerLivingEntityEvents.MOB_CONVERSION.register((viejo, nuevo, params) -> NivelDeMob.alConvertir(viejo, nuevo));
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoBloodmoonAndBosses.registrar(dispatcher, contexto));
		Eventos.registrar();

		// Para el cliente con el mod (cielo rojo en luna de sangre). Registrarlo no molesta a un cliente vanilla: no se le manda
		PayloadTypeRegistry.clientboundPlay().register(PaqueteLunaDeSangre.TYPE, PaqueteLunaDeSangre.CODEC);
		ServerPlayConnectionEvents.JOIN.register((conexion, enviador, servidor) -> LunaDeSangre.alConectarse(conexion.getPlayer()));
	}

	public static Configuracion config() {
		return config;
	}

	/** Si el archivo tiene un error se queda la configuración que había (al arrancar, la por defecto) y se devuelve el motivo. */
	public static @Nullable String cargarConfiguracion() {
		try {
			config = ArchivoDeConfiguracion.leer(ArchivoDeConfiguracion.ruta());
			return null;
		} catch (IOException | RuntimeException e) {
			LOGGER.error("No se pudo leer {}: {}", ArchivoDeConfiguracion.ruta(), e.getMessage());
			return e.getMessage();
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
