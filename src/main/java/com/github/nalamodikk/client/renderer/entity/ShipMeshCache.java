package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipMeshHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 飛船靜態方塊的「烤一次」快取，section 化(每 16³ 一個 section、每 section 每 render layer 一個 VBO)。
 * 編輯時整艘背景重烤(光傳播需要全船,且本來就 off-thread),但**只把「內容真的變了」的 section 重新上傳 GL**
 * (靠每 section 的 block+光 內容 hash 比對) → 主執行緒不再每次編輯都重傳整艘 = 不卡頓。
 * draw 按 layer 批(每層 setupState 一次 → 畫該層所有 section → clear 一次),draw call 狀態切換降到最低、不掉 FPS。
 * 走 vanilla chunk shader(AMD 安全)。BER 方塊(箱子)不在此,仍每幀由 dispatcher 畫。
 */
public class ShipMeshCache implements AutoCloseable, ShipMeshHandle {

    private static final long REBAKE_DELAY_MS = 400; // 編輯後 debounce

    // layer -> (sectionKey -> VBO)。按 layer 分組方便 draw 批次。
    private final Map<RenderType, Map<Long, VertexBuffer>> buffers = new HashMap<>();
    // 每 section 上次烤出來的內容 hash(block state + 光,含 1 圈邊界)。比對決定要不要重傳。
    private final Map<Long, Integer> sectionHash = new HashMap<>();
    private boolean built = false;
    private boolean dirty = false;
    private long dirtyAtMs = 0;
    private CompletableFuture<BakeResult> pending;
    private volatile ShipLight shipLight;

    private static long sectionKey(BlockPos p) {
        return BlockPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
    }

    /** 一 (section,layer) 的烤好結果。 */
    private record SecMesh(long section, RenderType layer, MeshData mesh, ByteBufferBuilder bytes) {}
    /** 一次背景烤:各 (section,layer) mesh + 每 section 的內容 hash + 算好的光。 */
    private record BakeResult(List<SecMesh> meshes, Map<Long, Integer> hashes, ShipLight light) {}

    @org.jetbrains.annotations.Nullable public ShipLight getShipLight() { return shipLight; }

    @Override
    public void markDirty() {
        dirty = true;
        dirtyAtMs = System.currentTimeMillis();
    }

    /** @return true 若這次剛上傳完(供渲染器清掉「每幀先畫的剛放方塊」)。 */
    public boolean buildIfNeeded(ShipContraption c, Level level) {
        boolean uploaded = false;
        if (pending != null && pending.isDone()) {
            uploadPending();
            uploaded = true;
        }
        if (!built && pending == null) {
            startBake(c, level);
        }
        if (built && dirty && pending == null && System.currentTimeMillis() - dirtyAtMs >= REBAKE_DELAY_MS) {
            startBake(c, level);
            dirty = false;
        }
        return uploaded;
    }

