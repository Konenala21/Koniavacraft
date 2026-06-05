package com.github.nalamodikk.space.orbit;

import org.joml.Vector3f;

/**
 * 小行星帶 / 柯伊伯帶定義：繞恆星的稀疏岩石環，位於黃道面。
 * 渲染為散布的小行星亮點（belt.fsh 用射線-黃道面相交 + 3D hash 散布）。
 */
public record BeltDef(
    String id,
    float innerRadius,   // 內緣半徑（方塊，離恆星）
    float outerRadius,   // 外緣半徑
    float thickness,     // 黃道面上下的厚度（Y 方向散布）
    float density,       // 密度（0.9=稀疏 0.99=極稀疏）
    Vector3f color       // 岩石基底色
) {}
