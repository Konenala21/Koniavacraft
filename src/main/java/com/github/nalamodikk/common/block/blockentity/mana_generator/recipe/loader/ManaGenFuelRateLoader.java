package com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.logic.FuelRegistryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@EventBusSubscriber
public class ManaGenFuelRateLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGenFuelRateLoader.class);
    private static final Gson GSON = new Gson();
    private static final Map<String, FuelRate> FUEL_RATES = new HashMap<>();

    private static final String DEFAULT_NAMESPACE = KoniavacraftMod.MOD_ID;
    private static final int DEFAULT_BURN_TIME = 0;  // 默認燃燒時間
    private static final int DEFAULT_ENERGY_RATE = 0;
    public static final int DEFAULT_INTERVAL = 1;
    private static final Set<ResourceLocation> warnedAlready = new HashSet<>();

    public ManaGenFuelRateLoader() {
        super(GSON, "recipe/mana_recipes/mana_fuel");  // 確保加載 mana_recipes/fuel 目錄
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ManaGenFuelRateLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        FUEL_RATES.clear();
        warnedAlready.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), "fuel_rate");
                JsonObject ingredientObject = GsonHelper.getAsJsonObject(jsonObject, "ingredient");
                String itemId = GsonHelper.getAsString(ingredientObject, "item", "");
                String tagId = GsonHelper.getAsString(ingredientObject, "tag", "");
                int manaRate = GsonHelper.getAsInt(jsonObject, "mana", 0); // 默認魔力生產為 0
                int burnTime = GsonHelper.getAsInt(jsonObject, "burn_time", 200); // 默認燃燒時間 200
                int energyRate = GsonHelper.getAsInt(jsonObject, "energy", 1); // 默認能量生產為 1
                int intervalTick = GsonHelper.getAsInt(jsonObject, "interval", DEFAULT_INTERVAL);

                FuelRate fuelRate = new FuelRate(manaRate, burnTime, energyRate,intervalTick );

                if (!itemId.isEmpty()) {
                    FUEL_RATES.put(itemId, fuelRate);
                } else if (!tagId.isEmpty()) {
                    FUEL_RATES.put("tag:" + tagId, fuelRate);
                } else {
                    throw new JsonParseException("Fuel rate must have either an item or a tag");
                }

            } catch (IllegalArgumentException | JsonParseException e) {
                LOGGER.error("Couldn't parse fuel rate for {}", id, e);
            }
        }

        LOGGER.debug("Loaded {} mana fuel entries from data pack:", FUEL_RATES.size());
        for (ResourceLocation id : objects.keySet()) {
            LOGGER.debug(" ├─ ID: {}", id);
            LOGGER.debug(" │   ↳ Path: data/{}/{}.json", id.getNamespace(), id.getPath());
        }

    }


    // Method to get fuel rate for an item
    public static FuelRate getFuelRateForItem(ResourceLocation itemId) {
        // 1️⃣ **先嘗試直接使用物品 ID 查找**
        FuelRate rate = FUEL_RATES.get(itemId.toString());
        if (rate != null) {
//            LOGGER.info("[FuelRateLoader] ✅ 透過物品 ID 找到燃料: {} | manaRate: {} | burnTime: {}",
//                    itemId, rate.getManaRate(), rate.getBurnTime());
            return rate;
        }

        // 2️⃣ **如果物品 ID 查找失敗，則嘗試標籤查找**
        for (Map.Entry<String, FuelRate> entry : FUEL_RATES.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("tag:")) {
                String tagName = key.substring(4);
                TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(),ResourceLocation.fromNamespaceAndPath(DEFAULT_NAMESPACE, tagName));
                if (BuiltInRegistries.ITEM.getTag(tag).stream()
                        .flatMap(holderSet -> holderSet.stream())
                        .anyMatch(holder -> holder.value().builtInRegistryHolder().key().location().equals(itemId))) {
//                    LOGGER.info("[FuelRateLoader] ✅ 透過標籤找到燃料: {} (標籤: {}) | manaRate: {} | burnTime: {}",
//                            itemId, tagName, entry.getValue().getManaRate(), entry.getValue().getBurnTime());
                    return entry.getValue();
                }
            }
        }

        // 3️⃣ **如果 `FuelRecipe` 內沒有對應物品，則使用 `ForgeHooks.getBurnTime()` 查找**
        Item item = BuiltInRegistries.ITEM.get(itemId);
        int defaultBurnTime = FuelRegistryHelper.getBurnTime(new ItemStack(item));
        if (defaultBurnTime > 0) {
            LOGGER.info("[FuelRateLoader] 🔥 Using ForgeHooks burn time fallback: {} | burnTime: {}", itemId, defaultBurnTime);
            return new FuelRate(0, defaultBurnTime, DEFAULT_ENERGY_RATE, DEFAULT_INTERVAL);
        }

        // 4️⃣ **如果完全找不到數據，使用預設燃燒時間**
        if (warnedAlready.add(itemId)) {
            LOGGER.warn("[FuelRateLoader] ❌ Fuel data not found for: {}. Using default values. manaRate: 0 | burnTime: {}", itemId, DEFAULT_BURN_TIME);
        }        return new FuelRate(0, DEFAULT_BURN_TIME, DEFAULT_ENERGY_RATE,DEFAULT_INTERVAL );
    }

    // Class representing fuel rate
    public static class FuelRate {
        public final int manaRate;
        public final int burnTime;
        public final int energyRate;
        private final int intervalTick; // ✅ 要記得定義這個欄位

        public FuelRate(int manaRate, int burnTime, int energyRate,int intervalTick) {
            this.manaRate = manaRate;
            this.burnTime = burnTime;
            this.energyRate = energyRate;
            this.intervalTick = intervalTick; // ✅ 記得存進來

        }

        public int getIntervalTick() {
            return Math.max(1, intervalTick);
        }
        public int getManaRate() {
            return manaRate;
        }

        public int getBurnTime() {
            return burnTime;
        }

        public int getEnergyRate() {
            return energyRate;
        }


    }

    public static Set<String> getAllDefinedFuelIds() {
        return FUEL_RATES.keySet();
    }


}

