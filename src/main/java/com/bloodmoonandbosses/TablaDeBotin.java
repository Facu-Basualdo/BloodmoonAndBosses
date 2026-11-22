package com.bloodmoonandbosses;

import java.util.function.Consumer;

import com.bloodmoonandbosses.mixin.AgresorAccessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * En 1.21.1 {@code dropFromLootTable} sólo tira la tabla propia del mob; en 26.x recibe cualquiera. Esto tira la que se
 * le pase con los mismos parámetros que arma vanilla al morir (quién lo mató, el último jugador que le pegó y su suerte).
 */
public final class TablaDeBotin {
	private TablaDeBotin() {
	}

	public static void soltar(LivingEntity muerto, ServerLevel level, DamageSource fuente, boolean matoUnJugador, ResourceKey<LootTable> clave,
			Consumer<ItemStack> destino) {
		LootTable tabla = level.getServer().reloadableRegistries().getLootTable(clave);
		LootParams.Builder parametros = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, muerto)
				.withParameter(LootContextParams.ORIGIN, muerto.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, fuente)
				.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, fuente.getEntity())
				.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, fuente.getDirectEntity());
		Player jugador = ((AgresorAccessor) muerto).bloodmoonandbosses$ultimoJugador();
		if (matoUnJugador && jugador != null) {
			parametros = parametros.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, jugador).withLuck(jugador.getLuck());
		}
		tabla.getRandomItems(parametros.create(LootContextParamSets.ENTITY), muerto.getLootTableSeed(), destino);
	}
}
