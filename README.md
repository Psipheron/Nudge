# Nudge

A subtle, client-side aim assist for Fabric. Instead of snapping your camera onto a target, Nudge applies a small logarithmic pull toward a randomized point in the target's hitbox, nudging your crosshair closer each tick.

Built for **Minecraft 26.3** on the Fabric mod loader. Client-side only — it does not need to be installed on a server.

## How it works

Every client tick, Nudge runs three steps:

1. **Target selection** (`AimAssistLogic.findBestTarget`) — scans all players in the current world, skipping yourself, dead players, and invisible players. Each candidate is rejected if it is beyond `maxRange` or outside the `fovThreshold` cone around your view direction. Among the survivors, the one with the smallest angular offset from your crosshair wins.
2. **Aim point selection** (`AimAssistLogic.getRandomHitboxPoint`) — picks a random point inside the target's bounding box using a Gaussian distribution biased toward center mass rather than a fixed lock-on point: horizontal offsets use 20% of the box width as their standard deviation, and the vertical bias sits at 55% of the target's height. The point is clamped to stay inside the hitbox, and is re-rolled when you switch targets or every 10 ticks.
3. **Nudge** (`AimAssistLogic.applyLogNudge` or `applyAggressiveNudge`, depending on `aimMode`) — the angular delta to the aim point is scaled logarithmically, so large errors produce a firm pull while small errors settle gently and never overshoot. Corrections are capped at the remaining delta and ignored inside a dead zone of half a mouse grid step (never less than 0.05°). Each correction is also multiplied by a random factor between 0.98 and 1.02 so successive ticks are not perfectly uniform.

Each nudge is then snapped onto Minecraft's mouse sensitivity grid by `MouseGridQuantizer`, so the rotation the mod applies per tick is one a real mouse movement could produce at your current sensitivity. See [Quantization](#quantization) for how sub-step corrections are preserved.

Rotation changes are written directly to the player's yaw and pitch each tick, with pitch clamped to ±90°.

## Aim modes

The pulling math is selected by the `aimMode` config value.

- **`LOG`** (default) — plain logarithmic tracking: `log(1 + delta) * strength`. Smooth and steady; the pull grows slowly as the error grows.
- **`NUDGE`** — the same logarithm, but the incoming delta is first raised to `nudgeJerkPower`: `log(1 + delta^nudgeJerkPower) * strength`. With an exponent above 1.0, errors greater than one degree are amplified and errors below one degree are damped, so large crosshair offsets accelerate faster while small ones settle more softly. The result is a snappier, more decisive feel on a fresh target. An exponent of exactly 1.0 makes this mode behave like `LOG`.

Both modes share the dead zone, overshoot cap, human noise, and quantization behavior.

## Quantization

One unit of raw mouse movement rotates the camera by `(sensitivity * 0.6 + 0.2)^3 * 8 * 0.15` degrees — about 0.16° at default sensitivity. Rounding each nudge to the nearest multiple of that step would discard every correction smaller than half a step, which at low `strength` is most of them.

Instead, `MouseGridQuantizer` carries the sub-step remainder from tick to tick:

- Each returned nudge is an exact multiple of the grid step, in either direction (including backwards, when carried error needs cancelling).
- The running total rotation still tracks the unquantized logarithmic pull, because remainders are never dropped.
- Yaw and pitch keep independent remainders.
- Remainders are reset when you switch targets or when no target is in range, since the carried error was measured against a point that no longer applies. They survive the 10-tick aim-point re-roll of the same target, so slow accumulation isn't discarded.

Because a carried remainder lets a nudge overshoot the remaining error by up to half a step, the dead zone above is sized to clear that overshoot. Without it the overshoot would flip sign each tick and the camera would buzz in place.

## Configuration

Settings are stored in `config/nudge_config.json` inside your Minecraft instance. The file is created with defaults on first launch. If Cloth Config and Mod Menu are installed, the same values can be edited in-game through the Mod Menu settings screen.

