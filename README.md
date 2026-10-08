# Mobs Poderosos

Mod de Fabric para **Minecraft Java 26.3** en el que los mobs hostiles aparecen con **niveles**. Cuanto más lejos del spawn del mundo, más nivel tienen: más vida, más daño, más armadura y más velocidad. El nivel se ve en el nombre del mob: **"Zombi [Nv 7]"**. Los mobs de más nivel también sueltan más loot y más experiencia.

Cerca de la base el juego se siente casi como vanilla; a varios miles de bloques, salir a explorar es otra cosa.

> **Estado:** versión 0.2.0. Los niveles cambian las estadísticas, el nombre, el loot y la experiencia. El equipo encantado y los ítems raros están planificados en [docs/plan.md](docs/plan.md).

## Cómo funciona en el juego

| Nivel (dónde) | Vida de un zombi (base 20) | Daño sin arma (base 3) | Armadura extra | Loot | Experiencia |
|---|---|---|---|---|---|
| 1 (en el spawn) | 22 | 3,1 | +0,2 | normal | ×1,1 |
| 10 (~1350 bloques) | 40 | 4,2 | +2 | ×2 | ×2 |
| 25 (~3600 bloques) | 70 | 6 | +5 | ×4 | ×3,5 |
| 40 (~5850 bloques) | 100 | 6 | +8 | ×6 | ×5 |
| 50 (máximo, ~7350 bloques) | 100 | 6 | +8 | ×7 | ×6 |

- **El nivel sube 1 cada 150 bloques** desde el spawn del mundo, con una variación de ±1 para que no sean todos iguales.
- **Nether:** cada bloque cuenta como 8 (como los portales) y suma 10 niveles. **End:** se mide desde el centro de la isla principal y suma 20.
- **El color del nombre** indica el rango: gris (1–9), verde (10–19), azul (20–29), violeta (30–39), dorado (40+).
- **El nombre se ve al mirar al mob**, no a través de las paredes.
- **Creepers de nivel 30 o más** explotan con un radio más grande.
- **Loot:** cada 8 niveles el mob tira su loot de vanilla una vez más (hasta 6 veces extra, desde el nivel 48). **Sólo si lo matás vos**: a un mob que muere en una granja automática, por lava o por caída sin que lo hayas golpeado, le toca el loot normal. El equipo que lleva puesto no se multiplica.
- **Evocadores:** también tienen tiradas extra, pero cada una sale **por probabilidad**: 1 % por nivel, hasta 50 %. Un evocador de nivel 30 tiene 3 tiradas extra al 30 % cada una (en promedio, casi un tótem extra); uno de nivel 50 tiene 6 al 50 % (3 tótems extra en promedio). Está en el tag `mobspoderosos:tiradas_extra_por_chance`.
- **Sin loot extra:** esqueletos del Wither (las calaveras serían triviales) y shulkers. Están en el tag `mobspoderosos:sin_tiradas_extra`.
- Los dos tags se pueden cambiar con un datapack.
- **Experiencia:** +10 % por nivel. Como en vanilla, sólo se da si lo mató un jugador.
- Con `/gamerule mobDrops false` no hay loot extra, igual que no hay loot normal.
- **Qué mobs tienen nivel:** los hostiles que aparecen solos en el mundo, en estructuras (mansiones, fortalezas, ciudades del End…), en patrullas y en raids. **No tienen nivel** los de spawners (para que las granjas no cambien), los de huevos o comandos, ni los jefes (dragón, wither, guardián anciano, warden).
- **Conversiones:** un zombi que se ahoga sigue con su nivel ("Ahogado [Nv 7]"), los slimes chicos heredan el nivel del grande, y un aldeano zombi curado vuelve a ser un aldeano normal.
- **Name tags:** si le ponés un name tag a un mob con nivel, el nombre pasa a ser el tuyo (se pierde el "[Nv N]"), pero sigue igual de fuerte.
- Los mobs con nivel desaparecen igual que en vanilla: el nombre no los vuelve permanentes.

### Comandos

| Comando | Qué hace |
|---|---|
| `/mobspoderosos zona` | Muestra el nivel de los mobs en la zona donde estás |
| `/mobspoderosos invocar <entidad> <nivel>` | Aparece un mob con ese nivel delante tuyo (requiere ser operador) |

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

- la fórmula del nivel en las tres dimensiones;
- las estadísticas y sus topes;
- el radio de los creepers;
- qué motivos de spawn dan nivel y cuáles no;
- que los mobs de estructuras reciban nivel al cargarse el chunk;
- que el nivel sobreviva a guardar y cargar el mundo;
- las conversiones (zombi a ahogado, aldeano curado, slimes divididos);
- el loot extra (sólo si lo mata un jugador, por probabilidad para el evocador, y nada para los mobs de `sin_tiradas_extra`) y la experiencia por nivel.

Para probar a mano, mirá [claude/general/probar-en-el-juego.md](claude/general/probar-en-el-juego.md).

## Estructura

```
src/main/       el mod: marca el mob al aparecer (mixin en finalizeSpawn), le aplica el nivel cuando entra al mundo, lo pasa en las conversiones y multiplica el loot y la experiencia al morir
src/gametest/   pruebas automáticas (Fabric GameTest)
claude/         documentación técnica para agentes de IA (invariantes, decisiones y bugs ya resueltos)
docs/           el plan del mod por fases
```

Para entender por qué el código está hecho así, empezá por [CLAUDE.md](CLAUDE.md) y [claude/mod/INDICE.md](claude/mod/INDICE.md).

## Licencia

[MIT](LICENSE): podés usarlo, modificarlo y redistribuirlo (también en modpacks) siempre que mantengas el aviso de copyright.
