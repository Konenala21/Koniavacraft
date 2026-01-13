package com.github.nalamodikk.particle.manager;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.emitter.ParticleEmitter;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子發射器管理系統
 * 負責發射器的註冊、生命週期管理和網絡同步
 */
public class ParticleEmittersManager {
    private static final ParticleEmittersManager INSTANCE = new ParticleEmittersManager();

    /** 發射器編解碼器註冊表 */
    private final Map<String, StreamCodec<FriendlyByteBuf, ? extends ParticleEmitter>> emitterCodecs = new HashMap<>();

    /** 服務器端活躍的發射器（UUID -> Emitter） */
    private final Map<UUID, ParticleEmitter> serverEmitters = new HashMap<>();

    /** 客戶端可見的發射器（UUID -> Emitter） */
    private final Map<UUID, ParticleEmitter> clientEmitters = new ConcurrentHashMap<>();

    /** 玩家可見性映射（PlayerUUID -> Set<Emitter>） */
    private final Map<UUID, Set<ParticleEmitter>> visibleToPlayers = new ConcurrentHashMap<>();

    private ParticleEmittersManager() {}

    public static ParticleEmittersManager getInstance() {
        return INSTANCE;
    }

    // ========== 註冊系統 ==========

    /**
     * 註冊發射器類型
     * @param id 發射器 ID
     * @param codec 編解碼器
     */
    public <T extends ParticleEmitter> void register(String id, StreamCodec<FriendlyByteBuf, T> codec) {
        if (emitterCodecs.containsKey(id)) {
            KoniavacraftMod.LOGGER.warn("Emitter ID already registered: {}", id);
            return;
        }
        emitterCodecs.put(id, codec);
        KoniavacraftMod.LOGGER.debug("Registered particle emitter: {}", id);
    }

    /**
     * 通過實例註冊發射器
     * @param instance 發射器實例
     */
    public void register(ParticleEmitter instance) {
        register(instance.getEmitterId(), instance.getCodec());
    }

    /**
     * 獲取編解碼器
     * @param id 發射器 ID
     */
    @SuppressWarnings("unchecked")
    public <T extends ParticleEmitter> StreamCodec<FriendlyByteBuf, T> getCodec(String id) {
        return (StreamCodec<FriendlyByteBuf, T>) emitterCodecs.get(id);
    }

    // ========== 服務器端管理 ==========

    /**
     * 在服務器端生成發射器
     * @param emitter 發射器實例
     */
    public void spawnEmitter(ParticleEmitter emitter) {
        Level world = emitter.getWorld();
        if (world == null || world.isClientSide()) {
            return;
        }

        serverEmitters.put(emitter.getUuid(), emitter);
        emitter.start();
        updateClientVisibility(emitter);
    }