- **Aim Assist Mode** (`aimMode`, default `LOG`) — `LOG` for smooth tracking, `NUDGE` for the snappier power-scaled variant described above.
- **Max Range** (`maxRange`, default `4.5`) — detection radius in blocks. Candidates beyond this are ignored.
- **FOV Threshold** (`fovThreshold`, default `35.0`) — maximum angular offset in degrees between your crosshair and a candidate for it to be considered a target. Range 5–180.
- **Nudge Strength** (`strength`, default `0.15`) — multiplier on the logarithmic pull. Higher values pull faster. Range 0.01–1.0.
- **Nudge Jerk Power** (`nudgeJerkPower`, default `1.3`) — exponent applied to the error delta before the logarithm. Only affects `NUDGE` mode. Range 1.0–3.0; higher values snap onto distant targets faster.
- **Log Base** (`logBase`, default `2.0`) — currently unused by the aiming math; reserved for future tuning. There is no in-game settings entry for it.

Lower `strength` and `fovThreshold` values produce a gentler, less noticeable assist. Raising `fovThreshold` widens the cone but makes the assist feel more intrusive.

## Building

Requires **JDK 25** (the project targets Java 25 and will not compile on older JDKs).

```sh
./gradlew build
```

The compiled mod is written to `build/libs/modid-1.0.0.jar`. Drop it into your `mods` folder alongside Fabric API and Cloth Config.

To run a development client with the mod loaded:

```sh
./gradlew runClient
```

## Requirements

Runtime dependencies, as declared in `fabric.mod.json`:

- Minecraft `~26.3`
- Fabric Loader `>=0.19.2`
- Java `>=25`
- Fabric API
- Cloth Config

Mod Menu is optional and only needed for the in-game settings screen.

## Project layout

- `src/main/java/com/psipheron/nudge/ModConfig.java` — JSON-backed config load/save, including the `AimMode` enum.
- `src/client/java/com/psipheron/nudge/NudgeClient.java` — client entrypoint; registers the end-of-tick handler and drives the aim loop.
- `src/client/java/com/psipheron/nudge/AimAssistLogic.java` — target selection, aim point randomization, and the logarithmic rotation math for both aim modes.
- `src/client/java/com/psipheron/nudge/MouseGridQuantizer.java` — per-axis snapping of corrections onto the mouse sensitivity grid.
- `src/client/java/com/psipheron/nudge/ModMenuIntegration.java` — Cloth Config screen wired into Mod Menu.

Gameplay code lives in the `client` source set (Loom's `splitEnvironmentSourceSets`), since the mod is client-only. The mod id (`nudge`) and Java package (`com.psipheron.nudge`) do not match the template `modid` namespace still used by resource paths and the Gradle project name.

## Known limitations

- **Players only.** Target scanning iterates `client.level.players()`, so mobs and other entities are not considered.
- **Always active.** There is no keybind or toggle; the assist runs on every tick while a valid target is in range.
- **No line-of-sight check.** Candidates behind walls are still tracked as long as they are within range and inside the FOV cone.
- **Quantized rotations do not go through the mouse path.** The applied deltas are grid-aligned, but they are written with `setYRot`/`setXRot`, so the client's own mouse-delta handling never sees them. Quantization makes the per-tick step plausible, not the final camera angle: the accumulated rotation is not grid-aligned because the base angle it started from isn't. Anything downstream that inspects raw mouse deltas (server-side anti-cheat, for example) sees nothing.
- **Target range uses feet position.** `findBestTarget` measures distance to the entity's base position rather than its hitbox center, so effective range is slightly shorter than `maxRange` implies.

## License

Declared as `LGPL-3.0-only` in `fabric.mod.json`.

The `LICENSE` file in this repository still contains the CC0 template text inherited from the Fabric example mod, which does not match. Replace it with the LGPL-3.0 text, or change the license field in `fabric.mod.json` — the two should agree before distribution.