    private void startBake(ShipContraption c, Level level) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>(c.getBlocks().size());
        for (var e : c.getBlocks().entrySet()) snapshot.put(e.getKey(), e.getValue().state());
        pending = CompletableFuture.supplyAsync(() -> bakeOffThread(snapshot, level), Util.backgroundExecutor());
    }

    /** worker thread：純 tesselate + 算光 + 算每 section 內容 hash。不碰 GL。 */
    private static BakeResult bakeOffThread(Map<BlockPos, BlockState> snapshot, Level level) {
        ShipRenderWorld world = new ShipRenderWorld(level, snapshot);
        ShipLight light = world.light();
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();
        PoseStack ps = new PoseStack();

        // (section,layer) -> builder。逐方塊 tesselate 進它所屬 section+layer 的 buffer。
        Map<Long, Map<RenderType, BufferBuilder>> builders = new HashMap<>();
        Map<Long, Map<RenderType, ByteBufferBuilder>> byteBuilders = new HashMap<>();

        for (var entry : snapshot.entrySet()) {
            BlockPos local = entry.getKey();
            BlockState state = entry.getValue();
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
            long sec = sectionKey(local);
            BakedModel model = brd.getBlockModel(state);
            ModelData md = model.getModelData(world, local, state, ModelData.EMPTY);
            var types = model.getRenderTypes(state, random, md);
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                if (!types.contains(layer)) continue;
                BufferBuilder bb = builders.computeIfAbsent(sec, k -> new HashMap<>()).computeIfAbsent(layer, l -> {
                    ByteBufferBuilder bytes = new ByteBufferBuilder(4096);
                    byteBuilders.computeIfAbsent(sec, k -> new HashMap<>()).put(l, bytes);
                    return new BufferBuilder(bytes, l.mode(), l.format());
                });
                ps.pushPose();
                ps.translate(local.getX(), local.getY(), local.getZ());
                mr.tesselateBlock(world, model, state, local, ps, bb, true, random,
                        state.getSeed(local), OverlayTexture.NO_OVERLAY, md, layer);
                ps.popPose();
            }
        }

        List<SecMesh> out = new ArrayList<>();
        for (var secEntry : builders.entrySet()) {
            long sec = secEntry.getKey();
            for (var layerEntry : secEntry.getValue().entrySet()) {
                RenderType layer = layerEntry.getKey();
                ByteBufferBuilder bytes = byteBuilders.get(sec).get(layer);
                MeshData mesh = layerEntry.getValue().build();
                if (mesh != null) out.add(new SecMesh(sec, layer, mesh, bytes));
                else bytes.close();
            }
        }

        // 每 section 的內容 hash:該 section + 1 圈邊界的 (blockStateId, blockLight, skyLight)。
        // 邊界納入 → 鄰居方塊/光變化也會讓本 section hash 變 → 不會漏傳(safe,寧可多傳不可少傳)。
        Map<Long, Integer> hashes = new HashMap<>();
        Set<Long> sections = new HashSet<>();
        for (BlockPos p : snapshot.keySet()) sections.add(sectionKey(p));
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (long sec : sections) {
            int sx = (int) (BlockPos.getX(sec)) << 4, sy = (int) (BlockPos.getY(sec)) << 4, sz = (int) (BlockPos.getZ(sec)) << 4;
            int h = 1;
            for (int x = sx - 1; x <= sx + 16; x++)
                for (int y = sy - 1; y <= sy + 16; y++)
                    for (int z = sz - 1; z <= sz + 16; z++) {
                        mp.set(x, y, z);
                        BlockState st = snapshot.getOrDefault(mp, Blocks.AIR.defaultBlockState());
                        h = h * 31 + Block.getId(st);
                        if (light != null) { h = h * 31 + light.block(mp); h = h * 31 + light.sky(mp); }
                    }
            hashes.put(sec, h);
        }

        return new BakeResult(out, hashes, light);
    }

    /** render thread：只上傳「hash 變了」的 section 的 VBO,沒變的沿用舊的。 */
    private void uploadPending() {
        BakeResult result;
        try {
            result = pending.join();
        } catch (Exception ex) {
            pending = null;
            built = true;
            return;
        }
        pending = null;
        shipLight = result.light();

        // 哪些 section 變了(hash 不同 = 內容變,要重傳)。
        Set<Long> changed = new HashSet<>();
        for (var e : result.hashes().entrySet()) {
            Integer old = sectionHash.get(e.getKey());
            if (old == null || !old.equals(e.getValue())) changed.add(e.getKey());
        }
        // 這次烤裡有的 section(其餘代表已清空)。
        Set<Long> present = result.hashes().keySet();

        // 變動的 section:先關掉它在各 layer 的舊 VBO(等下用新 mesh 重建)。
        for (long sec : changed) {
            for (Map<Long, VertexBuffer> byLayer : buffers.values()) {
                VertexBuffer old = byLayer.remove(sec);
                if (old != null) old.close();
            }
        }
        // 上傳變動 section 的新 mesh;沒變的 section 直接釋放新烤出來的 mesh(沿用舊 VBO)。
        for (SecMesh sm : result.meshes()) {
            if (changed.contains(sm.section())) {
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vb.bind();
                vb.upload(sm.mesh());
                VertexBuffer.unbind();
                buffers.computeIfAbsent(sm.layer(), k -> new HashMap<>()).put(sm.section(), vb);
            } else {
                sm.mesh().close();
            }
            sm.bytes().close();
        }
        // 已清空(這次烤沒有方塊)的 section:關掉殘留 VBO。
        Set<Long> dead = new HashSet<>();
        for (Map<Long, VertexBuffer> byLayer : buffers.values())
            for (long sec : byLayer.keySet()) if (!present.contains(sec)) dead.add(sec);
        for (long sec : dead)
            for (Map<Long, VertexBuffer> byLayer : buffers.values()) {
                VertexBuffer old = byLayer.remove(sec);
                if (old != null) old.close();
            }

        sectionHash.clear();
        sectionHash.putAll(result.hashes());
        built = true;
    }

    /** 每幀畫:按 layer 批(每層 setupState 一次 → 畫該層所有 section → clear 一次)。 */
    public void draw(PoseStack pose) {
        if (buffers.isEmpty()) return;
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        for (var layerEntry : buffers.entrySet()) {
            Map<Long, VertexBuffer> sections = layerEntry.getValue();
            if (sections.isEmpty()) continue;
            RenderType layer = layerEntry.getKey();
            layer.setupRenderState();
            for (VertexBuffer vb : sections.values()) {
                vb.bind();
                vb.drawWithShader(modelView, projection, RenderSystem.getShader());
            }
            VertexBuffer.unbind();
            layer.clearRenderState();
        }
    }

    @Override
    public void close() {
        if (pending != null) {
            try {
                for (SecMesh sm : pending.join().meshes()) { sm.mesh().close(); sm.bytes().close(); }
            } catch (Exception ignored) {}
            pending = null;
        }
        for (Map<Long, VertexBuffer> byLayer : buffers.values())
            for (VertexBuffer vb : byLayer.values()) vb.close();
        buffers.clear();
        sectionHash.clear();
    }
}
