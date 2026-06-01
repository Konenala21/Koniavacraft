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
 * 職業方塊 = 本源研究桌({@link ModBlocks#ASPECT_RESEARCH_DESK},純方塊、無 BlockEntity),
 * 跟玩家用的研究台分開,避免有動畫 BlockEntity 的方塊當 POI 造成卡頓/不任職。
 * 失業村民在研究桌附近就會轉職,交易在 {@link com.github.nalamodikk.common.event.ModVillagerTrades}。
 */
public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KoniavacraftMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> ASPECT_RESEARCHER_POI =
            POI_TYPES.register("aspect_researcher", () -> new PoiType(
                    Set.copyOf(ModBlocks.ASPECT_RESEARCH_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> ASPECT_RESEARCHER =
            PROFESSIONS.register("aspect_researcher", () -> new VillagerProfession(
                    "aspect_researcher",
                    holder -> holder.is(ASPECT_RESEARCHER_POI.getKey()),
                    holder -> holder.is(ASPECT_RESEARCHER_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LIBRARIAN));

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