    /**
     * 服務器端 Tick 更新
     */
    public void doServerTick() {
        Iterator<Map.Entry<UUID, ParticleEmitter>> iterator = serverEmitters.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ParticleEmitter> entry = iterator.next();
            ParticleEmitter emitter = entry.getValue();

            // 更新可見性
            updateClientVisibility(emitter);

            // Tick 更新
            emitter.tick();

            // 如果已取消，移除並通知所有可見玩家
            if (emitter.isCancelled()) {
                Set<UUID> visiblePlayers = getVisiblePlayers(emitter);
                for (UUID playerUuid : visiblePlayers) {
                    Level world = emitter.getWorld();
                    if (world != null) {
                        ServerPlayer player = (ServerPlayer) world.getPlayerByUUID(playerUuid);
                        if (player != null) {
                            removeFromView(player, emitter);
                        }
                    }
                    Set<ParticleEmitter> playerEmitters = visibleToPlayers.get(playerUuid);
                    if (playerEmitters != null) {
                        playerEmitters.remove(emitter);
                    }
                }
                iterator.remove();
            }
        }
    }

    // ========== 客戶端管理 ==========

    /**
     * 在客戶端添加發射器
     * @param emitter 發射器實例
     */
    public void addClientEmitter(ParticleEmitter emitter) {
        Level world = emitter.getWorld();
        if (world == null || !world.isClientSide()) {
            return;
        }

        clientEmitters.put(emitter.getUuid(), emitter);
        emitter.start();
        // TODO: 觸發 EmitterSpawnEvent
    }

    /**
     * 客戶端創建或更新發射器
     * @param emitter 發射器實例
     * @param viewWorld 視圖世界
     */
    public void createOrUpdateClient(ParticleEmitter emitter, Level viewWorld) {
        if (emitter.isCancelled()) {
            clientEmitters.remove(emitter.getUuid());
            return;
        }

        ParticleEmitter existing = clientEmitters.get(emitter.getUuid());
        if (existing != null) {
            existing.update(emitter);
            existing.setWorld(viewWorld);
        } else {
            emitter.setWorld(viewWorld);
            clientEmitters.put(emitter.getUuid(), emitter);
            // TODO: 觸發 EmitterSpawnEvent
        }
    }

    /**
     * 客戶端 Tick 更新
     */
    public void doClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) {
            clientEmitters.clear();
            return;
        }

        Iterator<Map.Entry<UUID, ParticleEmitter>> iterator = clientEmitters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ParticleEmitter> entry = iterator.next();
            ParticleEmitter emitter = entry.getValue();

            emitter.tick();

            if (emitter.isCancelled()) {
                iterator.remove();
                // TODO: 觸發 EmitterRemoveEvent
            }
        }
    }

    /**
     * 清除所有客戶端可見發射器
     */
    public void clearAllClientEmitters() {
        clientEmitters.values().forEach(emitter -> emitter.setCancelled(true));
        clientEmitters.clear();
    }

    // ========== 可見性管理 ==========

    /**
     * 獲取發射器對哪些玩家可見
     * @param emitter 發射器
     * @return 玩家 UUID 集合
     */
    public Set<UUID> getVisiblePlayers(ParticleEmitter emitter) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<ParticleEmitter>> entry : visibleToPlayers.entrySet()) {
            if (entry.getValue().contains(emitter)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 更新客戶端可見性
     * @param emitter 發射器
     */
    private void updateClientVisibility(ParticleEmitter emitter) {
        Level world = emitter.getWorld();
        if (world == null || world.isClientSide()) {
            return;
        }

        // TODO: 觸發 EmitterSpawnEvent（服務器端）

        // 遍歷所有在線玩家
        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            Set<ParticleEmitter> visibleSet = visibleToPlayers.computeIfAbsent(
                player.getUUID(), k -> new HashSet<>()
            );

            // 檢查世界是否匹配
            if (!player.level().equals(emitter.getWorld())) {
                if (visibleSet.contains(emitter)) {
                    removeFromView(player, emitter);
                    visibleSet.remove(emitter);
                }
                continue;
            }

            // 檢查玩家是否死亡
            if (player.isDeadOrDying()) {
                if (visibleSet.contains(emitter)) {
                    removeFromView(player, emitter);
                    visibleSet.remove(emitter);
                }
                continue;
            }

            // 檢查距離（可見範圍）
            double distance = player.position().distanceTo(emitter.getPos());
            if (distance > emitter.getVisibleRange()) {
                if (visibleSet.contains(emitter)) {
                    removeFromView(player, emitter);
                    visibleSet.remove(emitter);
                }
                continue;
            }

            // 如果不在可見列表中，添加
            if (!visibleSet.contains(emitter)) {
                addToView(player, emitter);
                visibleSet.add(emitter);
            }
        }
    }

    /**
     * 更新發射器狀態並同步到客戶端
     * @param emitter 發射器
     */
    public void updateEmitter(ParticleEmitter emitter) {
        Set<UUID> visiblePlayers = getVisiblePlayers(emitter);
        Level world = emitter.getWorld();
        if (world == null) {
            return;
        }

        for (UUID playerUuid : visiblePlayers) {
            ServerPlayer player = (ServerPlayer) world.getPlayerByUUID(playerUuid);
            if (player != null) {
                sendChange(emitter, player);
            }
        }
    }

    // ========== 網絡同步（占位符） ==========

    /**
     * 將發射器添加到玩家視野
     * @param player 玩家
     * @param emitter 發射器
     */
    private void addToView(ServerPlayer player, ParticleEmitter emitter) {
        // TODO: 發送網絡包到客戶端
        // byte[] data = encodeEmitterToBytes(emitter);
        // PacketParticleEmitterS2C packet = new PacketParticleEmitterS2C(
        //     emitter.getEmitterId(), data, PacketType.CREATE_OR_UPDATE
        // );
        // sendPacketToPlayer(packet, player);

        KoniavacraftMod.LOGGER.debug("Added emitter {} to player {}'s view",
            emitter.getUuid(), player.getName().getString());
    }

    /**
     * 從玩家視野移除發射器
     * @param player 玩家
     * @param emitter 發射器
     */
    private void removeFromView(ServerPlayer player, ParticleEmitter emitter) {
        // TODO: 發送移除網絡包
        // byte[] data = encodeEmitterToBytes(emitter);
        // PacketParticleEmitterS2C packet = new PacketParticleEmitterS2C(
        //     emitter.getEmitterId(), data, PacketType.REMOVE
        // );
        // sendPacketToPlayer(packet, player);

        KoniavacraftMod.LOGGER.debug("Removed emitter {} from player {}'s view",
            emitter.getUuid(), player.getName().getString());
    }

    /**
     * 發送發射器變更到玩家
     * @param emitter 發射器
     * @param player 玩家
     */
    private void sendChange(ParticleEmitter emitter, ServerPlayer player) {
        // TODO: 實現網絡包發送
        KoniavacraftMod.LOGGER.debug("Sending emitter {} update to player {}",
            emitter.getUuid(), player.getName().getString());
    }

    /**
     * 將發射器編碼為字節數組
     * @param emitter 發射器
     * @return 字節數組
     */
    private byte[] encodeEmitterToBytes(ParticleEmitter emitter) {
        @SuppressWarnings("unchecked")
        StreamCodec<FriendlyByteBuf, ParticleEmitter> codec =
            (StreamCodec<FriendlyByteBuf, ParticleEmitter>) emitter.getCodec();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, emitter);

        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    // ========== 調試信息 ==========

    public int getServerEmitterCount() {
        return serverEmitters.size();
    }

    public int getClientEmitterCount() {
        return clientEmitters.size();
    }

    public Map<UUID, ParticleEmitter> getServerEmitters() {
        return Collections.unmodifiableMap(serverEmitters);
    }

    public Map<UUID, ParticleEmitter> getClientEmitters() {
        return Collections.unmodifiableMap(clientEmitters);
    }
}
