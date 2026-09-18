# Building and installing on your PC

## The quick way

Download [`install.ps1`](install.ps1), put it anywhere, and run this in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\install.ps1
```

It fetches the source (no Git needed), builds it, copies the jar into your mods folder, and checks the
instance for Cobblemon, Kotlin For Forge, Sodium and Embeddium. A different instance:

```powershell
powershell -ExecutionPolicy Bypass -File .\install.ps1 -ModsDir "D:\Some\Other\mods"
```

You still need the Java 21 JDK first — see step 1. The rest of this page is the manual version.

---

This gets Cobblemon Battle Clarity from source to running in your Minecraft.

> **Read this first.** This mod has never been compiled — the Minecraft, NeoForge and Cobblemon
> package servers were blocked in the environment it was written in. Your first `build` is genuinely the
> first one. [Step 6](#6-if-the-build-fails) covers what to send me if it fails; expect that to be needed.

## 1. Install the prerequisites

**Java 21 (JDK, not just the runtime).** Get Temurin 21 from
[adoptium.net](https://adoptium.net/temurin/releases/?version=21). During the Windows install, tick
*"Set JAVA_HOME variable"*.

Open a new terminal (PowerShell on Windows) and check:

```
java -version
```

You want it to say `21.x`. If it says 17 or 8, Java 21 is not on your PATH and the build will fail.

**Git** — optional. Without it, use the "Download ZIP" button on the branch page instead of step 2.

## 2. Get the code

```
git clone https://github.com/Arsnyde/my-first-repo.git battleclarity
cd battleclarity
```

## 3. Build the jar

Windows (PowerShell or cmd):

```
.\gradlew.bat build
```

macOS / Linux:

```
./gradlew build
```

**The first build takes a while — 5 to 15 minutes, and roughly 1–2 GB of downloads.** NeoForge has to
fetch and decompile Minecraft itself. It looks like it has hung during `createMinecraftArtifacts`; it
has not. Later builds take seconds.

When it finishes you want:

```
build/libs/battleclarity-1.0.0.jar
```

That is the file you install. Ignore any `-sources.jar` next to it.

## 4. Set up the Minecraft instance

Everything must be the **1.21.1 NeoForge** build of each mod. Mixing loaders or versions is the most
common cause of a crash on startup.

| Mod | Where | Notes |
|---|---|---|
| NeoForge 21.1.x | [neoforged.net](https://neoforged.net/) | Run the installer, pick *Install client* |
| Cobblemon 1.8.1 | Modrinth / CurseForge | Must be the **NeoForge** file, not Fabric |
| Kotlin For Forge 5.10.0+ | Modrinth / CurseForge | **Required.** Cobblemon does not bundle it |
| `battleclarity-1.0.0.jar` | your `build/libs` | From step 3 |

Kotlin For Forge catches people out — Cobblemon is written in Kotlin but ships without the Kotlin
runtime on NeoForge, so without it the game crashes before reaching the title screen.

Drop `battleclarity-1.0.0.jar` into your instance's mods folder. For your CurseForge instance that is:

```
C:\Users\snyde\curseforge\minecraft\Instances\My Cobblemon Mods Test\mods
```

Cobblemon, Kotlin For Forge and NeoForge are presumably already there — check the folder and only add
what is missing. Then launch that instance.

**Sodium in that pack is fine.** It is explicitly supported; see step 7 if you want to confirm the
Sodium-specific hook actually engaged.

**This is a client-only mod.** If you play on a server, only you need it — the server doesn't, and putting
it there does nothing.

## 5. Check it works

1. Build yourself a small enclosed room, or step into a villager house.
2. Start a battle — throw out a Pokémon at a wild one.
3. The walls and ceiling between your camera and the Pokémon should stop being drawn, while the floor
   you are both standing on stays put.
4. End the battle. Everything comes straight back.

Move the camera around. Blocks should stay hidden for about a second and a half after they stop
covering anything, rather than flickering in and out.

## 6. If the build fails

Expected on the first run. Capture the errors:

```
.\gradlew.bat build --no-daemon > build-log.txt 2>&1
```

Send me `build-log.txt`, or just the lines containing `error:`. The likely culprits, in order:

1. **A mixin signature drifted.** `RenderChunkRegion#getBlockState` and
   `BlockEntityRenderDispatcher#render` were verified against documentation but not against the real
   1.21.1 jar. A mismatch here is a one-line fix. (The Sodium hook was read from Sodium's own source, so
   it is on firmer ground.)
2. **`LevelRenderer#setBlocksDirty` not accessible.** Fixed by adding an access transformer.
3. **A Cobblemon method name.** Less likely — all of them were read out of the Cobblemon 1.8.1 source.

None of these are structural. They are the kind of thing that is obvious and quick once the compiler
names them.

## 7. If it builds but nothing happens in game

Check `logs/latest.log`:

- Search for `battleclarity`. The mod is deliberately configured to **crash loudly at startup** if a
  mixin fails to apply, rather than silently doing nothing — so a clean startup means the hooks are live.
- Search for `Mixin apply failed`.
- To confirm the Sodium path engaged, search for `LevelSlice`. That mixin is applied only when Sodium is
  detected, so seeing it mentioned means the Sodium-specific hook is in place.

**If you run Embeddium rather than Sodium**, that is a known gap — it is a fork under different package
names and needs its own hook. Sodium is supported; Embeddium is not yet.

Settings live in `config/battleclarity-client.toml`, created on first launch. If the effect is too
aggressive or too timid, `sightMargin` and `dilation` are the two dials worth touching first — see the
table in [README.md](README.md#configuration).

## 8. Making changes

Don't rebuild-and-copy while iterating. This launches a dev client with Cobblemon already loaded:

```
.\gradlew.bat runClient
```
