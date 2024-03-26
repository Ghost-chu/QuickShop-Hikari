package com.ghostchu.quickshop.shop;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.ServiceInjector;
import com.ghostchu.quickshop.api.economy.Benefit;
import com.ghostchu.quickshop.api.event.*;
import com.ghostchu.quickshop.api.inventory.InventoryWrapper;
import com.ghostchu.quickshop.api.inventory.InventoryWrapperManager;
import com.ghostchu.quickshop.api.localization.text.ProxiedLocale;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.serialize.BlockPos;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopInfoStorage;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermissionGroup;
import com.ghostchu.quickshop.common.util.CommonUtil;
import com.ghostchu.quickshop.common.util.JsonUtil;
import com.ghostchu.quickshop.database.bean.SimpleDataRecord;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.shop.datatype.ShopSignPersistentDataType;
import com.ghostchu.quickshop.shop.display.AbstractDisplayItem;
import com.ghostchu.quickshop.shop.display.RealDisplayItem;
import com.ghostchu.quickshop.util.MsgUtil;
import com.ghostchu.quickshop.util.PackageUtil;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.quickshop.util.logging.container.ShopRemoveLog;
import com.ghostchu.quickshop.util.performance.PerfMonitor;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import io.papermc.lib.PaperLib;
import lombok.EqualsAndHashCode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ChestShop core
 */
@EqualsAndHashCode
public class ContainerShop implements Shop, Reloadable {
    // We use deprecated method to create a fake quickshop-reremake namespace to trick bukkit to access legacy data.
    @SuppressWarnings({"AliDeprecation", "deprecation"})
    private static final NamespacedKey LEGACY_SHOP_NAMESPACED_KEY = new NamespacedKey("quickshop", "shopsign");
    private static final String LEGACY_SHOP_SIGN_RECOGNIZE_PATTERN = "§d§o ";
    @NotNull
    private final Location location;
    private final YamlConfiguration extra;
    @EqualsAndHashCode.Exclude
    private final QuickShop plugin;
    @EqualsAndHashCode.Exclude
    private final UUID runtimeRandomUniqueId = UUID.randomUUID();
    @NotNull
    private final Map<UUID, String> playerGroup;
    private long shopId;
    private QUser owner;
    private double price;
    private ShopType shopType;
    private boolean unlimited;
    @NotNull
    private ItemStack item;
    @NotNull
    private ItemStack originalItem;
    @Nullable
    @EqualsAndHashCode.Exclude
    private AbstractDisplayItem displayItem;
    @EqualsAndHashCode.Exclude
    private volatile boolean isLoaded = false;
    @EqualsAndHashCode.Exclude
    private final boolean isDeleted = false;
    @EqualsAndHashCode.Exclude
    private volatile boolean createBackup = false;
    @EqualsAndHashCode.Exclude
    private InventoryPreview inventoryPreview = null;
    @EqualsAndHashCode.Exclude
    private boolean dirty;
    @EqualsAndHashCode.Exclude
    private boolean updating = false;
    @Nullable
    private String currency;
    private boolean disableDisplay;
    private QUser taxAccount;
    @NotNull
    private String inventoryWrapperProvider;
    @EqualsAndHashCode.Exclude
    private InventoryWrapper inventoryWrapper;
    @NotNull
    private String symbolLink;
    @Nullable
    private String shopName;

    @NotNull
    private Benefit benefit;

//    ContainerShop(@NotNull ContainerShop s) {
//        Util.ensureThread(false);
//        this.shopId = s.shopId;
//        this.shopType = s.shopType;
//        this.item = s.item.clone();
//        this.originalItem = s.originalItem.clone();
//        this.location = s.location.clone();
//        this.plugin = s.plugin;
//        this.unlimited = s.unlimited;
//        this.owner = s.owner;
//        this.price = s.price;
//        this.isLoaded = s.isLoaded;
//        this.isDeleted = s.isDeleted;
//        this.createBackup = s.createBackup;
//        this.extra = s.extra;
//        this.dirty = true;
//        this.inventoryPreview = null;
//        this.currency = s.currency;
//        this.disableDisplay = s.disableDisplay;
//        this.taxAccount = s.taxAccount;
//        this.inventoryWrapper = s.inventoryWrapper;
//        this.inventoryWrapperProvider = s.inventoryWrapperProvider;
//        this.symbolLink = s.symbolLink;
//        this.shopName = s.shopName;
//        this.playerGroup = s.playerGroup;
//        this.benefit = s.benefit;
//    }


    /**
     * Adds a new shop. You need call ShopManager#loadShop if you create from outside of
     * ShopLoader.
     *
     * @param location  The location of the chest block
     * @param price     The cost per item
     * @param item      The itemstack with the properties we want. This is .cloned, no need to worry
     *                  about references
     * @param owner     The shop owner
     * @param type      The shop type
     * @param unlimited The unlimited
     * @param plugin    The plugin instance
     * @param extra     The extra data saved by addon
     */
    public ContainerShop(
            @NotNull QuickShop plugin,
            long shopId,
            @NotNull Location location,
            double price,
            @NotNull ItemStack item,
            @NotNull QUser owner,
            boolean unlimited,
            @NotNull ShopType type,
            @NotNull YamlConfiguration extra,
            @Nullable String currency,
            boolean disableDisplay,
            @Nullable QUser taxAccount,
            @NotNull String inventoryWrapperProvider,
            @NotNull String symbolLink,
            @Nullable String shopName,
            @NotNull Map<UUID, String> playerGroup,
            @NotNull Benefit shopBenefit) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.location = location;
        this.price = price;
        this.benefit = shopBenefit;


