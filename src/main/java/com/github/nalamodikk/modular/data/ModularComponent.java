package com.github.nalamodikk.modular.data;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentType;



import java.util.List;

public record ModularComponent(int slotCount, List<ResourceLocation> installedModules) {

    public static final Codec<ModularComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slotCount").forGetter(ModularComponent::slotCount),
            ResourceLocation.CODEC.listOf().fieldOf("installedModules").forGetter(ModularComponent::installedModules)
    ).apply(instance, ModularComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ModularComponent::slotCount,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ModularComponent::installedModules,
                    ModularComponent::new
            );

    public static final DataComponentType<ModularComponent> TYPE = DataComponentType.<ModularComponent>builder()
            .persistent(CODEC)
            .networkSynchronized(STREAM_CODEC)  // ✅ 現在是明確泛型
            .build();

}
