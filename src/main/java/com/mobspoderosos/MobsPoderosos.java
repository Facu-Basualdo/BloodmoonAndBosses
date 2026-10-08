package com.mobspoderosos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.resources.Identifier;

public class MobsPoderosos implements ModInitializer {
	public static final String MOD_ID = "mobspoderosos";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		NivelDeMob.inicializar();

		ServerEntityEvents.ENTITY_LOAD.register(NivelDeMob::alCargar);
		ServerLivingEntityEvents.MOB_CONVERSION.register((viejo, nuevo, params) -> NivelDeMob.alConvertir(viejo, nuevo));
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoMobsPoderosos.registrar(dispatcher, contexto));
	}

	public static Configuracion config() {
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
