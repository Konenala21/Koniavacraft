package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity.TOTAL_SLOTS;

public class ManaCraftingMenu extends AbstractContainerMenu {
    private final ManaCraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final IItemHandler itemHandler;
    private final Player menuPlayer;
    private final DataSlot manaStored = DataSlot.standalone();
    private final DataSlot researchLocked = DataSlot.standalone();
    private static final Logger LOGGER = LogManager.getLogger();


    public static final int OUTPUT_SLOT = 9;
    public static final int INPUT_SLOT_START = 0;
    public static final int INPUT_SLOT_END = 8;
    private static final int MANA_COST_PER_CRAFT = 50;


    public ManaCraftingTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public static ManaCraftingMenu create(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInventory.player.level();

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ManaCraftingTableBlockEntity manaTable) {
            return new ManaCraftingMenu(windowId, playerInventory, manaTable.getItemHandler(),
                    ContainerLevelAccess.create(level, pos), level);
        }

        // fallback：沒有 block entity
        return new ManaCraftingMenu(windowId, playerInventory, new ItemStackHandler(TOTAL_SLOTS),
                ContainerLevelAccess.NULL, level);

    }


    public ManaCraftingMenu(int containerId, Inventory playerInventory, IItemHandler itemHandler, ContainerLevelAccess access, Level level) {
        super(ModMenuTypes.MANA_CRAFTING_MENU.get(), containerId);
        this.access = access;
        this.itemHandler = itemHandler;
        this.menuPlayer = playerInventory.player;

        // 初始化 blockEntity 變量
        this.blockEntity = access.evaluate((world, pos) -> {
            if (world != null) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof ManaCraftingTableBlockEntity) {
                    return (ManaCraftingTableBlockEntity) entity;
                }
            }
            return null;
        }).orElse(null);

