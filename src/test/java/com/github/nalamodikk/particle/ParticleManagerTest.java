package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.utils.PerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
        // 皜??桐????(璅⊥)
        // ?望?瘜???蝵桃???畾蛛???閮剜葫閰衣憓????????? manager.cleanup();
    }

    @Test
    public void testRegisterAndUnregister() {
        manager.registerParticle(particleId, particle);
        assertTrue(manager.getParticle(particleId).isPresent());

        manager.unregisterParticle(particleId);
        assertFalse(manager.getParticle(particleId).isPresent());
    }

    @Test
    public void testEvictionUnderLoad() {
        // 璅⊥ PerformanceMonitor
        try (MockedStatic<PerformanceMonitor> mockedMonitor = mockStatic(PerformanceMonitor.class)) {
            PerformanceMonitor monitor = mock(PerformanceMonitor.class);
            mockedMonitor.when(PerformanceMonitor::getInstance).thenReturn(monitor);

            // 閮剖?璆萎?????(靘? 2)
            when(monitor.getParticleLimit()).thenReturn(2);

            ICooParticle p1 = mock(ICooParticle.class);
            ICooParticle p2 = mock(ICooParticle.class);
            ICooParticle p3 = mock(ICooParticle.class);

            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            manager.registerParticle(id1, p1);
            manager.registerParticle(id2, p2);

            // 甇斗??府皛蹂?

            // 瘛餃?蝚砌????府閫貊 p1 ??remove
            manager.registerParticle(id3, p3);

            verify(p1).remove(); // 驗證 p1 被移除
        }
    }
}
