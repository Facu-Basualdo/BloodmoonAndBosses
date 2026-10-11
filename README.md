# Mobs Poderosos

Mod de Fabric para **Minecraft Java 26.3** en el que los mobs hostiles aparecen con **niveles**. Cuanto más lejos del spawn del mundo y cuanto más viejo es el mundo, más nivel tienen: más vida, más daño, más armadura y más velocidad, y aparecen con mejor equipo encantado. El nivel se ve en el nombre del mob: **"Zombi [Nv 7]"**. Los mobs de más nivel también sueltan más loot, ítems raros y más experiencia.

Cerca de la base el juego se siente casi como vanilla; a varios miles de bloques, salir a explorar es otra cosa.

> **Estado:** versión 0.7.0. Están todas las fases de [docs/plan.md](docs/plan.md): niveles, loot y experiencia, equipo encantado, ítems raros y archivo de configuración.

## Cómo funciona en el juego

| Nivel (dónde) | Vida de un zombi (base 20) | Daño sin arma (base 3) | Armadura extra | Loot | Experiencia |
|---|---|---|---|---|---|
| 1 (en el spawn) | 21,6 | 3,1 | +0,1 | normal | ×1,05 |
| 20 (~1900 bloques) | 52 | 5,4 | +2,4 | ×2 | ×2 |
| 50 (~4900 bloques) | 100 | 9 | +6 | ×4 | ×3,5 |
| 80 (~7900 bloques) | 148 | 12,6 | +9,6 | ×6 | ×5 |
| 100 (máximo, ~9900 bloques) | 180 | 15 | +12 | ×7 | ×6 |

- **El nivel sube 1 cada 100 bloques** desde el spawn del mundo, con una variación de ±1 para que no sean todos iguales.
- **Con el tiempo:** cada 5 días del mundo, los mobs que aparecen suman 1 nivel, hasta +20 a los 100 días, en cualquier zona. Cuentan los días del reloj del mundo (los que muestra F3): dormir los adelanta, y `/time set` los puede atrasar. Los mobs que ya estaban no cambian.
- **Campeones:** uno de cada 20 mobs (5 %) aparece con 10 niveles más que su zona. No tiene otra marca: se nota en el nivel y el color del nombre, así que conviene mirarlo antes de pelear.
- La distancia, el tiempo y los campeones se suman, y todo se corta en el nivel 100. La tabla de arriba es sin el tiempo ni los campeones.
- **Todo sigue subiendo hasta el nivel 100:** ninguna estadística llega a su tope antes, así que alejarse siempre se nota.
- **Pensado para jugarse con Estadísticas RPG:** con el equipo y los puntos que se esperan en cada zona, un zombi se mata en 5 o 6 golpes en todo el mundo, y lo que aguantás va bajando hacia el nivel 100. Sin ese mod, la zona de nivel 100 es para pocos: un zombi aguanta 19 golpes de una espada de netherita con Filo V.
- **Nether:** cada bloque cuenta como 8 (como los portales) y suma 20 niveles. **End:** se mide desde el centro de la isla principal y suma 40.
- **El color del nombre** indica el rango, por quintos del nivel máximo: gris (1–19), verde (20–39), azul (40–59), violeta (60–79), dorado (80+).
- **El nombre se ve al mirar al mob**, no a través de las paredes.
- **Creepers de nivel 60 o más** explotan con un radio más grande.
- **Equipo:** zombis, esqueletos, piglins y sus variantes pueden aparecer con armadura extra. La probabilidad de cada pieza es 0,75 % por nivel (hasta 50 %, desde el nivel 67) y el material depende del nivel:

  | Nivel | Material |
  |---|---|
  | 1–19 | cuero |
  | 20–39 | cadena u oro |
  | 40–59 | hierro |
  | 60–79 | diamante |
  | 80+ | netherita |

  - **Los que se queman al sol no reciben casco** (zombis, esqueletos): si no, sobrevivirían de día. El casco que les pone vanilla no se toca.
  - **Las armas y la armadura salen encantadas** según el nivel (en nivel 67, como una mesa de encantamiento de nivel 20). Con un arco, eso le da Poder: así escala el daño de los esqueletos.
  - **Es más fácil que suelten su equipo:** +0,2 % por nivel sobre la chance de vanilla, hasta 25 %. El equipo de diamante o netherita y los tridentes siguen con la chance de vanilla (8,5 %), y lo que vanilla no suelta nunca (la espada de los vex) sigue sin soltarse. Como en vanilla, sólo sueltan el equipo si los mataste vos.
