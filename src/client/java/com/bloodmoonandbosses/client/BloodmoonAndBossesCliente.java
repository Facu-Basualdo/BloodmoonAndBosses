package com.bloodmoonandbosses.client;

import com.bloodmoonandbosses.PaqueteLunaDeSangre;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opcional: con el mod en el cliente, la luna de sangre también se ve. Sin él, el servidor anda igual. */
public class BloodmoonAndBossesCliente implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(PaqueteLunaDeSangre.TYPE, (paquete, contexto) -> LunaDeSangreCliente.recibir(paquete.activa()));
		ClientTickEvents.END_CLIENT_TICK.register(cliente -> LunaDeSangreCliente.tick());
		// Si no, al entrar a otro servidor (o a otro mundo) el cielo seguiría rojo hasta el primer aviso
		ClientPlayConnectionEvents.DISCONNECT.register((conexion, cliente) -> LunaDeSangreCliente.reiniciar());
	}
}
