package com.github.nalamodikk.common.item.debug;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.register.ModCapabilities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManaDebugToolItem extends Item {
    public static final String TAG_MODE_INDEX = "ModeIndex";

    private static final int[] MANA_AMOUNTS = {10, 100, 1000, 10000, 100000};
    private static final String[] MODES = {
            "message.koniava.mana_mode_add_10",
            "message.koniava.mana_mode_add_100",
            "message.koniava.mana_mode_add_1000",
            "message.koniava.mana_mode_add_10000",
            "message.koniava.mana_mode_add_100000"
    };

    public ManaDebugToolItem(Properties properties) {
        super(properties);

    }

    public static final DataComponentType<Integer> MODE_INDEX =
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build();

    public static int getModeIndex(ItemStack stack) {
        return stack.getOrDefault(MODE_INDEX, 0);
    }

    public static void setModeIndex(ItemStack stack, int index) {
        stack.set(MODE_INDEX, index);
    }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            if (!level.isClientSide()) {
                BlockPos pos = context.getClickedPos();
                IUnifiedManaHandler manaStorage = level.getCapability(ModCapabilities.MANA, pos, null);

                if (manaStorage != null) {
                    ItemStack stack = context.getItemInHand();
                    int modeIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
                    int manaToAdd = MANA_AMOUNTS[modeIndex];
                    manaStorage.addMana(manaToAdd);

                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                                Component.translatable("message.koniava.mana_added", manaToAdd, manaStorage.getManaStored()), true);
                    }

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        // 🔧 治本：使用延遲更新避免立即觸發掃描
                        level.scheduleTick(pos, blockEntity.getBlockState().getBlock(), 1);
                    }

                    // ⚠️ 請自行確認 ManaUpdatePacket 是否已改寫為 NeoForge Payload 風格
                    PacketDistributor.sendToPlayer((ServerPlayer) context.getPlayer(),
                            new ManaUpdatePacket(pos, manaStorage.getManaStored()));

                    return InteractionResult.SUCCESS;
                }
            }
            return super.useOn(context);
        }

    public void cycleMode(ItemStack stack, boolean forward, ServerPlayer player) {
        int currentIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
        int newIndex;

        if (forward) {
            newIndex = (currentIndex + 1) % MANA_AMOUNTS.length;
        } else {
            newIndex = (currentIndex - 1 + MANA_AMOUNTS.length) % MANA_AMOUNTS.length;
        }

        stack.set(ManaDebugToolItem.MODE_INDEX, newIndex);

        // ✅ 使用具名模式的翻譯 key
        String modeKey = MODES[newIndex];

        player.displayClientMessage(Component.translatable(
                "message.koniava.mode_changed",
                Component.translatable(modeKey)
        ), true);

        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.info("[ModeChange] {} switched wand mode: {} → {}", player.getGameProfile().getName(), currentIndex, newIndex);
        }
    }


    public String getCurrentModeKey(ItemStack stack) {
        int modeIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
        return MODES[modeIndex];
    }




}