- **Loot:** cada 16 niveles el mob tira su loot de vanilla una vez más (hasta 6 veces extra, desde el nivel 96). **Sólo si lo matás vos**: a un mob que muere en una granja automática, por lava o por caída sin que lo hayas golpeado, le toca el loot normal. El equipo que lleva puesto no se multiplica.
- **Evocadores:** también tienen tiradas extra, pero cada una sale **por probabilidad**: 0,5 % por nivel, hasta 50 %. Un evocador de nivel 60 tiene 3 tiradas extra al 30 % cada una (en promedio, casi un tótem extra); uno de nivel 100 tiene 6 al 50 % (3 tótems extra en promedio). Está en el tag `mobspoderosos:tiradas_extra_por_chance`.
- **Sin loot extra:** esqueletos del Wither (las calaveras serían triviales) y shulkers. Están en el tag `mobspoderosos:sin_tiradas_extra`.
- Los dos tags se pueden cambiar con un datapack.
- **Ítems raros:** además de su loot, un mob con nivel que matás vos puede soltar un ítem de una de estas tablas. Cada una se sortea por separado:

  | Tabla | Desde el nivel | Probabilidad | Qué puede dar |
  |---|---|---|---|
  | Rara | 20 | 15 % | manzana dorada, diamantes, esmeraldas, hierro, libro encantado, botellas de experiencia, perlas de ender, etiqueta |
  | Épica | 50 | 10 % | diamantes, manzanas doradas, libro encantado de nivel 30, espada o pico de diamante encantados, chatarra de netherita, plantilla de mejora de netherita |
  | Legendaria | 80 | 5 % | manzana dorada encantada, lingote de netherita, plantilla de mejora de netherita, armadura de diamante con encantamientos altos, diamantes |

  Las tablas son `mobspoderosos:botin_por_nivel/raro`, `epico` y `legendario`: se pueden cambiar con un datapack. También aplican a los esqueletos del Wither y los shulkers.
- **Experiencia:** +5 % por nivel. Como en vanilla, sólo se da si lo mató un jugador.
- Con `/gamerule mobDrops false` no hay loot extra, igual que no hay loot normal.
- **Qué mobs tienen nivel:** los hostiles que aparecen solos en el mundo, en estructuras (mansiones, fortalezas, ciudades del End…), en patrullas y en raids. **No tienen nivel** los de spawners (para que las granjas no cambien), los de huevos o comandos, ni los jefes (dragón, wither, guardián anciano, warden).
- **Conversiones:** un zombi que se ahoga sigue con su nivel ("Ahogado [Nv 7]"), los slimes chicos heredan el nivel del grande, y un aldeano zombi curado vuelve a ser un aldeano normal.
- **Name tags:** si le ponés un name tag a un mob con nivel, el nombre pasa a ser el tuyo (se pierde el "[Nv N]"), pero sigue igual de fuerte.
- Los mobs con nivel desaparecen igual que en vanilla: el nombre no los vuelve permanentes.

### Comandos

| Comando | Qué hace |
|---|---|
| `/mobspoderosos zona` | Muestra el nivel de los mobs en la zona donde estás: lo que pone la distancia, lo que suman los días del mundo y la chance de campeón |
| `/mobspoderosos invocar <entidad> <nivel>` | Aparece un mob con ese nivel (y su equipo) delante tuyo (requiere ser operador) |
| `/mobspoderosos recargar` | Vuelve a leer `config/mobspoderosos.json` sin reiniciar el servidor (requiere ser operador) |

### Configuración

En el primer arranque se crea **`config/mobspoderosos.json`** con todos los valores por defecto. Editalo y usá `/mobspoderosos recargar`, o reiniciá el servidor.

- **Al recargar, los mobs que ya están no cambian:** la vida, el daño, la armadura, el nombre y el equipo se ponen cuando el mob aparece. El loot, los ítems raros y la experiencia se calculan al morir, así que esos cambios valen enseguida.
- **Si el archivo tiene un error** (JSON mal escrito, o un valor imposible como `bloquesPorNivel: 0`), se queda la configuración que había, el archivo no se toca y el error aparece en el chat y en la consola.
- Si falta una clave se usa su valor por defecto, y al leer el archivo se reescribe completo. Una clave que no existe se ignora con un aviso en la consola.
- Las escalas tienen la forma `{"porNivel": 0.08, "tope": 8.0}`: lo que suma cada nivel y el máximo.

