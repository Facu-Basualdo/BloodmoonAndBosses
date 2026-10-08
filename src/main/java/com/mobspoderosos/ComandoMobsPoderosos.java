package com.mobspoderosos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class ComandoMobsPoderosos {
	private ComandoMobsPoderosos() {
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext contexto) {
		dispatcher.register(Commands.literal(MobsPoderosos.MOD_ID)
				.then(Commands.literal("zona").executes(ComandoMobsPoderosos::zona))
				.then(Commands.literal("invocar")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("entidad", ResourceArgument.resource(contexto, Registries.ENTITY_TYPE))
								.suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
								.then(Commands.argument("nivel", IntegerArgumentType.integer(1))
										.executes(ComandoMobsPoderosos::invocar)))));
	}

	private static int zona(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack fuente = ctx.getSource();
		// Sin variación: es el nivel "de la zona", el que tendría un mob sin la tirada de ±1
		Configuracion config = MobsPoderosos.config().conVariacion(0);
		ServerLevel level = fuente.getLevel();
		BlockPos spawn = level.getServer().getRespawnData().pos();
		int nivel = NivelDeZona.calcular(level.dimension(), spawn, BlockPos.containing(fuente.getPosition()), config, level.getRandom());
		fuente.sendSuccess(() -> Component.literal("Los mobs de esta zona aparecen con nivel " + nivel), false);
		return nivel;
	}

	private static int invocar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack fuente = ctx.getSource();
		EntityType<?> tipo = ResourceArgument.getSummonableEntityType(ctx, "entidad").value();
		int nivel = IntegerArgumentType.getInteger(ctx, "nivel");

		// COMMAND no escala solo: vanilla le pone el equipo en finalizeSpawn y el nivel se aplica acá
		Entity entidad = tipo.spawn(fuente.getLevel(), delante(fuente), EntitySpawnReason.COMMAND);
		if (!(entidad instanceof Mob mob) || !NivelDeMob.puedeTenerNivel(mob)) {
			if (entidad != null) {
				entidad.discard();
			}
			fuente.sendFailure(Component.literal("Esa entidad no puede tener nivel"));
			return 0;
		}
		NivelDeMob.aplicar(mob, nivel);
		fuente.sendSuccess(() -> Component.literal("Apareció ").append(mob.getDisplayName()), true);
		return 1;
	}

	private static BlockPos delante(CommandSourceStack fuente) {
		Entity quien = fuente.getEntity();
		if (quien == null) {
			return BlockPos.containing(fuente.getPosition());
		}
		return BlockPos.containing(quien.position().add(quien.getLookAngle().multiply(3, 0, 3)));
	}
}
