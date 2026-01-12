package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.particle.utils.RelativeLocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleControlerDataBuffers {
    private static final Map<Class<?>, Class<?>> wrapperToPrimitive = new HashMap<>();
    public static final Map<ParticleControlerDataBuffer.Id, Class<? extends ParticleControlerDataBuffer<?>>> registerBuilder = new HashMap<>();
    public static final Map<Class<?>, ParticleControlerDataBuffer.Id> registerTypes = new HashMap<>();

    static {
        wrapperToPrimitive.put(Integer.class, int.class);
        wrapperToPrimitive.put(Double.class, double.class);
        wrapperToPrimitive.put(Long.class, long.class);
        wrapperToPrimitive.put(Float.class, float.class);
        wrapperToPrimitive.put(Boolean.class, boolean.class);
        wrapperToPrimitive.put(Character.class, char.class);
        wrapperToPrimitive.put(Byte.class, byte.class);
        wrapperToPrimitive.put(Short.class, short.class);
    }
    
    public static void register(Class<?> bufType, ParticleControlerDataBuffer.Id id, Class<? extends ParticleControlerDataBuffer<?>> type) {
        registerBuilder.put(id, type);
        registerTypes.put(bufType, id);
    }

    public static <T> byte[] encode(ParticleControlerDataBuffer<T> buffer) {
        byte[] code = buffer.encode();
        if (code == null) throw new IllegalStateException("buffer encode value is null");
        
        String decoderID = buffer.getClass().getName();
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] idBytes = decoderID.getBytes(StandardCharsets.UTF_8);
        byteBuf.writeInt(idBytes.length);
        byteBuf.writeBytes(idBytes);
        byteBuf.writeBytes(code);
        
        byte[] result = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> ParticleControlerDataBuffer<T> decodeToBuffer(ByteBuf byteBuf) {
        int len = byteBuf.readInt();
        byte[] toStringBytes = new byte[len];
        byteBuf.readBytes(toStringBytes);
        String decoderID = new String(toStringBytes, StandardCharsets.UTF_8);
        
        byte[] codeBytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(codeBytes);
        
        try {
            Class<?> clazz = Class.forName(decoderID);
            ParticleControlerDataBuffer<T> ins = (ParticleControlerDataBuffer<T>) clazz.getConstructor().newInstance();
            ins.setLoadedValue(ins.decode(codeBytes));
            return ins;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode buffer: " + decoderID, e);
        }
    }
    
    public static <T> ParticleControlerDataBuffer<T> decodeToBuffer(byte[] bytes) {
        return decodeToBuffer(Unpooled.wrappedBuffer(bytes));
    }

    // ========== Factory Methods ==========

    /**
     * 創建 Boolean 緩衝區
     */
    public static BooleanDataBuffer bool(boolean value) {
        return new BooleanDataBuffer(value);
    }

    /**
     * 創建 Integer 緩衝區
     */
    public static IntDataBuffer integer(int value) {
        return new IntDataBuffer(value);
    }

    /**
     * 創建 Float 緩衝區
     */
    public static FloatDataBuffer floatValue(float value) {
        return new FloatDataBuffer(value);
    }

    /**
     * 創建 Double 緩衝區
     */
    public static DoubleDataBuffer doubleValue(double value) {
        return new DoubleDataBuffer(value);
    }

    /**
     * 創建 String 緩衝區
     */
    public static StringDataBuffer string(String value) {
        return new StringDataBuffer(value);
    }

    /**
     * 創建 UUID 緩衝區
     */
    public static UUIDDataBuffer uuid(UUID value) {
        return new UUIDDataBuffer(value);
    }

    /**
     * 創建 Vec3 緩衝區
     */
    public static Vec3DataBuffer vec3(Vec3 value) {
        return new Vec3DataBuffer(value);
    }

    /**
     * 創建 Vec3 緩衝區（從坐標）
     */
    public static Vec3DataBuffer vec3(double x, double y, double z) {
        return new Vec3DataBuffer(new Vec3(x, y, z));
    }

    /**
     * 創建相對位置緩衝區
     * TODO: 實現專用的 RelativeLocationDataBuffer
     */
    public static Vec3DataBuffer relative(RelativeLocation location) {
        // 暫時使用 Vec3 代替
        return vec3(location.x, location.y, location.z);
    }
}
