# Cobblemon Battle Clarity

A client-side NeoForge mod that stops walls, ceilings and furniture from hiding your Cobblemon battles.

When a battle starts in a cramped space — a villager house, a cave, under a tree — the blocks that stand
between your camera and the battling Pokémon stop being drawn. Everything reappears the moment the
battle ends.

It does **not** blank the whole room. Only two categories of block are hidden:

1. **Blocks a Pokémon model is inside or phasing through.** A large Pokémon wedged into a low ceiling or a
   doorframe gets those blocks removed automatically, whether or not they were in your line of sight.
2. **Blocks that actually cover a Pokémon from where you are standing.** Rays are traced from the camera
   to points spread across each battler's silhouette; only blocks those rays pass through are hidden.

The floor is always protected: the block each battler stands on, and everything below the lowest
battler's feet, is never hidden, so the battle never looks like it is floating over a void.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.0+ (developed against 21.1.182) |
| Cobblemon | 1.8.x |
| Java | 21 |

Client-side only. It sends nothing to the server and never changes a block — it only skips geometry while
the chunk mesh is being built. That means it works on any server running Cobblemon, cannot desync, and
cannot grief a world.

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`. To launch a dev client with Cobblemon already loaded:

```bash
./gradlew runClient
```

The build pulls from:

- `https://maven.neoforged.net/releases` — NeoForge
- `https://maven.impactdev.net/repository/development/` — Cobblemon (`com.cobblemon:neoforge:1.8.1+1.21.1`)
- `https://thedarkcolour.github.io/KotlinForForge/` — Kotlin runtime that Cobblemon needs

## How it works

Cobblemon already tracks battle state on the client, so no server component is needed:

- `CobblemonClient.INSTANCE.getBattle()` is non-null exactly while a battle is on screen.
- Participants are found by matching `PokemonEntity.getBattleId()` against `ClientBattle.getBattleId()`,
  plus any `Player` that `ClientBattle.getParticipatingActor()` recognises.

Each battler contributes a **model box**: its hitbox, unioned with its render-culling box, then padded.
The padding matters because Cobblemon models routinely overhang their collision box — wings, tails and
horns render outside it, and those are exactly the parts that end up buried in a wall.

`OcclusionScanner` then produces the hidden set:

| Step | What it does |
|---|---|
| Body rule | Every block overlapping a model box is hidden. Unconditional. |
| Sight rule | Amanatides–Woo voxel traversal from the camera to a lattice of points on each battler's silhouette. Blocks the rays cross are hidden. |
| Dilation | Optional 1-block feather so the opening reads as a window rather than a pinhole. |
| Floor protection | Applied last and wins over everything: support blocks and anything below the lowest feet are restored. |

The set is published behind a single `volatile` reference to an effectively-immutable
`LongOpenHashSet`. Three mixins read it:

- `BlockRenderDispatcher#renderBatched` — normal blocks, cancelled during chunk meshing.
- `BlockRenderDispatcher#renderLiquid` — water and lava in walls.
- `BlockEntityRenderDispatcher#render` — chests, signs, beds and banners, which are drawn every frame
  outside the chunk mesh and would otherwise float in the hole.

The first two run on chunk-build worker threads, hence the lock-free snapshot.

### Keeping it cheap

Making a block vanish costs a chunk section rebuild, so the mod works hard to change the set as rarely
as possible:

- Rays are aimed at the battlers, not along your view direction, so **turning the camera never triggers a
  rebuild** — only actual movement does.
- A scan runs at most every `recomputeIntervalTicks` (default 4), and early-outs entirely unless the
  camera or the battlers moved 0.4 blocks.
- `stickyTicks` (default 30) keeps a block hidden for 1.5s after it stops blocking your view, so the
  opening stops flickering as you and the Pokémon shuffle around. As you orbit, the hole settles into the
  union of recent viewpoints instead of thrashing.
- Only the bounding box of the *changed* positions is marked dirty, and if a scan produces an identical
  set nothing is marked at all.
- `maxHiddenBlocks` (default 4000) caps the work.

When the battle ends the sticky timer is bypassed and everything is restored immediately.

## Configuration

`config/battleclarity-client.toml`, created on first launch.

| Option | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `modelPadding` | `0.25` | Flat padding added around each hitbox to approximate the model. |
| `modelPaddingScale` | `0.15` | Extra padding proportional to the Pokémon's size. |
| `hideNonSolidBlocks` | `true` | Also hide leaves, glass, doors and fluids. |
| `sightMargin` | `0.6` | How much to inflate a battler before tracing, i.e. how generous the window is. |
| `sampleDensity` | `5` | Lattice resolution per axis across each silhouette. Higher = more accurate, slower. |
| `dilation` | `1` | Feathering around the hole. `0` hides the strictly necessary blocks only. |
| `maxDistance` | `24.0` | Never hide a block further than this from the camera. |
| `stickyTicks` | `30` | Hysteresis, in ticks. |
| `recomputeIntervalTicks` | `4` | Minimum ticks between scans. |
| `maxHiddenBlocks` | `4000` | Hard ceiling on hidden blocks. |

Want the strictest possible reading of "only what's in the way"? Set `dilation = 0`, `sightMargin = 0.0`
and `hideNonSolidBlocks = false`.

## Known limitations

- **Sodium / Embeddium replace the chunk mesher and bypass `BlockRenderDispatcher#renderBatched`
  entirely.** With either installed, hidden blocks will keep rendering. Supporting them needs a separate
  compat mixin against their own block-rendering path. This is the most likely reason for "nothing
  happens" reports.
- Blocks are hidden, not literally alpha-blended. Chunk geometry is baked per render layer, so true
  per-block translucency would mean excluding the block from the mesh *and* re-drawing it yourself in a
  translucent pass with a cached vertex buffer. That is a sensible next step — and it would let the
  effect fade in and out instead of popping — but it is a lot more rendering machinery.
- Shaders (Iris/Oculus) are untested here.
- A hidden block also stops casting its face into the light/AO of its neighbours after the rebuild, so
  edges around the opening can look slightly flat. That is inherent to removing it from the mesh.

## Status

The Minecraft, NeoForge and Cobblemon mavens are blocked by the egress policy in the environment this was
written in, so **this code has never been compiled or run.** What *was* verified:

- Every Cobblemon API used was read from the actual Cobblemon 1.8.1 sources, not recalled from memory —
  `CobblemonClient.battle`, `ClientBattle.battleId`, `ClientBattle.getParticipatingActor`,
  `PokemonEntity.battleId`.
- Cobblemon 1.8.1 targets MC 1.21.1 / NeoForge 21.1.182, confirmed from its `libs.versions.toml`.
- The Gradle scripts configure successfully; the build reaches `createMinecraftArtifacts` and fails only
  on blocked hosts.
- All Java sources parse cleanly — the only compiler diagnostics are unresolved Minecraft/Cobblemon
  symbols from the absent classpath.

The parts most likely to need a fix on first compile are the mixin method signatures, since they could not
be checked against the real 1.21.1 jar. `injectors.defaultRequire` is set to `1` deliberately, so a
signature that no longer matches fails loudly at startup instead of silently doing nothing. If
`LevelRenderer#setBlocksDirty` turns out not to be accessible, it needs a one-line access transformer.

## Licence

MIT.
