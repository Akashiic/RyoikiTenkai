# RyoikiTenkai (領域展開)

**Domain Expansion** and **Domain Clash** for Minecraft **1.7.10** servers (Thermos/Crucible), drawn on the client by
the [Kunftig](https://github.com/Akashiic/Kunftig) mod.

> Versão em português: [README.pt-BR.md](README.pt-BR.md).

## What it is

A player expands their domain: a manga-style banner crosses the screen and a barrier closes in a radius around them.
For everyone caught inside, the world turns into the domain's interior (sky, colors, giant structures), and a sure-hit
effect works on all of them: paralysis, constant slashes, and so on.

When two or more players expand at the same time, a **domain clash** starts:

* the domains' scenes flicker over each other;
* a 15-second countdown appears;
* every hit on a rival caster fills the attacker's bar;
* whoever fills it first, or has the most when time runs out, wins;
* the winner's domain stays, the others shatter, and a tie breaks them all.

The full study (references, numbers, visuals, how it maps to Minecraft) is in [docs/ESTUDO.md](docs/ESTUDO.md)
(Portuguese).

## Status

**Just started.** First step: a plugin that uses only what Kunftig's API 13 already has (3D displays, HUD, keys,
effects and particles). Screen effects (sky, fog, flash, shake, camera) come with Kunftig's API 14.

## Installing (once there is a build)

| Where | What |
|---|---|
| Server | `RyoikiTenkai-<version>.jar` in `plugins/`, and the Kunftig mod in `mods/` |
| Client | The Kunftig mod (with GeckoLib) and the RyoikiTenkai resource pack |

## License

Not decided yet.
