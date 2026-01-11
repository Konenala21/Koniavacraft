package com.github.nalamodikk.particle.style.examples;

import com.github.nalamodikk.particle.style.ParticleShapeStyle;
import com.github.nalamodikk.particle.style.ParticleStyle;
import com.github.nalamodikk.particle.utils.MathPresets;
import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;

import java.util.List;
import java.util.UUID;

public class RomaMagicTestStyle extends ParticleShapeStyle {

    public RomaMagicTestStyle() {
        super();
        
        // 1. 外圈圓環
        PointsBuilder circleBuilder = new PointsBuilder().addCircle(3.5, 12);
        List<RelativeLocation> circlePoints = circleBuilder.create();
        
        // 2. 在每個點上生成羅馬數字 (1-12)
        // 由於 ParticleShapeStyle 的 appendBuilder 是靜態添加的，我們需要預先計算好所有點
        PointsBuilder romaBuilder = new PointsBuilder();
        
        for (int i = 0; i < circlePoints.size(); i++) {
            RelativeLocation center = circlePoints.get(i);
            int number = i + 1;
            
            // 生成羅馬數字，並移動到圓環上的對應位置
            // 這裡需要注意：羅馬數字預設是在原點生成，我們需要旋轉它使其面向圓心，然後移動到位置
            
            PointsBuilder numBuilder = new PointsBuilder()
                .withRomaNumber(number, 1.0)
                // 旋轉 180 度 (PI)，讓數字底部朝向圓心 (假設初始是正立的)
                // 具體角度需要測試微調
                .rotateAsAxis(Math.PI) 
                // 旋轉到該點的方向 (這會讓數字面向外)
                .rotateTo(center)
                // 移動到該點
                .pointsOnEach(p -> p.add(center));
                
            romaBuilder.withBuilder(numBuilder);
        }
        
        this.appendBuilder(romaBuilder, rel -> new ParticleStyle.StyleData("koniava:coo_particle"));
        
        // 3. 內圈裝飾 (兩個圓)
        PointsBuilder innerCircles = new PointsBuilder()
            .addCircle(3.0, 60)
            .addCircle(4.0, 60);
            
        this.appendBuilder(innerCircles, rel -> new ParticleStyle.StyleData("koniava:coo_particle"));
        
        // 4. 動畫：持續旋轉
        this.addPreTickAction(style -> {
            // 每 tick 旋轉 PI / 256
            // 注意：這裡應該旋轉整個 style 的 axis 還是重新計算點？
            // 參考專案是 rotateAsAxis
            // 但我們目前的 ParticleShapeStyle.tick 實作還沒包含旋轉邏輯，只是執行 preTickActions。
            // 為了讓粒子真正動起來，我們需要在 Style 層級維護一個旋轉屬性，或者在 display 時應用動態旋轉。
            
            // 暫時略過動態旋轉，因為這需要重新計算所有粒子的位置 (重CPU運算)。
            // 參考專案是在客戶端每幀重新計算位置。
        });
        
        // 5. 動畫：縮放展開
        this.loadScaleHelper(0.1, 1.0, 40); // 40 tick (2秒) 內從 0.1 放大到 1.0
    }
}