// 如果 blockEntity 為空，記錄警告日誌
        if (this.blockEntity == null) {
           // System.out.println("Warning: ManaCraftingTableBlockEntity could not be found at the specified position");
        }

        // 設置 3x3 合成槽
        updateResearchLockedState();

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new UpdatingSlotItemHandler(itemHandler, j + i * 3, 30 + j * 18, 17 + i * 18, this));
            }
        }

        // 設置輸出槽
        this.addSlot(new UpdatingSlotItemHandler(itemHandler, OUTPUT_SLOT, 124, 35 ,this) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 輸出槽不允許手動放入物品
            }

            @Override
            public ItemStack getItem() {
                return isResearchLocked() ? ItemStack.EMPTY : super.getItem();
            }

            @Override
            public boolean hasItem() {
                return !isResearchLocked() && super.hasItem();
            }

            @Override
            public boolean mayPickup(Player player) {
                return this.hasItem()
                        && !isResearchLocked();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (isResearchLocked()) {
                    return;
                }

                if (blockEntity != null) {
                    Optional<RecipeHolder<ManaCraftingTableRecipe>> recipeOpt = blockEntity.getLastMatchedRecipeHolder();

                    if (recipeOpt.isPresent()) {
                        ManaCraftingTableRecipe recipe = recipeOpt.get().value();
                        int manaCost = recipe.getManaCost();

                        if (blockEntity.hasSufficientMana(manaCost) && blockEntity.tryConsumeMana(manaCost)) {
                            // ✅ 扣材料
                            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                                blockEntity.getItemHandler().extractItem(i, 1, false);
                            }

                            // ❗ 這裡不建議清空 output，由 updateCraftingResult 決定要不要產物
                            blockEntity.updateCraftingResult();
                        }
                    }
                }

                super.onTake(player, stack);
            }
        });

        // 設置玩家的物品欄槽
        addPlayerInventorySlots(playerInventory);

        // 同步魔力值
        this.addDataSlot(manaStored);
        this.addDataSlot(researchLocked);
    }

    // 获取当前的魔力存储量
    public int getManaStored() {
        return manaStored.get();
    }

    public boolean isResearchLocked() {
        return researchLocked.get() != 0;
    }

    public boolean shouldShowMissingResearchWarning() {
        return isResearchLocked()
                && blockEntity != null
                && blockEntity.getLastMatchedRecipeHolder().isPresent();
    }


    /**
     * Handles shift-click output crafting from the output slot.
     * Supports automatic recipe re-evaluation and continuous crafting
     * while materials and inventory space are sufficient.
     */

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack clickedStack = slot.getItem();
        ItemStack originalStack = clickedStack.copy();
        if (index == OUTPUT_SLOT) {
            if (isResearchLocked()) {
                return ItemStack.EMPTY;
            }
            blockEntity.updateCraftingResult(); // ✅ 確保 lastMatchedRecipe 有內容
            LOGGER.debug("[QuickMove] Start");

            Optional<ManaCraftingTableRecipe> recipeOpt = blockEntity.getLastMatchedRecipe();
            if (recipeOpt.isEmpty()) return ItemStack.EMPTY;

            ManaCraftingTableRecipe recipe = recipeOpt.get();
            int manaCost = recipe.getManaCost();
            if (manaCost <= 0) return ItemStack.EMPTY;

            // 準備目前的輸入
            ManaCraftingTableRecipe.ManaCraftingInput input = blockEntity.getManaCraftingInput();

            // ✅ 計算最大合成次數：由配方判定哪些材料足夠
            int maxCraft = recipe.getMaxCraftsPossible(input);
            maxCraft = Math.min(maxCraft, blockEntity.getManaStored() / manaCost); // 再限制魔力

            LOGGER.debug("[QuickMove] maxCraft = {}", maxCraft);

            ItemStack totalResult = ItemStack.EMPTY;

            for (int i = 0; i < maxCraft; i++) {
                input = blockEntity.getManaCraftingInput(); // 每次重建輸入（已經會 shrink）
                ItemStack simulated = recipe.assemble(input, player.level().registryAccess());
                if (simulated.isEmpty()) break;

                // ✅ 扣魔力
                blockEntity.consumeMana(manaCost);

                // ✅ 扣材料
                for (int j = INPUT_SLOT_START; j <= INPUT_SLOT_END; j++) {
                    ItemStack in = blockEntity.getItemHandler().getStackInSlot(j);
                    if (!in.isEmpty()) in.shrink(1);
                }

                // ✅ 嘗試搬到背包
                if (!this.moveItemStackTo(simulated.copy(), TOTAL_SLOTS, this.slots.size(), true)) {
                    LOGGER.debug("[QuickMove] 背包滿了，中止批次合成");
                    break;
                }

                if (totalResult.isEmpty()) {
                    totalResult = simulated.copy();
                } else if (ItemStack.isSameItemSameComponents(totalResult, simulated)) {
                    totalResult.grow(simulated.getCount());
                } else {
                    LOGGER.debug("[QuickMove] 不同產物 → 中止批次合成");
                    break;
                }
            }

            blockEntity.setChanged();               // 同步
            blockEntity.updateCraftingResult();     // 更新產物
            return totalResult;
        }



        // ✅ 玩家背包 → 嘗試放進合成槽
        if (index >= TOTAL_SLOTS) {
            if (!this.moveItemStackTo(clickedStack, INPUT_SLOT_START, INPUT_SLOT_END + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        // ✅ 合成槽 → 移回背包
        else if (index >= INPUT_SLOT_START && index <= INPUT_SLOT_END) {
            if (!this.moveItemStackTo(clickedStack, TOTAL_SLOTS, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        }

        if (clickedStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (clickedStack.getCount() == originalStack.getCount()) {

            return ItemStack.EMPTY;
        }

        slot.onTake(player, clickedStack);
        return originalStack;
    }



    private boolean canInsertIntoPlayerInventory(ItemStack stack) {
        for (int i = TOTAL_SLOTS; i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.mayPlace(stack) && slot.hasItem()) {
                ItemStack existing = slot.getItem();
                if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    return true;
                }
            } else if (!slot.hasItem()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (blockEntity != null && !blockEntity.getLevel().isClientSide()) {
            blockEntity.updateCraftingResult(); // ✅ server 更新產物

            blockEntity.setChanged(); // 標記要同步
            BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
            blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
        }

        // ✅ ✅ ✅ 客戶端預測顯示結果（只加這一段）
        if (blockEntity != null && blockEntity.getLevel().isClientSide()) {
            blockEntity.updateCraftingResult(); // 客戶端顯示產物
        }
    }



    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ? blockEntity.getBlockState().getBlock() : null);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        // 主物品栏
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            Level level = blockEntity.getLevel();
            if (level != null && !level.isClientSide()) {
                manaStored.set(blockEntity.getManaStored());
                updateResearchLockedState();
            }
        }
        super.broadcastChanges();
    }

    private void updateResearchLockedState() {
        if (blockEntity == null) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide()) {
            researchLocked.set(ResearchGate.hasResearch("mana_crafting_table", menuPlayer, level) ? 0 : 1);
        }
    }

}