| Clave | Por defecto | Qué hace |
|---|---|---|
| `bloquesPorNivel` | 100 | Cada cuántos bloques del spawn sube un nivel |
| `nivelMaximo` | 100 | Nivel máximo |
| `extraNether` / `extraEnd` | 20 / 40 | Niveles que se suman en el Nether y el End |
| `variacion` | 1 | Variación al azar del nivel (±) |
| `diasPorNivelExtra` / `nivelesPorTiempoMaximos` | 5 / 20 | Cada cuántos días del mundo los mobs suman un nivel, y el máximo que suma el tiempo (`diasPorNivelExtra` en 0 lo apaga) |
| `chanceDeCampeon` / `nivelesDeCampeon` | 0.05 / 10 | Probabilidad de que un mob aparezca como campeón (de 0 a 1), y cuántos niveles extra tiene |
| `escalarSpawners` | `false` | Si los mobs de spawners también tienen nivel |
| `nombreSiempreVisible` | `false` | Si el nombre se ve a través de las paredes |
| `vida`, `danio`, `velocidad`, `rangoDeSeguimiento` | +8 %, +4 %, +0,2 %, +0,5 % por nivel | Porcentaje sobre la base, por nivel y tope (`0.08` = +8 %) |
| `armadura`, `resistenciaAlEmpuje` | +0,12 / +0,005 por nivel | Puntos que se suman, por nivel y tope |
| `nivelRadioCreeper` / `radioCreeperExtra` | 60 / 1 | Desde qué nivel y cuánto crece la explosión del creeper |
| `chanceArmaduraExtra` | 0,75 % por nivel, tope 50 % | Probabilidad de cada pieza de armadura extra |
| `nivelesPorMaterialDeArmadura` | 20 | Cada cuántos niveles mejora el material |
| `cascoParaLosQueSeQueman` | `false` | Si zombis y esqueletos también pueden recibir casco |
| `poderDeEncantamiento` | 0,3 por nivel, tope 30 | Nivel de encantamiento del equipo (como el de la mesa) |
| `chanceSoltarEquipo` | +0,2 % por nivel, tope 25 % | Lo que se suma a la chance de vanilla de soltar el equipo |
| `nivelesPorTiradaExtra` / `tiradasExtraMaximas` | 16 / 6 | Cada cuántos niveles el loot sale una vez más, y el máximo |
| `chanceTiradaExtra` | 0,5 % por nivel, tope 50 % | Probabilidad de cada tirada extra de los mobs de `tiradas_extra_por_chance` |
| `botinRaro`, `botinEpico`, `botinLegendario` | ver arriba | `{"nivelMinimo": 20, "chance": 0.15}`: desde qué nivel y con qué probabilidad se tira cada tabla |
| `experienciaPorNivel` | 0.05 | Experiencia extra por nivel (`0.05` = +5 %) |

## Requisitos

| | Versión |
|---|---|
| Minecraft Java | 26.3 |
| [Fabric Loader](https://fabricmc.net/use/) | 0.19.5 o más nuevo |
| [Fabric API](https://modrinth.com/mod/fabric-api) | para 26.3 |
| Java | 25 |

## Instalación

Toda la lógica corre en el servidor.

- **Servidor dedicado:** copiá `mobspoderosos-<versión>.jar` y Fabric API a la carpeta `mods/` del servidor. Los jugadores no necesitan instalar nada: el nombre con el nivel lo ve cualquier cliente.
- **Singleplayer o LAN:** instalalo en el cliente del que abre el mundo, junto con Fabric API.
- Se puede usar junto con **Inventario Compartido**: no se tocan.

## Compilar

```bash
./gradlew build          # Windows: gradlew build
```

El jar queda en `build/libs/`.

El proyecto pide Java 25 (`gradle/gradle-daemon-jvm.properties`). Si Gradle no encuentra tu JDK 25, ajustá `org.gradle.java.installations.paths` en `gradle.properties` con la ruta donde lo tengas instalado.

## Probar

```bash
./gradlew runGameTest    # pruebas automáticas en un servidor sin ventana
```

Las pruebas verifican:

- la fórmula del nivel en las tres dimensiones, el nivel extra por los días del mundo y la chance de campeón;
- las estadísticas y sus topes;
- el radio de los creepers;
- qué motivos de spawn dan nivel y cuáles no;
- que los mobs de estructuras reciban nivel al cargarse el chunk;
- que el nivel sobreviva a guardar y cargar el mundo;
- las conversiones (zombi a ahogado, aldeano curado, slimes divididos);
- el loot extra (sólo si lo mata un jugador, por probabilidad para el evocador, y nada para los mobs de `sin_tiradas_extra`) y la experiencia por nivel;
- la armadura por nivel (material, sin casco para los que se queman, sólo para los mobs que la dibujan), los encantamientos y la chance de soltar el equipo;
- que las tablas de ítems raros carguen y se tiren sólo desde su nivel y si mató un jugador;
- la lectura del archivo de configuración (valores por defecto, claves que faltan, archivos inválidos).

Para probar a mano, mirá [claude/general/probar-en-el-juego.md](claude/general/probar-en-el-juego.md).

## Estructura

```
src/main/       el mod: marca el mob al aparecer (mixin en finalizeSpawn), le aplica el nivel y el equipo cuando entra al mundo, lo pasa en las conversiones y multiplica el loot y la experiencia al morir
src/gametest/   pruebas automáticas (Fabric GameTest)
claude/         documentación técnica para agentes de IA (invariantes, decisiones y bugs ya resueltos)
docs/           el plan del mod por fases
```

Para entender por qué el código está hecho así, empezá por [CLAUDE.md](CLAUDE.md) y [claude/mod/INDICE.md](claude/mod/INDICE.md).

## Licencia

[MIT](LICENSE): podés usarlo, modificarlo y redistribuirlo (también en modpacks) siempre que mantengas el aviso de copyright.
