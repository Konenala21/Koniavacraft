package com.github.nalamodikk.client.renderer.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Map;

/**
 * 飛船光照:在方塊快照上跑兩個 BFS 算出每格的 block light + sky light(像 vanilla 的傳播衰減),
 * 取代「自己+鄰居發光值、天空恆 15」的假光,讓火把/發光石真的照出半徑、封閉船艙會暗。
 * 烤 mesh 時(背景執行緒)算一次,範圍只在船 bounds + 1 圈。
 *
 * 用扁平 byte 陣列(local 座標當 index)+ 預存 occlusion 陣列,BFS 純陣列存取,不碰 HashMap(原本 ~3.4ms 的元兇)。
 * block:從發光方塊 BFS、每格 -1 衰減、穿透非遮擋方塊,不透明方塊只受光不再傳。
 * sky:頭上沒不透明船方塊的格 = 15(露天),被遮的 0 起算,再從露天格 BFS -1 漫進船艙。日夜變暗由渲染端 LightTexture 處理。
 */
public final class ShipLight {

    private final int minX, minY, minZ, sizeX, sizeY, sizeXY;
    private final byte[] block, sky;

    public ShipLight(Map<BlockPos, BlockState> blocks) {
        if (blocks.isEmpty()) { minX = minY = minZ = 0; sizeX = sizeY = sizeXY = 0; block = sky = new byte[0]; return; }
        int mnX = Integer.MAX_VALUE, mnY = Integer.MAX_VALUE, mnZ = Integer.MAX_VALUE;
        int mxX = Integer.MIN_VALUE, mxY = Integer.MIN_VALUE, mxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks.keySet()) {
            mnX = Math.min(mnX, p.getX()); mnY = Math.min(mnY, p.getY()); mnZ = Math.min(mnZ, p.getZ());
            mxX = Math.max(mxX, p.getX()); mxY = Math.max(mxY, p.getY()); mxZ = Math.max(mxZ, p.getZ());
        }
        minX = mnX - 1; minY = mnY - 1; minZ = mnZ - 1; // +1 圈
        sizeX = (mxX + 1) - minX + 1; sizeY = (mxY + 1) - minY + 1; int sizeZ = (mxZ + 1) - minZ + 1;
        sizeXY = sizeX * sizeY;
        int n = sizeXY * sizeZ;
        block = new byte[n]; sky = new byte[n];
        byte[] occ = new byte[n];
        int[] topOcc = new int[sizeX * sizeZ]; // 每 (x,z) 柱最高的不透明方塊 local-y;-1 = 沒有
        java.util.Arrays.fill(topOcc, -1);

        ArrayDeque<Integer> blockQ = new ArrayDeque<>();
        for (var e : blocks.entrySet()) {
            BlockPos p = e.getKey(); BlockState st = e.getValue();
            int lx = p.getX() - minX, ly = p.getY() - minY, lz = p.getZ() - minZ;
            int i = lx + ly * sizeX + lz * sizeXY;
            if (st.canOcclude()) { occ[i] = 1; int col = lx + lz * sizeX; if (ly > topOcc[col]) topOcc[col] = ly; }
            int em = st.getLightEmission();
            if (em > 0) { block[i] = (byte) em; blockQ.add(i); }
        }
        bfs(blockQ, block, occ);

        ArrayDeque<Integer> skyQ = new ArrayDeque<>();
        for (int lx = 0; lx < sizeX; lx++)
            for (int lz = 0; lz < sizeZ; lz++) {
                int topY = topOcc[lx + lz * sizeX];
                for (int ly = topY + 1; ly < sizeY; ly++) { // 露天(最高不透明之上)
                    int i = lx + ly * sizeX + lz * sizeXY;
                    sky[i] = 15; skyQ.add(i);
                }
            }
        bfs(skyQ, sky, occ);
    }

    public int block(BlockPos p) { int i = idx(p.getX(), p.getY(), p.getZ()); return i < 0 ? 0 : block[i]; }
    public int sky(BlockPos p) { int i = idx(p.getX(), p.getY(), p.getZ()); return i < 0 ? 0 : sky[i]; }

    private int idx(int wx, int wy, int wz) {
        int lx = wx - minX, ly = wy - minY, lz = wz - minZ;
        if (lx < 0 || lx >= sizeX || ly < 0 || ly >= sizeY || lz < 0 || lz * sizeXY >= block.length) return -1;
        return lx + ly * sizeX + lz * sizeXY;
    }

    /** 共用 BFS(純陣列):從種子 -1 漫延,穿透非遮擋格,不透明格只受光不再傳。 */
    private void bfs(ArrayDeque<Integer> q, byte[] light, byte[] occ) {
        int sizeZ = block.length / sizeXY;
        while (!q.isEmpty()) {
            int i = q.poll();
            int L = light[i];
            if (L <= 1) continue;
            int nL = L - 1;
            int lx = i % sizeX, ly = (i / sizeX) % sizeY, lz = i / sizeXY;
            if (lx > 0)         step(i - 1,      nL, light, occ, q);
            if (lx < sizeX - 1) step(i + 1,      nL, light, occ, q);
            if (ly > 0)         step(i - sizeX,  nL, light, occ, q);
            if (ly < sizeY - 1) step(i + sizeX,  nL, light, occ, q);
            if (lz > 0)         step(i - sizeXY, nL, light, occ, q);
            if (lz < sizeZ - 1) step(i + sizeXY, nL, light, occ, q);
        }
    }

    private static void step(int ni, int nL, byte[] light, byte[] occ, ArrayDeque<Integer> q) {
        if (nL > light[ni]) {
            light[ni] = (byte) nL;
            if (occ[ni] == 0) q.add(ni); // 不透明只受光、不再往外傳
        }
    }
}
