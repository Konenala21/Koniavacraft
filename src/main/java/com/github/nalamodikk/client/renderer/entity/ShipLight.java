package com.github.nalamodikk.client.renderer.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * 飛船光照:在方塊快照上跑兩個 BFS 算出每格的 block light + sky light(像 vanilla 的傳播衰減),
 * 取代原本「自己+鄰居發光值、天空恆 15」的假光,讓火把/發光石真的照出半徑、封閉船艙會暗。
 * 烤 mesh 時(背景執行緒)算一次,範圍只在船 bounds + 1 圈(讓外殼面/露天柱也拿得到光)。
 *
 * block:從發光方塊 BFS、每格 -1 衰減、穿透非遮擋方塊(canOcclude=false),不透明方塊只受光不再傳。
 * sky:沒有不透明船方塊遮頭的格 = 15(露天),被遮的 0 起算,再從露天格側向/向下 BFS -1 漫進船艙。
 * 日夜變暗由渲染端 LightTexture 對 sky 通道處理,這裡只給原始光值。
 */
public final class ShipLight {

    private final Map<Long, Byte> blockLight = new HashMap<>();
    private final Map<Long, Byte> skyLight = new HashMap<>();

    public ShipLight(Map<BlockPos, BlockState> blocks) {
        if (blocks.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        minX--; minY--; minZ--; maxX++; maxY++; maxZ++; // +1 圈
        computeBlock(blocks, minX, minY, minZ, maxX, maxY, maxZ);
        computeSky(blocks, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int block(BlockPos p) { return blockLight.getOrDefault(p.asLong(), (byte) 0); }
    public int sky(BlockPos p) { return skyLight.getOrDefault(p.asLong(), (byte) 0); }

    private static boolean occludes(Map<BlockPos, BlockState> blocks, BlockPos p) {
        BlockState s = blocks.get(p);
        return s != null && s.canOcclude();
    }

    private static boolean inBounds(BlockPos p, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ;
    }

    private void computeBlock(Map<BlockPos, BlockState> blocks, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        ArrayDeque<Long> q = new ArrayDeque<>();
        for (var e : blocks.entrySet()) {
            int em = e.getValue().getLightEmission();
            if (em > 0) {
                long k = e.getKey().asLong();
                blockLight.put(k, (byte) em);
                q.add(k);
            }
        }
        bfs(q, blockLight, blocks, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void computeSky(Map<BlockPos, BlockState> blocks, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // 每個 (x,z) 柱最高的不透明方塊 Y;在它之上的格露天 = 15
        Map<Long, Integer> topOcclude = new HashMap<>();
        for (var e : blocks.entrySet()) {
            if (!e.getValue().canOcclude()) continue;
            BlockPos p = e.getKey();
            long col = ((long) p.getX() << 32) | (p.getZ() & 0xFFFFFFFFL);
            topOcclude.merge(col, p.getY(), Math::max);
        }
        ArrayDeque<Long> q = new ArrayDeque<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long col = ((long) x << 32) | (z & 0xFFFFFFFFL);
                Integer top = topOcclude.get(col);
                int topY = top == null ? Integer.MIN_VALUE : top;
                for (int y = minY; y <= maxY; y++) {
                    if (y > topY) { // 露天
                        long k = BlockPos.asLong(x, y, z);
                        skyLight.put(k, (byte) 15);
                        q.add(k);
                    }
                }
            }
        }
        bfs(q, skyLight, blocks, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 共用 BFS:從種子格 -1 漫延,穿透非遮擋方塊,不透明方塊只受光不再傳。 */
    private static void bfs(ArrayDeque<Long> q, Map<Long, Byte> light, Map<BlockPos, BlockState> blocks,
                            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        while (!q.isEmpty()) {
            long k = q.poll();
            int L = light.getOrDefault(k, (byte) 0);
            if (L <= 1) continue;
            BlockPos p = BlockPos.of(k);
            for (Direction d : Direction.values()) {
                mp.setWithOffset(p, d);
                if (!inBounds(mp, minX, minY, minZ, maxX, maxY, maxZ)) continue;
                long nk = mp.asLong();
                int nL = L - 1;
                if (nL > light.getOrDefault(nk, (byte) 0)) {
                    light.put(nk, (byte) nL);
                    if (!occludes(blocks, mp)) q.add(nk); // 不透明只受光、不再往外傳
                }
            }
        }
    }
}
