package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ScanHudOverlay {

    private static final ResourceLocation HEX_CELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");
    private static final int CELL_W = 18, CELL_H = 18;
    private static final int ICON = 10;
    private static final int ICON_GAP = 4;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return;

        boolean holdingWatch = player.getMainHandItem().getItem() instanceof NaraWatchItem
                || player.getOffhandItem().getItem() instanceof NaraWatchItem;
        if (!holdingWatch) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        ResourceLocation targetId = resolveTargetId(mc, hit);
        if (targetId == null) return;

        boolean scanned = ClientResearchCache.hasScanned(targetId);
        List<ResourceLocation> aspectIds = ClientResearchCache.getScannedAspects(targetId);

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        Component statusText = Component.translatable(
                scanned ? "hud.koniava.scan.recorded" : "hud.koniava.scan.unrecorded")
                .withStyle(scanned ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        int y = cy + 16;
        g.drawCenteredString(font, statusText, cx, y, 0xFFFFFF);

        if (scanned && !aspectIds.isEmpty()) {
            renderAspectRow(g, font, cx, y + 12, aspectIds);
        }
    }

    private static void renderAspectRow(GuiGraphics g, Font font, int cx, int y,
                                         List<ResourceLocation> aspectIds) {
        int totalW = 0;
        for (ResourceLocation id : aspectIds) {
            Aspect a = ModAspects.get(id);
            if (a == null) continue;
            totalW += ICON + 2 + font.width(a.getName().getString()) + ICON_GAP;
        }
        totalW = Math.max(0, totalW - ICON_GAP);

        int x = cx - totalW / 2;
        for (ResourceLocation id : aspectIds) {
            Aspect aspect = ModAspects.get(id);
            if (aspect == null) continue;

            int c = aspect.getColor();
            RenderSystem.setShaderColor(
                    ((c >> 16) & 0xFF) / 255f,
                    ((c >> 8)  & 0xFF) / 255f,
                    (c         & 0xFF) / 255f,
                    0.9f);
            g.blit(HEX_CELL_TEXTURE, x, y, 0, 0, ICON, ICON, CELL_W, CELL_H);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            String path = id.getPath();
            String letter = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('/') + 2).toUpperCase();
            g.drawString(font, letter, x + 2, y + 1, 0xFFFFFF, true);

            String name = aspect.getName().getString();
            g.drawString(font, name, x + ICON + 2, y + 1, 0xDDDDDD, true);

            x += ICON + 2 + font.width(name) + ICON_GAP;
        }
    }

    @Nullable
    private static ResourceLocation resolveTargetId(Minecraft mc, HitResult hit) {
        if (hit instanceof BlockHitResult blockHit) {
            if (mc.level == null) return null;
            var fluid = mc.level.getFluidState(blockHit.getBlockPos());
            if (!fluid.isEmpty()) return BuiltInRegistries.FLUID.getKey(fluid.getType());
            var state = mc.level.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) return null;
            return BuiltInRegistries.BLOCK.getKey(state.getBlock());
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof Player tp) {
                String uuidHex = tp.getStringUUID().replace("-", "");
                return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "player_" + uuidHex);
            }
            if (entity instanceof ItemEntity ie) {
                return BuiltInRegistries.ITEM.getKey(ie.getItem().getItem());
            }
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }
        return null;
    }
}
