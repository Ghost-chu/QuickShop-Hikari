package com.ghostchu.quickshop.shop.inventory;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.inventory.InventoryWrapper;
import com.ghostchu.quickshop.api.inventory.InventoryWrapperIterator;
import com.ghostchu.quickshop.api.inventory.InventoryWrapperManager;
import com.ghostchu.quickshop.api.inventory.InventoryWrapperType;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class BukkitInventoryWrapper implements InventoryWrapper {
    private final Inventory inventory;
    private final InventoryWrapperManager manager;
    private final Supplier<String> eigenCodeProvider;
    private final String eigenCode;

    public BukkitInventoryWrapper(@NotNull Inventory inventory) {
        this(inventory, () -> null);
    }

    public BukkitInventoryWrapper(@NotNull Inventory inventory, Supplier<String> eigenCodeProvider) {
        this.inventory = inventory;
        this.manager = QuickShop.getInstance().getInventoryWrapperManager();
        this.eigenCodeProvider = eigenCodeProvider;
        this.eigenCode = eigenCodeProvider.get();
    }

    @Override
    public @NotNull InventoryWrapperIterator iterator() {
        return InventoryWrapperIterator.ofBukkitInventory(inventory);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public @NotNull ItemStack[] createSnapshot() {
        ItemStack[] content = this.inventory.getContents();
        ItemStack[] snapshot = new ItemStack[content.length];
        for (int i = 0; i < content.length; i++) {
            if (content[i] != null) {
                snapshot[i] = content[i].clone();
            } else {
                snapshot[i] = null;
            }

        }
        return snapshot;
    }

    @Override
    public @NotNull InventoryWrapperManager getWrapperManager() {
        return this.manager;
    }

    @Override
    public InventoryHolder getHolder() {
        return inventory.getHolder();
    }

    @Override
    public @NotNull InventoryWrapperType getInventoryType() {
        return InventoryWrapperType.BUKKIT;
    }

    @Override
    public @Nullable Location getLocation() {
        return inventory.getLocation();
    }

    @Override
    public boolean isValid() {
        if (this.inventory.getHolder() != null) {
            return true;
        } else {
            return this.inventory instanceof InventoryHolder;
        }
    }

    @Override
    public boolean isNeedUpdate() {
        return Objects.equals(eigenCode, eigenCodeProvider.get());
    }

    @Override
    public boolean restoreSnapshot(@NotNull ItemStack[] snapshot) {
        this.inventory.setContents(snapshot);
        return true;
    }

    @Override
    public @NotNull Map<Integer, ItemStack> addItem(ItemStack... itemStacks) {
        return inventory.addItem(itemStacks);
    }

    @Override
    public void setContents(ItemStack[] itemStacks) {
        inventory.setStorageContents(itemStacks);
    }
}
