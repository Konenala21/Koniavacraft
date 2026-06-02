![image](https://cdn.modrinth.com/data/cached_images/fff67d7abb28efe649b7a0956cc93f17a6c5110e.png)

# Koniavacraft

A magic-industrial mod built around one question:
what if mana could be quantified, studied, engineered, and reinterpreted through science?

You drop coal into a generator. It starts burning. Mana flows into the network.
That's the starting point.

From there it grows into research, resonance structures, animated magical cores, custom equipment, and a personal guide who is not quite what she seems.

Instead of traditional fantasy spellcasting, Koniavacraft leans toward arcane engineering: treating mana as an industrial resource you can measure, route, and control.

Still heavily in development. Still experimental. Still changing.

---

## Nara

Early on you bind a holographic assistant named Nara.

She walks you through your first machines, reacts to what you build, and slowly reveals more about the world (and herself) as you progress. She is the voice of the mod: playful, a little chaotic, occasionally serious without warning.

She is also the reason the mod is not only about machines. The outer shell is "mana made measurable". Underneath, the further out you explore, the more it turns inward.

---

## Research and Aspects

Everything you build is gated behind a research tree, unlocked through a hex-grid puzzle at the Research Table.

To solve those puzzles you need Aspects: the elemental "source" essences behind the world. Point Nara's holographic watch at blocks, items, fluids, mobs (and even players) to scan and collect them. There are over 80 aspects, from the six primary elements up through layered compounds.

Research is not just a tech gate. It is how the mod reveals its central idea: that mana is the world's original energy, and understanding it means understanding what everything is made of.

---

## Machines and Networks

* **Mana Generator:** burns fuel to produce mana or RF energy. Upgradeable and switchable.
* **Arcane Conduit:** connects machines into a shared mana network. Higher conduit tiers raise total network capacity.
* **Mana Grinder:** processes and transforms materials, sometimes producing unstable or corrupted byproducts.
* **Mana Infuser:** injects magical energy into items to change what they are.
* **Mana Crafting Table:** craft using stored mana instead of traditional progression.
* **Solar Mana Collector:** passive generation from ambient magical energy.
* **Deployer, Charger and more:** automation pieces for building real production lines.

---

## The Aspect Altar

A multiblock ritual structure that grows as you do.

Feed it catalysts and materials, channel mana through it, and it performs rituals that ordinary crafting cannot. As you ring it with resonance structures it upgrades through tiers, each one a larger, more elaborate, animated form. Progression is meant to feel physical, not just "higher tier blocks".

Starve a high-tier ritual of mana and it will not fail quietly.

---

## Equipment and Combat

* **Modular Wands:** a wand rod with swappable cores and upgrade plugins. Build the tool you actually want.
* **Floating Turrets (Nara Star-Ring):** carry them to fire by hand, or slot them as orbiting auto-turrets that defend you. Deeply upgradeable: capacity, healing, auto-aim, control shots, and more.
* **Mana Alloy Armor:** a full set with its own mana storage and a unified upgrade screen. Double jump, dash boots, night vision, mana shields, and per-piece upgrade slots.

---

## The Mirror Dimension

Push a high-tier ritual past its limit and it can tear open a rift.

Step through, and you face something that fights exactly like you do: a mirror boss built from your own skin, gear, inventory and turrets. It walls you in, drains your mana, and transforms. Beating it is the current end of the line, and a key to what comes next.

---

## Materials and World

Mana Dust, Condensed Mana, Refined Mana, and corrupted variants when things go wrong. Mana is not just fuel: it is the foundation of the entire industrial chain.

Explore the overworld to find the Mana Prairie, a biome shaped by abnormal magical concentration.

---

> [!WARNING]
> Minecraft 1.20.1 is no longer maintained or updated.
> Current development is focused on NeoForge 1.21.1.

---

## Compatibility

* NeoForge 1.21.1
* JEI support
* Jade support
* RF / FE compatible
* Works alongside other tech mods

---

## Notice for Modpack Authors

Koniavacraft customizes the **main title screen** by default:

* Replaces the vanilla Minecraft logo with the mod's own logo (subtle scale pulse)
* Replaces the splash text with a curated pool of mod-themed lines (mixed Chinese/English)
* Adds a small toggle button in the top-right of the title screen so players can switch back to vanilla instantly

This is intentional for solo players who install the mod alone, but modpack authors usually want their own pack branding. To handle this:

* **Auto-defer**: if another title-screen-customizing mod (FancyMenu, CustomMainMenu, BetterTitleScreen, TitleTweaks) is also loaded, Koniavacraft automatically steps aside and does not override the title.
* **Manual disable**: in `config/koniavacraft-client.toml` under `[titleScreen]`, set `customTitleScreenEnabled = false` and `showTitleToggleButton = false` to fully hide our title and button.

No restart needed: players can also use the in-game toggle button to switch between modded and vanilla title.

---

## About the project

Koniavacraft is a solo passion project developed during free time.

No corporate roadmap. No forced release schedule. Just experimenting, learning, and slowly building a strange magical-industrial world with a story underneath it.

Licensed under LGPL-3.0.

Bug reports and feedback are always welcome.
