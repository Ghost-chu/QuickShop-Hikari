package com.ghostchu.quickshop.shop.history;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.ServiceInjector;
import com.ghostchu.quickshop.api.database.bean.DataRecord;
import com.ghostchu.quickshop.api.event.ShopHistoryGuiOpenEvent;
import com.ghostchu.quickshop.api.localization.text.ProxiedLocale;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.util.PackageUtil;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.quickshop.util.skin.BukkitSkullProvider;
import com.ghostchu.quickshop.util.skin.SkullProvider;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ShopHistoryGUI {
    private final ShopHistory shopHistory;
    private final ChestGui chestGui;
    private final QuickShop plugin;
    private final StaticPane header;
    private final OutlinePane body;
    private final StaticPane footer;
    private final Player player;
    private final Shop shop;
    private final SkullProvider skullProvider;
    private final ProxiedLocale locale;
    private int page = 1;
    private List<ShopHistory.ShopHistoryRecord> queryResult;
    private ShopHistory.ShopSummary summary;


    public ShopHistoryGUI(QuickShop plugin, Player player, ShopHistory shopHistory) {
        this.plugin = plugin;
        this.shopHistory = shopHistory;
        this.player = player;
        this.locale = plugin.getTextManager().findRelativeLanguages(player);
        this.shop = shopHistory.shop;
        this.chestGui = new ChestGui(6, plugin.text().of(player, "history.shop.gui-title").legacy());
        this.header = new StaticPane(0, 0, 9, 1);
        this.body = new OutlinePane(0, 1, 9, 4);
        this.footer = new StaticPane(0, 5, 9, 1);
        chestGui.addPane(header);
        chestGui.addPane(body);
        chestGui.addPane(footer);
        this.skullProvider = ServiceInjector.getInjectedService(SkullProvider.class, new BukkitSkullProvider());
    }

    private void reQuery() {
        Util.ensureThread(true);
        try {
            this.queryResult = shopHistory.query(page, body.getLength() * body.getLength());
            if (summary == null) {
                summary = shopHistory.generateSummary().join();
                Log.debug(summary.toString());
            }
        } catch (SQLException e) {
            plugin.logger().error("Couldn't query the shop history for shop {}.", shopHistory.shop, e);
            Util.mainThreadRun(() -> this.chestGui.getViewers().forEach(p -> {
                p.closeInventory();
                plugin.text().of(p, "internal-error", p).send();
            }));
        }
    }

    private ItemStack getHeaderIcon() {
        ItemStack headerSkullItem = new ItemStack(Material.PLAYER_HEAD);
        String shopName = shop.getShopName() == null ? shop.getLocation().getWorld().getName() + " " + shop.getLocation().getBlockX() + ", " + shop.getLocation().getBlockY() + ", " + shop.getLocation().getBlockZ() : shop.getShopName();
        Component shopNameComponent = LegacyComponentSerializer.legacySection().deserialize(shopName);
        List<Component> lore = plugin.text().ofList(player, "history.shop.header-icon-description", shop.getShopType().name(),
                        shop.getOwner().getDisplay(),
                        Util.getItemStackName(shop.getItem()),
                        shop.getPrice(), shop.getShopStackingAmount(),
                        shop.getLocation().getWorld().getName() + " " + shop.getLocation().getBlockX() + ", " + shop.getLocation().getBlockY() + ", " + shop.getLocation().getBlockZ())
                .forLocale();
        plugin.getPlatform().setDisplayName(headerSkullItem, shopNameComponent);
        plugin.getPlatform().setLore(headerSkullItem, lore);
        if (PackageUtil.parsePackageProperly("renderSkullTexture").asBoolean(true)) {
            QUser owner = shopHistory.shop.getOwner();
            if (owner.isRealPlayer()) {
                if (owner.getUniqueId() != null) {
                    skullProvider.provide(owner.getUniqueId()).thenAccept(skull -> {
                        headerSkullItem.setItemMeta(skull.getItemMeta());
                        plugin.getPlatform().setDisplayName(headerSkullItem, shopNameComponent);
                        plugin.getPlatform().setLore(headerSkullItem, lore);
                    });
                } else if (owner.getUsername() != null) {
                    skullProvider.provide(owner.getUsername()).thenAccept(skull -> {
                        headerSkullItem.setItemMeta(skull.getItemMeta());
                        plugin.getPlatform().setDisplayName(headerSkullItem, shopNameComponent);
                        plugin.getPlatform().setLore(headerSkullItem, lore);
                    });
                }
            }
        }
        return headerSkullItem;
    }


    public void open() {
        refreshGui();
        chestGui.show(player);
        new ShopHistoryGuiOpenEvent(shop, player, chestGui.getInventory()).callEvent();
    }


    private void refreshGui() {
        Util.ensureThread(false);
        updateHeaderIcon();
        enterQueryingMode();
        updateFooterPageIcons();
        chestGui.update();
        CompletableFuture.supplyAsync(() -> {
            reQuery();
            updateHeaderIcon();
            updateFooterPageIcons();
            updateBodyWithResult();
            return null;
        }).thenAccept((ignored) -> Util.mainThreadRun(chestGui::update));
    }

    private void updateHeaderIcon() {
        header.clear();
        header.addItem(new GuiItem(getSummaryIcon(), cancelEvent()), 0, 0);
        header.addItem(new GuiItem(getHeaderIcon(), cancelEvent()), 4, 0);
        header.addItem(new GuiItem(getValuableCustomerIcon(), cancelEvent()), 8, 0);
    }

    private ItemStack getValuableCustomerIcon() {
        if (summary == null) {
            return queryingPlaceHolderItem();
        }
        ItemStack itemStack = new ItemStack(Material.DIAMOND);
        plugin.getPlatform().setDisplayName(itemStack, plugin.text().of(player, "history.shop.top-n-valuable-customers-title", summary.valuableCustomers().size()).forLocale());
        List<Component> description = new ArrayList<>(summary.valuableCustomers().size());
        summary.valuableCustomers().forEach((uuid, count) -> description.add(plugin.text().of(player, "history.shop.top-n-valuable-customers-entry", QUserImpl.createSync(plugin.getPlayerFinder(), uuid).getDisplay(), count).forLocale()));
        plugin.getPlatform().setLore(itemStack, description);
        return itemStack;
    }

    private ItemStack getSummaryIcon() {
        if (summary == null) {
            return queryingPlaceHolderItem();
        }
        ItemStack itemStack = new ItemStack(Material.OAK_SIGN);
        plugin.getPlatform().setDisplayName(itemStack, plugin.text().of(player, "history.shop.summary-icon-title").forLocale());
        List<Component> description = new ArrayList<>();
        description.add(plugin.text().of(player, "history.shop.total-unique-purchasers", locale.getNumberFormat().format(summary.uniquePurchasers())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchases", hours(24), locale.getNumberFormat().format(summary.recentPurchases24h())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchases", days(3), locale.getNumberFormat().format(summary.recentPurchases3d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchases", days(7), locale.getNumberFormat().format(summary.recentPurchases7d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchases", days(30), locale.getNumberFormat().format(summary.recentPurchases30d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.total-purchases", locale.getNumberFormat().format(summary.totalPurchases())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchase-balance", hours(24), locale.getNumberFormat().format(summary.recentPurchasesBalance24h())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchase-balance", days(3), locale.getNumberFormat().format(summary.recentPurchasesBalance3d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchase-balance", days(7), locale.getNumberFormat().format(summary.recentPurchasesBalance7d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.recent-purchase-balance", days(30), locale.getNumberFormat().format(summary.recentPurchasesBalance30d())).forLocale());
        description.add(plugin.text().of(player, "history.shop.total-balances", locale.getNumberFormat().format(summary.totalBalance())).forLocale());

        plugin.getPlatform().setLore(itemStack, description);
        return itemStack;
    }

    private Component hours(int hours) {
        return plugin.text().of(player, "timeunit.hours", hours).forLocale();
    }

    private Component days(int days) {
        return plugin.text().of(player, "timeunit.days", days).forLocale();
    }

    private void enterQueryingMode() {
        body.clear();
        for (int i = 0; i < body.getHeight() * body.getLength(); i++) {
            body.addItem(new GuiItem(queryingPlaceHolderItem(), cancelEvent()));
        }
    }

    private ItemStack queryingPlaceHolderItem() {
        ItemStack querying = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        plugin.getPlatform().setDisplayName(querying, plugin.text().of(player, "history.shop.query-icon").forLocale());
        return querying;
    }

    private ItemStack noResultPlaceHolderItem() {
        ItemStack querying = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        plugin.getPlatform().setDisplayName(querying, plugin.text().of(player, "history.shop.no-result").forLocale());
        return querying;
    }


    private void updateFooterPageIcons() {
        footer.clear();
        ItemStack previousPage = new ItemStack(Material.PAPER);
        ItemStack currentPage = new ItemStack(Material.BOOK);
        ItemStack nextPage = new ItemStack(Material.PAPER);
        plugin.getPlatform().setDisplayName(previousPage, plugin.text().of("history.shop.previous-page").forLocale());
        plugin.getPlatform().setDisplayName(currentPage, plugin.text().of("history.shop.current-page", page).forLocale());
        plugin.getPlatform().setDisplayName(nextPage, plugin.text().of("history.shop.next-page").forLocale());
        currentPage.setAmount(Math.min(1, Math.max(page, currentPage.getMaxStackSize())));
        footer.addItem(new GuiItem(previousPage, e -> {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            page = Math.max(1, page - 1);
            refreshGui();
        }), 0, 0);
        footer.addItem(new GuiItem(currentPage, cancelEvent()), 4, 0);
        footer.addItem(new GuiItem(nextPage, e -> {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            page = page + 1;
            refreshGui();
        }), 8, 0);
    }

    private void updateBodyWithResult() {
        Util.ensureThread(true);
        body.clear();
        String timeFormat = plugin.text().of(player, "timeunit.std-format").plain();
        SimpleDateFormat format = new SimpleDateFormat(timeFormat);
        if (queryResult.isEmpty()) {
            for (int i = 0; i < body.getHeight() * body.getLength(); i++) {
                body.addItem(new GuiItem(noResultPlaceHolderItem(), cancelEvent()));
            }
            return;
        }
        for (ShopHistory.ShopHistoryRecord record : this.queryResult) {
            String userName = QUserImpl.createSync(plugin.getPlayerFinder(), record.buyer()).getDisplay();
            DataRecord dataRecord = plugin.getDatabaseHelper().getDataRecord(record.dataId()).join();
            if (dataRecord == null) continue;
            Component itemName;
            try {
                ItemStack historyItem = Util.deserialize(dataRecord.getItem());
                if (historyItem == null) {
                    historyItem = new ItemStack(Material.STONE);
                    ItemMeta meta = historyItem.getItemMeta();
                    meta.setDisplayName("Failed to deserialize item");
                    historyItem.setItemMeta(meta);
                }
                itemName = Util.getItemStackName(historyItem);
            } catch (InvalidConfigurationException e) {
                itemName = plugin.text().of(player, "internal-error").forLocale();
                plugin.logger().error("Failed to deserialize itemstack {}", dataRecord.getItem(), e);
            }
            List<Component> lore = plugin.text().ofList(player, "history.shop.log-icon-description",
                    userName,
                    itemName, record.amount(),
                    record.money(),
                    record.tax()).forLocale();
            ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
            if (PackageUtil.parsePackageProperly("renderSkullTexture").asBoolean(true)) {
                skullProvider.provide(record.buyer()).thenAccept(skull -> {
                    stack.setItemMeta(skull.getItemMeta());
                    // Reapply the name and lore
                    plugin.getPlatform().setDisplayName(stack, plugin.text().of(player, "history.shop.log-icon-title",
                            format.format(record.date())).forLocale());
                    plugin.getPlatform().setLore(stack, lore);
                });
            }
            plugin.getPlatform().setDisplayName(stack, plugin.text().of(player, "history.shop.log-icon-title",
                    format.format(record.date())).forLocale());
            plugin.getPlatform().setLore(stack, lore);
            int amount = Math.min(stack.getMaxStackSize(), record.amount());
            stack.setAmount(Math.max(amount, 1)); // Sometimes the amount could be zero
            body.addItem(new GuiItem(stack, cancelEvent()));
        }
    }

    private Consumer<InventoryClickEvent> cancelEvent() {
        return e -> {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
        };
    }
}
