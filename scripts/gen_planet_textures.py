"""
生成 Titan 和 Pluto 的程序化行星貼圖。
輸出：2048x1024 等矩形投影 JPG，風格與 Solar System Scope 貼圖一致。
"""

import numpy as np
from PIL import Image
import os

W, H = 2048, 1024
OUT = os.path.join(os.path.dirname(__file__),
    "../src/main/resources/assets/koniava/textures/space")
os.makedirs(OUT, exist_ok=True)


# ── FBM 噪聲（純 numpy，不需要額外套件）────────────────────────────────────

def hash2(p):
    p = p % 289.0
    return (p * (p * 34.0 + 1.0)) % 289.0

def value_noise(x, y):
    ix, iy = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = x - np.floor(x), y - np.floor(y)
    u = fx * fx * (3.0 - 2.0 * fx)
    v = fy * fy * (3.0 - 2.0 * fy)

    def h(ax, ay):
        return (np.sin(ax * 127.1 + ay * 311.7) * 43758.5453) % 1.0

    return (h(ix, iy) * (1-u) * (1-v) +
            h(ix+1, iy) * u * (1-v) +
            h(ix, iy+1) * (1-u) * v +
            h(ix+1, iy+1) * u * v)

def fbm(x, y, octaves=6, gain=0.5, lacunarity=2.1):
    val = np.zeros_like(x)
    amp, freq = 0.5, 1.0
    for _ in range(octaves):
        val += amp * (value_noise(x * freq, y * freq) * 2.0 - 1.0)
        amp *= gain
        freq *= lacunarity
    return val

# UV 網格（等矩形投影）
u = np.linspace(0, 2 * np.pi, W)
v = np.linspace(0, np.pi, H)
U, V = np.meshgrid(u, v)

# 3D 球面座標（用於讓噪聲在球面上連續）
X = np.sin(V) * np.cos(U)
Y = np.sin(V) * np.sin(U)
Z = np.cos(V)


# ── Titan（土衛六）─────────────────────────────────────────────────────────
print("生成 Titan...")

# 底色：橙褐
base = fbm(X * 3.0, Y * 3.0 + Z * 2.0, octaves=6)
detail = fbm(X * 7.0 + 1.3, Y * 7.0 + 2.7, octaves=4)
combined = base * 0.65 + detail * 0.35

# 極區甲烷湖（暗斑）
lat = V - np.pi / 2  # -pi/2 ~ pi/2
polar_mask = np.exp(-((lat - np.pi * 0.38) ** 2) / 0.04) * 0.6
polar_mask += np.exp(-((lat + np.pi * 0.40) ** 2) / 0.06) * 0.5
lake_noise = fbm(X * 12.0, Z * 12.0, octaves=3)
lake = (lake_noise > 0.15).astype(float) * polar_mask * 0.8

# 顏色映射
t = np.clip(combined * 0.5 + 0.5, 0.0, 1.0)
# 亮色：橙金    暗色：深棕
bright = np.array([210, 140, 55])
dark   = np.array([105, 60, 25])
mid    = np.array([170, 100, 38])
img_t = (t[:, :, None] * bright + (1 - t[:, :, None]) * dark).astype(np.float32)

# 加一層帶狀紋理（類木星帶）
band = np.sin(V * 14.0 + base * 4.0) * 0.5 + 0.5
img_t = img_t * (0.85 + band[:, :, None] * 0.15)

# 甲烷湖疊暗色
img_t = img_t * (1.0 - lake[:, :, None] * 0.7)

# 霧化效果（略微提亮並加橙色調）
haze = fbm(X * 2.0, Y * 2.0, octaves=3) * 0.5 + 0.5
img_t = img_t + haze[:, :, None] * np.array([15, 8, 0])

img_t = np.clip(img_t, 0, 255).astype(np.uint8)
Image.fromarray(img_t).save(os.path.join(OUT, "titan.jpg"), quality=92)
print("  titan.jpg 完成")


# ── Pluto（冥王星）────────────────────────────────────────────────────────
print("生成 Pluto...")

base2 = fbm(X * 4.0, Y * 4.0 + Z * 3.0, octaves=7)
detail2 = fbm(X * 9.0 + 3.1, Z * 9.0 + 1.7, octaves=4)
combined2 = base2 * 0.6 + detail2 * 0.4

# 冥心（Tombaugh Regio）：心形氮冰亮區
# 大致在赤道下方偏西的橢圓亮斑
heart_lon = U - np.pi * 1.3  # 中心經度
heart_lat = V - np.pi * 0.62  # 略偏南
heart = np.exp(-(heart_lon**2 / 0.35 + heart_lat**2 / 0.18))
# 加一點噪聲讓邊緣自然
heart_noise = fbm(X * 6.0, Y * 6.0, octaves=3) * 0.3
heart = np.clip(heart - heart_noise, 0, 1)

# 顏色映射：深紅棕（托林有機物）
t2 = np.clip(combined2 * 0.5 + 0.5, 0.0, 1.0)
red_dark  = np.array([90, 50, 35])    # 深紅棕（托林）
gray_mid  = np.array([140, 115, 100]) # 灰棕
img_p = (t2[:, :, None] * gray_mid + (1 - t2[:, :, None]) * red_dark).astype(np.float32)

# 冥心：亮白奶油色
heart_col = np.array([245, 235, 210])  # 氮冰色
img_p = img_p * (1 - heart[:, :, None]) + heart_col * heart[:, :, None]

# 極冠（略白）
polar2 = np.exp(-((np.abs(lat) - np.pi * 0.38) ** 2) / 0.08)
img_p = img_p + polar2[:, :, None] * np.array([40, 35, 30])

img_p = np.clip(img_p, 0, 255).astype(np.uint8)
Image.fromarray(img_p).save(os.path.join(OUT, "pluto.jpg"), quality=92)
print("  pluto.jpg 完成")

print("\n全部完成，輸出至:", OUT)
