package com.github.nalamodikk.register;


import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KoniavacraftMod.MOD_ID);

    private static final Codec<ItemStack> ITEMSTACK_CODEC_ALLOW_EMPTY =
            ItemStack.OPTIONAL_CODEC;


    public static final Supplier<AttachmentType<Boolean>> NARA_BOUND =
            ATTACHMENT_TYPES.register("nara_bound", () ->
                    AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build());

    // 飾品裝備本身的附加資料（10格：8格一般裝備 + 2格浮游砲槽）
    public static final Supplier<AttachmentType<NonNullList<ItemStack>>> EXTRA_EQUIPMENT =
            ATTACHMENT_TYPES.register("extra_equipment", () ->
                    AttachmentType.<NonNullList<ItemStack>>builder(() -> NonNullList.withSize(10, ItemStack.EMPTY))
                            .serialize(Codec.list(ITEMSTACK_CODEC_ALLOW_EMPTY).xmap(
                                    list -> {
                                        NonNullList<ItemStack> result = NonNullList.withSize(10, ItemStack.EMPTY);
                                        for (int i = 0; i < Math.min(list.size(), 10); i++) {
                                            result.set(i, list.get(i));
                                        }
                                        return result;
                                    },
                                    list -> {
                                        List<ItemStack> plain = new ArrayList<>(10);
                                        for (int i = 0; i < 10; i++) {
                                            plain.add(i < list.size() ? list.get(i) : ItemStack.EMPTY);
                                        }
                                        return plain;
                                    }
                            ))
                            .copyOnDeath()
                            .build()
            );

    // 玩家最後一次進入戰鬥狀態的遊戲 tick（-1 = 未曾戰鬥）
    public static final Supplier<AttachmentType<Long>> LAST_COMBAT_TIME =
            ATTACHMENT_TYPES.register("last_combat_time", () ->
                    AttachmentType.<Long>builder(() -> -1L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 進入鏡中世界前的返回點（維度 + 座標），死亡或結算時送回
    public static final Supplier<AttachmentType<Optional<GlobalPos>>> RETURN_POINT =
            ATTACHMENT_TYPES.register("return_point", () ->
                    AttachmentType.<Optional<GlobalPos>>builder(Optional::empty)
                            .serialize(GlobalPos.CODEC.optionalFieldOf("return_point").codec())
                            .copyOnDeath() // 死亡後保留，否則 respawn 時 onPlayerRespawn 讀不到、娜拉不嘲諷
                            .build()
            );


// 九格存儲飾品裝備
public static final Supplier<AttachmentType<NonNullList<ItemStack>>> NINE_GRID =
        ATTACHMENT_TYPES.register("nine_grid", () ->
                AttachmentType.<NonNullList<ItemStack>>builder(() -> NonNullList.withSize(9, ItemStack.EMPTY))
                        .serialize(Codec.list(ITEMSTACK_CODEC_ALLOW_EMPTY).xmap(
                                    list -> {
                                        NonNullList<ItemStack> result = NonNullList.withSize(9, ItemStack.EMPTY);
                                        for (int i = 0; i < Math.min(list.size(), 9); i++) {
                                            result.set(i, list.get(i));
                                        }
                                        return result;
                                    },
                                    list -> {
                                        List<ItemStack> plain = new ArrayList<>(9);
                                        for (int i = 0; i < 9; i++) {
                                            plain.add(i < list.size() ? list.get(i) : ItemStack.EMPTY);
                                        }
                                        return plain;
                                    }
                            ))
                            .copyOnDeath() // 可選：死亡是否保留
                            .build()
            );



    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
