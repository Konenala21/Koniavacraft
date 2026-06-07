package com.github.nalamodikk.client.sound;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 駕駛飛船時的引擎/飛行循環音，跟著船、音量隨速度。佔位用 vanilla MINECART_INSIDE，之後可換自訂 ogg。
 * 自己 tick：當地玩家不再駕駛這艘船(或船消失)就 stop。
 */
public class ShipFlySoundInstance extends AbstractTickableSoundInstance {

    private final ShipEntity ship;
    private final Player listener;
    private double lastX, lastY, lastZ;

    public ShipFlySoundInstance(ShipEntity ship, Player listener) {
        super(SoundEvents.MINECART_INSIDE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.ship = ship;
        this.listener = listener;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.25f;
        this.x = ship.getX();
        this.y = ship.getY();
        this.z = ship.getZ();
        this.lastX = ship.getX();
        this.lastY = ship.getY();
        this.lastZ = ship.getZ();
    }

    @Override
    public void tick() {
        if (ship.isRemoved() || listener.getVehicle() != ship || ship.getControllingPassenger() != listener) {
            stop();
            return;
        }
        this.x = ship.getX();
        this.y = ship.getY();
        this.z = ship.getZ();
        // 船用 setPos 移動，deltaMovement=0，所以用位置差算速度
        double speed = Math.sqrt(sq(x - lastX) + sq(y - lastY) + sq(z - lastZ));
        lastX = x; lastY = y; lastZ = z;
        this.volume = 0.25f + Mth.clamp((float) speed * 3.0f, 0.0f, 0.75f); // 怠速 hum + 速度
        this.pitch = 0.8f + Mth.clamp((float) speed * 1.5f, 0.0f, 0.5f);
    }

    private static double sq(double v) { return v * v; }
}
