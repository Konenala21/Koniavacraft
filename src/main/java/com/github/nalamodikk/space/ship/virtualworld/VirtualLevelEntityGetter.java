// Ported from Create (Creators-of-Create/Create, MIT licensed) foundation.virtualWorld, mc1.21.1/dev.
// Only change: package. Used by飛船 VirtualRenderWorld 給 BER 一個假 Level 查 contraption 方塊。
package com.github.nalamodikk.space.ship.virtualworld;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

public class VirtualLevelEntityGetter<T extends EntityAccess> implements LevelEntityGetter<T> {
	@Override
	public T get(int id) {
		return null;
	}

	@Override
	public T get(UUID uuid) {
		return null;
	}

	@Override
	public Iterable<T> getAll() {
		return Collections.emptyList();
	}

	@Override
	public <U extends T> void get(EntityTypeTest<T, U> test, AbortableIterationConsumer<U> consumer) {
	}

	@Override
	public void get(AABB boundingBox, Consumer<T> consumer) {
	}

	@Override
	public <U extends T> void get(EntityTypeTest<T, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {
	}
}
