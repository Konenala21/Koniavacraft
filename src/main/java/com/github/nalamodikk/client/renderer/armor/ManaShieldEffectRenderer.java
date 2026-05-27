package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ManaShieldEffectRenderer {

    private static final int STACKS          = 14;
    private static final int SLICES          = 28;
    private static final int VERTS_PER_SPHERE = STACKS * SLICES * 6;
    private static final int FLOATS_PER_VERT = 7;
    private static final int MAX_EFFECTS     = 16;
    private static final FloatBuffer BUF =
            BufferUtils.createFloatBuffer(MAX_EFFECTS * VERTS_PER_SPHERE * FLOATS_PER_VERT);

    private static int  programId   = -1;
    private static int  vaoId       = -1;
    private static int  vboId       = -1;
    private static int  locProj, locModelView;
    private static boolean initialized = false;
    private static boolean initFailed  = false;

    private static final float[] PROJ_ARR      = new float[16];
    private static final float[] MODELVIEW_ARR = new float[16];

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long  gameTime = mc.level.getGameTime();
        float pt       = event.getPartialTick().getGameTimeDeltaTicks();

        ManaShieldEffectManager.prune(gameTime);
        Map<Integer, Long> active = ManaShieldEffectManager.getActive();
        if (active.isEmpty()) return;

        if (!initialized) {
            if (initFailed) return;
            init();
            if (!initialized) return;
        }

        event.getProjectionMatrix().get(PROJ_ARR);
        event.getModelViewMatrix().get(MODELVIEW_ARR);
        Vec3 cam = event.getCamera().getPosition();

        BUF.clear();
        int rendered = 0;
        for (Map.Entry<Integer, Long> entry : active.entrySet()) {
            if (rendered >= MAX_EFFECTS) break;
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null) continue;
            float age      = (gameTime - entry.getValue()) + pt;
            float progress = age / ManaShieldEffectManager.DURATION_TICKS;
            if (progress < 0 || progress >= 1.0f) continue;

            float cx = (float)(entity.getX() - cam.x);
            float cy = (float)(entity.getY() + entity.getBbHeight() * 0.5 - cam.y);
            float cz = (float)(entity.getZ() - cam.z);

            buildSphere(BUF, cx, cy, cz, progress, entity.getBbHeight(), entity.getBbWidth());
            rendered++;
        }
        BUF.flip();
        if (BUF.limit() == 0) return;

        int     prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDep   = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBl    = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCull  = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean wasDW    = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK) != 0;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        GL20.glUseProgram(programId);
        GL20.glUniformMatrix4fv(locProj,      false, PROJ_ARR);
        GL20.glUniformMatrix4fv(locModelView, false, MODELVIEW_ARR);

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BUF, GL15.GL_STREAM_DRAW);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, BUF.limit() / FLOATS_PER_VERT);

        GL30.glBindVertexArray(0);
        GL20.glUseProgram(prevProg);

        if (wasDep) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (!wasBl)  GL11.glDisable(GL11.GL_BLEND);
        if (wasCull) GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(wasDW);
    }

    private static void buildSphere(FloatBuffer buf,
                                     float cx, float cy, float cz,
                                     float progress, float entityHeight, float entityWidth) {
        // 半徑取身高為主、寬度為輔，確保整個玩家被包住，並隨時間略為擴張
        float baseR = Math.max(entityHeight * 0.62f, entityWidth * 0.80f);
        float radius = baseR + progress * 0.22f;

        float alpha = (float) Math.pow(1.0 - progress, 1.4);

        float r = 0.25f;
        float g = 0.60f;
        float b = 1.00f;

        for (int i = 0; i < STACKS; i++) {
            float t1 = (float)(i       * Math.PI / STACKS);
            float t2 = (float)((i + 1) * Math.PI / STACKS);
            float st1 = (float) Math.sin(t1), ct1 = (float) Math.cos(t1);
            float st2 = (float) Math.sin(t2), ct2 = (float) Math.cos(t2);

            for (int j = 0; j < SLICES; j++) {
                float p1 = (float)(j       * 2 * Math.PI / SLICES);
                float p2 = (float)((j + 1) * 2 * Math.PI / SLICES);
                float cp1 = (float) Math.cos(p1), sp1 = (float) Math.sin(p1);
                float cp2 = (float) Math.cos(p2), sp2 = (float) Math.sin(p2);

                // 四角頂點（球面參數化）
                float x11 = cx + radius * st1 * cp1, y1 = cy + radius * ct1, z11 = cz + radius * st1 * sp1;
                float x12 = cx + radius * st1 * cp2,                         z12 = cz + radius * st1 * sp2;
                float x21 = cx + radius * st2 * cp1, y2 = cy + radius * ct2, z21 = cz + radius * st2 * sp1;
                float x22 = cx + radius * st2 * cp2,                         z22 = cz + radius * st2 * sp2;

                float a = alpha * 0.30f;

                putVert(buf, x11, y1, z11, r, g, b, a);
                putVert(buf, x21, y2, z21, r, g, b, a);
                putVert(buf, x22, y2, z22, r, g, b, a);
                putVert(buf, x11, y1, z11, r, g, b, a);
                putVert(buf, x22, y2, z22, r, g, b, a);
                putVert(buf, x12, y1, z12, r, g, b, a);
            }
        }
    }

    private static void putVert(FloatBuffer buf,
                                 float x, float y, float z,
                                 float r, float g, float b, float a) {
        buf.put(x).put(y).put(z).put(r).put(g).put(b).put(a);
    }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/armor/shield_ring.vsh");
            String frag = read(rm, "shaders/armor/shield_ring.fsh");

            int v = compile(GL20.GL_VERTEX_SHADER,   vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
            if (initFailed) { GL20.glDeleteShader(v); GL20.glDeleteShader(f); return; }

            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            GL20.glBindAttribLocation(programId, 0, "Position");
            GL20.glBindAttribLocation(programId, 1, "Color");
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);

            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[ShieldRing] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                initFailed = true;
                return;
            }

            GL20.glUseProgram(programId);
            locProj      = GL20.glGetUniformLocation(programId, "ProjMat");
            locModelView = GL20.glGetUniformLocation(programId, "ModelViewMat");
            GL20.glUseProgram(0);

            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

            int stride = FLOATS_PER_VERT * Float.BYTES;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);

            GL30.glBindVertexArray(0);
            initialized = true;
            KoniavacraftMod.LOGGER.debug("[ShieldRing] Shader initialized.");
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[ShieldRing] Init failed", e);
            if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
            initFailed = true;
        }
    }

    public static void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId);  programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);      vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId     = -1; }
        initialized = false;
    }

    private static String read(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[ShieldRing] Compile: {}", GL20.glGetShaderInfoLog(id));
            initFailed = true;
        }
        return id;
    }
}
