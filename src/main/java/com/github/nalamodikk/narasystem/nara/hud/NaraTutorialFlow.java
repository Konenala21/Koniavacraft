package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public class NaraTutorialFlow {

    public static final String RESEARCH_TABLE   = "research_table";
    public static final String ALTAR_T6         = "altar_t6";
    public static final String FIRST_SCAN       = "first_scan";
    public static final String FIRST_WATCH_OPEN = "first_watch_open";
    public static final String MANA_GEN_CRAFT       = "mana_gen_craft";
    public static final String MANA_GEN_PLACED      = "mana_gen_placed";
    public static final String FIRST_RESEARCH       = "first_research";
    public static final String FIRST_ALTAR_FORMED   = "first_altar_formed";
    public static final String WAND_ROD_CRAFT        = "wand_rod_craft";
    public static final String WAND_ROD_NO_ITEMS     = "wand_rod_no_items";
    public static final String WAND_ROD_READY        = "wand_rod_ready";
    public static final String WAND_ROD_GOT_CORE     = "wand_rod_got_core";
    public static final String MANA_GRINDER_CRAFT    = "mana_grinder_craft";
    public static final String MANA_INFUSER_CRAFT    = "mana_infuser_craft";
    public static final String MANA_CRAFTING_CRAFT   = "mana_crafting_craft";
    public static final String ASPECT_SYNTHESIS_OPEN = "aspect_synthesis_open";
    public static final String MANA_DEPLOYER_CRAFT        = "mana_deployer_craft";
    public static final String MANA_CHARGER_CRAFT         = "mana_charger_craft";
    public static final String SOLAR_COLLECTOR_CRAFT      = "solar_collector_craft";
    public static final String WAND_ROD_CRAFT_SEEN        = "wand_rod_craft_seen";
    public static final String ASPECT_SYNTHESIS_OPEN_SEEN = "aspect_synthesis_open_seen";
    public static final String BOOTS_EQUIP                = "boots_equip";

    private static boolean altarT6Shown = false;
    private static boolean aspectSynthesisShown = false;
    private static boolean wandRodNoItemsShown = false;
    private static boolean wandRodReadyShown = false;
    private static boolean wandRodCraftShown = false;
    private static boolean bootsEquipShown = false;

    public static void resetSessionFlags() {
        altarT6Shown = false;
        aspectSynthesisShown = false;
        wandRodNoItemsShown = false;
        wandRodReadyShown = false;
        wandRodCraftShown = false;
        bootsEquipShown = false;
    }

    public static boolean isWandRodCraftShown() { return wandRodCraftShown; }

    public static boolean claimAspectSynthesisShown() {
        if (aspectSynthesisShown) return false;
        aspectSynthesisShown = true;
        return true;
    }

    public static boolean claimWandRodNoItemsShown() {
        if (wandRodNoItemsShown) return false;
        wandRodNoItemsShown = true;
        return true;
    }

    public static boolean claimWandRodReadyShown() {
        if (wandRodReadyShown) return false;
        wandRodReadyShown = true;
        return true;
    }

    public static void start(String tutorialId) {
        switch (tutorialId) {
            case RESEARCH_TABLE   -> startResearchTable();
            case ALTAR_T6         -> startAltarT6();
            case FIRST_SCAN       -> startFirstScan();
            case FIRST_WATCH_OPEN -> startFirstWatchOpen();
            case MANA_GEN_CRAFT       -> startManaGenCraft();
            case MANA_GEN_PLACED      -> startManaGenPlaced();
            case FIRST_RESEARCH       -> startFirstResearch();
            case FIRST_ALTAR_FORMED   -> startFirstAltarFormed();
            case WAND_ROD_CRAFT       -> startWandRodCraft();
            case WAND_ROD_NO_ITEMS    -> startWandRodNoItems();
            case WAND_ROD_READY      -> startWandRodReady();
            case WAND_ROD_GOT_CORE   -> startWandRodGotCore();
            case MANA_GRINDER_CRAFT  -> startManaGrinderCraft();
            case MANA_INFUSER_CRAFT  -> startManaInfuserCraft();
            case MANA_CRAFTING_CRAFT -> startManaCraftingCraft();
            case ASPECT_SYNTHESIS_OPEN -> startAspectSynthesisOpen();
            case MANA_DEPLOYER_CRAFT    -> startManaDeployerCraft();
            case MANA_CHARGER_CRAFT     -> startManaChargerCraft();
            case SOLAR_COLLECTOR_CRAFT  -> startSolarCollectorCraft();
            case WAND_ROD_CRAFT_SEEN        -> wandRodCraftShown = true;
            case ASPECT_SYNTHESIS_OPEN_SEEN -> aspectSynthesisShown = true;
            case BOOTS_EQUIP                -> startBootsEquip();
        }
    }

    private static void startResearchTable() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line1")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line2"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line2")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line3"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line3")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.research_table.line4"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.research_table.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line4"))
        ));
    }

    private static void startAltarT6() {
        if (altarT6Shown) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        altarT6Shown = true;
        Component playerName = mc.player.getDisplayName();
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.altar_t6.line1", playerName))
                        .withOnStart(() -> NaraSoundHelper.play("altar_t6", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.altar_t6.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.altar_t6.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("altar_t6", "line2"))
        ));
    }

    private static void startFirstScan() {
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.first_scan.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("first_scan", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.first_scan.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.first_scan.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("first_scan", "line2"))
        ));
    }

    private static void startFirstWatchOpen() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.first_watch_open.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("first_watch_open", "line1")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.first_watch_open.line2"))
                        .withOnStart(() -> NaraSoundHelper.play("first_watch_open", "line2")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.first_watch_open.line3"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.first_watch_open.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("first_watch_open", "line3"))
        ));
    }

    private static void startManaGenCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.mana_gen_craft.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("mana_gen_craft", "line1")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.mana_gen_craft.line2"))
                        .withOnStart(() -> NaraSoundHelper.play("mana_gen_craft", "line2"))
        ));
    }

    private static void startManaGenPlaced() {
        NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        boolean hasJei = ModList.get().isLoaded("jei");
        List<NaraDialogueLine> lines = new ArrayList<>();
        lines.add(NaraDialogueLine.simple(
                Component.translatable("nara.dialogue.tutorial.mana_gen_placed.line1"))
                .withOnStart(() -> NaraSoundHelper.play("mana_gen_placed", "line1")));
        if (hasJei) {
            lines.add(NaraDialogueLine.simple(
                    Component.translatable("nara.dialogue.tutorial.mana_gen_placed.line2"))
                    .withOnStart(() -> NaraSoundHelper.play("mana_gen_placed", "line2")));
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_gen_placed.line3"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_gen_placed.confirm"),
                            () -> NaraDialogueManager.clearGuiHighlight())),
                    0, null)
                    .withOnStart(() -> {
                        NaraSoundHelper.play("mana_gen_placed", "line3");
                        NaraDialogueManager.setGuiHighlight(42, 37, 40);
                    }));
        } else {
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_gen_placed.line2"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_gen_placed.confirm"),
                            () -> {})),
                    0, null)
                    .withOnStart(() -> NaraSoundHelper.play("mana_gen_placed", "line2")));
        }
        NaraDialogueManager.startDialogue(lines);
    }

    private static void startFirstResearch() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.first_research.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("first_research", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.first_research.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.first_research.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("first_research", "line2"))
        ));
    }

    private static void startFirstAltarFormed() {
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.first_altar_formed.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("first_altar_formed", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.first_altar_formed.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.first_altar_formed.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("first_altar_formed", "line2"))
        ));
    }

    private static void startWandRodCraft() {
        wandRodCraftShown = true;
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.craft.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "craft_line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.craft.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.wand_rod.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "craft_line2"))
        ));
    }

    private static void startWandRodNoItems() {
        claimWandRodNoItemsShown();
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.no_items.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "no_items_line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.no_items.line2",
                                Component.keybind("key.koniava.open_upgrade_gui")),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.wand_rod.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "no_items_line2"))
        ));
    }

    private static void startWandRodReady() {
        claimWandRodReadyShown();
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.ready.line1",
                                Component.keybind("key.koniava.open_upgrade_gui")),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.wand_rod.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "ready_line1"))
        ));
    }

    private static void startWandRodGotCore() {
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.wand_rod.got_core.line1",
                                Component.keybind("key.koniava.open_upgrade_gui")),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.wand_rod.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("wand_rod", "got_core_line1"))
        ));
    }

    private static void startManaGrinderCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        boolean hasJei = ModList.get().isLoaded("jei");
        List<NaraDialogueLine> lines = new ArrayList<>();
        lines.add(NaraDialogueLine.simple(
                Component.translatable("nara.dialogue.tutorial.mana_grinder.line1"))
                .withOnStart(() -> NaraSoundHelper.play("mana_grinder", "line1")));
        if (hasJei) {
            lines.add(NaraDialogueLine.simple(
                    Component.translatable("nara.dialogue.tutorial.mana_grinder.line2"))
                    .withOnStart(() -> NaraSoundHelper.play("mana_grinder", "line2")));
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_grinder.line3"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_grinder.confirm"),
                            () -> NaraDialogueManager.clearGuiHighlight())),
                    0, null)
                    .withOnStart(() -> {
                        NaraSoundHelper.play("mana_grinder", "line3");
                        NaraDialogueManager.setGuiHighlight(159, 11, 25);
                    }));
        } else {
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_grinder.line2"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_grinder.confirm"),
                            () -> {})),
                    0, null)
                    .withOnStart(() -> NaraSoundHelper.play("mana_grinder", "line2")));
        }
        NaraDialogueManager.startDialogue(lines);
    }

    private static void startManaInfuserCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        boolean hasJei = ModList.get().isLoaded("jei");
        List<NaraDialogueLine> lines = new ArrayList<>();
        lines.add(NaraDialogueLine.simple(
                Component.translatable("nara.dialogue.tutorial.mana_infuser.line1"))
                .withOnStart(() -> NaraSoundHelper.play("mana_infuser", "line1")));
        if (hasJei) {
            lines.add(NaraDialogueLine.simple(
                    Component.translatable("nara.dialogue.tutorial.mana_infuser.line2"))
                    .withOnStart(() -> NaraSoundHelper.play("mana_infuser", "line2")));
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_infuser.line3"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_infuser.confirm"),
                            () -> NaraDialogueManager.clearGuiHighlight())),
                    0, null)
                    .withOnStart(() -> {
                        NaraSoundHelper.play("mana_infuser", "line3");
                        NaraDialogueManager.setGuiHighlight(159, 11, 25);
                    }));
        } else {
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_infuser.line2"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_infuser.confirm"),
                            () -> {})),
                    0, null)
                    .withOnStart(() -> NaraSoundHelper.play("mana_infuser", "line2")));
        }
        NaraDialogueManager.startDialogue(lines);
    }

    private static void startManaCraftingCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        boolean hasJei = ModList.get().isLoaded("jei");
        List<NaraDialogueLine> lines = new ArrayList<>();
        lines.add(NaraDialogueLine.simple(
                Component.translatable("nara.dialogue.tutorial.mana_crafting.line1"))
                .withOnStart(() -> NaraSoundHelper.play("mana_crafting", "line1")));
        if (hasJei) {
            lines.add(NaraDialogueLine.simple(
                    Component.translatable("nara.dialogue.tutorial.mana_crafting.line2"))
                    .withOnStart(() -> NaraSoundHelper.play("mana_crafting", "line2")));
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_crafting.line3"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_crafting.confirm"),
                            () -> NaraDialogueManager.clearGuiHighlight())),
                    0, null)
                    .withOnStart(() -> {
                        NaraSoundHelper.play("mana_crafting", "line3");
                        NaraDialogueManager.setGuiHighlight(110, 55, 30);
                    }));
        } else {
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.mana_crafting.line2"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.mana_crafting.confirm"),
                            () -> {})),
                    0, null)
                    .withOnStart(() -> NaraSoundHelper.play("mana_crafting", "line2")));
        }
        NaraDialogueManager.startDialogue(lines);
    }

    private static void startAspectSynthesisOpen() {
        claimAspectSynthesisShown();
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        boolean hasJei = ModList.get().isLoaded("jei");
        List<NaraDialogueLine> lines = new ArrayList<>();
        lines.add(NaraDialogueLine.simple(
                Component.translatable("nara.dialogue.tutorial.aspect_synthesis.line1"))
                .withOnStart(() -> NaraSoundHelper.play("aspect_synthesis", "line1")));
        if (hasJei) {
            lines.add(NaraDialogueLine.simple(
                    Component.translatable("nara.dialogue.tutorial.aspect_synthesis.line2"))
                    .withOnStart(() -> NaraSoundHelper.play("aspect_synthesis", "line2")));
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.aspect_synthesis.line3"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.aspect_synthesis.confirm"),
                            () -> NaraDialogueManager.clearGuiHighlight())),
                    0, null)
                    .withOnStart(() -> {
                        NaraSoundHelper.play("aspect_synthesis", "line3");
                        NaraDialogueManager.setGuiHighlight(122, 18, 30);
                    }));
        } else {
            lines.add(NaraDialogueLine.withChoices(
                    Component.translatable("nara.dialogue.tutorial.aspect_synthesis.line2"),
                    List.of(new NaraChoice(
                            Component.translatable("nara.dialogue.tutorial.aspect_synthesis.confirm"),
                            () -> {})),
                    0, null)
                    .withOnStart(() -> NaraSoundHelper.play("aspect_synthesis", "line2")));
        }
        NaraDialogueManager.startDialogue(lines);
    }

    private static void startManaDeployerCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.mana_deployer.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("mana_deployer", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.mana_deployer.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.mana_deployer.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("mana_deployer", "line2"))
        ));
    }

    private static void startManaChargerCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.mana_charger.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("mana_charger", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.mana_charger.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.mana_charger.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("mana_charger", "line2"))
        ));
    }

    private static void startSolarCollectorCraft() {
        if (Minecraft.getInstance().screen != null) NaraDialogueManager.setOverlayOnScreen(true);
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.solar_collector.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("solar_collector", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.solar_collector.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.solar_collector.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("solar_collector", "line2"))
        ));
    }

    private static void startBootsEquip() {
        if (bootsEquipShown) return;
        bootsEquipShown = true;
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.boots_equip.line1"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.boots_equip.confirm"),
                                () -> {})),
                        0, null)
        ));
    }

    private NaraTutorialFlow() {}
}
