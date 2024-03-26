package com.ghostchu.quickshop.api.event;

import com.ghostchu.quickshop.api.inventory.InventoryWrapper;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.shop.Shop;
import org.jetbrains.annotations.NotNull;

/**
 * Calling when success purchase in shop
 */
public class ShopSuccessPurchaseEvent extends AbstractQSEvent {

    @NotNull
    private final Shop shop;

    private final int amount;

    @NotNull
    private final QUser purchaser;

    @NotNull
    private final InventoryWrapper purchaserInventory;

    private final double tax;

    private final double
            total; // Don't use getter, we have important notice need told dev in javadoc.

    /**
     * Builds a new shop purchase event
     * Will called when purchase ended
     *
     * @param shop               The shop bought from
     * @param purchaser          The player buying, may offline if purchase by plugin
     * @param purchaserInventory The purchaseing target inventory, *MAY NOT A PLAYER INVENTORY IF PLUGIN PURCHASE THIS*
     * @param amount             The amount they're buying
     * @param tax                The tax in this purchase
     * @param total              The money in this purchase
     */
    public ShopSuccessPurchaseEvent(
            @NotNull Shop shop, @NotNull QUser purchaser, @NotNull InventoryWrapper purchaserInventory, int amount, double total, double tax) {
        this.shop = shop;
        this.purchaser = purchaser;
        this.purchaserInventory = purchaserInventory;
        this.amount = amount * shop.getItem().getAmount();
        this.tax = tax;
        this.total = total;
    }

    /**
     * Gets the item stack amounts
     *
     * @return Item stack amounts
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * The total money changes in this purchase. Calculate tax, if you want get total without tax,
     * please use getBalanceWithoutTax()
     *
     * @return the total money with calculate tax
     */
    public double getBalance() {
        return this.total - tax;
    }

    /**
     * The total money changes in this purchase. No calculate tax, if you want get total with tax,
     * please use getBalance()
     *
     * @return the total money without calculate tax
     */
    public double getBalanceWithoutTax() {
        return this.total;
    }


    /**
     * Gets the purchaser, that maybe is a online/offline/virtual player.
     *
     * @return The purchaser uuid
     */
    public @NotNull QUser getPurchaser() {
        return this.purchaser;
    }

    /**
     * Gets the inventory of purchaser (the item will put to)
     *
     * @return The inventory
     */
    public @NotNull InventoryWrapper getPurchaserInventory() {
        return this.purchaserInventory;
    }

    /**
     * Gets the shop
     *
     * @return the shop
     */
    public @NotNull Shop getShop() {
        return this.shop;
    }

    /**
     * Gets the tax in this purchase
     *
     * @return The tax
     */
    public double getTax() {
        return this.tax;
    }
}