        // Upgrade the shop moderator
        this.owner = owner;
        this.item = item.clone();
        this.originalItem = item.clone();
        this.plugin = plugin;
        this.playerGroup = new HashMap<>(playerGroup);
        if (!plugin.isAllowStack()) {
            this.item.setAmount(1);
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().matches("\\{.*}")) {
                //https://hub.spigotmc.org/jira/browse/SPIGOT-5964
                meta.setDisplayName(meta.getDisplayName());
                //Correct both items
                item.setItemMeta(meta);
                this.item.setItemMeta(meta);
            }
        }
        this.shopType = type;
        this.unlimited = unlimited;
        this.extra = extra;
        this.currency = currency;
        this.disableDisplay = disableDisplay;
        this.taxAccount = taxAccount;
        this.dirty = false;
        if (symbolLink == null) {
            throw new IllegalArgumentException("SymbolLink cannot be null");
        }
        if (inventoryWrapperProvider == null) {
            throw new IllegalArgumentException("InventoryWrapperProvider cannot be null");
        }
        this.symbolLink = symbolLink;
        this.inventoryWrapperProvider = inventoryWrapperProvider;
        updateShopData();
        // ContainerShop constructor is not allowed to write any persistent data to disk
        // ContainerShop constructor may run on both ServerThread and AsyncThread
    }

    private void updateShopData() {
        ConfigurationSection section = getExtra(plugin.getJavaPlugin());
        if (section.getString("currency") != null) {
            this.currency = section.getString("currency");
            section.set("currency", null);
            Log.debug("Shop " + this + " currency data upgrade successful.");
            setDirty();
        }

    }

    /**
     * Add an item to shops chest.
     *
     * @param item   The itemstack. The amount does not matter, just everything else
     * @param amount The amount to add to the shop.
     */
    @Override
    public void add(@NotNull ItemStack item, int amount) {
        Util.ensureThread(false);
        if (this.unlimited) {
            return;
        }
        item = item.clone();
        int itemMaxStackSize = Util.getItemMaxStackSize(item.getType());
        InventoryWrapper inv = this.getInventory();
        if (inv == null) {
            throw new IllegalArgumentException("Failed to add item to shop " + this + ", the inventory is null!");
        }
        int remains = amount;
        while (remains > 0) {
            int stackSize = Math.min(remains, itemMaxStackSize);
            item.setAmount(stackSize);
            Objects.requireNonNull(inv).addItem(item);
            remains -= stackSize;
        }
        this.setSignText();
    }

    /**
     * Buys amount of item from Player p. Does NOT check our inventory, or balances
     *
     * @param buyer          The player to buy from
     * @param buyerInventory The buyer's inventory
     * @param loc2Drop       The location to drop items if inventory are full
     * @param amount         The amount to buy
     */
    @Override
    public void buy(@NotNull QUser buyer, @NotNull InventoryWrapper buyerInventory,
                    @NotNull Location loc2Drop, int amount) throws Exception {
        Util.ensureThread(false);
        amount = amount * item.getAmount();
        if (amount < 0) {
            this.sell(buyer, buyerInventory, loc2Drop, -amount);
            return;
        }
        if (this.isUnlimited()) {
            SimpleInventoryTransaction transaction = SimpleInventoryTransaction
                    .builder()
                    .from(buyerInventory)
                    .to(null) // To void
                    .item(this.getItem())
                    .amount(amount)
                    .build();
            if (!transaction.failSafeCommit()) {
                if (plugin.getSentryErrorReporter() != null) {
                    plugin.getSentryErrorReporter().ignoreThrow();
                }
                throw new IllegalStateException("Failed to commit transaction! Economy Error Response:" + transaction.getLastError());
            }
        } else {
            InventoryWrapper chestInv = this.getInventory();
            if (chestInv == null) {
                plugin.logger().warn("Failed to process buy, reason: {} x{} to shop {}: Inventory null.", item, amount, this);
                Log.debug("Failed to process buy, reason: " + item + " x" + amount + " to shop " + this + ": Inventory null.");
                return;
            }
            SimpleInventoryTransaction transaction = SimpleInventoryTransaction
                    .builder()
                    .from(buyerInventory)
                    .to(chestInv) // To void
                    .item(this.getItem())
                    .amount(amount)
                    .build();
            if (!transaction.failSafeCommit()) {
                if (plugin.getSentryErrorReporter() != null) {
                    plugin.getSentryErrorReporter().ignoreThrow();
                }
                throw new IllegalStateException("Failed to commit transaction! Economy Error Response:" + transaction.getLastError());
            }
        }
        //Update sign
        this.setSignText(plugin.text().findRelativeLanguages(buyer, false));
    }

    @Override
    public void checkDisplay() {
        Util.ensureThread(false);
        boolean displayStatus = plugin.isDisplayEnabled() && !isDisableDisplay() && this.isLoaded() && !this.isDeleted();
        if (!displayStatus) {
            if (this.displayItem != null) {
                this.displayItem.remove();
            }
            return;
        }
        if (this.displayItem == null) {
            try {
                DisplayProvider provider = ServiceInjector.getInjectedService(DisplayProvider.class, null);
                if (provider != null) {
                    this.displayItem = provider.provide(this);
                } else {
                    this.displayItem = switch (AbstractDisplayItem.getNowUsing()) {
                        case VIRTUALITEM -> {
                            if (plugin.getVirtualDisplayItemManager() != null) {
                                yield plugin.getVirtualDisplayItemManager().createVirtualDisplayItem(this);
                            } else {
                                yield new RealDisplayItem(this);
                            }
                        }
                        default -> new RealDisplayItem(this);
                    };
                }
            } catch (Throwable anyError) {
                plugin.logger().warn("Failed to init the displayItem for shop {}, the display now disabled for this shop. Did you have ProtocolLib installed?", this, anyError);
                return;
            }
        }
        if (!this.displayItem.isSpawned()) {
            /* Not spawned yet. */
            this.displayItem.spawn();
        } else {
            /* If not spawned, we didn't need check these, only check them when we need. */
            if (this.displayItem.checkDisplayNeedRegen()) {
                this.displayItem.fixDisplayNeedRegen();
            } else {
                /* If display was regened, we didn't need check it moved, performance! */
                if (this.displayItem.checkDisplayIsMoved()) {
                    this.displayItem.fixDisplayMoved();
                }
            }
        }
        /* Dupe is always need check, if enabled display */
        this.displayItem.removeDupe();
    }

    @Override
    public void claimShopSign(@NotNull Sign sign) {
        if (!sign.getPersistentDataContainer().has(Shop.SHOP_NAMESPACED_KEY, ShopSignPersistentDataType.INSTANCE)) {
            sign.getPersistentDataContainer().set(Shop.SHOP_NAMESPACED_KEY, ShopSignPersistentDataType.INSTANCE, saveToShopSignStorage());
            sign.update();
        }
    }

    /**
     * Gets the currency that shop use
     *
     * @return The currency name
     */
    @Override
    public @Nullable String getCurrency() {
        return this.currency;
    }

    /**
     * Sets the currency that shop use
     *
     * @param currency The currency name; null to use default currency
     */
    @Override
    public void setCurrency(@Nullable String currency) {
        if (Objects.equals(this.currency, currency)) {
            return;
        }
        this.currency = currency;
        setDirty();
    }

    /**
     * @return The durability of the item
     */
    @Override
    public short getDurability() {
        return (short) ((Damageable) this.item.getItemMeta()).getDamage();
    }

    /**
     * Gets the plugin's k-v map to storage the data. It is spilt by plugin name, different name
     * have different map, the data won't conflict. But if you plugin name is too common, add a
     * prefix will be a good idea.
     *
     * @param plugin Plugin instance
     * @return The data table
     */
    @Override
    public @NotNull ConfigurationSection getExtra(@NotNull Plugin plugin) {
        ConfigurationSection section = extra.getConfigurationSection(plugin.getName());
        if (section == null) {
            section = extra.createSection(plugin.getName());
        }
        return section;
    }

    /**
     * @return The chest this shop is based on.
     */
    @Override
    public @Nullable InventoryWrapper getInventory() {
        if (inventoryWrapper == null || inventoryWrapper.isNeedUpdate()) {
            Util.ensureThread(false);
            Log.debug("Loading inventory from symbol link: " + symbolLink);
            try {
                inventoryWrapper = locateInventory(symbolLink);
            } catch (Exception e) {
                Log.debug("Cannot locate the Inventory with symbol link: " + symbolLink + ", provider: " + inventoryWrapperProvider);
                return null;
            }
        }
        if (inventoryWrapper.isValid()) {
            return inventoryWrapper;
        }
        if (!createBackup) {
            createBackup = false;
            if (createBackup) {
                plugin.getShopManager().deleteShop(this);
            }
        } else {
            plugin.getShopManager().unregisterShop(this, false);
        }
        plugin.logEvent(new ShopRemoveLog(QUserImpl.createFullFilled(CommonUtil.getNilUniqueId(), "SYSTEM", false), "Inventory Invalid", this.saveToInfoStorage()));
        Log.debug("Inventory doesn't exist anymore: " + this + " shop was deleted.");
        return null;
    }

    @Override
    public @NotNull String getInventoryWrapperProvider() {
        return inventoryWrapperProvider;
    }

    /**
     * @return Returns a dummy itemstack of the item this shop is selling.
     */
    @Override
    public @NotNull ItemStack getItem() {
        return item;
    }

    @Override
    public void setItem(@NotNull ItemStack item) {
        Util.ensureThread(false);
        ShopItemChangeEvent event = new ShopItemChangeEvent(this, this.item, item);
        if (Util.fireCancellableEvent(event)) {
            Log.debug("A plugin cancelled the item change event.");
            return;
        }
        this.item = item;
        this.originalItem = item;
        if (this.displayItem != null) {
            this.displayItem.remove();
        }
        this.displayItem = null;
        checkDisplay();
        setDirty();
    }

    /**
     * @return The location of the shops chest
     */
    @Override
    public @NotNull Location getLocation() {
        return this.location;
    }

    /**
     * @return The name of the player who owns the shop.
     */
    @Override
    public @NotNull QUser getOwner() {
        return this.owner;
    }

    /**
     * Changes the owner of this shop to the given player.
     *
     * @param owner the new owner
     */
    @Override
    public void setOwner(@NotNull QUser owner) {
        Util.ensureThread(false);
        if (this.owner.equals(owner)) {
            return;
        }
        this.owner = owner;
        setDirty();
        setSignText(plugin.getTextManager().findRelativeLanguages(owner, false));
    }

    /**
     * Gets registered to this shop's permission audiences.
     *
     * @return registered audiences
     */
    @Override
    public @NotNull Map<UUID, String> getPermissionAudiences() {
        Map<UUID, String> clonedPlayerGroup = new HashMap<>(playerGroup);
        Optional<UUID> uuid = getOwner().getUniqueIdOptional();
        if (uuid.isPresent()) {
            clonedPlayerGroup.put(getOwner().getUniqueId(), BuiltInShopPermissionGroup.ADMINISTRATOR.getNamespacedNode());
        }
        return clonedPlayerGroup;
    }

    /**
     * Gets the player's group in this shop
     *
     * @param player the player
     * @return the group
     */
    @Override
    public @NotNull String getPlayerGroup(@NotNull UUID player) {
        if (player.equals(getOwner().getUniqueId())) {
            return BuiltInShopPermissionGroup.ADMINISTRATOR.getNamespacedNode();
        }
        String group = getPermissionAudiences().getOrDefault(player, BuiltInShopPermissionGroup.EVERYONE.getNamespacedNode());
        if (plugin.getShopPermissionManager().hasGroup(group)) {
            return group;
        }
        return BuiltInShopPermissionGroup.EVERYONE.getNamespacedNode();
    }

    /**
     * @return The price per item this shop is selling
     */
    @Override
    public double getPrice() {
        return this.price;
    }

    /**
     * Sets the price of the shop.
     *
     * @param price The new price of the shop.
     */
    @Override
    public void setPrice(double price) {
        Util.ensureThread(false);
        this.price = price;
        setDirty();
        setSignText();
    }

    /**
     * Returns the number of free spots in the chest for the particular item.
     *
     * @return remaining space
     */
    @Override
    public int getRemainingSpace() {
        if (Bukkit.isPrimaryThread()) {
            if (this.unlimited) {
                return -1;
            }
            if (this.getInventory() == null) {
                Log.debug("Failed to calc RemainingSpace for shop " + this + ": Inventory null.");
                return 0;
            }
            int space = Util.countSpace(this.getInventory(), this);
            new ShopInventoryCalculateEvent(this, space, -1).callEvent();
            Log.debug("Space count is: " + space);
            return space;
        } else {
            return plugin.getShopManager().queryShopInventoryCacheInDatabase(this).join().getSpace();
        }
    }

    /**
     * Returns the number of items this shop has in stock.
     *
     * @return The number of items available for purchase.
     */
    @Override
    public int getRemainingStock() {
        if (Bukkit.isPrimaryThread()) {
            if (this.unlimited) {
                return -1;
            }
            if (this.getInventory() == null) {
                Log.debug("Failed to calc RemainingStock for shop " + this + ": Inventory null.");
                return 0;
            }
            int stock = Util.countItems(this.getInventory(), this);
            new ShopInventoryCalculateEvent(this, -1, stock).callEvent();
            return stock;
        } else {
            return plugin.getShopManager().queryShopInventoryCacheInDatabase(this).join().getStock();
        }
    }

    /**
     * WARNING: This UUID will changed after plugin reload, shop reload or server restart DO NOT USE
     * IT TO STORE DATA!
     *
     * @return Random UUID
     */
    @Override
    public @NotNull UUID getRuntimeRandomUniqueId() {
        return this.runtimeRandomUniqueId;
    }

    @Override
    public long getShopId() {
        return this.shopId;
    }

    @Override
    public void setShopId(long newId) {
        if (this.shopId != -1) {
            throw new IllegalStateException("Cannot set shop id once it fully created.");
        }
        this.shopId = newId;
        setDirty();
    }

    /**
     * Gets this shop name that set by player
     *
     * @return Shop name, or null if not set
     */
    @Override
    public @Nullable String getShopName() {
        return this.shopName;
    }

    /**
     * Sets shop name
     *
     * @param shopName shop name, null to remove currently name
     */
    @Override
    public void setShopName(@Nullable String shopName) {
        if (StringUtils.equals(this.shopName, shopName)) {
            return;
        }
        this.shopName = shopName;
        setDirty();
    }

    /**
     * Getting the item stacking amount of the shop.
     *
     * @return The item stacking amount of the shop.
     */
    @Override
    public int getShopStackingAmount() {
        if (isStackingShop()) {
            return item.getAmount();
        }
        return 1;
    }

    @Override
    public @NotNull ShopType getShopType() {
        return this.shopType;
    }

    /**
     * Changes a shop type to Bu ying or Selling. Also updates the signs nearby.
     *
     * @param newShopType The new type (ShopType.BUYING or ShopType.SELLING)
     */
    @Override
    public void setShopType(@NotNull ShopType newShopType) {
        Util.ensureThread(false);
        if (this.shopType == newShopType) {
            return; //Ignore if there actually no changes
        }
        if (Util.fireCancellableEvent(new ShopTypeChangeEvent(this, this.shopType, newShopType))) {
            Log.debug(
                    "Some addon cancelled shop type changes, target shop: " + this);
            return;
        }
        this.shopType = newShopType;
        this.setSignText();
        setDirty();
    }

    @Override
    public List<Component> getSignText(@NotNull ProxiedLocale locale) {
        Util.ensureThread(false);
        List<Component> lines = new ArrayList<>();
        //Line 1
        String headerKey = inventoryAvailable() ? "signs.header-available" : "signs.header-unavailable";
        lines.add(plugin.text().of(headerKey, this.ownerName(false, locale)).forLocale(locale.getLocale()));
        //Line 2
        String tradingStringKey;
        String noRemainingStringKey;
        int shopRemaining;

        switch (shopType) {
            case BUYING -> {
                shopRemaining = getRemainingSpace();
                tradingStringKey = isStackingShop() ? "signs.stack-buying" : "signs.buying";
                noRemainingStringKey = "signs.out-of-space";
            }
            case SELLING -> {
                shopRemaining = getRemainingStock();
                tradingStringKey = isStackingShop() ? "signs.stack-selling" : "signs.selling";
                noRemainingStringKey = "signs.out-of-stock";
            }
            default -> {
                shopRemaining = 0;
                tradingStringKey = "MissingKey for shop type:" + shopType;
                noRemainingStringKey = "MissingKey for shop type:" + shopType;
            }
        }
        Component line2 = switch (shopRemaining) {
            //Unlimited
            case -1 ->
                    plugin.text().of(tradingStringKey, plugin.text().of("signs.unlimited").forLocale(locale.getLocale())).forLocale(locale.getLocale());
            //No remaining
            case 0 -> plugin.text().of(noRemainingStringKey).forLocale(locale.getLocale());
            //Has remaining
            default -> plugin.text().of(tradingStringKey, Component.text(shopRemaining)).forLocale(locale.getLocale());
        };
        lines.add(line2);

        //line 3
        if (plugin.getConfig().getBoolean("shop.force-use-item-original-name") || !this.getItem().hasItemMeta() || !this.getItem().getItemMeta().hasDisplayName()) {
            Component left = plugin.text().of("signs.item-left").forLocale(locale.getLocale());
            Component right = plugin.text().of("signs.item-right").forLocale(locale.getLocale());
            Component itemName = Util.getItemStackName(getItem());
            lines.add(left.append(itemName).append(right));
        } else {
            lines.add(plugin.text().of("signs.item-left").forLocale(locale.getLocale()).append(Util.getItemStackName(getItem()).append(plugin.text().of("signs.item-right").forLocale(locale.getLocale()))));
        }

        //line 4
        Component line4;
        if (this.isStackingShop()) {
            line4 = plugin.text().of("signs.stack-price",
                    plugin.getShopManager().format(this.getPrice(), this),
                    item.getAmount(),
                    Util.getItemStackName(item)).forLocale(locale.getLocale());
        } else {
            line4 = plugin.text().of("signs.price", plugin.getShopManager().format(this.getPrice(), this)).forLocale(locale.getLocale());
        }
        lines.add(line4);

        return lines;
    }

    /**
     * Returns a list of signs that are attached to this shop (QuickShop and blank signs only)
     *
     * @return a list of signs that are attached to this shop (QuickShop and blank signs only)
     */
    @Override
    public @NotNull List<Sign> getSigns() {
        Util.ensureThread(false);
        List<Sign> signs = new ArrayList<>(4);
        if (this.getLocation().getWorld() == null) {
            return Collections.emptyList();
        }
        Block[] blocks = new Block[4];
        blocks[0] = location.getBlock().getRelative(BlockFace.EAST);
        blocks[1] = location.getBlock().getRelative(BlockFace.NORTH);
        blocks[2] = location.getBlock().getRelative(BlockFace.SOUTH);
        blocks[3] = location.getBlock().getRelative(BlockFace.WEST);
        for (Block b : blocks) {
            if (b == null) {
                continue;
            }
            BlockState state = PaperLib.getBlockState(b, false).getState();
            if (!(state instanceof Sign sign)) {
                continue;
            }
            if (isShopSign(sign)) {
                claimShopSign(sign);
                signs.add(sign);
            }
        }

        return signs;
    }


    @Override
    @Nullable
    public QUser getTaxAccount() {
        QUser uuid = null;
        if (taxAccount != null) {
            uuid = taxAccount;
        } else {
            if (((SimpleShopManager) plugin.getShopManager()).getCacheTaxAccount() != null) {
                uuid = ((SimpleShopManager) plugin.getShopManager()).getCacheTaxAccount();
            }
        }
        ShopTaxAccountGettingEvent event = new ShopTaxAccountGettingEvent(this, uuid);
        event.callEvent();
        return event.getTaxAccount();

    }

    @Override
    public void setTaxAccount(@Nullable QUser taxAccount) {
        if (Objects.equals(taxAccount, this.taxAccount)) {
            return;
        }
        ShopTaxAccountChangeEvent event = new ShopTaxAccountChangeEvent(this, taxAccount);
        if (Util.fireCancellableEvent(event)) {
            return;
        }
        this.taxAccount = taxAccount;
        setDirty();
    }

    @Override
    @Nullable
    public QUser getTaxAccountActual() {
        return taxAccount;
    }

    @Override
    public boolean inventoryAvailable() {
        if (isUnlimited()) {
            return true;
        }
        if (isSelling()) {
            return getRemainingStock() > 0;
        }
        if (isBuying()) {
            return getRemainingSpace() > 0;
        }
        return true;
    }

    @Override
    public boolean isAttached(@NotNull Block b) {
        Util.ensureThread(false);
        return this.getLocation().getBlock().equals(Util.getAttached(b));
    }

    @Override
    public boolean isBuying() {
        return this.shopType == ShopType.BUYING;
    }

    private boolean isDeleted() {
        return this.isDeleted;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean isDirty) {
        this.dirty = isDirty;
    }

    @Override
    public boolean isDisableDisplay() {
        return disableDisplay;
    }

    @Override
    public void setDisableDisplay(boolean disabled) {
        if (this.disableDisplay == disabled) {
            return;
        }
        this.disableDisplay = disabled;
        setDirty();
        checkDisplay();
    }

    /**
     * Check if this shop is free shop
     *
     * @return Free Shop
     */
    @Override
    public boolean isFreeShop() {
        return this.price == 0.0d;
    }

    @Override
    public boolean isLoaded() {
        return this.isLoaded;
    }


    @Override
    public boolean isSelling() {
        return this.shopType == ShopType.SELLING;
    }

    /**
     * Checks if a Sign is a ShopSign
     *
     * @param sign Target {@link Sign}
     * @return Is shop info sign
     */
    @Override
    public boolean isShopSign(@NotNull Sign sign) {
        // Check for new shop sign
        Component[] lines = new Component[sign.getLines().length];
        for (int i = 0; i < sign.getLines().length; i++) {
            lines[i] = plugin.getPlatform().getLine(sign, i);
        }
        // Can be claim

        boolean empty = true;
        for (Component line : lines) {
            if (!Util.isEmptyComponent(line)) {
                empty = false;
                break;
            }
        }

        if (empty) {
            return true;
        }

        // Check for exists shop sign (modern)
        ShopSignStorage shopSignStorage = sign.getPersistentDataContainer().get(SHOP_NAMESPACED_KEY, ShopSignPersistentDataType.INSTANCE);
        if (shopSignStorage == null) {
            // Try to read Reremake sign namespaced key
            shopSignStorage = sign.getPersistentDataContainer().get(LEGACY_SHOP_NAMESPACED_KEY, ShopSignPersistentDataType.INSTANCE);
        }
        if (shopSignStorage == null) {
            // Try more hard to read Reremake sign namespaced key
            if (sign.getLine(1).startsWith(LEGACY_SHOP_SIGN_RECOGNIZE_PATTERN)) {
                return true;
            }
        }
        if (shopSignStorage != null) {
            return shopSignStorage.equals(getLocation().getWorld().getName(), getLocation().getBlockX(), getLocation().getBlockY(), getLocation().getBlockZ());
        }
        return false;
    }

    /**
     * Gets shop status is stacking shop
     *
     * @return The shop stacking status
     */
    @Override
    public boolean isStackingShop() {
        return plugin.isAllowStack() && this.item.getAmount() > 1;
    }

    @Override
    public boolean isUnlimited() {
        return this.unlimited;
    }

    @Override
    public void setUnlimited(boolean unlimited) {
        if (this.unlimited == unlimited) {
            return;
        }
        Util.ensureThread(false);
        this.unlimited = unlimited;
        setDirty();
        this.setSignText();
    }

    /**
     * Check shop is or not still Valid.
     *
     * @return isValid
     */
    @Override
    public boolean isValid() {
        Util.ensureThread(false);
        if (this.isDeleted) {
            return false;
        }
        return Util.canBeShop(this.getLocation().getBlock());
    }

    /**
     * Returns true if the ItemStack matches what this shop is selling/buying
     *
     * @param item The ItemStack
     * @return True if the ItemStack is the same (Excludes amounts)
     */
    @Override
    public boolean matches(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemStack givenItem = item.clone();
        givenItem.setAmount(1);
        ItemStack shopItem = this.item.clone();
        shopItem.setAmount(1);
        return plugin.getItemMatcher().matches(shopItem, givenItem);
    }

    @Override
    public void onClick(@NotNull Player clicker) {
        Util.ensureThread(false);
        ShopClickEvent event = new ShopClickEvent(this, clicker);
        if (Util.fireCancellableEvent(event)) {
            Log.debug("Ignore shop click, because some plugin cancel it.");
            return;
        }
        setSignText(plugin.getTextManager().findRelativeLanguages(clicker));
    }

    /**
     * Load ContainerShop.
     */
    @Override
    public void handleLoading() {
        Util.ensureThread(false);
        if (this.isLoaded) {
            Log.debug("Dupe load request, canceled.");
            return;
        }
        try (PerfMonitor ignored = new PerfMonitor("Shop Inventory Locate", Duration.of(1, ChronoUnit.SECONDS))) {
            inventoryWrapper = locateInventory(symbolLink);
        } catch (Exception e) {
            plugin.logger().warn("Failed to load shop: {}: {}: {}", symbolLink, e.getClass().getName(), e.getMessage());
            if (plugin.getConfig().getBoolean("debug.delete-corrupt-shops")) {
                plugin.logger().warn("Deleting corrupt shop...");
                plugin.getShopManager().deleteShop(this);
            } else {
                plugin.logger().warn("Unloading shops from memory, set `debug.delete-corrupt-shops` to true to delete corrupted shops.");
                plugin.getShopManager().deleteShop(this);
            }
            return;
        }
        if (Util.fireCancellableEvent(new ShopLoadEvent(this))) {
            return;
        }
        this.isLoaded = true;
        //disable schedule check due to performance issue
        //plugin.getShopContainerWatcher().scheduleCheck(this);
        try (PerfMonitor ignored = new PerfMonitor("Shop Display Check", Duration.of(1, ChronoUnit.SECONDS))) {
            checkDisplay();
        }
        if (PackageUtil.parsePackageProperly("updateShopSignOnLoad").asBoolean(false)) {
            Log.debug("Scheduled sign update for shop " + this + " because updateShopSignOnLoad has been enabled.");
            plugin.getSignUpdateWatcher().scheduleSignUpdate(this);
        }
    }

    /**
     * Unload ContainerShop.
     */
    @Override
    public void handleUnloading() {
        Util.ensureThread(false);
        if (!this.isLoaded) {
            Log.debug("Dupe unload request, canceled.");
            return;
        }
        if (inventoryPreview != null) {
            inventoryPreview.close();
        }
        if (this.displayItem != null) {
            this.displayItem.remove();
        }
        this.isLoaded = false;
        plugin.getShopManager().getLoadedShops().remove(this);
        new ShopUnloadEvent(this).callEvent();
    }

    @Override
    public void openPreview(@NotNull Player player) {
        if (inventoryPreview == null) {
            inventoryPreview = new InventoryPreview(plugin, getItem().clone(), player.getLocale());
        }
        inventoryPreview.show(player);

    }

    @Override
    public @NotNull Component ownerName(boolean forceUsername, @NotNull ProxiedLocale locale) {
        Component name;
        if (!forceUsername && isUnlimited()) {
            name = plugin.text().of("admin-shop").forLocale(locale.getLocale());
        } else {
            String playerName = this.getOwner().getUsername();
//            if (plugin.getConfig().getBoolean("shop.async-owner-name-fetch", false)) {
//                CompletableFuture<String> future = CompletableFuture
//                        .supplyAsync(() -> plugin.getPlayerFinder().uuid2Name(owner), QuickExecutor.getCommonExecutor());
//                try {
//                    playerName = future.get(20, TimeUnit.MILLISECONDS);
//                } catch (InterruptedException | ExecutionException | TimeoutException e) {
//                    playerName = "N/A";
//                }
//            } else {
//                playerName = plugin.getPlayerFinder().uuid2Name(this.getOwner());
//            }
            if (playerName == null) {
                name = plugin.text().of("unknown-owner").forLocale(locale.getLocale());
            } else {
                name = Component.text(playerName);
            }
        }
        if (getOwner().isRealPlayer()) {
            name = name.hoverEvent(
                    plugin.text().of("real-player-component-hover", getOwner().getUniqueId(), getOwner().getUsername(), getOwner().getDisplay()).forLocale(locale.getLocale())
            );
        } else {
            name = name.hoverEvent(
                    plugin.text().of("virtual-player-component-hover", getOwner().getUniqueId(), getOwner().getUsername(), getOwner().getDisplay()).forLocale(locale.getLocale())
            );

        }
        ShopOwnerNameGettingEvent event = new ShopOwnerNameGettingEvent(this, getOwner(), name);
        event.callEvent();
        name = event.getName();
        return name;
    }

    @Override
    public @NotNull Component ownerName(@NotNull ProxiedLocale locale) {
        return ownerName(false, locale);
    }

    @Override
    public @NotNull Component ownerName() {
        return ownerName(false, MsgUtil.getDefaultGameLanguageLocale());
    }

    /**
     * Check if player has permission to authorize the specified permission node.
     *
     * @param player     the player
     * @param namespace  the plugin instance for the permission node (namespace)
     * @param permission the permission node
     * @return true if player has permission, false otherwise
     */
    @Override
    public boolean playerAuthorize(@NotNull UUID player, @NotNull Plugin namespace, @NotNull String permission) {
        if (player.equals(getOwner().getUniqueId())) {
            Log.permission("Check permission " + namespace.getName().toLowerCase(Locale.ROOT) + "." + permission + " for " + player + " -> " + "true");
            return true;
        }
        String group = getPlayerGroup(player);
        boolean r = plugin.getShopPermissionManager().hasPermission(group, namespace, permission);
        ShopAuthorizeCalculateEvent event = new ShopAuthorizeCalculateEvent(this, player, namespace, permission, r);
        event.callEvent();
        Log.permission("Check permission " + namespace.getName().toLowerCase(Locale.ROOT) + "." + permission + ": " + player + " -> " + event.getResult());
        return event.getResult();

    }

    /**
     * Check if player has permission to authorize the specified permission node.
     *
     * @param player     the player
     * @param permission the permission node
     * @return true if player has permission, false otherwise
     */
    @Override
    public boolean playerAuthorize(@NotNull UUID player, @NotNull BuiltInShopPermission permission) {
        return playerAuthorize(player, plugin.getJavaPlugin(), permission.getRawNode());
    }

    @Override
    public List<UUID> playersCanAuthorize(@NotNull BuiltInShopPermission permission) {
        return playersCanAuthorize(plugin.getJavaPlugin(), permission.getRawNode());
    }

    @Override
    public List<UUID> playersCanAuthorize(@NotNull BuiltInShopPermissionGroup permissionGroup) {
        return getPermissionAudiences().entrySet().stream().filter(entry -> entry.getValue().equals(permissionGroup.getNamespacedNode())).map(Map.Entry::getKey).toList();
    }

    @Override
    public List<UUID> playersCanAuthorize(@NotNull Plugin namespace, @NotNull String permission) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, String> uuidStringEntry : this.getPermissionAudiences().entrySet()) {
            String group = uuidStringEntry.getValue();
            boolean r = plugin.getShopPermissionManager().hasPermission(group, namespace, permission);
            ShopAuthorizeCalculateEvent event = new ShopAuthorizeCalculateEvent(this, uuidStringEntry.getKey(), namespace, permission, r);
            event.callEvent();
            r = event.getResult();
            if (r) {
                result.add(uuidStringEntry.getKey());
            }
        }
        Log.permission("Check permission " + namespace.getName().toLowerCase(Locale.ROOT) + "." + permission + ": " + CommonUtil.list2String(result.stream().map(UUID::toString).toList()));
        return result;
    }

    /**
     * Removes an item from the shop.
     *
     * @param item   The itemstack. The amount does not matter, just everything else
     * @param amount The amount to remove from the shop.
     */
    @Override
    public void remove(@NotNull ItemStack item, int amount) {
        Util.ensureThread(false);
        if (this.unlimited) {
            return;
        }
        item = item.clone();
        int itemMaxStackSize = Util.getItemMaxStackSize(item.getType());
        InventoryWrapper inv = this.getInventory();
        if (inv == null) {
            plugin.logger().warn("Failed to process item remove, reason: {} x{} to shop {}: Inventory null.", item, amount, this);
            return;
        }
        int remains = amount;
        while (remains > 0) {
            int stackSize = Math.min(remains, itemMaxStackSize);
            item.setAmount(stackSize);
            Objects.requireNonNull(inv).removeItem(item);
            remains -= stackSize;
        }
        this.setSignText();
    }

    @Override
    public @NotNull String saveExtraToYaml() {
        return extra.saveToString();
    }

    @Override
    public ShopInfoStorage saveToInfoStorage() {
        return new ShopInfoStorage(getLocation().getWorld().getName(),
                new BlockPos(getLocation()), this.owner, this.price,
                Util.serialize(this.originalItem), isUnlimited() ? 1 : 0
                , getShopType().toID(),
                saveExtraToYaml(), this.currency, this.disableDisplay,
                this.taxAccount, inventoryWrapperProvider,
                saveToSymbolLink(), this.playerGroup);
    }

    @Override
    @NotNull
    public String saveToSymbolLink() {
        return symbolLink;
    }

    /**
     * Sells amount of item to Player p. Does NOT check our inventory, or balances
     *
     * @param seller          The seller
     * @param sellerInventory The seller's inventory
     * @param loc2Drop        Location to drop items if inventory are full
     * @param amount          The amount to sell
     */
    @Override
    public void sell(@NotNull QUser seller, @NotNull InventoryWrapper sellerInventory,
                     @NotNull Location loc2Drop, int amount) throws Exception {
        Util.ensureThread(false);
        amount = item.getAmount() * amount;
        if (amount < 0) {
            this.buy(seller, sellerInventory, loc2Drop, -amount);
            return;
        }
        // Items to drop on floor
        if (this.isUnlimited()) {
            SimpleInventoryTransaction transaction = SimpleInventoryTransaction
                    .builder()
                    .from(null)
                    .to(sellerInventory) // To void
                    .item(this.getItem())
                    .amount(amount)
                    .build();
            if (!transaction.failSafeCommit()) {
                if (plugin.getSentryErrorReporter() != null) {
                    plugin.getSentryErrorReporter().ignoreThrow();
                }
                throw new IllegalStateException("Failed to commit transaction! Economy Error Response:" + transaction.getLastError());
            }
        } else {
            InventoryWrapper chestInv = this.getInventory();
            if (chestInv == null) {
                plugin.logger().warn("Failed to process sell, reason: {} to shop {}: Inventory null.", item, amount);
                return;
            }
            SimpleInventoryTransaction transactionTake = SimpleInventoryTransaction
                    .builder()
                    .from(chestInv)
                    .to(sellerInventory) // To void
                    .item(this.getItem())
                    .amount(amount)
                    .build();
            if (!transactionTake.failSafeCommit()) {
                if (plugin.getSentryErrorReporter() != null) {
                    plugin.getSentryErrorReporter().ignoreThrow();
                }
                throw new IllegalStateException("Failed to commit transaction! Economy Error Response:" + transactionTake.getLastError());
            }
            this.setSignText(plugin.getTextManager().findRelativeLanguages(seller, false));
        }
    }

    @Override
    public void setDirty() {
        this.dirty = true;
    }

    /**
     * Save the extra data to the shop.
     *
     * @param plugin Plugin instace
     * @param data   The data table
     */
    @Override
    public void setExtra(@NotNull Plugin plugin, @NotNull ConfigurationSection data) {
        extra.set(plugin.getName(), data);
        setDirty();
    }

    @Override
    public void setInventory(@NotNull InventoryWrapper wrapper, @NotNull InventoryWrapperManager manager) {
        String provider = plugin.getInventoryWrapperRegistry().find(manager);
        if (provider == null) {
            throw new IllegalArgumentException("The manager " + manager.getClass().getName() + " not registered in registry.");
        }
        this.inventoryWrapper = wrapper;
        this.inventoryWrapperProvider = provider;
        this.symbolLink = manager.mklink(wrapper);
        setDirty();
        Log.debug("Inventory changed: " + this.symbolLink + ", wrapper provider:" + inventoryWrapperProvider);
        new ShopInventoryChangedEvent(wrapper, manager).callEvent();
    }

    @Override
    public void setPlayerGroup(@NotNull UUID player, @Nullable String group) {
        if (group == null) {
            group = BuiltInShopPermissionGroup.EVERYONE.getNamespacedNode();
        }
        new ShopPlayerGroupSetEvent(this, player, getPlayerGroup(player), group).callEvent();
        if (group.equals(BuiltInShopPermissionGroup.EVERYONE.getNamespacedNode())) {
            this.playerGroup.remove(player);
        } else {
            this.playerGroup.put(player, group);
        }
        setDirty();
    }

    @Override
    public void setPlayerGroup(@NotNull UUID player, @Nullable BuiltInShopPermissionGroup group) {
        if (group == null) {
            group = BuiltInShopPermissionGroup.EVERYONE;
        }
        new ShopPlayerGroupSetEvent(this, player, getPlayerGroup(player), group.getNamespacedNode()).callEvent();
        if (group == BuiltInShopPermissionGroup.EVERYONE) {
            this.getPermissionAudiences().remove(player);
        } else {
            setPlayerGroup(player, group.getNamespacedNode());
        }
        setDirty();
    }

    /**
     * Updates signs attached to the shop
     */
    @Override
    public void setSignText() {
        Util.ensureThread(false);
        if (!Util.isLoaded(this.location)) {
            return;
        }
        this.setSignText(getSignText(plugin.getTextManager().findRelativeLanguages(MsgUtil.getDefaultGameLanguageCode())));
    }

    /**
     * Changes all lines of text on a sign near the shop
     *
     * @param lines The array of lines to change. Index is line number.
     */
    @Override
    public void setSignText(@NotNull List<Component> lines) {
        Util.ensureThread(false);
        Log.debug("Globally sign text setting...");
        List<Sign> signs = this.getSigns();
        for (Sign sign : signs) {
            if (plugin.getGameVersion().isSignTextDyeSupport()) {
                DyeColor dyeColor = Util.getDyeColor();
                if (dyeColor != null) {
                    sign.setColor(dyeColor);
                }
            }
            if (plugin.getGameVersion().isSignGlowingSupport()) {
                boolean isGlowing = plugin.getConfig().getBoolean("shop.sign-glowing");
                sign.setGlowingText(isGlowing);
            }
            sign.update(true);
            //plugin.getPlatform().setLine(sign, i, lines.get(i));
            plugin.getPlatform().setLines(sign, lines);
            new ShopSignUpdateEvent(this, sign).callEvent();
        }
        if (plugin.getSignHooker() != null) {
            Log.debug("Start sign broadcast...");
            Bukkit.getScheduler().runTaskLater(plugin.getJavaPlugin(), () -> plugin.getSignHooker().updatePerPlayerShopSignBroadcast(getLocation(), this), 2);
            Log.debug("Sign broadcast completed.");
        }
    }

    /**
     * Updates signs attached to the shop
     *
     * @param locale The locale to use for the sign text
     */
    @Override
    public void setSignText(@NotNull ProxiedLocale locale) {
        Util.ensureThread(false);
        if (!Util.isLoaded(this.location)) {
            return;
        }
        this.setSignText(getSignText(locale));
    }

    /**
     * Updates the shop into the database.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> update() {
        // Warning! This method can be run in async thread.
        if (updating) {
            return CompletableFuture.completedFuture(null);
        }
        if (this.shopId == -1) {
            Log.debug("Skip shop database update because it not fully setup!");
            return CompletableFuture.completedFuture(null);
        }
        ShopUpdateEvent shopUpdateEvent = new ShopUpdateEvent(this);
        if (Util.fireCancellableEvent(shopUpdateEvent)) {
            Log.debug("The Shop update action was canceled by a plugin.");
            return CompletableFuture.completedFuture(null);
        }
        updating = true;
        return plugin.getDatabaseHelper().updateShop(this)
                .whenComplete((result, throwable) -> {
                    updating = false;
                    if (throwable == null) {
                        this.dirty = false;
                    } else {
                        plugin.logger().warn(
                                "Could not update a shop in the database! Changes will revert after a reboot!", throwable);
                    }
                });
    }

    @Override
    public @NotNull Benefit getShopBenefit() {
        return this.benefit;
    }

    @Override
    public void setShopBenefit(@NotNull Benefit benefit) {
        this.benefit = benefit;
        setDirty();
    }

    public @NotNull SimpleDataRecord createDataRecord() {
        return new SimpleDataRecord(
                getOwner(),
                Util.serialize(getItem()),
                getShopName(),
                getShopType().toID(),
                getCurrency(),
                getPrice(),
                isUnlimited(),
                isDisableDisplay(),
                getTaxAccount(),
                JsonUtil.getGson().toJson(getPermissionAudiences()),
                saveExtraToYaml(),
                getInventoryWrapperProvider(),
                saveToSymbolLink(),
                new Date(),
                getShopBenefit().serialize()
        );
    }

    /**
     * Returns the display item associated with this shop.
     *
     * @return The display item associated with this shop.
     */
    @Nullable
    public AbstractDisplayItem getDisplayItem() {
        return this.displayItem;
    }

    /**
     * @return The enchantments the shop has on its items.
     */
    public @NotNull Map<Enchantment, Integer> getEnchants() {
        if (this.item.hasItemMeta() && this.item.getItemMeta().hasEnchants()) {
            return Objects.requireNonNull(this.item.getItemMeta()).getEnchants();
        }
        return Collections.emptyMap();
    }

    /**
     * @return The ItemStack type of this shop
     */
    public @NotNull Material getMaterial() {
        return this.item.getType();
    }

    private @NotNull InventoryWrapper locateInventory(@Nullable String symbolLink) {
        if (symbolLink == null || symbolLink.isEmpty()) {
            throw new IllegalStateException("Symbol link is empty, that's not right bro.");
        }
        InventoryWrapperManager manager = plugin.getInventoryWrapperRegistry().get(getInventoryWrapperProvider());
        if (manager == null) {
            throw new IllegalStateException("Failed load shop data, the InventoryWrapper provider " + getInventoryWrapperProvider() + " invalid or failed to load!");
        }
        try {
            // this.symbolLink = manager.mklink(inventoryWrapper);
            return manager.locate(symbolLink);
        } catch (Exception e) {
            throw new IllegalStateException("Failed load shop data, the InventoryWrapper provider " + getInventoryWrapperProvider() + " returns error: " + e.getMessage(), e);
        }
    }

    @Override
    public ReloadResult reloadModule() throws Exception {
        if (!plugin.isAllowStack()) {
            this.item.setAmount(1);
        } else {
            this.item.setAmount(this.originalItem.getAmount());
        }
        return Reloadable.super.reloadModule();
    }

    private ShopSignStorage saveToShopSignStorage() {
        return new ShopSignStorage(getLocation().getWorld().getName(), getLocation().getBlockX(), getLocation().getBlockY(), getLocation().getBlockZ());
    }

    @Override
    public String toString() {

        return "Shop " +
                (location.getWorld() == null ? "unloaded world" : location.getWorld().getName()) +
                "(" +
                location.getBlockX() +
                ", " +
                location.getBlockY() +
                ", " +
                location.getBlockZ() +
                ")" +
                " Owner: " + LegacyComponentSerializer.legacySection().serialize(this.ownerName(false, MsgUtil.getDefaultGameLanguageLocale())) + " - " + getOwner() +
                ", Unlimited: " + isUnlimited() +
                ", Item: " + LegacyComponentSerializer.legacySection().serialize(Util.getItemStackName(getItem())) +
                ", Price: " + getPrice();
    }
}
