package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.commands.SetVelocityCommand;
import com.github.nalamodikk.particle.commands.RotateToCommand;
import com.github.nalamodikk.particle.commands.ColorTransitionCommand;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CommandArchitectureTest {

    @Mock
    private ICooParticle particle;

    @Test
    public void testSetVelocityCommand() {
        IParticleCommand command = new SetVelocityCommand(1.0, 2.0, 3.0);
        command.execute(particle);
        verify(particle).setVelocity(1.0, 2.0, 3.0);
    }

    @Test
    public void testRotateToCommand() {
        Quaternionf rot = new Quaternionf().rotateX(1.0f);
        IParticleCommand command = new RotateToCommand(rot);
        command.execute(particle);
        verify(particle).setRotation(rot);
    }

    @Test
    public void testColorTransitionCommand() {
        IParticleCommand command = new ColorTransitionCommand(1.0f, 0.0f, 0.0f);
        command.execute(particle);
        verify(particle).setColor(1.0f, 0.0f, 0.0f);
    }
}
