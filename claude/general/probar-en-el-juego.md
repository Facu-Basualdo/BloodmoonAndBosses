# Probar en el juego

[← Hub](../../CLAUDE.md)

Los gametests ([tests: niveles](../tests/niveles.md)) cubren la lógica, pero no muestran cómo se ve el nombre en el cliente, ni si un mob de nivel 80 es divertido o injusto. Para eso hace falta un servidor y un cliente.

## Pasos

1. `gradlew runServer` una vez. Crea `run/` y se cierra pidiendo la EULA.
2. En `run/eula.txt` poné `eula=true` (es aceptar la EULA de Mojang: lo hace la persona, no el agente).
3. En `run/server.properties` poné **`online-mode=false`**: los clientes de desarrollo no tienen cuenta autenticada y sin esto el servidor los rechaza.
4. `gradlew runServer` de nuevo, y `op Player…` en la consola del servidor para poder usar `/mobspoderosos invocar`.
5. `gradlew runClient` (y `gradlew runClient2` si querés dos jugadores) → *Multijugador → Conexión directa → `localhost`*.

**Después de cambiar código hay que reiniciar el cliente y el servidor:** no hay hot reload de mixins.

`run/` y `run-client2/` están en `.gitignore`.

## Comandos

| Comando | Para qué |
|---|---|
| `/mobspoderosos zona` | Nivel de la zona donde estás (sin la variación de ±1), con lo que suman los días del mundo |
| `/mobspoderosos invocar <entidad> <nivel> [afijo]` | Aparece el mob delante tuyo con ese nivel y su equipo (y el afijo, si va): evita caminar miles de bloques. Requiere op |
| `/mobspoderosos evento estampida\|jefe\|luna_de_sangre` | Larga el evento sin esperar el sorteo. La luna, sólo de noche (`/time set night`). Requiere op |
| `/mobspoderosos recargar` | Vuelve a leer `run/config/mobspoderosos.json`. Requiere op. Los mobs que ya están no cambian de estadísticas ni de equipo |
| `/tp @s 4500 ~ 0` | Ir a una zona de nivel ~46 (más lo que sume el tiempo) para ver los spawns naturales |

## Qué mirar

- Que el nombre aparezca **al mirar** al mob (por defecto no se ve a través de las paredes) y con el color del rango.
- La vida con F3 + daño o con `/data get entity @e[limit=1,sort=nearest] Health`.
- Que un creeper de nivel 60+ rompa un radio más grande.
- Ahogar un zombi con nivel: el ahogado tiene que decir "Ahogado [Nv N]", no "Zombi".
- Curar un aldeano zombi con nivel: el aldeano queda sin nombre.
- `/mobspoderosos invocar skeleton 45`: armadura de netherita encantada **sin casco**, y el arco encantado.
- Matar mobs de nivel 80+ y ver que a veces sueltan ítems raros; matarlos con `/kill` y ver que no.
- `/mobspoderosos invocar zombie 30 invocador` y pegarle: llama dos zombis; `… explosivo` y matarlo: suena la mecha y explota un segundo y medio después, sin romper bloques.
- `/mobspoderosos evento estampida` al aire libre: cuerno a lo lejos, barra roja, todos llegan del mismo lado. Matarlos con la espada da el premio en el inventario.
- `/mobspoderosos evento jefe` en una cueva: aparece en la cueva, con barra violeta; alejarse más de 48 bloques un minuto lo hace desaparecer.
- `/time set night` y `/mobspoderosos evento luna_de_sangre`: título, barra que se vacía, `/mobspoderosos zona` avisa +10; `/time set day` la termina.
- Editar `run/config/mobspoderosos.json` con un error (por ejemplo `"bloquesPorNivel": 0`), `/mobspoderosos recargar`: tiene que fallar con el motivo en el chat y el archivo tiene que quedar como estaba.
- **Con un cliente vanilla sin el mod:** todo lo anterior tiene que verse igual. El mod no tiene que hacer falta del lado del cliente.

## Ver también

- [Entorno Java y Gradle](entorno-java-gradle.md)
- [Niveles](../mod/niveles.md)
- [Configuración](../mod/configuracion.md)
