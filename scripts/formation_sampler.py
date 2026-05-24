#!/usr/bin/env python3
"""
formation_sampler.py  --  法陣圖片採樣工具

把 PNG 圖片的亮色區域採樣成粒子座標，輸出 Java float[][] 程式碼。
圖片不是直接複製，而是取樣後在遊戲中用幾何方式重新排列。

用法:
    python formation_sampler.py                       # 輸出內建九角星圖案
    python formation_sampler.py img.png               # 採樣圖片，預設 400 點
    python formation_sampler.py img.png 300 1.5       # 限制 300 點，縮放 1.5 格

需要 Pillow (圖片採樣才需要):
    pip install pillow numpy
"""

import sys
import math

try:
    from PIL import Image
    import numpy as np
    HAS_PIL = True
except ImportError:
    HAS_PIL = False


# ─── 內建測試圖案（不需要外部圖片）────────────────────────────────────────

def generate_builtin():
    """
    重構參考法陣：
      外雙環 + 符文環 + 內環 + 九角星 {9/4} + 中心環
    """
    import random
    rng = random.Random(42)
    pts = []

    def ring(r, n):
        for i in range(n):
            t = i * 2 * math.pi / n
            pts.append((math.cos(t) * r, math.sin(t) * r))

    def rune_ring(r, n):
        for i in range(n):
            t = i * 2 * math.pi / n + rng.uniform(-0.05, 0.05)
            rr = r + rng.uniform(-0.03, 0.03)
            pts.append((math.cos(t) * rr, math.sin(t) * rr))

    def enneagram(r, ppe=16):
        n, step = 9, 4
        vx = [math.cos(k * 2 * math.pi / n - math.pi / 2) * r for k in range(n)]
        vy = [math.sin(k * 2 * math.pi / n - math.pi / 2) * r for k in range(n)]
        cur = 0
        for _ in range(n):
            nxt = (cur + step) % n
            for p in range(ppe):
                tt = p / ppe
                pts.append((vx[cur] + tt * (vx[nxt] - vx[cur]),
                             vy[cur] + tt * (vy[nxt] - vy[cur])))
            cur = nxt

    ring(1.42, 128)
    ring(1.50, 136)
    rune_ring(1.18, 72)
    ring(0.72, 80)
    enneagram(0.58, 14)
    ring(0.20, 28)

    return pts


# ─── 圖片採樣 ─────────────────────────────────────────────────────────────

def sample_image(path, max_points=400, scale=1.5, threshold=180):
    """
    採樣圖片亮色像素，正規化到 [-scale, scale] 座標空間。
    上方 = 玩家前方 (v+)，右方 = 玩家右方 (u+)。
    輸出結果貼進 DevRenderTestItem3.FORMATION_POINTS 後，
    用 spawnFromPoints() 在遊戲中還原。
    """
    if not HAS_PIL:
        print("// ERROR: pip install pillow numpy")
        sys.exit(1)

    img = Image.open(path).convert("L")
    img = img.resize((96, 96), Image.LANCZOS)
    arr = np.array(img)
    h, w = arr.shape

    pts = []
    for y in range(h):
        for x in range(w):
            if arr[y, x] >= threshold:
                u = (x / (w - 1) - 0.5) * 2 * scale
                v = -(y / (h - 1) - 0.5) * 2 * scale   # image Y 上 = world forward
                pts.append((u, v))

    # 均勻抽樣 (不是隨機，保留空間分佈)
    if len(pts) > max_points:
        step = len(pts) / max_points
        pts = [pts[int(i * step)] for i in range(max_points)]

    return pts


# ─── 輸出 ─────────────────────────────────────────────────────────────────

def output_java(pts, var="FORMATION_POINTS"):
    print(f"// {len(pts)} points — paste into DevRenderTestItem3.java")
    print(f"private static final float[][] {var} = {{")
    for u, v in pts:
        print(f"    {{{u:.5f}f, {v:.5f}f}},")
    print("};")
    print()
    print("// 在 spawnFormation() 內使用這些點：")
    print("// for (float[] p : FORMATION_POINTS) {")
    print("//     spawnPoint(level, cx, cy, cz, rx, rz, fx, fz, p[0], p[1]);")
    print("// }")


# ─── 主程式 ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    if len(sys.argv) >= 2:
        path      = sys.argv[1]
        max_pts   = int(sys.argv[2])   if len(sys.argv) >= 3 else 400
        scale     = float(sys.argv[3]) if len(sys.argv) >= 4 else 1.5
        threshold = int(sys.argv[4])   if len(sys.argv) >= 5 else 180
        pts = sample_image(path, max_pts, scale, threshold)
        print(f"// Source: {path}  ({len(pts)} points, scale={scale}, threshold={threshold})")
    else:
        pts = generate_builtin()
        print("// Built-in: enneagram {9/4} + rings (no image needed)")

    print()
    output_java(pts)
