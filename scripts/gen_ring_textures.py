"""
生成天王星環和海王星環貼圖。
輸出：512x4 RGBA PNG，U 座標從內緣到外緣，A=不透明度。
"""

import numpy as np
from PIL import Image
import os

W = 512
H = 64  # 至少 64 避免 AMD 極端比例貼圖崩潰（原 4 = 128:1 比例，AMD 26.x 無法接受）
OUT = os.path.join(os.path.dirname(__file__),
    "../src/main/resources/assets/koniava/textures/space")
os.makedirs(OUT, exist_ok=True)

def make_ring_texture(rings, bg_color, filename):
    """
    rings: list of (u_center, half_width, color_rgba)
    u_center: 0.0=內緣, 1.0=外緣
    half_width: 像素半寬
    """
    img = np.zeros((H, W, 4), dtype=np.float32)
    img[:, :, :3] = bg_color  # 背景色
    img[:, :, 3] = 0.0        # 背景完全透明

    u = np.linspace(0, 1, W)
    for (uc, hw_px, col) in rings:
        uc_px = uc * (W - 1)
        dist = np.abs(np.arange(W) - uc_px)
        mask = dist < hw_px
        # 高斯軟化邊緣
        alpha = np.exp(-0.5 * (dist[mask] / max(hw_px * 0.4, 0.5))**2)
        for row in range(H):
            for ci, c in enumerate(col[:3]):
                img[row, mask, ci] = c
            img[row, mask, 3] = np.maximum(img[row, mask, 3],
                                            col[3] * alpha)

    result = np.clip(img * 255, 0, 255).astype(np.uint8)
    Image.fromarray(result, 'RGBA').save(os.path.join(OUT, filename))
    print(f"  {filename} 完成")


# ── 天王星環 ──────────────────────────────────────────────────────────────
# 13 條環，主要暗灰藍色，ε (epsilon) 最寬最亮
# 現實位置歸一化（內=0, 外=1），epsilon 在 ~0.97
print("生成天王星環...")
uranus_rings = [
    # (u_center, half_width_px, RGBA)  — 遊戲放大版，保留相對比例
    (0.00, 1.2, (0.45, 0.52, 0.60, 0.65)),  # ζ zeta
    (0.13, 1.0, (0.42, 0.48, 0.56, 0.70)),  # 6
    (0.18, 1.0, (0.42, 0.48, 0.56, 0.70)),  # 5
    (0.23, 1.0, (0.42, 0.48, 0.56, 0.70)),  # 4
    (0.47, 1.2, (0.48, 0.54, 0.62, 0.75)),  # α alpha
    (0.54, 1.2, (0.45, 0.52, 0.60, 0.75)),  # β beta
    (0.64, 1.5, (0.48, 0.54, 0.62, 0.80)),  # η eta
    (0.70, 1.5, (0.45, 0.52, 0.60, 0.80)),  # γ gamma
    (0.77, 1.8, (0.48, 0.54, 0.62, 0.82)),  # δ delta
    (0.89, 1.2, (0.42, 0.48, 0.56, 0.65)),  # λ lambda
    (0.97, 4.0, (0.72, 0.80, 0.88, 0.95)),  # ε epsilon（最寬最亮）
]
make_ring_texture(uranus_rings,
    bg_color=(0.05, 0.06, 0.08),
    filename="uranus_ring.png")

# ── 海王星環 ──────────────────────────────────────────────────────────────
# 5 條環，暗紅棕色，亞當斯環最亮（最外）
# Galle / Le Verrier / Lassell / Arago / Adams
print("生成海王星環...")
neptune_rings = [
    (0.00, 2.0, (0.42, 0.28, 0.20, 0.60)),  # Galle（最寬）
    (0.50, 1.4, (0.48, 0.32, 0.22, 0.75)),  # Le Verrier
    (0.57, 1.0, (0.42, 0.28, 0.20, 0.60)),  # Lassell
    (0.65, 1.0, (0.42, 0.28, 0.20, 0.60)),  # Arago
    (1.00, 2.8, (0.75, 0.50, 0.35, 0.92)),  # Adams（最亮）
]
make_ring_texture(neptune_rings,
    bg_color=(0.04, 0.03, 0.02),
    filename="neptune_ring.png")

print("\n完成，輸出至:", OUT)
