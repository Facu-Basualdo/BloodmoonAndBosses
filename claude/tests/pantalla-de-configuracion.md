# Tests de la pantalla de configuración

[← Índice Tests](INDICE.md)

`ConfiguracionClienteTest` (entrypoint `fabric-client-gametest`, `gradlew runClientGameTest`): un cliente de verdad con un mundo singleplayer. **Abre la pantalla con `context.setScreen`**, porque Mod Menu no está en el entorno de prueba: la integración con Mod Menu (el entrypoint) no se prueba acá. Deja capturas en `build/run/clientGameTest/screenshots`.

## Qué cubre

- *Bloques por nivel* en 80 se guarda y `BloodmoonAndBosses.config().bloquesPorNivel()` del servidor integrado da 80.
- *Probabilidad de campeón* en 5: `validar()` lo rechaza (el `ERROR` «chanceDeCampeon va de 0 a 1» del log es esperado). La pantalla queda abierta, el archivo sigue con `0.05` y con el 80, y la config también.

## Gotchas

- **El cliente de prueba arranca en inglés** (`lang:en_us`): el test pone `getLanguageManager().setSelected("es_ar")` antes de crear el mundo para buscar los campos por su nombre en español. Eso no recarga los recursos de vanilla: en las capturas **Cancel** y **ON** siguen en inglés, y los nombres de los mobs también.
- **Los campos no están en `screen.children()` directamente**: están adentro del contenedor de `ScrollableLayout`. El test recorre los `ContainerEventHandler` y filtra los `EditBox` por `getMessage()` (el nombre de la fila). `clickScreenButton("Guardar")` sí lo encuentra, porque está en el pie.
- **`CycleButton.onPress` necesita un `InputWithModifiers` de verdad** (`new KeyEvent(InputConstants.KEY_RETURN, 0, 0)`): con null revienta en `hasShiftDown()`. Los `CycleButton` se buscan por su valor (`getValue()`), no por posición: los sí/no también son `CycleButton`.
- **`runClientGameTest` borra `build/run/clientGameTest/config` antes de arrancar**: el test deja valores cambiados, y la corrida siguiente tiene que empezar con los de por defecto.

## Ver también

- [Mod: pantalla de configuración](../mod/pantalla-de-configuracion.md)
