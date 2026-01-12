package com.github.nalamodikk.render.client;

import com.github.nalamodikk.display.DisplayEntity;
import com.github.nalamodikk.display.DisplayEntityManager;
import com.github.nalamodikk.render.shader.pipe.ShaderPipe;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客戶端渲染管理器
 * 負責管理渲染階段的實體收集與 Shader 管道執行
 */
public class ClientRenderEntityManager {
    private static final ClientRenderEntityManager INSTANCE = new ClientRenderEntityManager();
    private final Map<ResourceLocation, List<DisplayEntity>> PIPE_GROUPS = new ConcurrentHashMap<>();
    private final List<ShaderPipe> PIPES = new ArrayList<>();

    public static ClientRenderEntityManager getInstance() {
        return INSTANCE;
    }

    public void registerPipe(ShaderPipe pipe) {
        PIPES.add(pipe);
    }

    /**
     * 在渲染 Tick 中執行 (由 Mixin 調用)
     */
    public void renderTick(float partialTick) {
        // 1. 準備數據 (清空舊的分組)
        PIPE_GROUPS.clear();

        // 2. 收集當前需要渲染的實體
        for (DisplayEntity entity : DisplayEntityManager.getAllEntities()) {
            if (!entity.isRemoved()) {
                // 根據實體類型分組（目前簡化處理，都放入 default 組）
                PIPE_GROUPS.computeIfAbsent(
                        ResourceLocation.fromNamespaceAndPath("koniavacraft", "default"),
                        k -> new ArrayList<>()).add(entity);
            }
        }

        // 3. 執行 Shader 管道
        for (ShaderPipe pipe : PIPES) {
            pipe.preRender();
            pipe.render(partialTick);
            pipe.postRender();
        }
    }

    public Map<ResourceLocation, List<DisplayEntity>> getPipeGroups() {
        return PIPE_GROUPS;
    }
}
