package com.github.nalamodikk.barrages;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class Barrage {
    protected Vec3 loc;
    protected final ServerLevel world;
    protected HitBox hitBox;
    protected LivingEntity shooter;
    protected Vec3 direction;
    protected boolean launch = false;
    protected boolean valid = true;
    protected final UUID uuid = UUID.randomUUID();

    public Barrage(ServerLevel world, Vec3 loc) {
        this.world = world;
        this.loc = loc;
        this.direction = Vec3.ZERO;
        this.hitBox = HitBox.of(0.5, 0.5, 0.5);
    }

    public abstract void tick();
    
    // 省略了 hit/onHit 等細節，專注於核心抽象
    public boolean isValid() {
        return valid;
    }
    
    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
    }
    
    public void setDirection(Vec3 direction) {
        this.direction = direction;
    }
    
    public void launch() {
        this.launch = true;
    }
}
