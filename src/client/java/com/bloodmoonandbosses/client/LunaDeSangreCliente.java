package com.bloodmoonandbosses.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * El cielo rojo de la luna de sangre. Lo que vale es lo que dice el servidor (PaqueteLunaDeSangre): el cliente no
 * sabe qué noches son de sangre. El color entra y sale de a poco, en unos segundos, para que no sea un corte.
 */
public final class LunaDeSangreCliente {
	private static final float TICKS_DE_TRANSICION = 60;
	// Hacia qué color va cada cosa y cuánto (1 = del todo). En 1.21.1 la luz del cielo (la que en 26.x tiñe apenas el
	// mundo afuera) se calcula adentro del lightmap y no se toca
	private static final Vector3fc CIELO = new Vector3f(0.32f, 0.03f, 0.04f);
	private static final float FUERZA_CIELO = 0.75f;
	private static final Vector3fc NIEBLA = new Vector3f(0.30f, 0.04f, 0.04f);
	private static final float FUERZA_NIEBLA = 0.7f;
	private static final Vector3fc NUBES = new Vector3f(0.45f, 0.12f, 0.12f);
	private static final float FUERZA_NUBES = 0.6f;
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

	/** El color del cielo de vanilla, corrido hacia el rojo. */
	public static Vec3 cielo(Vec3 color, float intensidad) {
		return hacia(color, CIELO, FUERZA_CIELO * intensidad);
	}

	public static Vec3 nubes(Vec3 color, float intensidad) {
		return hacia(color, NUBES, FUERZA_NUBES * intensidad);
	}

	/** La niebla, que en 1.21.1 son tres floats sueltos. */
	public static Vector3f niebla(float rojo, float verde, float azul, float intensidad) {
		return new Vector3f(rojo, verde, azul).lerp(NIEBLA, FUERZA_NIEBLA * intensidad);
	}

	private static Vec3 hacia(Vec3 color, Vector3fc objetivo, float fuerza) {
		return color.lerp(new Vec3(objetivo.x(), objetivo.y(), objetivo.z()), fuerza);
	}

	/** El color de la luna multiplicado: rojo, sin tocar la transparencia (que la esconde con lluvia o de día). */
	public static void colorearLuna(Vector4f color, float intensidad) {
		color.mul(1, Mth.lerp(intensidad, 1, VERDE_DE_LA_LUNA), Mth.lerp(intensidad, 1, AZUL_DE_LA_LUNA), 1);
	}

	public static float tamanioDeLaLuna(float tamanio, float intensidad) {
		return tamanio * Mth.lerp(intensidad, 1, TAMANIO_DE_LA_LUNA);
	}
}
