package com.github.nalamodikk.compat.jade;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class KoniavaJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 祭壇：自訂 provider（成形狀態 + 底座）
        registration.registerBlockDataProvider(AspectAltarJadeProvider.INSTANCE, AspectAltarBlockEntity.class);

        // 魔力顯示：所有有魔力的機器
        registration.registerBlockDataProvider(ManaJadeProvider.INSTANCE, AbstractManaMachineEntityBlock.class);
        registration.registerBlockDataProvider(ManaJadeProvider.INSTANCE, AspectAltarBlockEntity.class);
        registration.registerBlockDataProvider(ManaJadeProvider.INSTANCE, ArcaneConduitBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 祭壇自訂顯示
        registration.registerBlockComponent(AspectAltarJadeProvider.INSTANCE, AspectAltarBlock.class);

        // 魔力顯示
        registration.registerBlockComponent(ManaJadeProvider.INSTANCE, BaseMachineBlock.class);
        registration.registerBlockComponent(ManaJadeProvider.INSTANCE, AspectAltarBlock.class);
        registration.registerBlockComponent(ManaJadeProvider.INSTANCE, ArcaneConduitBlock.class);
    }
}
