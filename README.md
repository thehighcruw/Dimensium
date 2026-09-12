# Dimensium

A builder-wand mod for Minecraft 1.7.10, inspired by [Axiom](https://modrinth.com/mod/axiom). Dimensium brings powerful in-game world editing tools — selection, clipboard, shape placement, brush operations — to the GTNH modpack ecosystem.

> **Warning:** Dimensium can place or destroy thousands of blocks at once. Use it **only in creative mode** and keep **frequent backups** of your world. One misclick can cause irreversible terrain damage. The authors are not responsible for lost progress.

---

## Features

- Selection box with clipboard (copy / paste / fill)
- Brush tool with configurable shape and radius
- Shape placement (sphere, cylinder, etc.)
- Noise brush for organic terrain
- ImGui-based floating panel UI
- Full client–server authority: all block operations go through server-side packets

---

## Requirements

| Mod | Version |
|-----|---------|
| Minecraft Forge | 1.7.10-10.13.x |
| [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) | 0.11.35+ |
| [NotEnoughItems (GTNH fork)](https://github.com/GTNewHorizons/NotEnoughItems) | 2.8.118-GTNH+ |

GTNHLib and NEI are required at runtime. If you are running a GT: New Horizons modpack, both are already included.

---

## Installation

1. Drop the Dimensium jar into your `mods/` folder.
2. Ensure GTNHLib and NEI are present.
3. Launch in creative mode. **Do not use on survival worlds without a backup.**

---

## Building from source

```bash
./gradlew classes      # compile check
./gradlew test         # run unit tests
./gradlew runClient    # launch the game
./gradlew jar          # build distributable jar
```

Requires JDK 8 (Azul Zulu recommended). The build will automatically download ImGui natives on first run.

---

## License

Source available. No license file yet — contact the author before redistributing.
