package com.ghostchu.quickshop.api.inventory;

import com.ghostchu.quickshop.api.QuickShopAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper to handle inventory/fake inventory/custom inventory etc.
 */
public interface InventoryWrapper extends Iterable<ItemStack> {

    /**
     * Change items in the inventory by index
     * Set the item-stack type to air or amount to zero will remove it
     * <p>
     * It's not thread-safe, please use that in main-thread
     *
     * @see ItemChanger
     */
    default void changeItem(ItemChanger itemChanger) {
        InventoryWrapperIterator iterator = iterator();
        int index = 0;
        boolean shouldContinue = true;
        while (shouldContinue && iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            shouldContinue = itemChanger.changeItem(index, itemStack);
            if (itemStack.getAmount() == 0 || itemStack.getType() == Material.AIR) {
                iterator.setCurrent(null);

            } else {
                iterator.setCurrent(itemStack);
            }
            index++;
        }
    }

    /**
     * Return the iterator for this inventory
     * It's not thread-safe, please use that in main-thread
     *
     * @return the iterator for this inventory
     */
    @NotNull
    @Override
    InventoryWrapperIterator iterator();

    /**
     * Clear the inventory
     */
    void clear();

    /**
     * Create an Inventory snapshot (including Empty slots).
     * QuickShop-Hikari will shoot a snapshot for inventory used for purchase failure rollback.
     * Note: provide an invalid snapshot will cause rollback break whole Inventory!
     * WARNING: High recommend to override this method, default method doing by badways and low-performance,
     * may mess up Inventory item order.
     *
     * @return The inventory contents for snapshot use.
     */
    @NotNull
    default ItemStack[] createSnapshot() {
        Logger.getLogger("QuickShop-Hikari").log(Level.WARNING, "InventoryWrapper provider " + getWrapperManager().getClass().getName() + " didn't override default InventoryWrapper#createSnapshot method, it may cause un-excepted behavior like item missing, mess order and heavy hit performance! Please report this issue to InventoryWrapper provider plugin author!");
        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack stack : this) {
            if (stack == null) {
                continue;
            }
            contents.add(stack.clone());
        }
        return contents.toArray(new ItemStack[0]);
    }

    /**
     * Gets the Inventory Wrapper Manager
     *
     * @return Wrapper Manager
     */
    @NotNull InventoryWrapperManager getWrapperManager();

    /**
     * Gets the block or entity belonging to the open inventory
     *
     * @return The holder of the inventory; null if it has no holder.
     */
    @Nullable InventoryHolder getHolder();

    /**
     * Gets the Inventory Type
     *
     * @return The Inventory Type
     */
    @NotNull InventoryWrapperType getInventoryType();

    /**
     * Get the location of the block or entity which corresponds to this inventory. May return null if this container
     * was custom created or is a virtual / subcontainer.
     *
     * @return location or null if not applicable.
     */
    @Nullable Location getLocation();

    /**
     * Do valid check, check if this Inventory is valid.
     *
     * @return valid
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Do update check, check if inventory-wrapper should discard and re-create via locateSymbolLink
     * @return should re-locate inventory
     */
    boolean isNeedUpdate();

    /**
     * Remove specific items from inventory
     *
     * @param itemStacks items to remove
     * @return The map of containing item index and itemStack itself which is not fit
     */
    @NotNull
    default Map<Integer, ItemStack> removeItem(ItemStack... itemStacks) {
        if (itemStacks.length == 0) {
            return Collections.emptyMap();
        }
        InventoryWrapperIterator iterator = iterator();
        Map<Integer, ItemStack> integerItemStackMap = new HashMap<>();
        RemoveProcess:
        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack itemStackToRemove = itemStacks[i];
            while (iterator.hasNext()) {
                ItemStack itemStack = iterator.next();
                // TODO: Need lots of verification, it cause mismatch between items under non-Bukkit item matcher
                if (itemStack != null && QuickShopAPI.getInstance().getItemMatcher().matches(itemStackToRemove, itemStack)) {
                    int couldRemove = itemStack.getAmount();
                    int actuallyRemove = Math.min(itemStackToRemove.getAmount(), couldRemove);
                    itemStack.setAmount(itemStack.getAmount() - actuallyRemove);
                    int needsNow = itemStackToRemove.getAmount() - actuallyRemove;
                    itemStackToRemove.setAmount(needsNow);
                    iterator.setCurrent(itemStack);
                    if (needsNow == 0) {
                        continue RemoveProcess;
                    }
                }
            }
            if (itemStackToRemove.getAmount() != 0) {
                integerItemStackMap.put(i, itemStackToRemove);
            }
        }
        return integerItemStackMap;
    }

    /**
     * Rollback Inventory by a snapshot.
     * Snapshot can be created by InventoryWrapper#createSnapshot()
     * WARNING: High recommend to override this method, default method doing by badways and low-performance,
     * may mess up Inventory item order.
     *
     * @param snapshot The inventory content snapshot
     * @return The result of rollback.
     */
    default boolean restoreSnapshot(@NotNull ItemStack[] snapshot) {
        Logger.getLogger("QuickShop-Hikari").log(Level.WARNING, "InventoryWrapper provider " + getWrapperManager().getClass().getName() + " didn't override default InventoryWrapper#restoreSnapshot method, it may cause un-excepted behavior like item missing, mess order and heavy hit performance! Please report this issue to InventoryWrapper provider plugin author!");
        InventoryWrapperIterator it = iterator();
        while (it.hasNext()) {
            it.remove();
        }
        Map<Integer, ItemStack> result = addItem(snapshot);
        return result.isEmpty();
    }

    /**
     * Add specific items from inventory
     *
     * @param itemStacks items to add
     * @return The map of containing item index and itemStack itself which is not fit
     */
    @NotNull
    default Map<Integer, ItemStack> addItem(ItemStack... itemStacks) {
        if (itemStacks.length == 0) {
            return Collections.emptyMap();
        }
        InventoryWrapperIterator iterator = iterator();
        Map<Integer, ItemStack> integerItemStackMap = new HashMap<>();
        AddProcess:
        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack itemStackToAdd = itemStacks[i];
            while (iterator.hasNext()) {
                ItemStack itemStack = iterator.next();
                if (itemStack == null) {
                    iterator.setCurrent(itemStackToAdd);
                    itemStackToAdd.setAmount(0);
                    continue AddProcess;
                } else {
                    if (itemStack.isSimilar(itemStackToAdd)) {
                        int couldAdd = itemStack.getMaxStackSize() - Math.min(itemStack.getMaxStackSize(), itemStack.getAmount());
                        int actuallyAdd = Math.min(itemStackToAdd.getAmount(), couldAdd);
                        itemStack.setAmount(itemStack.getAmount() + actuallyAdd);
                        int needsNow = itemStackToAdd.getAmount() - actuallyAdd;
                        itemStackToAdd.setAmount(needsNow);
                        iterator.setCurrent(itemStack);
                        if (needsNow == 0) {
                            continue AddProcess;
                        }
                    }
                }
            }
            if (itemStackToAdd.getAmount() != 0) {
                integerItemStackMap.put(i, itemStackToAdd);
            }
        }
        return integerItemStackMap;
    }

    /**
     * Set the contents of inventory
     *
     * @param itemStacks the contents you want to set
     */
    void setContents(ItemStack[] itemStacks);

    /**
     * Change the item from Inventory
     */
    interface ItemChanger {
        /**
         * Do item change action in the inventory
         *
         * @param index     the item index in the inventory, start from zero
         * @param itemStack the item in this index
         * @return If continue to change items in the next index
         */
        boolean changeItem(int index, ItemStack itemStack);
    }

}
