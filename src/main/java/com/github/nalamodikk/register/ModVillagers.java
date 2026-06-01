package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * 自訂村民職業「本源研究員」(Aspect Researcher)。
 *
 * 職業方塊 = 研究台(ResearchTable):把研究台所有 blockstate 註冊成一個 PoiType,
 * 失業村民在研究台附近就會轉職成本源研究員。交易在 {@link com.github.nalamodikk.common.event.ModVillagerTrades}。
 */
public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KoniavacraftMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, KoniavacraftMod.MOD_ID);

    /** 研究台 POI:每張桌一個工作席位(maxTickets=1)、搜尋範圍 1。 */
    public static final DeferredHolder<PoiType, PoiType> ASPECT_RESEARCHER_POI =
            POI_TYPES.register("aspect_researcher", () -> new PoiType(
                    Set.copyOf(ModBlocks.RESEARCH_TABLE.get().getStateDefinition().getPossibleStates()), 1, 1));

    /** 本源研究員:以研究台為職業方塊。 */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> ASPECT_RESEARCHER =
            PROFESSIONS.register("aspect_researcher", () -> new VillagerProfession(
                    "aspect_researcher",
                    holder -> holder.is(ASPECT_RESEARCHER_POI.getKey()),
                    holder -> holder.is(ASPECT_RESEARCHER_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC));

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
