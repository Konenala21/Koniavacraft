package com.github.nalamodikk.common.block.blockentity.altar.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.projection.GhostProjectionState;
import com.github.nalamodikk.common.block.blockentity.altar.AltarPillarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarMultiblockCategory implements IRecipeCategory<AltarStructureInfo> {

    public static final RecipeType<AltarStructureInfo> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "altar_multiblock", AltarStructureInfo.class);

    private static final int WIDTH  = 178;
    private static final int HEIGHT = 138;

    // Slider row at top
    private static final int SLIDER_H = 14;
    private static final int SL_X1    = 12;
    private static final int SL_X2    = 166;
    private static final int SL_STEPS = 6;   // 0‥6, 7 snap positions

    // Control row
    private static final int CTRL_Y   = 100;
    private static final int CTRL_H   = 12;
    private static final int TOGGLE_X = 96;
    private static final int TOGGLE_W = 80;

    // 3D preview anchor
    private static final float PREVIEW_X  = 89f;
    private static final float PREVIEW_Y  = 57f;
    private static final float BLOCK_SIZE = 13f;
    private static final float TILT       = 30f;

    // Material row
    private static final int MAT_Y      = 116;
    private static final int MAT_X1     = 34;
    private static final int MAT_X2     = 80;
    private static final int MAT_X3     = 126;
    // Inventory-check button (right side of material row)
    private static final int INV_BTN_X  = 154;
    private static final int INV_BTN_W  = 22;
    private static final int INV_BTN_H  = 12;
    // Project-to-world button (left side of material row)
    private static final int PRJ_BTN_X  = 4;
    private static final int PRJ_BTN_W  = 26;
    private static final int PRJ_BTN_H  = 12;

    // Base structure block positions
    private static final int[][] ALL_POSITIONS = {
        { 0, 0, 0},
        {-3,-2,-3}, {-3,-1,-3},
        {-3,-2, 3}, {-3,-1, 3},
        { 3,-2,-3}, { 3,-1,-3},
        { 3,-2, 3}, { 3,-1, 3},
        { 0,-2,-3},
        {-3,-2, 0},
        { 0,-2, 0},
        { 3,-2, 0},
        { 0,-2, 3},
    };

    // View state
    private int     sliderTier  = 0;
    private boolean showFormed  = false;
    private float   azimuth     = 45f;
    private float   elevation   = TILT;
    private double  scale       = 1.0;

    // Inventory check state — only updated when the Inv button is pressed
    private boolean showInventoryStatus = false;
    private int[]   cachedCounts        = {0, 0, 0}; // [altar, mana_block, pedestal]

    private final IDrawable icon;

    public AltarMultiblockCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ASPECT_ALTAR.get()));
    }

    @Override public @NotNull RecipeType<AltarStructureInfo> getRecipeType() { return RECIPE_TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.koniava.altar_multiblock.title"); }
    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    // ── Recipe layout (JEI slots + tooltip callbacks) ──────────────────────────

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder,
                          @NotNull AltarStructureInfo recipe,
                          @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, MAT_X1, MAT_Y)
                .addItemStack(new ItemStack(ModBlocks.ASPECT_ALTAR.get()))
                .addTooltipCallback((sv, tip) -> appendInvTooltip(tip, 0, 1));

        builder.addSlot(RecipeIngredientRole.INPUT, MAT_X2, MAT_Y)
                .addItemStack(new ItemStack(ModBlocks.MANA_BLOCK.get()))
                .addTooltipCallback((sv, tip) -> appendInvTooltip(tip, 1, cumulativeManaCount()));

        builder.addSlot(RecipeIngredientRole.INPUT, MAT_X3, MAT_Y)
                .addItemStack(new ItemStack(ModBlocks.ASPECT_PEDESTAL.get()))
                .addTooltipCallback((sv, tip) -> appendInvTooltip(tip, 2, 5));
    }

    /** Append need/have/missing lines to a JEI slot tooltip when inventory mode is active. */
    private void appendInvTooltip(List<Component> tip, int idx, int needed) {
        if (!showInventoryStatus) return;
        int have = cachedCounts[idx];
        tip.add(Component.translatable("jei.koniava.altar_multiblock.inv.need", needed)
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("jei.koniava.altar_multiblock.inv.have", have)
                .withStyle(have >= needed ? ChatFormatting.GREEN
                         : have > 0       ? ChatFormatting.YELLOW
                                          : ChatFormatting.RED));
        if (have < needed)
            tip.add(Component.translatable("jei.koniava.altar_multiblock.inv.missing", needed - have)
                    .withStyle(ChatFormatting.YELLOW));
    }

    // ── Extra widgets ──────────────────────────────────────────────────────────

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder,
                                   @NotNull AltarStructureInfo recipe,
                                   @NotNull IFocusGroup focuses) {
        builder.addGuiEventListener(new SliderWidget());
        builder.addGuiEventListener(new PreviewWidget());
        builder.addGuiEventListener(new ToggleWidget());
        builder.addGuiEventListener(new InventoryButtonWidget());
        builder.addGuiEventListener(new ProjectButtonWidget());
    }

    // ── Draw ───────────────────────────────────────────────────────────────────

    @Override
    public void draw(@NotNull AltarStructureInfo recipe, @NotNull IRecipeSlotsView slotsView,
                     @NotNull GuiGraphics gfx, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // ── Slider ───────────────────────────────────────────────────────────
        drawSlider(gfx);

        // ── 3D preview ────────────────────────────────────────────────────────
        render3DPreview(gfx);

        // ── Control row ───────────────────────────────────────────────────────
        gfx.fill(0, CTRL_Y - 1, WIDTH, CTRL_Y + CTRL_H + 1, 0xFF141420);
        gfx.fill(0, CTRL_Y - 1, WIDTH, CTRL_Y, 0xFF555566);

        // Tier text (left)
        String tierText = sliderTier == 0
                ? Component.translatable("jei.koniava.altar_multiblock.base_only").getString()
                : "T" + sliderTier + "  +" + sliderTier + " "
                  + Component.translatable("jei.koniava.altar_multiblock.rings").getString();
        gfx.drawString(font, tierText, 4, CTRL_Y + 2, 0xFFFF88, false);

        // Toggle button (right)
        boolean hovToggle = mouseX >= TOGGLE_X && mouseX < TOGGLE_X + TOGGLE_W
                         && mouseY >= CTRL_Y    && mouseY < CTRL_Y + CTRL_H;
        String toggleLabel = Component.translatable(
                showFormed ? "jei.koniava.altar_multiblock.btn_unformed"
                           : "jei.koniava.altar_multiblock.btn_formed").getString();
        gfx.fill(TOGGLE_X, CTRL_Y, TOGGLE_X + TOGGLE_W, CTRL_Y + CTRL_H,
                showFormed ? 0xFF2E6E42 : (hovToggle ? 0xFF4A4A6A : 0xFF333355));
        gfx.renderOutline(TOGGLE_X, CTRL_Y, TOGGLE_W, CTRL_H, 0xFF888888);
        gfx.drawString(font, toggleLabel,
                TOGGLE_X + TOGGLE_W / 2 - font.width(toggleLabel) / 2,
                CTRL_Y + (CTRL_H - font.lineHeight) / 2 + 1, 0xFFFFFF, false);

        // ── Material row count labels ─────────────────────────────────────────
        // Items are rendered by JEI slots; only draw count with optional color
        int totalMana = cumulativeManaCount();
        drawCount(gfx, font, "×1",            MAT_X1, MAT_Y, countColor(0, 1));
        drawCount(gfx, font, "×" + totalMana, MAT_X2, MAT_Y, countColor(1, totalMana));
        drawCount(gfx, font, "×5",            MAT_X3, MAT_Y, countColor(2, 5));

        // ── Project button (left of material row) ─────────────────────────────
        int pjY = MAT_Y + 2;
        boolean hovPrj  = mouseX >= PRJ_BTN_X && mouseX < PRJ_BTN_X + PRJ_BTN_W
                       && mouseY >= pjY        && mouseY < pjY + PRJ_BTN_H;
        boolean prjActive = GhostProjectionState.isActive();
        String prjLabel = prjActive ? "PRJ✓" : "PRJ";
        gfx.fill(PRJ_BTN_X, pjY, PRJ_BTN_X + PRJ_BTN_W, pjY + PRJ_BTN_H,
                prjActive ? 0xFF2E5E7A : (hovPrj ? 0xFF3A5A7A : 0xFF253045));
        gfx.renderOutline(PRJ_BTN_X, pjY, PRJ_BTN_W, PRJ_BTN_H,
                prjActive ? 0xFF55AAFF : 0xFF6688AA);
        gfx.drawString(font, prjLabel,
                PRJ_BTN_X + PRJ_BTN_W / 2 - font.width(prjLabel) / 2,
                pjY + (PRJ_BTN_H - font.lineHeight) / 2 + 1,
                prjActive ? 0xFF88CCFF : 0xFF8899BB, false);

        // ── Inventory button ──────────────────────────────────────────────────
        int ibY = MAT_Y + 2;
        boolean hovInv = mouseX >= INV_BTN_X && mouseX < INV_BTN_X + INV_BTN_W
                      && mouseY >= ibY        && mouseY < ibY + INV_BTN_H;
        String invLabel = showInventoryStatus ? "Inv✓" : "Inv";
        gfx.fill(INV_BTN_X, ibY, INV_BTN_X + INV_BTN_W, ibY + INV_BTN_H,
                showInventoryStatus ? 0xFF2E5E2E : (hovInv ? 0xFF4A4A6A : 0xFF333355));
        gfx.renderOutline(INV_BTN_X, ibY, INV_BTN_W, INV_BTN_H,
                showInventoryStatus ? 0xFF44BB44 : 0xFF888888);
        gfx.drawString(font, invLabel,
                INV_BTN_X + INV_BTN_W / 2 - font.width(invLabel) / 2,
                ibY + (INV_BTN_H - font.lineHeight) / 2 + 1,
                showInventoryStatus ? 0xFF88FF88 : 0xFFAAAAAA, false);
    }

    /** Draw a count label at the standard item-decoration position (x+17-width, y+9, z+200). */
    private void drawCount(GuiGraphics gfx, net.minecraft.client.gui.Font font,
                           String label, int x, int y, int color) {
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 200);
        gfx.drawString(font, label, x + 17 - font.width(label), y + 9, color, true);
        gfx.pose().popPose();
    }

    /** White normally; green/orange/red when inventory mode is active. */
    private int countColor(int idx, int needed) {
        if (!showInventoryStatus) return 0xFFFFFFFF;
        int have = cachedCounts[idx];
        if (have >= needed) return 0xFF44FF44;
        if (have > 0)       return 0xFFFFAA00;
        return 0xFFFF4444;
    }

    private void drawSlider(GuiGraphics gfx) {
        gfx.fill(0, 0, WIDTH, SLIDER_H, 0xFF1A1A2E);
        gfx.fill(0, SLIDER_H - 1, WIDTH, SLIDER_H, 0xFF555566);

        int cy = SLIDER_H / 2;
        // Track
        gfx.fill(SL_X1, cy - 1, SL_X2, cy + 1, 0xFF555577);
        // Ticks
        for (int i = 0; i <= SL_STEPS; i++) {
            int sx = sliderX(i);
            gfx.fill(sx - 1, cy - 4, sx + 1, cy + 4,
                    i == sliderTier ? 0xFFAABBFF : 0xFF666688);
        }
        // Thumb
        int tx = sliderX(sliderTier);
        gfx.fill(tx - 5, cy - 5, tx + 5, cy + 5, 0xFF5566BB);
        gfx.renderOutline(tx - 5, cy - 5, 10, 10, 0xFFAABBFF);
    }

    private int sliderX(int step) {
        return SL_X1 + step * (SL_X2 - SL_X1) / SL_STEPS;
    }

    private int cumulativeManaCount() {
        int total = 8; // 8 corner blocks in base
        for (int i = 0; i < sliderTier; i++)
            total += AspectAltarBlockEntity.ALL_RINGS.get(i).size();
        return total;
    }

    private void scanInventory() {
        cachedCounts[0] = countInInventory(ModBlocks.ASPECT_ALTAR.get().asItem());
        cachedCounts[1] = countInInventory(ModBlocks.MANA_BLOCK.get().asItem());
        cachedCounts[2] = countInInventory(ModBlocks.ASPECT_PEDESTAL.get().asItem());
    }

    private static int countInInventory(net.minecraft.world.item.Item item) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int n = 0;
        for (ItemStack s : player.getInventory().items)
            if (s.is(item)) n += s.getCount();
        return n;
    }

    // ── 3D rendering ───────────────────────────────────────────────────────────

    private void render3DPreview(GuiGraphics gfx) {
        var m  = gfx.pose().last().pose();
        int cx = Math.round(m.m30());
        int cy = Math.round(m.m31());
        gfx.enableScissor(cx, cy + SLIDER_H, cx + WIDTH, cy + CTRL_Y);

        Map<BlockPos, BlockState> renderMap = buildStructureMap();
        for (int i = 0; i < sliderTier; i++)
            renderMap.putAll(buildRingMap(i));

        var mc         = Minecraft.getInstance();
        var dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        RandomSource rng = RandomSource.create(42L);

        var ps = gfx.pose();
        ps.pushPose();
        ps.translate(PREVIEW_X, PREVIEW_Y, 200);
        float sz = (float) (BLOCK_SIZE * scale);
        ps.scale(sz, -sz, sz);
        ps.mulPose(Axis.XP.rotationDegrees(elevation));
        ps.mulPose(Axis.YP.rotationDegrees(azimuth));

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        for (Map.Entry<BlockPos, BlockState> entry : renderMap.entrySet()) {
            BlockPos   pos   = entry.getKey();
            BlockState state = entry.getValue();
            ps.pushPose();
            ps.translate(pos.getX(), pos.getY(), pos.getZ());
            if (state.is(ModBlocks.ALTAR_PILLAR.get())) {
                renderPillar(ps, buf, state, rng, pillarRotation(pos.getX(), pos.getZ()));
            } else {
                renderCulledBlock(ps, buf, state, renderMap, pos, rng, dispatcher);
            }
            ps.popPose();
        }

        buf.endBatch();
        RenderSystem.disableDepthTest();
        ps.popPose();
        gfx.disableScissor();
    }

    private void renderPillar(PoseStack ps, MultiBufferSource.BufferSource buf,
                               BlockState state, RandomSource rng, int rotation) {
        if (state.getValue(AltarPillarBlock.TOP)) return;
        ps.translate(0.5, 1.0, 0.5);
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-rotation)));
        var model    = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        var consumer = buf.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        var pose     = ps.last();
        for (BakedQuad q : model.getQuads(state, null, rng, ModelData.EMPTY, null))
            consumer.putBulkData(pose, q, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        for (Direction dir : Direction.values())
            for (BakedQuad q : model.getQuads(state, dir, rng, ModelData.EMPTY, null))
                consumer.putBulkData(pose, q, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private void renderCulledBlock(PoseStack ps, MultiBufferSource.BufferSource buf,
                                    BlockState state, Map<BlockPos, BlockState> map,
                                    BlockPos pos, RandomSource rng,
                                    net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher) {
        var bakedModel = dispatcher.getBlockModel(state);
        var consumer   = buf.getBuffer(RenderType.solid());
        var pose       = ps.last();
        for (BakedQuad q : bakedModel.getQuads(state, null, rng, ModelData.EMPTY, null))
            consumer.putBulkData(pose, q, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (adj.getY() == pos.getY() && map.containsKey(adj)) continue;
            for (BakedQuad q : bakedModel.getQuads(state, dir, rng, ModelData.EMPTY, null))
                consumer.putBulkData(pose, q, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }

    private Map<BlockPos, BlockState> buildStructureMap() {
        Map<BlockPos, BlockState> map = new HashMap<>();
        for (int[] pos : ALL_POSITIONS)
            map.put(new BlockPos(pos[0], pos[1], pos[2]), stateAt(pos[0], pos[1], pos[2]));
        return map;
    }

    private Map<BlockPos, BlockState> buildRingMap(int ringIdx) {
        Map<BlockPos, BlockState> map = new HashMap<>();
        for (Vec3i v : AspectAltarBlockEntity.ALL_RINGS.get(ringIdx))
            map.put(new BlockPos(v.getX(), v.getY(), v.getZ()),
                    ModBlocks.MANA_BLOCK.get().defaultBlockState());
        return map;
    }

    private BlockState stateAt(int wx, int wy, int wz) {
        if (wx == 0 && wy == 0 && wz == 0)
            return ModBlocks.ASPECT_ALTAR.get().defaultBlockState();
        if (Math.abs(wx) == 3 && Math.abs(wz) == 3)
            return showFormed
                    ? ModBlocks.ALTAR_PILLAR.get().defaultBlockState().setValue(AltarPillarBlock.TOP, wy == -1)
                    : ModBlocks.MANA_BLOCK.get().defaultBlockState();
        return ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState();
    }

    private static int pillarRotation(int wx, int wz) {
        if (wx < 0 && wz > 0) return 90;
        if (wx > 0 && wz > 0) return 0;
        if (wx > 0 && wz < 0) return 270;
        return 180;
    }

    private double computeAutoScale() {
        int maxR = 3;
        for (int i = 0; i < sliderTier; i++)
            for (Vec3i v : AspectAltarBlockEntity.ALL_RINGS.get(i))
                maxR = Math.max(maxR, Math.max(Math.abs(v.getX()),
                       Math.max(Math.abs(v.getY()), Math.abs(v.getZ()))));
        return Math.max(0.2, Math.min(2.0, 55.0 / (maxR * BLOCK_SIZE * 1.5)));
    }

    // ── Widgets ────────────────────────────────────────────────────────────────

    private class SliderWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() { return new ScreenRectangle(0, 0, WIDTH, SLIDER_H); }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            snap(mx); return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dX, double dY) {
            if (btn != 0) return false;
            snap(mx); return true;
        }

        private void snap(double mx) {
            int t = (int) Math.round((mx - SL_X1) / (double)(SL_X2 - SL_X1) * SL_STEPS);
            t = Math.max(0, Math.min(SL_STEPS, t));
            if (t != sliderTier) { sliderTier = t; scale = computeAutoScale(); }
        }
    }

    private class PreviewWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, SLIDER_H, WIDTH, CTRL_Y - SLIDER_H);
        }

        @Override public boolean mouseClicked(double mx, double my, int btn) { return btn == 0; }

        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dX, double dY) {
            if (btn != 0) return false;
            azimuth   = (float) ((azimuth + dX * 0.9) % 360f);
            elevation = (float) Math.max(5f, Math.min(85f, elevation + dY * 0.9));
            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double sx, double sy) {
            scale = Math.max(0.2, Math.min(2.5, scale + sy * 0.1));
            return true;
        }
    }

    private class ToggleWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() { return new ScreenRectangle(TOGGLE_X, CTRL_Y, TOGGLE_W, CTRL_H); }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            showFormed = !showFormed;
            return true;
        }
    }

    /** Press to scan inventory once; press again to hide the inventory overlay. */
    private class InventoryButtonWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(INV_BTN_X, MAT_Y + 2, INV_BTN_W, INV_BTN_H);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            showInventoryStatus = !showInventoryStatus;
            if (showInventoryStatus) scanInventory();
            return true;
        }
    }

    /**
     * Projects the current structure (base + rings up to sliderTier) as translucent
     * ghost blocks in the world.  Press again to hide, or right-click in the world.
     */
    private class ProjectButtonWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(PRJ_BTN_X, MAT_Y + 2, PRJ_BTN_W, PRJ_BTN_H);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            if (GhostProjectionState.isActive()) {
                GhostProjectionState.deactivate();
            } else {
                Map<BlockPos, BlockState> projBlocks = buildStructureMap();
                for (int i = 0; i < sliderTier; i++)
                    projBlocks.putAll(buildRingMap(i));
                GhostProjectionState.activate(projBlocks);
                // Close JEI so the player can see the world projection
                Minecraft.getInstance().setScreen(null);
            }
            return true;
        }
    }
}
