package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends AdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, List.of(new KoniavaAdvancements()));
    }

    static class KoniavaAdvancements implements AdvancementSubProvider {
        @Override
        public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> writer) {
            AdvancementHolder naraWelcome = Advancement.Builder.advancement()
                    .display(ModItems.NARA_WATCH.get(),
                            Component.translatable("advancement.koniava.welcome.title"),
                            Component.translatable("advancement.koniava.welcome.description"),
                            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                            AdvancementType.TASK, true, true, false)
                    .addCriterion("obtained", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":nara_welcome");

            AdvancementHolder naraIgnored = Advancement.Builder.advancement()
                    .parent(naraWelcome)
                    .display(Items.CREEPER_HEAD,
                            Component.translatable("advancement.koniava.nara_ignored.title"),
                            Component.translatable("advancement.koniava.nara_ignored.description"),
                            null, AdvancementType.CHALLENGE, true, true, true)
                    .addCriterion("obtained", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":nara_ignored");

            Advancement.Builder.advancement()
                    .parent(naraIgnored)
                    .display(Items.TNT,
                            Component.translatable("advancement.koniava.nara_angry.title"),
                            Component.translatable("advancement.koniava.nara_angry.description"),
                            null, AdvancementType.CHALLENGE, true, true, true)
                    .addCriterion("obtained", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":nara_angry");

            Advancement.Builder.advancement()
                    .parent(naraWelcome)
                    .display(ModBlocks.RESEARCH_TABLE.get(),
                            Component.translatable("advancement.koniava.craft_research_table.title"),
                            Component.translatable("advancement.koniava.craft_research_table.description"),
                            null, AdvancementType.TASK, true, false, false)
                    .addCriterion("get_research_table",
                            InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.RESEARCH_TABLE.get()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":craft_research_table");

            AdvancementHolder firstAltarRitual = Advancement.Builder.advancement()
                    .parent(naraWelcome)
                    .display(ModBlocks.ASPECT_ALTAR.get(),
                            Component.translatable("advancement.koniava.first_altar_ritual.title"),
                            Component.translatable("advancement.koniava.first_altar_ritual.description"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("trigger", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":first_altar_ritual");

            AdvancementHolder altarT5 = Advancement.Builder.advancement()
                    .parent(firstAltarRitual)
                    .display(ModBlocks.RESONANCE_RING.get(),
                            Component.translatable("advancement.koniava.altar_upgrade_t5.title"),
                            Component.translatable("advancement.koniava.altar_upgrade_t5.description"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("obtained", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":altar_upgrade_t5");

            Advancement.Builder.advancement()
                    .parent(altarT5)
                    .display(ModBlocks.ASPECT_ALTAR.get(),
                            Component.translatable("advancement.koniava.altar_upgrade_t6.title"),
                            Component.translatable("advancement.koniava.altar_upgrade_t6.description"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("obtained", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(writer, KoniavacraftMod.MOD_ID + ":altar_upgrade_t6");
        }
    }
}
