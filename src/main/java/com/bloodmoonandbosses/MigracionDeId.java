package com.bloodmoonandbosses;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Hasta la 0.10.0 el id del mod era {@code mobspoderosos}. Todo lo que se guardó con ese id (el nivel y el afijo de
 * cada mob, las esperas de cada jugador, la luna de sangre del mundo, los modificadores de los mobs y la config) se
 * pasa al id nuevo la primera vez que se lo encuentra.
 *
 * <p>Los attachments viejos tienen que seguir registrados para siempre: Fabric descarta al cargar un attachment cuyo
 * id no conoce, y un mob de un chunk que nadie visita desde antes del cambio perdería su nivel al volver a cargarse.
 */
public final class MigracionDeId {
	public static final String ID_VIEJO = "mobspoderosos";

	/** De un attachment viejo a su par nuevo, con el mismo tipo de valor. */
	private record Par<T>(AttachmentType<T> viejo, AttachmentType<T> nuevo) {
		/** El nuevo gana si ya está: lo puso el mod con el id nuevo, y el viejo sería de antes. */
		boolean pasar(AttachmentTarget objetivo) {
			T valor = objetivo.getAttached(viejo);
			if (valor == null) {
				return false;
			}
			if (!objetivo.hasAttached(nuevo)) {
				objetivo.setAttached(nuevo, valor);
			}
			objetivo.removeAttached(viejo);
			return true;
		}
	}

	// En mobs y jugadores
	private static final List<Par<?>> DE_ENTIDADES = List.of(
			new Par<>(AttachmentRegistry.createPersistent(viejo("nivel"), Codec.INT), NivelDeMob.NIVEL),
			new Par<>(AttachmentRegistry.createPersistent(viejo("pendiente"), Codec.BOOL), NivelDeMob.PENDIENTE),
			new Par<>(AttachmentRegistry.createPersistent(viejo("afijo"), Afijo.CODEC), Afijo.AFIJO),
			new Par<>(AttachmentRegistry.createPersistent(viejo("invocaciones"), Codec.INT), Afijo.INVOCACIONES),
			new Par<>(AttachmentRegistry.createPersistent(viejo("jefe"), Codec.BOOL), JefeDeZona.JEFE),
			new Par<>(AttachmentRegistry.createPersistent(viejo("furia"), Codec.BOOL), JefeDeZona.FURIA),
			new Par<>(AttachmentRegistry.create(viejo("ultimo_jefe"), builder -> builder.persistent(Codec.LONG).copyOnDeath()), JefeDeZona.ULTIMO),
			new Par<>(AttachmentRegistry.create(viejo("ultima_estampida"), builder -> builder.persistent(Codec.LONG).copyOnDeath()), Estampida.ULTIMA));
	// En el overworld
	private static final List<Par<?>> DEL_MUNDO = List.of(
			new Par<>(AttachmentRegistry.createPersistent(viejo("luna_de_sangre"), Codec.LONG), LunaDeSangre.NOCHE_ACTIVA),
			new Par<>(AttachmentRegistry.createPersistent(viejo("noche_sorteada"), Codec.LONG), LunaDeSangre.NOCHE_SORTEADA));

	private MigracionDeId() {
	}

	/**
	 * En {@code onInitialize}, antes de leer la config: {@code config/mobspoderosos.json} → {@code config/bloodmoonandbosses.json}.
	 * Al ser lo primero que se llama de esta clase, también registra los attachments viejos antes de que cargue un mundo.
	 */
	public static void migrarConfig() {
		Path carpeta = FabricLoader.getInstance().getConfigDir();
		if (moverSiFalta(carpeta.resolve(ID_VIEJO + ".json"), carpeta.resolve(BloodmoonAndBosses.MOD_ID + ".json"))) {
			BloodmoonAndBosses.LOGGER.info("Se renombró config/{}.json a config/{}.json (el id del mod cambió)", ID_VIEJO, BloodmoonAndBosses.MOD_ID);
		}
	}

	/** SERVER_STARTED: la luna de sangre y el sorteo de la noche están en el overworld. */
	public static void migrarMundos(MinecraftServer servidor) {
		for (ServerLevel level : servidor.getAllLevels()) {
			for (Par<?> par : DEL_MUNDO) {
				par.pasar(level);
			}
		}
	}

	/**
	 * ENTITY_LOAD, registrado antes que el resto: {@code NivelDeMob.alCargar} y {@code JefeDeZona.alCargar} tienen que
	 * ver el nivel y la marca de jefe ya en el id nuevo, o le pondrían un nivel nuevo a un mob que ya tenía uno.
	 */
	public static void migrarEntidad(Entity entidad, ServerLevel level) {
		boolean paso = false;
		for (Par<?> par : DE_ENTIDADES) {
			paso |= par.pasar(entidad);
		}
		if (entidad instanceof LivingEntity viva) {
			paso |= renombrarModificadores(viva);
		}
		if (paso) {
			BloodmoonAndBosses.LOGGER.debug("Se pasó {} al id nuevo", entidad);
		}
	}

	/**
	 * Vanilla guarda los modificadores permanentes en el NBT con su id. Si quedaran con el id viejo, el mob seguiría con
	 * su vida y su daño, pero {@code aplicar} (una conversión, la furia) pondría los nuevos al lado y sumarían dos veces.
	 */
	public static boolean renombrarModificadores(LivingEntity viva) {
		boolean cambio = false;
		for (var atributo : BuiltInRegistries.ATTRIBUTE.listElements().toList()) {
			AttributeInstance instancia = viva.getAttribute(atributo);
			if (instancia == null) {
				continue;
			}
			for (AttributeModifier modificador : List.copyOf(instancia.getPermanentModifiers())) {
				if (modificador.id().getNamespace().equals(ID_VIEJO)) {
					instancia.removeModifier(modificador.id());
					instancia.addOrReplacePermanentModifier(new AttributeModifier(BloodmoonAndBosses.id(modificador.id().getPath()),
							modificador.amount(), modificador.operation()));
					cambio = true;
				}
			}
		}
		return cambio;
	}

	/** Mueve el archivo sólo si el nuevo no existe: si ya hay uno con el id nuevo, es el que vale. */
	public static boolean moverSiFalta(Path viejo, Path nuevo) {
		if (!Files.exists(viejo) || Files.exists(nuevo)) {
			return false;
		}
		try {
			Files.createDirectories(nuevo.getParent());
			Files.move(viejo, nuevo);
			return true;
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo mover " + viejo + " a " + nuevo, e);
		}
	}

	/** Para los tests: el attachment viejo con ese path ({@code "nivel"} → {@code mobspoderosos:nivel}). */
	@SuppressWarnings("unchecked")
	public static <T> AttachmentType<T> attachmentViejo(String path) {
		for (List<Par<?>> pares : List.of(DE_ENTIDADES, DEL_MUNDO)) {
			for (Par<?> par : pares) {
				if (par.viejo().identifier().getPath().equals(path)) {
					return (AttachmentType<T>) par.viejo();
				}
			}
		}
		throw new IllegalArgumentException("No hay un attachment viejo " + path);
	}

	public static Identifier viejo(String path) {
		return Identifier.fromNamespaceAndPath(ID_VIEJO, path);
	}
}
