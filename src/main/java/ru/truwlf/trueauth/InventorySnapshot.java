package ru.truwlf.trueauth;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class InventorySnapshot {
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final int heldSlot;

    private InventorySnapshot(PlayerInventory inventory) {
        storage = copy(inventory.getStorageContents());
        armor = copy(inventory.getArmorContents());
        offhand = copy(inventory.getItemInOffHand());
        heldSlot = inventory.getHeldItemSlot();
    }
    static InventorySnapshot capture(PlayerInventory inventory) { return new InventorySnapshot(inventory); }
    void restore(PlayerInventory inventory) {
        inventory.setStorageContents(copy(storage));
        inventory.setArmorContents(copy(armor));
        inventory.setItemInOffHand(copy(offhand));
        inventory.setHeldItemSlot(heldSlot);
    }
    static void clear(PlayerInventory inventory) {
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
    }
    private static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) result[i] = copy(items[i]);
        return result;
    }
    private static ItemStack copy(ItemStack item) { return item == null ? null : item.clone(); }
}
