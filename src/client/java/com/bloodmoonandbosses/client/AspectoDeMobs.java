package com.bloodmoonandbosses.client;

import org.jspecify.annotations.Nullable;

import com.bloodmoonandbosses.Afijo;
import com.bloodmoonandbosses.JefeDeZona;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * El tinte del modelo de jefes y campeones, para reconocerlos de un vistazo. Sale de los attachments sincronizados
 * (JEFE, FURIA, AFIJO): el cliente no calcula nada, sólo pinta. El tinte multiplica la textura, así que la deja ver.
 */
public final class AspectoDeMobs {
	public static final RenderStateDataKey<Integer> TINTE = RenderStateDataKey.create(() -> "bloodmoonandbosses:tinte");

	// Cuánto se corre el blanco hacia el color: poco en los campeones (son muchos), más en el jefe
	private static final float FUERZA_AFIJO = 0.4f;
	private static final float FUERZA_JEFE = 0.45f;
	private static final int COLOR_JEFE = 0xAA00AA;
	// Con furia late en rojo, una vez por segundo
	private static final int COLOR_FURIA = 0xFF3030;
	private static final float FUERZA_FURIA_MINIMA = 0.35f;
	private static final float FUERZA_FURIA_MAXIMA = 0.65f;
	private static final float TICKS_POR_LATIDO = 20;

	private AspectoDeMobs() {
	}

	/** null para un mob común: así no se toca el tinte de vanilla. */
	public static @Nullable Integer tinte(LivingEntity mob, float edad) {
		if (JefeDeZona.esJefe(mob)) {
			if (JefeDeZona.estaFurioso(mob)) {
				float latido = (Mth.sin(edad * Mth.TWO_PI / TICKS_POR_LATIDO) + 1) / 2;
				return haciaColor(COLOR_FURIA, Mth.lerp(latido, FUERZA_FURIA_MINIMA, FUERZA_FURIA_MAXIMA));
			}
			return haciaColor(COLOR_JEFE, FUERZA_JEFE);
		}
		Afijo afijo = mob.getAttached(Afijo.AFIJO);
		if (afijo == null) {
			return null;
		}
		TextColor color = TextColor.fromLegacyFormat(afijo.color());
		return color == null ? null : haciaColor(color.getValue(), FUERZA_AFIJO);
	}

	/** El blanco (no tiñe) corrido hacia ese color: con 1 sería el color puro y la textura quedaría tapada. */
	private static int haciaColor(int rgb, float fuerza) {
		int r = (int) Mth.lerp(fuerza, 255, ARGB.red(rgb));
		int g = (int) Mth.lerp(fuerza, 255, ARGB.green(rgb));
		int b = (int) Mth.lerp(fuerza, 255, ARGB.blue(rgb));
		return ARGB.color(255, r, g, b);
	}
}
