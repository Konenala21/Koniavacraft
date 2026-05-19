package com.github.nalamodikk.common.item;

import com.github.nalamodikk.client.renderer.BezierCurveRenderer;
import com.github.nalamodikk.client.renderer.ClientEffectEvents;
import com.github.nalamodikk.client.renderer.EpicycloidRenderer;
import com.github.nalamodikk.client.renderer.FlowFieldRenderer;
import com.github.nalamodikk.client.renderer.FourierCurveRenderer;
import com.github.nalamodikk.client.renderer.FresnelSphereRenderer;
import com.github.nalamodikk.client.renderer.KochSnowflakeRenderer;
import com.github.nalamodikk.client.renderer.LissajousRenderer;
import com.github.nalamodikk.client.renderer.LorenzAttractorRenderer;
import com.github.nalamodikk.client.renderer.MagicCircleRenderer;
import com.github.nalamodikk.client.renderer.MobiusStripRenderer;
import com.github.nalamodikk.client.renderer.NBodyRenderer;
import com.github.nalamodikk.client.renderer.OrbitalTestShaderRenderer;
import com.github.nalamodikk.client.renderer.PolarRoseRenderer;
import com.github.nalamodikk.client.renderer.RippleRenderer;
import com.github.nalamodikk.client.renderer.SpiralCurveRenderer;
import com.github.nalamodikk.client.renderer.TorusKnotRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DevRenderTestItem2 extends Item {

    public DevRenderTestItem2(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.literal("Right-click: spawn current mode effect"));
        tooltipComponents.add(Component.literal("Shift+Scroll: cycle modes"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ClientEffectEvents.DevMode2 mode = ClientEffectEvents.currentMode2;
            Vec3 pos = player.position();
            long tick = level.getGameTime();

            switch (mode) {
                case FOURIER -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
                    FourierCurveRenderer.spawnEffect(pos, tick);
                }
                case MAGIC_CIRCLE -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
                    MagicCircleRenderer.spawnEffect(pos, tick);
                }
                case FUSION -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 0.8f, 0.9f);
                    MagicCircleRenderer.spawnEffect(pos, tick);
                    FourierCurveRenderer.spawnEffect(pos.add(0, 0.06, 0), tick);
                    OrbitalTestShaderRenderer.spawnEffect(pos.add(0, 2.5, 0), tick);
                }
                case CHAOS -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.8f);
                    // NEW effects only — no existing ones
                    RippleRenderer.spawnEffect(pos, tick);
                    SpiralCurveRenderer.spawnEffect(pos.add(0, 0.04, 0), tick);
                    LissajousRenderer.spawnEffect(pos.add(0, 0.08, 0), tick);
                    FresnelSphereRenderer.spawnEffect(pos.add(0, 1.8, 0), tick);
                }
                case POLAR_ROSE -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
                    PolarRoseRenderer.spawnEffect(pos, tick);
                }
                case TORUS_KNOT -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 0.8f);
                    TorusKnotRenderer.spawnEffect(pos, tick);
                }
                case EPICYCLOID -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.0f);
                    EpicycloidRenderer.spawnEffect(pos, tick);
                }
                case MOBIUS -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 0.7f, 1.2f);
                    MobiusStripRenderer.spawnEffect(pos, tick);
                }
                case FLOW_FIELD -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.9f);
                    FlowFieldRenderer.spawnEffect(pos, tick);
                }
                case KOCH -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.3f);
                    KochSnowflakeRenderer.spawnEffect(pos, tick);
                }
                case LORENZ -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.4f, 1.6f);
                    LorenzAttractorRenderer.spawnEffect(pos, tick);
                }
                case BEZIER -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.1f);
                    BezierCurveRenderer.spawnEffect(pos, tick);
                }
                case N_BODY -> {
                    level.playSound(player, pos.x, pos.y, pos.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.0f);
                    NBodyRenderer.spawnEffect(pos, tick);
                }
            }

            player.displayClientMessage(
                Component.literal("[Dev2] " + mode.label), true
            );
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
