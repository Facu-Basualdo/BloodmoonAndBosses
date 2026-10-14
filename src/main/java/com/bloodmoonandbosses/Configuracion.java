package com.bloodmoonandbosses;

public record Configuracion(
		int bloquesPorNivel,
		int nivelMaximo,
		int extraNether,
		int extraEnd,
		int variacion,
		int diasPorNivelExtra,
		int nivelesPorTiempoMaximos,
		double chanceDeCampeon,
		int nivelesDeCampeon,
		boolean escalarSpawners,
		boolean nombreSiempreVisible,
		boolean vidaEnElNombre,
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
		double experienciaPorNivel,
		Afijos afijos,
		Estampida estampida,
		LunaDeSangre lunaDeSangre,
		JefeDeZona jefeDeZona,
		String idioma) {

	/*
	 * Hasta el nivel 100 (a unos 10.000 bloques) y pensados junto con Hero's Forge: cada escala llega a su
	 * tope recién en el nivel 100, así que alejarse siempre se nota, y un jugador con el equipo y los puntos que
	 * se esperan en cada zona mata a un zombi en 5 o 6 golpes.
	 */
	public static final Configuracion POR_DEFECTO = new Configuracion(
			100,
			100,
			20,
			40,
			1,
			// Con el mundo más viejo, el spawn también se pone peligroso: a los 100 días los mobs de al lado
			// de la base son de nivel 21, lo que pide diamante y algunos puntos de Hero's Forge
			5,
			20,
			// Uno de cada veinte mobs aparece 10 niveles por encima de su zona: la sorpresa que obliga a mirar el nombre
			0.05,
			10,
			false,
			// Un nombre siempre visible se dibuja a través de las paredes: sería un rayos X de mobs en las cuevas
			false,
			true,
			new Escala(0.08, 8.0),
			new Escala(0.04, 4.0),
			new Escala(0.12, 12.0),
			new Escala(0.002, 0.20),
			new Escala(0.005, 0.50),
			new Escala(0.005, 0.5),
			60,
			1,
			new Escala(0.0075, 0.5),
			20,
			// Con casco, zombis y esqueletos sobreviven de día, llenan el límite de mobs y el ciclo día/noche deja de importar
			false,
			new Escala(0.3, 30),
			// Se suma a la chance de vanilla (0,085): desde el nivel 83 un arma o armadura común sale una de cada cuatro veces
			new Escala(0.002, 0.25),
			// Con el nivel máximo en 100, el tope de 6 se alcanza en el nivel 96
			16,
			// Si se sube nivelMaximo, que un mob muy lejano no suelte su tabla decenas de veces
			6,
			// Sólo para el tag tiradas_extra_por_chance: un evocador de nivel 60 tendría 3 tótems extra seguros
			new Escala(0.005, 0.5),
			new Rareza(20, 0.15),
			new Rareza(50, 0.10),
			new Rareza(80, 0.05),
			0.05,
			// Todos los campeones con afijo: así se distinguen en el nombre. La explosión es la de un creeper común. El
			// blindado suma media armadura de hierro; el veneno y la lentitud son de nivel II, como las pociones fuertes. Sin
			// tamaño extra: un zombi o un esqueleto apenas un 3 % más grande ya no entra en un túnel de 2 de alto
			new Afijos(1.0, 0.3, 0.5, 3.0f, 30, 2, 3, 10, 6.0, 0.5, 5, 2, 3, 2, 0.5, 3, true, 10, 0.0),
			// Con 3 % por minuto, una noche (unos 7 minutos) tiene ~20 % de estampida; después hay 3 días de respiro
			new Estampida(true, 0.03, 3, 10, 6, 10, 16, 24, 32, 0.25, 180),
			// La luna llena es una noche de cada ocho: con 50 %, una luna de sangre cada 16 días en promedio
			new LunaDeSangre(true, 0.5, 10, 0.5),
			// Con 1 % por minuto, alrededor de un jefe cada hora y media de juego por jugador. A media vida se enfurece; cada
			// 10 segundos usa una habilidad, que avisa un segundo antes para que se pueda esquivar. El contorno se ve a
			// través de las paredes, pero el jefe ya se anuncia y tiene barra: no delata nada que no se sepa. Un 40 % más
			// grande, para reconocerlo entre sus escoltas; aparece sólo donde entra con ese tamaño
			new JefeDeZona(true, 0.01, 2, 15, 20, 3.0, 3, 48, 60, 0.5, 0.25, 0.3, 2, 10, 6, 1.0, 1.5, 24, true, 0.4),
			// El de los nombres de los mobs y la barra de la luna para quien no tiene el mod en el cliente, y el de la
			// consola: en el chat y los títulos, cada jugador ve el de su cliente (ver Idioma)
			Idioma.INGLES);

	public Configuracion conVariacion(int variacion) {
		return new Configuracion(bloquesPorNivel, nivelMaximo, extraNether, extraEnd, variacion, diasPorNivelExtra,
				nivelesPorTiempoMaximos, chanceDeCampeon, nivelesDeCampeon, escalarSpawners, nombreSiempreVisible, vidaEnElNombre, vida, danio, armadura, velocidad, rangoDeSeguimiento, resistenciaAlEmpuje,
				nivelRadioCreeper, radioCreeperExtra, chanceArmaduraExtra, nivelesPorMaterialDeArmadura,
				cascoParaLosQueSeQueman, poderDeEncantamiento, chanceSoltarEquipo, nivelesPorTiradaExtra,
				tiradasExtraMaximas, chanceTiradaExtra, botinRaro, botinEpico, botinLegendario, experienciaPorNivel,
				afijos, estampida, lunaDeSangre, jefeDeZona, idioma);
	}

	public Configuracion conExtras(int diasPorNivelExtra, int nivelesPorTiempoMaximos, double chanceDeCampeon, int nivelesDeCampeon) {
		return new Configuracion(bloquesPorNivel, nivelMaximo, extraNether, extraEnd, variacion, diasPorNivelExtra,
				nivelesPorTiempoMaximos, chanceDeCampeon, nivelesDeCampeon, escalarSpawners, nombreSiempreVisible, vidaEnElNombre,
				vida, danio, armadura, velocidad, rangoDeSeguimiento, resistenciaAlEmpuje,
				nivelRadioCreeper, radioCreeperExtra, chanceArmaduraExtra, nivelesPorMaterialDeArmadura,
				cascoParaLosQueSeQueman, poderDeEncantamiento, chanceSoltarEquipo, nivelesPorTiradaExtra,
				tiradasExtraMaximas, chanceTiradaExtra, botinRaro, botinEpico, botinLegendario, experienciaPorNivel,
				afijos, estampida, lunaDeSangre, jefeDeZona, idioma);
	}

	/** Los valores que harían dividir por cero o dejarían un mob sin nivel: mejor rechazar el archivo que arrancar así. */
	public void validar() {
		exigir(bloquesPorNivel >= 1, "bloquesPorNivel tiene que ser 1 o más");
		exigir(nivelMaximo >= 1, "nivelMaximo tiene que ser 1 o más");
		exigir(variacion >= 0, "variacion no puede ser negativa");
		exigir(diasPorNivelExtra >= 0, "diasPorNivelExtra no puede ser negativo (0 lo desactiva)");
		exigir(nivelesPorTiempoMaximos >= 0, "nivelesPorTiempoMaximos no puede ser negativo");
		exigir(chanceDeCampeon >= 0 && chanceDeCampeon <= 1, "chanceDeCampeon va de 0 a 1");
		exigir(nivelesDeCampeon >= 0, "nivelesDeCampeon no puede ser negativo");
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
		exigir(afijos != null && estampida != null && lunaDeSangre != null && jefeDeZona != null, "falta uno de los eventos");
		exigir(afijos.chance() >= 0 && afijos.chance() <= 1, "afijos.chance va de 0 a 1");
		exigir(afijos.chanceTeletransporte() >= 0 && afijos.chanceTeletransporte() <= 1, "afijos.chanceTeletransporte va de 0 a 1");
		exigir(afijos.ticksEntreParticulas() >= 1, "afijos.ticksEntreParticulas tiene que ser 1 o más");
		exigir(afijos.escala() >= 0 && jefeDeZona.escala() >= 0, "afijos.escala y jefeDeZona.escala no pueden ser negativas (0 = tamaño normal)");
		exigir(afijos.nivelVeneno() >= 1 && afijos.nivelCongelante() >= 1, "afijos.nivelVeneno y nivelCongelante tienen que ser 1 o más (1 = nivel I)");
		exigir(estampida.chancePorMinuto() >= 0 && estampida.chancePorMinuto() <= 1, "estampida.chancePorMinuto va de 0 a 1");
		exigir(estampida.nivelesPorMobExtra() >= 1, "estampida.nivelesPorMobExtra tiene que ser 1 o más");
		exigir(estampida.mobsBase() >= 1, "estampida.mobsBase tiene que ser 1 o más");
		exigir(estampida.distanciaMinima() >= 1 && estampida.distanciaMaxima() >= estampida.distanciaMinima(),
				"estampida.distanciaMaxima tiene que ser mayor o igual que distanciaMinima, y las dos 1 o más");
		exigir(estampida.segundosMaximos() >= 1, "estampida.segundosMaximos tiene que ser 1 o más");
		exigir(lunaDeSangre.chance() >= 0 && lunaDeSangre.chance() <= 1, "lunaDeSangre.chance va de 0 a 1");
		exigir(lunaDeSangre.chanceDeCompania() >= 0 && lunaDeSangre.chanceDeCompania() <= 1, "lunaDeSangre.chanceDeCompania va de 0 a 1");
		exigir(jefeDeZona.chancePorMinuto() >= 0 && jefeDeZona.chancePorMinuto() <= 1, "jefeDeZona.chancePorMinuto va de 0 a 1");
		exigir(jefeDeZona.multiplicadorDeVida() >= 1, "jefeDeZona.multiplicadorDeVida tiene que ser 1 o más");
		exigir(jefeDeZona.radioDeBarra() >= 1 && jefeDeZona.segundosAbandonado() >= 1, "jefeDeZona.radioDeBarra y segundosAbandonado tienen que ser 1 o más");
		exigir(jefeDeZona.vidaFuria() >= 0 && jefeDeZona.vidaFuria() <= 1, "jefeDeZona.vidaFuria va de 0 a 1 (0 la apaga)");
		exigir(jefeDeZona.segundosEntreHabilidades() >= 1, "jefeDeZona.segundosEntreHabilidades tiene que ser 1 o más");
		exigir(jefeDeZona.radioOnda() >= 1 && jefeDeZona.radioTiron() >= jefeDeZona.radioOnda(),
				"jefeDeZona.radioTiron tiene que ser mayor o igual que radioOnda, y los dos 1 o más");
		exigir(Idioma.DISPONIBLES.contains(idioma), "idioma tiene que ser " + String.join(" o ", Idioma.DISPONIBLES));
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

	/**
	 * Rasgos de los campeones. chance: probabilidad de que un campeón tenga uno. velocidadVeloz y curacionVampiro son
	 * fracciones (0.3 = +30 % de velocidad, 0.5 = se cura la mitad del daño que hace). armaduraBlindado y
	 * empujeBlindado se suman a los atributos. nivelVeneno y nivelCongelante son el nivel del efecto (2 = II). particulas:
	 * si los campeones sueltan la partícula de su afijo, cada ticksEntreParticulas. escala: cuánto más grandes son
	 * (0.15 = +15 %); el lugar donde aparecen lo elige vanilla con el tamaño normal, así que pueden quedar trabados.
	 */
	public record Afijos(double chance, double velocidadVeloz, double curacionVampiro, float potenciaExplosivo, int ticksDeMecha,
			int refuerzosInvocador, int invocacionesMaximas, int segundosEntreInvocaciones, double armaduraBlindado, double empujeBlindado,
			int segundosVeneno, int nivelVeneno, int segundosCongelante, int nivelCongelante, double chanceTeletransporte,
			int segundosEntreTeletransportes, boolean particulas, int ticksEntreParticulas, double escala) {
	}

	/** Los días son de juego (20 minutos reales), contados con el gameTime: dormir no los adelanta. */
	public record Estampida(boolean activa, double chancePorMinuto, int diasEntreEstampidas, int nivelMinimo, int mobsBase,
			int nivelesPorMobExtra, int mobsMaximos, int distanciaMinima, int distanciaMaxima, double velocidadExtra, int segundosMaximos) {
	}

	/** chance: probabilidad de que una noche de luna llena sea de sangre. */
	public record LunaDeSangre(boolean activa, double chance, int nivelesExtra, double chanceDeCompania) {
	}

	/**
	 * vidaFuria: con qué fracción de la vida se enfurece (0 = nunca). velocidadFuria y danioFuria son fracciones que se
	 * suman. danioOnda es en golpes del jefe (1.0 = lo que pega con la mano); fuerzaOnda, cuánto empuja. escala: cuánto
	 * más grande es (0.4 = +40 %), sumado a la escala del afijo que tenga.
	 */
	public record JefeDeZona(boolean activo, double chancePorMinuto, int diasEntreJefes, int nivelMinimo, int nivelesExtra,
			double multiplicadorDeVida, int escoltas, int radioDeBarra, int segundosAbandonado, double vidaFuria, double velocidadFuria,
			double danioFuria, int escoltasFuria, int segundosEntreHabilidades, int radioOnda, double danioOnda, double fuerzaOnda,
			int radioTiron, boolean contorno, double escala) {
	}
}
