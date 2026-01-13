package com.github.nalamodikk.particle.animation;

/**
 * 插值類型枚舉
 * 定義不同的插值曲線用於動畫
 */
public enum InterpolationType {
    /**
     * 線性插值：恆定速度
     * f(t) = t
     */
    LINEAR {
        @Override
        public float interpolate(float progress) {
            return progress;
        }
    },

    /**
     * 緩入：開始慢，結束快
     * f(t) = t²
     */
    EASE_IN {
        @Override
        public float interpolate(float progress) {
            return progress * progress;
        }
    },

    /**
     * 緩出：開始快，結束慢
     * f(t) = 1 - (1 - t)²
     */
    EASE_OUT {
        @Override
        public float interpolate(float progress) {
            return 1 - (1 - progress) * (1 - progress);
        }
    },

    /**
     * 緩入緩出：開始和結束都慢，中間快
     * f(t) = 3t² - 2t³
     */
    EASE_IN_OUT {
        @Override
        public float interpolate(float progress) {
            return progress * progress * (3.0f - 2.0f * progress);
        }
    },

    /**
     * 貝茲曲線：平滑的 S 曲線
     * f(t) = t² * (3 - 2t)
     */
    BEZIER {
        @Override
        public float interpolate(float progress) {
            return progress * progress * (3 - 2 * progress);
        }
    },

    /**
     * 彈性：帶有彈跳效果
     */
    ELASTIC {
        @Override
        public float interpolate(float progress) {
            if (progress == 0 || progress == 1) {
                return progress;
            }
            float p = 0.3f;
            float s = p / 4.0f;
            return (float) (Math.pow(2, -10 * progress) * Math.sin((progress - s) * (2 * Math.PI) / p) + 1);
        }
    },

    /**
     * 彈跳：模擬彈跳效果
     */
    BOUNCE {
        @Override
        public float interpolate(float progress) {
            if (progress < 1 / 2.75f) {
                return 7.5625f * progress * progress;
            } else if (progress < 2 / 2.75f) {
                float t = progress - 1.5f / 2.75f;
                return 7.5625f * t * t + 0.75f;
            } else if (progress < 2.5f / 2.75f) {
                float t = progress - 2.25f / 2.75f;
                return 7.5625f * t * t + 0.9375f;
            } else {
                float t = progress - 2.625f / 2.75f;
                return 7.5625f * t * t + 0.984375f;
            }
        }
    },

    /**
     * 回彈：超過目標後回彈
     */
    BACK {
        @Override
        public float interpolate(float progress) {
            float s = 1.70158f;
            return progress * progress * ((s + 1) * progress - s);
        }
    },

    /**
     * 圓形：使用圓形曲線
     */
    CIRCULAR {
        @Override
        public float interpolate(float progress) {
            return (float) (1 - Math.sqrt(1 - progress * progress));
        }
    };

    /**
     * 計算插值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果（0.0 - 1.0）
     */
    public abstract float interpolate(float progress);

    /**
     * 反向插值（用於反向動畫）
     * @param progress 進度（0.0 - 1.0）
     * @return 反向插值結果
     */
    public float interpolateReverse(float progress) {
        return interpolate(1.0f - progress);
    }

    /**
     * 組合兩個插值類型
     * @param other 另一個插值類型
     * @param splitPoint 分割點（0.0 - 1.0）
     * @param progress 進度（0.0 - 1.0）
     * @return 組合插值結果
     */
    public float interpolateCombined(InterpolationType other, float splitPoint, float progress) {
        if (progress < splitPoint) {
            return this.interpolate(progress / splitPoint) * splitPoint;
        } else {
            return splitPoint + other.interpolate((progress - splitPoint) / (1.0f - splitPoint)) * (1.0f - splitPoint);
        }
    }
}
