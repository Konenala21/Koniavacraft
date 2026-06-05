"""
生成天王星環和海王星環貼圖。
輸出：512x64 RGBA PNG，U 座標從內緣到外緣，A=不透明度。
高度最少 64 避免 AMD 26.x 極端比例貼圖崩潰。
"""

import numpy as np
from PIL import Image
import os

W = 512
H = 64
OUT = os.path.join(os.path.dirname(__file__),
    "../src/main/resources/assets/koniava/textures/space")
os.makedirs(OUT, exist_ok=True)

def make_ring_texture(rings, bg_color, filename):
    img = np.zeros((H, W, 4), dtype=np.float32)
    img[:, :, :3] = bg_color
    img[:, :, 3] = 0.0

    for (uc, hw_px, col) in rings:
        uc_px = uc * (W - 1)
        dist = np.abs(np.arange(W) - uc_px)
        mask = dist < hw_px
        alpha = np.exp(-0.5 * (dist[mask] / max(hw_px * 0.4, 0.5))**2)
        for row in range(H):
            for ci, c in enumerate(col[:3]):
                img[row, mask, ci] = c
            img[row, mask, 3] = np.maximum(img[row, mask, 3], col[3] * alpha)

    result = np.clip(img * 255, 0, 255).astype(np.uint8)
    Image.fromarray(result, 'RGBA').save(os.path.join(OUT, filename))
    print(f"  {filename} 完成")


# ── 天王星環 ──────────────────────────────────────────────────────────────
# 13 條極細暗環，藍灰色調，大量黑縫隙
# 真實反照率極低（~5%），只有 ε 環稍亮
print("生成天王星環...")
uranus_rings = [
    # 內環群（極細極暗）
    (0.08, 0.6, (0.22, 0.26, 0.38, 0.40)),  # 6
    (0.14, 0.6, (0.22, 0.26, 0.38, 0.40)),  # 5
    (0.20, 0.6, (0.22, 0.26, 0.38, 0.40)),  # 4
    # α β（稍亮）
    (0.44, 0.9, (0.28, 0.33, 0.46, 0.52)),  # α
    (0.52, 0.9, (0.26, 0.31, 0.44, 0.52)),  # β
    # η γ δ
    (0.62, 1.1, (0.28, 0.33, 0.46, 0.58)),  # η
    (0.68, 1.1, (0.26, 0.31, 0.44, 0.58)),  # γ
    (0.76, 1.3, (0.28, 0.33, 0.46, 0.60)),  # δ
    # λ（細且暗）
    (0.88, 0.6, (0.20, 0.24, 0.36, 0.38)),  # λ
    # ε：唯一明顯寬環，天王星環標誌，藍灰色帶些許白
    (0.97, 4.5, (0.55, 0.65, 0.82, 0.95)),  # ε epsilon
]
make_ring_texture(uranus_rings,
    bg_color=(0.01, 0.01, 0.03),
    filename="uranus_ring.png")


# ── 海王星環 ──────────────────────────────────────────────────────────────
# 5 條暗紅棕環，亞當斯環有特殊弧狀亮塊
# 整體非常暗，比天王星環更難見
print("生成海王星環...")
neptune_rings = [
    # Galle：最內最寬，非常瀰散暗淡
    (0.00, 3.5, (0.25, 0.14, 0.09, 0.28)),
    # Le Verrier：窄而稍明亮
    (0.50, 1.3, (0.35, 0.20, 0.13, 0.58)),
    # Lassell（Le Verrier 延伸的瀰散帶）
    (0.58, 0.9, (0.28, 0.16, 0.10, 0.38)),
    # Arago
    (0.66, 0.9, (0.28, 0.16, 0.10, 0.38)),
    # Adams：最外最亮，帶弧狀亮塊（Liberté/Égalité/Fraternité）
    (0.99, 2.5, (0.52, 0.30, 0.18, 0.82)),  # Adams 主體
    (0.97, 1.2, (0.72, 0.44, 0.26, 0.95)),  # 弧 Liberté（更亮）
    (1.00, 1.2, (0.68, 0.40, 0.24, 0.90)),  # 弧 Égalité
]
make_ring_texture(neptune_rings,
    bg_color=(0.01, 0.01, 0.01),
    filename="neptune_ring.png")


print("\n完成，輸出至:", OUT)
