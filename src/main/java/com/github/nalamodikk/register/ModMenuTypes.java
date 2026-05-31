package com.github.nalamodikk.register;
/**
 * 📦 NeoForge 1.21.1 GUI 菜單註冊總管
 *
 * 支援：
 * - 普通菜單註冊（無 BlockEntity）
 * - 自動 BlockEntity 解析（id, Inventory, BlockEntity）
 * - 自動 FriendlyByteBuf 傳輸與驗證
 * - 特殊手動菜單（如 UpgradeMenu）
 *
 * 用法範例：
 * registerMenuType("mana_generator", entityMenu(ManaGeneratorBlockEntity.class, ManaGeneratorMenu::new));
 */

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorMenu;
import com.github.nalamodikk.common.block.blockentity.skillencoder.SkillEncoderBlockEntity;
import com.github.nalamodikk.common.block.blockentity.skillencoder.SkillEncoderMenu;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitConfigMenu;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingMenu;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorMenu;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerMenu;
import com.github.nalamodikk.common.block.blockentity.mana_charger.ManaChargerBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_charger.ManaChargerMenu;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderMenu;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserMenu;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressMenu;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.common.screen.block.shared.FallbackUpgradeMenu;
import com.github.nalamodikk.common.screen.block.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.screen.block.shared.UpgradeMenu;
import com.github.nalamodikk.common.screen.player.ExtraEquipmentMenu;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;


