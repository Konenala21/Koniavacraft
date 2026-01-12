package com.github.nalamodikk.particle.render.shader.pipe.manager;

import com.github.nalamodikk.particle.render.shader.ShaderProgramBuilder;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.glsl.GlShader;
import com.github.nalamodikk.particle.render.shader.glsl.GlShaderType;
import com.github.nalamodikk.particle.render.shader.api.pipe.GlobalUniform;
import com.github.nalamodikk.particle.render.shader.api.pipe.PipeLinker;
import com.github.nalamodikk.particle.render.shader.api.pipe.PipeLinkerNode;
import com.github.nalamodikk.particle.render.shader.api.pipe.ShaderPipe;
import com.github.nalamodikk.particle.render.shader.exceptions.RenderPipeLinkerNotSetException;
import com.github.nalamodikk.particle.render.shader.exceptions.RenderPipeOutputNotSetException;
import com.github.nalamodikk.particle.render.shader.glsl.FileShader;
import com.github.nalamodikk.particle.render.shader.pipe.Matrix4fGlobalUniform;
import com.github.nalamodikk.particle.render.shader.pipe.SimpleShaderPipe;
import com.github.nalamodikk.particle.render.shader.vertex.SimpleVertexBuffer;
import com.github.nalamodikk.particle.render.shader.vertex.VertexBuffers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShaderPipeManager {
    public final ResourceLocation pipeID;
    private boolean initialized = false;
    private final SimpleVertexBuffer screenBuffer = VertexBuffers.getScreenBuffer();
    private final GlShader screenVertex = new FileShader("pipe/vertexes/screen.vsh", GlShaderType.VERTEX);
    private final GlShader screenFragment = new FileShader("pipe/frags/screen.fsh", GlShaderType.FRAGMENT);
    private final CooShaderProgram screenProgram = new ShaderProgramBuilder().vertex(screenVertex).fragment(screenFragment).build();
    private final Map<String, GlobalUniform<?>> uploadUniformPrePrograms = new HashMap<>();
    public final Set<ShaderPipe> pipes = new HashSet<>();
    public final GraphPipeLinker linker = new GraphPipeLinker();
    private Supplier<Integer> depthSupplier = () -> -1;
    private ShaderPipe valueInputPipe = null;
    private ShaderPipe valueOutput = null;
    private final List<Consumer<ShaderPipeManager>> beforeInitPipe = new ArrayList<>();
    public boolean enableBlend = true;
    public int blendFuncSrc = GL33.GL_ONE;
    public int blendFuncDst = GL33.GL_ONE;
    public boolean useDepth = true;
    private boolean linkerSet = false;
    private Consumer<PipeLinker> linkerFunc = (l) -> {};

    public ShaderPipeManager(ResourceLocation pipeID) { this.pipeID = pipeID; }
    public void setDepthSupplier(Supplier<Integer> supplier) { this.depthSupplier = supplier; }
    public ShaderPipeManager addGlobalUniform(GlobalUniform<?> upload) { uploadUniformPrePrograms.put(upload.key, upload); return this; }

    public ShaderPipeManager setLinkerFunc(Consumer<PipeLinker> func) {
        linkerSet = true;
        linkerFunc = func;
        return this;
    }

    public ShaderPipeManager valueInput(ShaderPipe pipe) { this.valueInputPipe = pipe; addPipe(pipe); return this; }
    public ShaderPipeManager valueOutput(ShaderPipe pipe) { this.valueOutput = pipe; addPipe(pipe); return this; }

    public void init() {
        if (initialized) return;
        if (!linkerSet) throw new RenderPipeLinkerNotSetException(pipeID);
        for (Consumer<ShaderPipeManager> action : beforeInitPipe) action.accept(this);
        initialized = true;
        if (valueInputPipe == null) valueInputPipe = new SimpleShaderPipe(new FileShader("pipe/frags/screen.fsh", GlShaderType.FRAGMENT), depthSupplier);
        pipes.add(valueInputPipe);
        for (ShaderPipe pipe : pipes) pipe.init();
        screenBuffer.init();
        screenProgram.init();
        linkerFunc.accept(linker);
    }

    public ShaderPipe addPipe(ShaderPipe pipe) {
        for (GlobalUniform<?> uniform : uploadUniformPrePrograms.values()) pipe.addRenderHandler(uniform::upload);
        if (initialized) pipe.init();
        pipes.add(pipe);
        return pipe;
    }

    public ShaderPipeManager writeFrame(Runnable draw) {
        if (!initialized) return this;
        if (useDepth) RenderSystem.depthMask(true);
        valueInputPipe.write(p -> draw.run());
        return this;
    }

    private void inputPipe(Set<ShaderPipe> initPipes, ShaderPipe targetPipe) {
        initPipes.add(targetPipe);
        Map<Integer, PipeLinkerNode> all = linker.findAllChannel(targetPipe);
        if (all.isEmpty()) return;
        List<Integer> sortedChannels = new ArrayList<>(all.keySet());
        Collections.sort(sortedChannels);
        FramePipeChannels pipeChannels = new FramePipeChannels();
        for (int channel : sortedChannels) {
            PipeLinkerNode output = all.get(channel);
            if (!initPipes.contains(output.pipe)) inputPipe(initPipes, output.pipe);
            pipeChannels.addChannel(output.pipe.getFrameOutput().getChannel(output.channel));
        }
        targetPipe.writeFromChannel(pipeChannels);
    }

    public void render() {
        if (!initialized) return;
        if (valueOutput == null) throw new RenderPipeOutputNotSetException(pipeID);
        inputPipe(new HashSet<>(), valueOutput);
        if (enableBlend) { RenderSystem.enableBlend(); RenderSystem.blendFunc(blendFuncSrc, blendFuncDst); }
        RenderSystem.depthMask(false);
        screenProgram.useOnContext(() -> valueOutput.getFrameOutput().useOnContext(screenBuffer::draw));
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    public void release() { for (ShaderPipe pipe : pipes) pipe.release(); }
    public void resize(int width, int height) { for (ShaderPipe pipe : pipes) pipe.resize(width, height); }
}
