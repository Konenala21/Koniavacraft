package com.github.nalamodikk.compat.jade;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class KoniavaJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(AspectAltarJadeProvider.INSTANCE, AspectAltarBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(AspectAltarJadeProvider.INSTANCE, AspectAltarBlock.class);
    }
}
