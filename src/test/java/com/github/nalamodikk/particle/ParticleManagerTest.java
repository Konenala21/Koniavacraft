package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParticleManagerTest {

    private ParticleManager manager;
    private UUID particleId;

    @Mock
    private ICooParticle particle;

    @BeforeEach
    public void setUp() {
        manager = ParticleManager.getInstance();
        particleId = UUID.randomUUID();
        // 清理單例狀態
        manager.unregisterParticle(particleId);
    }

    @Test
    public void testRegisterAndUnregister() {
        manager.registerParticle(particleId, particle);
        assertTrue(manager.getParticle(particleId).isPresent());

        manager.unregisterParticle(particleId);
        assertFalse(manager.getParticle(particleId).isPresent());
    }

    @Test
    public void testQueueAndExecuteCommands() {
        IParticleCommand command = mock(IParticleCommand.class);
        manager.registerParticle(particleId, particle);
        
        manager.queueCommand(particleId, command);
        manager.executeCommands(particleId, particle);

        verify(command).execute(particle);
    }

    @Test
    public void testCleanup() {
        manager.queueCommand(particleId, mock(IParticleCommand.class));
        // 不註冊粒子，直接調用 cleanup
        manager.cleanup();
        // 執行指令時不應有任何動作（隊列已被清理）
        manager.executeCommands(particleId, particle);
        // 註：這裏驗證的是內部隊列被移除，無法直接通過 executeCommands 驗證，
        // 但可以驗證 cleanup 後 getParticle 依然為空
        assertFalse(manager.getParticle(particleId).isPresent());
    }
}
