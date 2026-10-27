# Nudge

A subtle, client-side aim assist for Fabric. Instead of snapping your camera onto a target, Nudge applies a small logarithmic pull toward a randomized point in the target's hitbox, nudging your crosshair closer each tick.

Built for **Minecraft 26.3** on the Fabric mod loader. Client-side only — it does not need to be installed on a server.

## How it works

Every client tick, Nudge runs three steps:

1. **Target selection** (`AimAssistLogic.findBestTarget`) — scans all players in the current world, skipping yourself, dead players, and invisible players. Each candidate is rejected if it is beyond `maxRange` or outside the `fovThreshold` cone around your view direction. Among the survivors, the one with the smallest angular offset from your crosshair wins.
2. **Aim point selection** (`AimAssistLogic.getRandomHitboxPoint`) — picks a random point inside the target's bounding box using a Gaussian distribution biased toward center mass rather than a fixed lock-on point. The point is clamped to stay inside the hitbox, and is re-rolled when you switch targets or every 10 ticks.
3. **Logarithmic nudge** (`AimAssistLogic.applyLogNudge`) — the angular delta to the aim point is scaled by `log(1 + delta) * strength`, so large errors produce a firm pull while small errors settle gently and never overshoot. Corrections are capped at the remaining delta and ignored below 0.05°.

Rotation changes are written directly to the player's yaw and pitch each tick.

## Configuration

Settings are stored in `config/nudge_config.json` inside your Minecraft instance. The file is created with defaults on first launch. If Cloth Config and Mod Menu are installed, the same values can be edited in-game through the Mod Menu settings screen.

- **Max Range** (`maxRange`, default `4.5`) — detection radius in blocks. Candidates beyond this are ignored.
- **FOV Threshold** (`fovThreshold`, default `35.0`) — maximum angular offset in degrees between your crosshair and a candidate for it to be considered a target. Range 5–180.
- **Nudge Strength** (`strength`, default `0.15`) — multiplier on the logarithmic pull. Higher values pull faster. Range 0.01–1.0.
- **Log Base** (`logBase`, default `2.0`) — currently unused by the aiming math; reserved for future tuning.

Lower `strength` and `fovThreshold` values produce a gentler, less noticeable assist. Raising `fovThreshold` widens the cone but makes the assist feel more intrusive.

## Building

Requires **JDK 25** (the project targets Java 25 and will not compile on older JDKs).

```sh
./gradlew build
```

The compiled mod is written to `build/libs/modid-<version>.jar`. Drop it into your `mods` folder alongside Fabric API and Cloth Config.

To run a development client with the mod loaded:

```sh
./gradlew runClient
```

## Requirements

Runtime dependencies, as declared in `fabric.mod.json`:

- Minecraft `~26.3`
- Fabric Loader `>=0.19.2`
- Fabric API
- Cloth Config

Mod Menu is optional and only needed for the in-game settings screen.

## Project layout

- `src/main/java/com/psipheron/nudge/ModConfig.java` — JSON-backed config load/save.
- `src/client/java/com/psipheron/nudge/NudgeClient.java` — client entrypoint; registers the end-of-tick handler and drives the aim loop.
- `src/client/java/com/psipheron/nudge/AimAssistLogic.java` — target selection, aim point randomization, and rotation math.
- `src/client/java/com/psipheron/nudge/ModMenuIntegration.java` — Cloth Config screen wired into Mod Menu.

Note that gameplay code lives in the `client` source set (Loom's `splitEnvironmentSourceSets`), since the mod is client-only.

## Known limitations

- **Players only.** Target scanning iterates `client.level.players()`, so mobs and other entities are not considered.
- **Always active.** There is no keybind or toggle; the assist runs on every tick while a valid target is in range.
- **Quantization is cosmetic.** `quantizeToMouseGrid` snaps corrections to Minecraft's sensitivity grid, but because rotations are applied through `setYRot`/`setXRot` rather than the mouse input path, the snapping has no functional effect.
- **Target range uses feet position.** `findBestTarget` measures distance to the entity's base position rather than its hitbox center, so effective range is slightly shorter than `maxRange` implies.

## License

Declared as `LGPL-3.0-only` in `fabric.mod.json`.

The `LICENSE` file in this repository still contains the CC0 template text inherited from the Fabric example mod, which does not match. Replace it with the LGPL-3.0 text, or change the license field in `fabric.mod.json` — the two should agree before distribution.
