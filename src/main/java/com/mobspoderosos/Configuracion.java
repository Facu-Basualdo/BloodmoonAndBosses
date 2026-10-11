package com.mobspoderosos;

public record Configuracion(
		int bloquesPorNivel,
		int nivelMaximo,
		int extraNether,
		int extraEnd,
		int variacion,
		boolean escalarSpawners,
		boolean nombreSiempreVisible,
		Escala vida,
		Escala danio,
		Escala armadura,
		Escala velocidad,
		Escala rangoDeSeguimiento,
		Escala resistenciaAlEmpuje,
		int nivelRadioCreeper,
		int radioCreeperExtra,
		Escala chanceArmaduraExtra,
		int nivelesPorMaterialDeArmadura,
		boolean cascoParaLosQueSeQueman,
		Escala poderDeEncantamiento,
		Escala chanceSoltarEquipo,
		int nivelesPorTiradaExtra,
		int tiradasExtraMaximas,
		Escala chanceTiradaExtra,
		Rareza botinRaro,
		Rareza botinEpico,
		Rareza botinLegendario,
		double experienciaPorNivel) {

	public static final Configuracion POR_DEFECTO = new Configuracion(
			150,
			50,
			10,
			20,
			1,
			false,
			// Un nombre siempre visible se dibuja a través de las paredes: sería un rayos X de mobs en las cuevas
			false,
			new Escala(0.10, 4.0),
			new Escala(0.04, 1.0),
			new Escala(0.2, 8.0),
			new Escala(0.005, 0.20),
			new Escala(0.01, 0.50),
			new Escala(0.01, 0.5),
			30,
			1,
			new Escala(0.015, 0.5),
			10,
			// Con casco, zombis y esqueletos sobreviven de día, llenan el límite de mobs y el ciclo día/noche deja de importar
			false,
			new Escala(0.5, 30),
			// Se suma a la chance de vanilla (0,085): en nivel 50 un arma o armadura común sale una de cada cuatro veces
			new Escala(0.004, 0.25),
			// Con el nivel máximo en 50, el tope de 6 se alcanza en el nivel 48
			8,
			// Si se sube nivelMaximo, que un mob muy lejano no suelte su tabla decenas de veces
			6,
			// Sólo para el tag tiradas_extra_por_chance: un evocador de nivel 30 tendría 3 tótems extra seguros
			new Escala(0.01, 0.5),
			new Rareza(10, 0.15),
			new Rareza(25, 0.10),
			new Rareza(40, 0.05),
			0.1);

	public Configuracion conVariacion(int variacion) {
		return new Configuracion(bloquesPorNivel, nivelMaximo, extraNether, extraEnd, variacion, escalarSpawners,
				nombreSiempreVisible, vida, danio, armadura, velocidad, rangoDeSeguimiento, resistenciaAlEmpuje,
				nivelRadioCreeper, radioCreeperExtra, chanceArmaduraExtra, nivelesPorMaterialDeArmadura,
				cascoParaLosQueSeQueman, poderDeEncantamiento, chanceSoltarEquipo, nivelesPorTiradaExtra,
				tiradasExtraMaximas, chanceTiradaExtra, botinRaro, botinEpico, botinLegendario, experienciaPorNivel);
	}

	/** Los valores que harían dividir por cero o dejarían un mob sin nivel: mejor rechazar el archivo que arrancar así. */
	public void validar() {
		exigir(bloquesPorNivel >= 1, "bloquesPorNivel tiene que ser 1 o más");
		exigir(nivelMaximo >= 1, "nivelMaximo tiene que ser 1 o más");
		exigir(variacion >= 0, "variacion no puede ser negativa");
		exigir(nivelesPorMaterialDeArmadura >= 1, "nivelesPorMaterialDeArmadura tiene que ser 1 o más");
		exigir(nivelesPorTiradaExtra >= 1, "nivelesPorTiradaExtra tiene que ser 1 o más");
		exigir(tiradasExtraMaximas >= 0, "tiradasExtraMaximas no puede ser negativo");
		for (Escala escala : new Escala[] {vida, danio, armadura, velocidad, rangoDeSeguimiento, resistenciaAlEmpuje,
				chanceArmaduraExtra, poderDeEncantamiento, chanceSoltarEquipo, chanceTiradaExtra}) {
			exigir(escala != null, "falta una de las escalas");
		}
		for (Rareza rareza : new Rareza[] {botinRaro, botinEpico, botinLegendario}) {
			exigir(rareza != null, "falta uno de los botines");
			exigir(rareza.nivelMinimo() >= 1, "el nivelMinimo de un botín tiene que ser 1 o más");
		}
	}

	private static void exigir(boolean condicion, String mensaje) {
		if (!condicion) {
			throw new IllegalArgumentException(mensaje);
		}
	}

	public record Escala(double porNivel, double tope) {
		public double en(int nivel) {
			return Math.min(porNivel * nivel, tope);
		}

		/** Como en, pero arrancando desde un valor que ya tenía (la chance de vanilla, por ejemplo). */
		public double sobre(double base, int nivel) {
			return Math.min(base + porNivel * nivel, tope);
		}
	}

	/** Un botín de los ítems raros: desde qué nivel se tira la tabla y con qué probabilidad. */
	public record Rareza(int nivelMinimo, double chance) {
	}
}
