package com.github.nalamodikk.common.capability;

import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;


public class ManaStorage implements IUnifiedManaHandler , INBTSerializable<CompoundTag> {

    private int mana;
    private int capacity; // 🆕 改為可變，支援動態容量調整
    protected boolean allowInsert = true;
    protected boolean allowExtract = true;
    private Runnable changeListener;

    public ManaStorage(int capacity) {
        this(capacity, null);
    }

    public ManaStorage(int capacity, Runnable changeListener) {
        this.capacity = capacity;
        this.mana = 0;
        this.changeListener = changeListener;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    /**
     * 🆕 設定新的容量
     * 如果當前魔力超過新容量，會被截斷
     */
    public void setCapacity(int newCapacity) {
        int oldMana = this.mana;
        this.capacity = newCapacity;
        // 如果當前魔力超過新容量，截斷到新容量
        if (this.mana > newCapacity) {
            this.mana = newCapacity;
        }
        if (this.mana != oldMana) {
            onChanged();
        }
    }

    public boolean canInsert() {
        return allowInsert && this.mana < this.capacity;
    }

    @Override
    public boolean canExtract() {
        return allowExtract && this.mana > 0;
    }

    public void setAllowInsert(boolean value) {
        this.allowInsert = value;
    }

    public void setAllowExtract(boolean value) {
        this.allowExtract = value;
    }



    public boolean isFull() {
        return this.getManaStored() >= this.getMaxManaStored();
    }

    @Override
    public void addMana(int amount) {
        int oldMana = this.mana;
        this.mana = Math.min(this.mana + amount, capacity);
        if (this.mana != oldMana) {
            onChanged(); // 添加魔力時通知變化
        }
    }

    @Override
    public void consumeMana(int amount) {
        int oldMana = this.mana;
        this.mana = Math.max(this.mana - amount, 0);
        if (this.mana != oldMana) {
            onChanged(); // 消耗魔力時通知變化
        }
    }

    @Override
    public int getManaStored() {
        return mana;
    }

    @Override
    public void setMana(int amount) {
        int oldMana = this.mana;
        this.mana = Math.max(0, Math.min(amount, capacity));
        if (this.mana != oldMana) {
            onChanged();
        }
    }

    @Override
    public void onChanged() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    @Override
    public int getMaxManaStored() {
        return capacity;
    }



    /** 這裡修正多槽位的問題，因為這個 class 只有一個 Mana 容器 */
    @Override
    public int getManaContainerCount() {
        return 1; // 這是一個單獨的 Mana 儲存容器
    }

    @Override
    public int getManaStored(int container) {
        return container == 0 ? getManaStored() : 0; // 只支援 container 0
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            setMana(mana);
        }
    }

    @Override
    public int getMaxManaStored(int container) {
        return container == 0 ? getMaxManaStored() : 0;
    }

    @Override
    public int getNeededMana(int container) {
        return container == 0 ? getMaxManaStored() - getManaStored() : 0;
    }

    public int getNeededMana() {
        return getMaxManaStored() - getManaStored(); // 單槽版本等同 container 0
    }
    public float getFillRatio() {
        return this.capacity > 0 ? (float) this.mana / this.capacity : 0f;
    }


    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        if (container != 0) {
            return amount; // 只允許 container 0 存魔力
        }
        return insertMana(amount, action);
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        if (container != 0) {
            return 0; // 只允許 container 0 提取魔力
        }
        return extractMana(amount, action);
    }

    @Override
    public int extractMana(int amount, ManaAction action) {
        if (amount <= 0 || mana == 0) {
            return 0;
        }

        int manaExtracted = Math.min(amount, mana);

        if (action.execute() && manaExtracted > 0) {
            mana -= manaExtracted;
            onChanged(); // 確保變更被通知
        }

        return manaExtracted;
    }

    @Override
    public int receiveMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int toReceive = Math.min(amount, getMaxManaStored() - getManaStored());
        if (action.execute() && toReceive > 0) {
            addMana(toReceive);
        }
        return toReceive;
    }

    @Override
    public int insertMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int needed = getMaxManaStored() - getManaStored();
        if (needed <= 0) {
            return amount;
        }
        int toAdd = Math.min(amount, needed);
        if (action.execute() && toAdd > 0) {
            int oldMana = this.mana;
            this.mana += toAdd;
            if (this.mana != oldMana) {
                onChanged();
            }
        }
        return amount - toAdd;
    }



    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", this.mana);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.mana = nbt.getInt("Mana");

    }
}
