package com.github.nalamodikk.common.capability;

import com.github.nalamodikk.common.capability.mana.ManaAction;

// 新的接口，整合了 ISimpleManaHandler 和 IStrictManaHandler
public interface IUnifiedManaHandler {

    // 單一槽位管理
    int getManaStored();
    void addMana(int amount);
    void consumeMana(int amount);
    void setMana(int amount);
    void onChanged();
    int getMaxManaStored();
    boolean canExtract();

    default int getNeeded() {
        int needed = Math.max(0, getMaxManaStored() - getManaStored());
        return needed;
    }


    /**
     * Checks if this handler can receive mana.
     * @return True if mana can be received.
     */
    default boolean canReceive() {
        return getNeeded() > 0; // 如果還有空間則表示可以接收
    }

    /**
     * Receives mana from an external source.
     * @param amount Mana to receive.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The amount of mana that was actually received.
     */
    default int receiveMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int toReceive = Math.min(amount, getNeeded());
        if (action.execute()) {
            addMana(toReceive);
            onChanged(); // 接收成功後通知變化
        }
        return toReceive;
    }


    default int insertMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int needed = getNeeded();
        if (needed == 0) {
            return amount; // 容器已滿，返回全部未能插入的量
        }
        int toAdd = Math.min(amount, needed);
        if (action.execute()) {
            addMana(toAdd);
            onChanged(); // 插入成功後通知變化
        }
        return amount - toAdd;
    }

    default int extractMana(int amount, ManaAction action) {
        if (amount <= 0 || getManaStored() == 0) {
            return 0;
        }
        int extracted = Math.min(amount, getManaStored());
        if (action.execute()) {
            consumeMana(extracted);
            onChanged(); // 提取成功後通知變化
        }
        return extracted;
    }

    // 多槽位管理
    int getManaContainerCount();

    int getManaStored(int container);

    void setMana(int container, int mana);

    int getMaxManaStored(int container);

    int getNeededMana(int container);

    int insertMana(int container, int amount, ManaAction action);

    int extractMana(int container, int amount, ManaAction action);

    // 插入到多個槽位
    default int insertManaToAllContainers(int amount, ManaAction action) {
        int remaining = amount;
        for (int i = 0; i < getManaContainerCount(); i++) {
            remaining = insertMana(i, remaining, action);
            if (remaining == 0) {
                break;
            }
        }
        return remaining;
    }

    // 從多個槽位提取
    default int extractManaFromAllContainers(int amount, ManaAction action) {
        int extracted = 0;
        for (int i = 0; i < getManaContainerCount(); i++) {
            extracted += extractMana(i, amount - extracted, action);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }
}
