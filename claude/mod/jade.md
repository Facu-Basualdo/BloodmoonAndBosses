# Compatibilidad con Jade

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **con Jade, la vida de un mob se ve sólo pegado a él**, **el mod no arranca en un cliente sin Jade**, o **Jade muestra un ítem tirado a 40 bloques**.

Jade solo, sin el mod en el cliente, ya muestra el nombre del mob ("Zombi [Nv 7] ❤ 34/40") y su vida: la vida es la sincronizada de vanilla (`LivingEntity.getHealth`/`getMaxHealth` en el cliente). Lo que no alcanza es la distancia: apunta hasta `entityInteractionRange` (3) + su *Reach distance* (`extendedReach`, tope +20). El plugin sólo estira ese alcance, para los mobs. **Es todo del lado del cliente**: no tiene `IServerDataProvider` ni le pide nada al servidor.

| Pieza | Qué hace |
|---|---|
| `compat/jade/PluginJade` | Entrypoint `jade` en `fabric.mod.json` (Jade lo busca ahí; `@WailaPlugin` es opcional). Registra la opción `bloodmoonandbosses:alcance` (48 por defecto, 0–128) y un callback de raytrace |
| `mixin/jade/RayTracingMixin` | `@ModifyArg` sobre el tercer argumento (`entityReach`) de `RayTracing.rayTrace(Entity, double, double)` dentro de `fire()`: `max(el de Jade, alcance)` |
| `compat/jade/MixinsDeJade` | Plugin de mixins: aplica `bloodmoonandbosses.jade.mixins.json` sólo si `isModLoaded("jade")` |

- **Hace falta el mixin: un `addRayTraceCallback` no alcanza.** `WailaTickHandler` corta antes de los callbacks si `RayTracing` no le pegó a nada (`target == null`), y mirar un mob a 40 bloques al aire libre es justamente eso.
- **Sólo el alcance contra entidades.** El de bloques queda como lo configuró el jugador: si no, Jade mostraría el pasto a 48 bloques. La pared en el medio la sigue resolviendo Jade (el clip `COLLIDER` hasta la entidad).
- **El callback descarta lo que no es `Mob` más allá del alcance normal de Jade** (devolver `null` vuelve al `EmptyAccessor`): ítems tirados, marcos, soportes de armadura y jugadores siguen con el alcance de siempre.
- **Sin Jade el mixin no puede cargar:** apunta a `snownee.jade.overlay.RayTracing` por nombre (`targets`, no `.class`), lleva `@Pseudo`, y el plugin de mixins lo saltea. La config va con `"environment": "client"` en `fabric.mod.json`, así que un servidor dedicado ni la mira. `PluginJade` sólo lo carga Jade.
- **La distancia real la limita el servidor**: el cliente sólo conoce las entidades que le manda (rango de rastreo de los monstruos y `entity-broadcast-range-percentage`). Por eso el tope es 128.
- **Más allá de unos 24 bloques Jade no le manda datos del servidor** (`Jade.isOutOfReach`, salvo al dueño de un singleplayer): no importa, la vida no los necesita.
- La opción no es un número de balance: es del jugador y vive en la config de plugins de Jade, no en `Configuracion`. Sus textos son `config.jade.plugin_bloodmoonandbosses` y `.alcance`/`.alcance_desc` en los dos lang (los lee Jade: `"plugin_" + id.toLanguageKey()`).

## Dependencia

`compileOnly "maven.modrinth:jade:${jade_version}"` desde el maven de Modrinth (`exclusiveContent` para el grupo `maven.modrinth`); `fabric.mod.json` lo pone en `suggests`. **No está en el runtime de desarrollo**: los gametests corren sin Jade. Para probar en `runClient` se copia el jar en `run/mods/`. Si Jade cambia `RayTracing.fire` o la firma de `rayTrace`, el `@ModifyArg` (`defaultRequire` 1) rompe el cliente al cargar la clase, recién al entrar a un mundo: después de subir `jade_version`, probalo entrando a un mundo.

## Ver también

- [Niveles](niveles.md): el nombre con la vida que muestra Jade.
- [Probar en el juego](../general/probar-en-el-juego.md)
