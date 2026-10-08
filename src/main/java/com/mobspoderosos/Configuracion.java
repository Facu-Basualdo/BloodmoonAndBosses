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
		int radioCreeperExtra) {

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
			1);

	public Configuracion conVariacion(int variacion) {
		return new Configuracion(bloquesPorNivel, nivelMaximo, extraNether, extraEnd, variacion, escalarSpawners,
				nombreSiempreVisible, vida, danio, armadura, velocidad, rangoDeSeguimiento, resistenciaAlEmpuje,
				nivelRadioCreeper, radioCreeperExtra);
	}

	public record Escala(double porNivel, double tope) {
		public double en(int nivel) {
			return Math.min(porNivel * nivel, tope);
		}
	}
}
