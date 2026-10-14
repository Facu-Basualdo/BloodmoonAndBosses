package com.bloodmoonandbosses.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * El cielo rojo de la luna de sangre. Lo que vale es lo que dice el servidor (PaqueteLunaDeSangre): el cliente no
 * sabe qué noches son de sangre. El color entra y sale de a poco, en unos segundos, para que no sea un corte.
 */
public final class LunaDeSangreCliente {
	private static final float TICKS_DE_TRANSICION = 60;
	// Hacia qué color va cada cosa y cuánto (1 = del todo). La luz del cielo, poco: es la que tiñe el mundo afuera
	private static final Vector3fc CIELO = new Vector3f(0.32f, 0.03f, 0.04f);
	private static final float FUERZA_CIELO = 0.75f;
	private static final Vector3fc NIEBLA = new Vector3f(0.30f, 0.04f, 0.04f);
	private static final float FUERZA_NIEBLA = 0.7f;
	private static final Vector3fc NUBES = new Vector3f(0.45f, 0.12f, 0.12f);
	private static final float FUERZA_NUBES = 0.6f;
	private static final Vector3fc LUZ_DEL_CIELO = new Vector3f(1.0f, 0.35f, 0.30f);
	private static final float FUERZA_LUZ_DEL_CIELO = 0.45f;
	// La luna: roja y más grande
	private static final float VERDE_DE_LA_LUNA = 0.25f;
	private static final float AZUL_DE_LA_LUNA = 0.2f;
	private static final float TAMANIO_DE_LA_LUNA = 1.5f;

	private static boolean activa;
	private static float anterior;
	private static float actual;

	private LunaDeSangreCliente() {
	}

	static void recibir(boolean activa) {
		LunaDeSangreCliente.activa = activa;
	}

	static void tick() {
		anterior = actual;
		actual = Mth.clamp(actual + (activa ? 1 : -1) / TICKS_DE_TRANSICION, 0, 1);
	}

	static void reiniciar() {
		activa = false;
		anterior = 0;
		actual = 0;
	}

	/** De 0 (noche común) a 1 (luna de sangre del todo), entre el tick anterior y el actual. */
	public static float intensidad(float parcial) {
		return Mth.lerp(parcial, anterior, actual);
	}

	/** El valor de vanilla, corrido hacia el rojo si es uno de los colores del cielo. Lo demás vuelve igual. */
	public static Object colorear(EnvironmentAttribute<?> atributo, Object valor, float intensidad) {
		if (atributo == EnvironmentAttributes.SKY_COLOR && valor instanceof Vector3fc color) {
			return hacia(color, CIELO, FUERZA_CIELO * intensidad);
		} else if (atributo == EnvironmentAttributes.FOG_COLOR && valor instanceof Vector3fc color) {
			return hacia(color, NIEBLA, FUERZA_NIEBLA * intensidad);
		} else if (atributo == EnvironmentAttributes.SKY_LIGHT_COLOR && valor instanceof Vector3fc color) {
			return hacia(color, LUZ_DEL_CIELO, FUERZA_LUZ_DEL_CIELO * intensidad);
		} else if (atributo == EnvironmentAttributes.CLOUD_COLOR && valor instanceof Vector4fc color) {
			Vector3f rgb = hacia(new Vector3f(color.x(), color.y(), color.z()), NUBES, FUERZA_NUBES * intensidad);
			return new Vector4f(rgb, color.w());
		}
		return valor;
	}

	// Uno nuevo: el de vanilla puede ser una constante compartida
	private static Vector3f hacia(Vector3fc color, Vector3fc objetivo, float fuerza) {
		return new Vector3f(color).lerp(objetivo, fuerza);
	}

	/** El color de la luna multiplicado: rojo, sin tocar la transparencia (que la esconde con lluvia o de día). */
	public static void colorearLuna(Vector4f color, float intensidad) {
		color.mul(1, Mth.lerp(intensidad, 1, VERDE_DE_LA_LUNA), Mth.lerp(intensidad, 1, AZUL_DE_LA_LUNA), 1);
	}

	public static float tamanioDeLaLuna(float tamanio, float intensidad) {
		return tamanio * Mth.lerp(intensidad, 1, TAMANIO_DE_LA_LUNA);
	}
}
