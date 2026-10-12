package com.bloodmoonandbosses;

import java.util.Arrays;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class ComandoBloodmoonAndBosses {
	// Corto y no el id del mod: se escribe en el chat
	public static final String COMANDO = "bloodmoon";

	private ComandoBloodmoonAndBosses() {
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext contexto) {
		dispatcher.register(Commands.literal(COMANDO)
				.then(Commands.literal("zona").executes(ComandoBloodmoonAndBosses::zona))
				.then(Commands.literal("invocar")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("entidad", ResourceArgument.resource(contexto, Registries.ENTITY_TYPE))
								.suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
								.then(Commands.argument("nivel", IntegerArgumentType.integer(1))
										.executes(ctx -> invocar(ctx, null))
										.then(Commands.argument("afijo", StringArgumentType.word())
												.suggests((ctx, sugerencias) -> SharedSuggestionProvider.suggest(
														Arrays.stream(Afijo.values()).map(Afijo::getSerializedName), sugerencias))
												.executes(ctx -> invocar(ctx, StringArgumentType.getString(ctx, "afijo")))))))
				.then(Commands.literal("evento")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("estampida")
								.executes(ctx -> estampida(ctx, ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("jugador", EntityArgument.player())
										.executes(ctx -> estampida(ctx, EntityArgument.getPlayer(ctx, "jugador")))))
						.then(Commands.literal("jefe")
								.executes(ctx -> jefe(ctx, ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("jugador", EntityArgument.player())
										.executes(ctx -> jefe(ctx, EntityArgument.getPlayer(ctx, "jugador")))))
						.then(Commands.literal("luna_de_sangre").executes(ComandoBloodmoonAndBosses::lunaDeSangre)))
				.then(Commands.literal("recargar")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ComandoBloodmoonAndBosses::recargar)));
	}

	private static int zona(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack fuente = ctx.getSource();
		// Sin variación: es el nivel "de la zona", el que tendría un mob sin la tirada de ±1
		Configuracion config = BloodmoonAndBosses.config().conVariacion(0);
		ServerLevel level = fuente.getLevel();
		BlockPos spawn = level.getServer().getRespawnData().pos();
		int porDistancia = NivelDeZona.calcular(level.dimension(), spawn, BlockPos.containing(fuente.getPosition()), config, level.getRandom());
		long dias = NivelDeZona.dias(level);
		int porTiempo = NivelDeZona.porTiempo(dias, config);
		int nivel = Math.min(porDistancia + porTiempo + LunaDeSangre.nivelesExtra(level), config.nivelMaximo());
		String idioma = Idioma.de(fuente.getPlayer());
		MutableComponent texto = Idioma.tr(idioma, "bloodmoonandbosses.zona.nivel", nivel);
		if (porTiempo > 0) {
			texto.append(Idioma.tr(idioma, "bloodmoonandbosses.zona.detalle", porDistancia, porTiempo, dias));
		}
		if (config.chanceDeCampeon() > 0 && config.nivelesDeCampeon() > 0) {
			texto.append(Idioma.tr(idioma, "bloodmoonandbosses.zona.campeon", Math.round(1 / config.chanceDeCampeon()), config.nivelesDeCampeon()));
		}
		int porLuna = LunaDeSangre.nivelesExtra(level);
		if (porLuna > 0) {
			texto.append(Idioma.tr(idioma, "bloodmoonandbosses.zona.luna", porLuna));
		}
		fuente.sendSuccess(() -> texto, false);
		return nivel;
	}

	private static int invocar(CommandContext<CommandSourceStack> ctx, @Nullable String idAfijo) throws CommandSyntaxException {
		CommandSourceStack fuente = ctx.getSource();
		EntityType<?> tipo = ResourceArgument.getSummonableEntityType(ctx, "entidad").value();
		int nivel = IntegerArgumentType.getInteger(ctx, "nivel");
		@Nullable Afijo afijo = idAfijo == null ? null : Afijo.porId(idAfijo);
		if (idAfijo != null && afijo == null) {
			fuente.sendFailure(Idioma.tr(fuente, "bloodmoonandbosses.comando.afijo_desconocido", idAfijo));
			return 0;
		}

		// COMMAND no escala solo: vanilla le pone el equipo en finalizeSpawn y el nivel se aplica acá
		Entity entidad = tipo.spawn(fuente.getLevel(), delante(fuente), EntitySpawnReason.COMMAND);
		if (!(entidad instanceof Mob mob) || !NivelDeMob.puedeTenerNivel(mob)) {
			if (entidad != null) {
				entidad.discard();
			}
			fuente.sendFailure(Idioma.tr(fuente, "bloodmoonandbosses.comando.sin_nivel"));
			return 0;
		}
		NivelDeMob.aparecer(mob, nivel, afijo);
		fuente.sendSuccess(() -> Idioma.tr(fuente, "bloodmoonandbosses.comando.aparecio", mob.getDisplayName()), true);
		return 1;
	}

	private static int estampida(CommandContext<CommandSourceStack> ctx, ServerPlayer jugador) {
		Idioma.@Nullable Mensaje error = Estampida.empezar(jugador, BloodmoonAndBosses.config().estampida());
		String idioma = Idioma.de(ctx.getSource().getPlayer());
		if (error != null) {
			ctx.getSource().sendFailure(Idioma.tr(idioma, "bloodmoonandbosses.comando.estampida_fallida", error.en(idioma)));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Idioma.tr(idioma, "bloodmoonandbosses.comando.estampida", jugador.getName()), true);
		return 1;
	}

	private static int jefe(CommandContext<CommandSourceStack> ctx, ServerPlayer jugador) {
		Mob jefe = JefeDeZona.aparecer(jugador, BloodmoonAndBosses.config().jefeDeZona(), 16, 24);
		if (jefe == null) {
			ctx.getSource().sendFailure(Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.jefe_sin_lugar", jugador.getName()));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.aparecio", NivelDeMob.nombreSinVida(jefe)), true);
		return 1;
	}

	private static int lunaDeSangre(CommandContext<CommandSourceStack> ctx) {
		if (!LunaDeSangre.empezar(ctx.getSource().getServer().overworld())) {
			ctx.getSource().sendFailure(Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.luna_de_dia"));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.luna"), true);
		return 1;
	}

	private static int recargar(CommandContext<CommandSourceStack> ctx) {
		@Nullable String error = BloodmoonAndBosses.cargarConfiguracion();
		if (error != null) {
			ctx.getSource().sendFailure(Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.recarga_fallida", error));
			return 0;
		}
		// Las estadísticas y el equipo se ponen al aparecer: los mobs que ya están no cambian
		ctx.getSource().sendSuccess(() -> Idioma.tr(ctx.getSource(), "bloodmoonandbosses.comando.recargada"), true);
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
