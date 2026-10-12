# Idioma de los textos

[← Índice Mod](INDICE.md)

**Hay dos clases de texto y se traducen distinto.** Si un jugador ve `bloodmoonandbosses.jefe.titulo` en vez de "Jefe de zona", el texto se armó con `Component.translatable` sin respaldo. Si un aviso le llega en otro idioma, se armó una vez para todos en vez de una por jugador.

| Texto | Cómo | Ejemplos |
|---|---|---|
| Lo que le llega a un jugador | `Idioma.tr(jugador, …)`: lo traduce el servidor en el idioma de su cliente (`clientInformation().language()`) | Títulos de estampida, jefe y luna de sangre, avisos en la barra de acción, mensajes de `/bloodmoon`, la barra de la estampida (la ve sólo el perseguido) |
| Lo que ven todos a la vez | `Idioma.guardado(…)`: `translatableWithFallback`, con respaldo en el `idioma` de la config | El nombre de cada mob ("[Nv 7]", "[Vampiro]"), el " · Furia" de la barra del jefe, la barra de la luna de sangre |

**Un cliente vanilla ve el respaldo; uno con el mod, su idioma.** El tipo del mob ("Zombi") es de vanilla y lo traduce siempre el cliente: con la config en inglés y un cliente vanilla en español se ve "Zombi [Lv 7]". Para un servidor de habla hispana, `"idioma": "es_ar"`.

**Un aviso para varios jugadores se arma dentro del bucle**, uno por jugador: `JefeDeZona.avisar` recibe la clave y el color, no un `Component`. Lo mismo `alMorir` del jefe.

Los errores que vuelven a un comando (`Estampida.empezar`) son `Idioma.Mensaje` (clave y argumentos), no texto: se traducen recién en el comando, en el idioma de quien lo corrió. El de `terminar` de la estampida, en el del jugador perseguido.

## El nombre de un mob se reconoce en cualquier idioma

**`esNombreDelMod` compara el nombre actual con el que armaría el mod, y ese nombre depende del idioma de la config.** Si sólo comparara con el del idioma actual, cambiar `idioma` (o actualizar desde la 0.13, cuando el nombre era texto fijo en español) haría que todos los mobs que ya estaban parezcan bautizados por un jugador: su nombre dejaría de seguir a la vida y las conversiones lo borrarían.

Por eso compara con:

| Forma | Desde |
|---|---|
| `nombre(idioma, …)` para cada idioma de `Idioma.DISPONIBLES` | 0.14.0: el tipo, un espacio y `translatableWithFallback("bloodmoonandbosses.nombre.nivel", respaldo, nivel)` |
| `nombreViejo(…)`: `tipo.copy().append(" [Nv N]")` y `Afijo.etiquetaVieja()` | Hasta la 0.13.x: texto fijo en español |

Al reconocerlo, `actualizarVida` lo reescribe con el formato y el idioma de ahora. `reconoceNombresDeOtroIdiomaYViejos` lo prueba con un nombre viejo, uno en español y un name tag.

## Archivos

`assets/bloodmoonandbosses/lang/en_us.json` y `es_ar.json`, con el formato de vanilla (`%s`, `%1$s`, `%%`): los respaldos los formatea el cliente. `Idioma` es la misma clase que la de Mythic Loot, más `Mensaje` y `guardado(idioma, …)` para reconocer nombres de otro idioma.

**Los ids no se traducen**: los de `Afijo` van en un attachment de cada mob, en la config y en `/bloodmoon invocar`.

`textosEnLosDosIdiomas` verifica que los dos archivos tengan las mismas claves y que cada afijo tenga nombre. Los tests que miran el nombre de un mob comparan contra `Idioma.guardado(…).getString()`: corren en el idioma de la config del servidor de prueba.

Los errores de la config (`Configuracion.validar`) siguen en español.

## Ver también

- [Niveles](niveles.md): cómo se arma y se actualiza el nombre.
- [Eventos y afijos](eventos.md)
- [Configuración](configuracion.md)
