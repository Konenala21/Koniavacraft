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
import net.minecraft.core.Direction;
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

    private static final long REBAKE_DELAY_MS = 120; // 編輯後 debounce(縮短:讓 pending 更快進正式 VBO、累積更少)

    // layer -> (sectionKey -> VBO)。按 layer 分組方便 draw 批次。
    private final Map<RenderType, Map<Long, VertexBuffer>> buffers = new HashMap<>();
    // pending(剛放、烤好前)的暫存 VBO:只在 pending 集合變動時重 tesselate 一次,之後每幀只畫(不再每幀重算 = 不卡)。
    private final Map<RenderType, VertexBuffer> pendingBuffers = new HashMap<>();
    private int pendingSig = -1;
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
    /** 一次背景烤:只含「改動 section」的 mesh + 全 section 的內容 hash + 改動 section 集 + 算好的光。 */
    private record BakeResult(List<SecMesh> meshes, Map<Long, Integer> hashes, Set<Long> changed, ShipLight light) {}

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
        Map<Long, Integer> prevHashes = new HashMap<>(sectionHash); // 給背景烤比對,只重烤改動的 section
        pending = CompletableFuture.supplyAsync(() -> bakeOffThread(snapshot, level, prevHashes), Util.backgroundExecutor());
    }

    /** worker thread：純 tesselate + 算光 + 算每 section 內容 hash。不碰 GL。 */
    private static BakeResult bakeOffThread(Map<BlockPos, BlockState> snapshot, Level level, Map<Long, Integer> prevHashes) {
        ShipRenderWorld world = new ShipRenderWorld(level, snapshot);
        ShipLight light = world.light();
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();

        // 1. 算每 section 的內容 hash:只算會進 mesh 的 MODEL 方塊,每方塊用「位置+blockId+自身與6鄰的光」,
        //    用 sum 累進(交換律→與遍歷順序無關)。便宜:每方塊 ~7 次光查詢,不再掃 18³ 全空氣(原本 hash 5.5ms 的元兇)。
        Map<Long, Integer> hashes = new HashMap<>();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (var entry : snapshot.entrySet()) {
            BlockPos p = entry.getKey();
            BlockState st = entry.getValue();
            if (st.isAir() || st.getRenderShape() != RenderShape.MODEL) continue;
            int bh = p.hashCode() * 31 + Block.getId(st);
            if (light != null) {
                bh = bh * 31 + light.block(p) * 7 + light.sky(p);
                for (Direction d : Direction.values()) { mp.setWithOffset(p, d); bh = bh * 31 + light.block(mp) * 7 + light.sky(mp); }
            }
            hashes.merge(sectionKey(p), bh, Integer::sum);
        }

        // 2. 找改動的 section(hash 不同/新增/清空)。初次烤 prevHashes 空 → 全部 changed。
        Set<Long> changed = new HashSet<>();
        for (var e : hashes.entrySet()) {
            Integer old = prevHashes.get(e.getKey());
            if (old == null || !old.equals(e.getValue())) changed.add(e.getKey());
        }
        for (long sec : prevHashes.keySet()) if (!hashes.containsKey(sec)) changed.add(sec); // 清空的 section

        // 3. 只 tesselate 改動的 section(沒變的沿用舊 VBO)。這是把 tesselate 從整艘砍成幾個 section 的關鍵。
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();
        PoseStack ps = new PoseStack();
        Map<Long, Map<RenderType, BufferBuilder>> builders = new HashMap<>();
        Map<Long, Map<RenderType, ByteBufferBuilder>> byteBuilders = new HashMap<>();
        for (var entry : snapshot.entrySet()) {
            BlockPos local = entry.getKey();
            BlockState state = entry.getValue();
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
            long sec = sectionKey(local);
            if (!changed.contains(sec)) continue; // 沒變的 section 不重烤
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
        return new BakeResult(out, hashes, changed, light);
    }

    /** render thread：上傳背景烤回來的「改動 section」的 VBO(bake 已只產 changed section,沒變的沿用舊的)。 */
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

        // 改動的 section(含被清空的):先關掉各 layer 的舊 VBO。
        for (long sec : result.changed()) {
            for (Map<Long, VertexBuffer> byLayer : buffers.values()) {
                VertexBuffer old = byLayer.remove(sec);
                if (old != null) old.close();
            }
        }
        // 上傳改動 section 的新 mesh(被清空的 section 沒有 mesh → 上面已關掉就沒了)。
        for (SecMesh sm : result.meshes()) {
            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(sm.mesh());
            VertexBuffer.unbind();
            buffers.computeIfAbsent(sm.layer(), k -> new HashMap<>()).put(sm.section(), vb);
            sm.bytes().close();
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

    /** 畫 pending(剛放、烤好前)方塊:只在 pending 集合變動時重 tesselate 一次進暫存 VBO,之後每幀只畫。 */
    public void drawPending(PoseStack pose, Set<BlockPos> pending, ShipContraption c, Level level) {
        if (pending.isEmpty()) {
            if (!pendingBuffers.isEmpty()) { for (VertexBuffer vb : pendingBuffers.values()) vb.close(); pendingBuffers.clear(); }
            pendingSig = -1;
            return;
        }
        int sig = pending.size();
        for (BlockPos p : pending) sig = sig * 31 + p.hashCode();
        if (sig != pendingSig) { rebuildPending(pending, c, level); pendingSig = sig; }
        if (pendingBuffers.isEmpty()) return;
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        for (var e : pendingBuffers.entrySet()) {
            e.getKey().setupRenderState();
            e.getValue().bind();
            e.getValue().drawWithShader(modelView, projection, RenderSystem.getShader());
            VertexBuffer.unbind();
            e.getKey().clearRenderState();
        }
    }

    private void rebuildPending(Set<BlockPos> pending, ShipContraption c, Level level) {
        for (VertexBuffer vb : pendingBuffers.values()) vb.close();
        pendingBuffers.clear();
        ShipRenderWorld world = new ShipRenderWorld(level, c); // live 模式(crude 光,暫時的,馬上被真烤取代)
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();
        PoseStack ps = new PoseStack();
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            ByteBufferBuilder bytes = new ByteBufferBuilder(2048);
            BufferBuilder bb = new BufferBuilder(bytes, layer.mode(), layer.format());
            boolean any = false;
            for (BlockPos local : pending) {
                var info = c.getBlocks().get(local);
                if (info == null) continue;
                BlockState state = info.state();
                if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                BakedModel model = brd.getBlockModel(state);
                ModelData md = model.getModelData(world, local, state, ModelData.EMPTY);
                if (!model.getRenderTypes(state, random, md).contains(layer)) continue;
                ps.pushPose();
                ps.translate(local.getX(), local.getY(), local.getZ());
                mr.tesselateBlock(world, model, state, local, ps, bb, true, random,
                        state.getSeed(local), OverlayTexture.NO_OVERLAY, md, layer);
                ps.popPose();
                any = true;
            }
            MeshData mesh = any ? bb.build() : null;
            if (mesh != null) {
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vb.bind();
                vb.upload(mesh);
                VertexBuffer.unbind();
                pendingBuffers.put(layer, vb);
            }
            bytes.close();
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
        for (VertexBuffer vb : pendingBuffers.values()) vb.close();
        pendingBuffers.clear();
    }
}