public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ManaCraftingMenu>> MANA_CRAFTING_MENU =
            registerMenuType("mana_crafting", ManaCraftingMenu::create);

    public static final DeferredHolder<MenuType<?>, MenuType<UniversalConfigMenu>> UNIVERSAL_CONFIG_MENU =
            registerMenuType("universal_config", (id, inv, buf) ->
                    new UniversalConfigMenu(id, inv, buf) // ✅ 使用客戶端構造函數
            );

    public static final DeferredHolder<MenuType<?>, MenuType<ManaGeneratorMenu>> MANA_GENERATOR_MENU =
            registerMenuType("mana_generator_menu", entityMenu(
                    ManaGeneratorBlockEntity.class,
                    ManaGeneratorMenu::new
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarManaCollectorMenu>> SOLAR_MANA_COLLECTOR_MENU =
            registerMenuType("solar_mana_collector", entityMenu(
                    SolarManaCollectorBlockEntity.class,
                    SolarManaCollectorMenu::new
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<UpgradeMenu>> UPGRADE_MENU =
            registerMenuType("upgrade_menu", ModMenuTypes::createUpgradeMenu);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneConduitConfigMenu>> CONDUIT_CONFIG_MENU =
            registerMenuType("conduit_config_menu", ArcaneConduitConfigMenu::new);


    // === ⚡ 魔力充能台菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<ManaChargerMenu>> MANA_CHARGER_MENU =
            registerMenuType("mana_charger", (id, inv, buf) -> {
                BlockPos pos = buf.readBlockPos();
                Level level = inv.player.level();
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof ManaChargerBlockEntity charger))
                    throw new IllegalStateException("Expected ManaChargerBlockEntity at " + pos);
                return new ManaChargerMenu(id, inv, charger);
            });

    // === 🔮 新增：魔力注入機菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<ManaInfuserMenu>> MANA_INFUSER =
            registerMenuType("mana_infuser",
                    (id, inv, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        Level level = inv.player.level();
                        BlockEntity be = level.getBlockEntity(pos);
                        if (!(be instanceof ManaInfuserBlockEntity infuser)) {
                            throw new IllegalStateException("Expected ManaInfuserBlockEntity at " + pos + " but found " +
                                    (be != null ? be.getClass().getSimpleName() : "null"));
                        }
                        return new ManaInfuserMenu(id, inv, infuser);
                    });

    // === 🪄 技能核心編碼台菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<SkillEncoderMenu>> SKILL_ENCODER_MENU =
            registerMenuType("skill_encoder",
                    entityMenu(SkillEncoderBlockEntity.class, SkillEncoderMenu::new));

    // === ⚙️ 新增：魔力粉碎機菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<ManaGrinderMenu>> MANA_GRINDER_MENU =
            registerMenuType("mana_grinder",
                    (id, inv, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        Level level = inv.player.level();
                        BlockEntity be = level.getBlockEntity(pos);
                        if (!(be instanceof ManaGrinderBlockEntity grinder)) {
                            throw new IllegalStateException("Expected ManaGrinderBlockEntity at " + pos + " but found " +
                                    (be != null ? be.getClass().getSimpleName() : "null"));
                        }
                        return new ManaGrinderMenu(id, inv, grinder);
                    });

    // === 魔力部署器菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<ManaDeployerMenu>> MANA_DEPLOYER_MENU =
            registerMenuType("mana_deployer",
                    (id, inv, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        Level level = inv.player.level();
                        BlockEntity be = level.getBlockEntity(pos);
                        if (!(be instanceof ManaDeployerBlockEntity deployer)) {
                            throw new IllegalStateException("Expected ManaDeployerBlockEntity at " + pos + " but found " +
                                    (be != null ? be.getClass().getSimpleName() : "null"));
                        }
                        return new ManaDeployerMenu(id, inv, deployer);
                    });

    // === 🔩 魔力壓板機菜單 ===
    public static final DeferredHolder<MenuType<?>, MenuType<ManaPlatePressMenu>> MANA_PLATE_PRESS_MENU =
            registerMenuType("mana_plate_press",
                    (id, inv, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        Level level = inv.player.level();
                        BlockEntity be = level.getBlockEntity(pos);
                        if (!(be instanceof ManaPlatePressBlockEntity press)) {
                            throw new IllegalStateException("Expected ManaPlatePressBlockEntity at " + pos + " but found " +
                                    (be != null ? be.getClass().getSimpleName() : "null"));
                        }
                        return new ManaPlatePressMenu(id, inv, press);
                    });

    /**
     *
     * 關於玩家
     *
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ExtraEquipmentMenu>> EXTRA_EQUIPMENT_MENU =
            registerMenuType("extra_equipment", ExtraEquipmentMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            registerMenuType("research_table", ResearchTableMenu::new);


    /***
     *
     * @param id
     * @param playerInv
     * @param extraData
     * @return
     */
    @SuppressWarnings("resource")
    private static UpgradeMenu createUpgradeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        Level level = playerInv.player.level();

        if (!level.isLoaded(pos)) {
            KoniavacraftMod.LOGGER.warn("UpgradeMenu open failed: chunk at {} not loaded.", pos);
            return new FallbackUpgradeMenu(id, playerInv);

        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IUpgradeableMachine machine) {
            return new UpgradeMenu(id, playerInv, machine.getUpgradeInventory(), machine);
        }

        KoniavacraftMod.LOGGER.warn("UpgradeMenu open failed: BlockEntity at {} is not IUpgradeableMachine.", pos);
        return new FallbackUpgradeMenu(id, playerInv);
    }


    public static <T extends BlockEntity, M extends AbstractContainerMenu> IContainerFactory<M> entityMenu(
            Class<T> entityClass,
            BiFunction<Integer, T, M> constructor
    ) {
        return (id, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (!entityClass.isInstance(be)) {
                throw new IllegalStateException("Expected %s at %s but found %s".formatted(
                        entityClass.getSimpleName(), pos, be != null ? be.getClass().getSimpleName() : "null"));
            }
            return constructor.apply(id, entityClass.cast(be));
        };
    }

    public static <T extends BlockEntity, M extends AbstractContainerMenu> IContainerFactory<M> entityMenu(
            Class<T> entityClass,
            TriFunction<Integer, Inventory, T, M> constructor
    ) {
        return (id, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (!entityClass.isInstance(be)) {
                throw new IllegalStateException("Expected %s at %s but found %s".formatted(
                        entityClass.getSimpleName(), pos, be != null ? be.getClass().getSimpleName() : "null"));
            }
            return constructor.apply(id, inv, entityClass.cast(be));
        };
    }



    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }


}
