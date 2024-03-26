package com.ghostchu.quickshop.command.subcommand;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.api.command.CommandParser;
import com.ghostchu.quickshop.api.event.ShopNamingEvent;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.economy.SimpleEconomyTransaction;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.logger.Log;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SubCommand_Name implements CommandHandler<Player> {

    private final QuickShop plugin;

    public SubCommand_Name(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(@NotNull Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        final Shop shop = getLookingShop(sender);
        if (shop == null) {
            plugin.text().of(sender, "not-looking-at-shop").send();
            return;
        }
        if (!shop.playerAuthorize(sender.getUniqueId(), BuiltInShopPermission.SET_NAME)
                && !plugin.perm().hasPermission(sender, "quickshop.other.shopnaming")) {
            plugin.text().of(sender, "not-managed-shop").send();
        }

        if (parser.getArgs().isEmpty()) {
            shop.setShopName(null);
            plugin.text().of(sender, "shop-name-unset").send();
            return;
        }

        String shopName = parser.getArgs().get(0);
        // Translate the all chat colors
        shopName = ChatColor.translateAlternateColorCodes('&', shopName);
        // Then strip all of them, Shop name reference is disallow any color
        shopName = ChatColor.stripColor(shopName);

        int maxLength = plugin.getConfig().getInt("shop.name-max-length", 32);
        if (shopName.length() > maxLength) {
            plugin.text().of(sender, "shop-name-too-long", maxLength).send();
            return;
        }

        double fee = plugin.getConfig().getDouble("shop.name-fee", 0);
        SimpleEconomyTransaction transaction = null;
        if (fee > 0) {
            if (!plugin.perm().hasPermission(sender, "quickshop.bypass.namefee")) {
                transaction = SimpleEconomyTransaction.builder()
                        .world(shop.getLocation().getWorld())
                        .from(QUserImpl.createFullFilled(sender))
                        .to(shop.getTaxAccount())
                        .currency(plugin.getCurrency())
                        .taxAccount(shop.getTaxAccount())
                        .taxModifier(0.0d)
                        .core(plugin.getEconomy())
                        .amount(fee)
                        .build();
                if (!transaction.checkBalance()) {
                    plugin.text().of(sender, "you-cant-afford-shop-naming", plugin.getShopManager().format(fee, shop.getLocation().getWorld(), plugin.getCurrency())).send();
                    return;
                }
            }
        }
        ShopNamingEvent namingEvent = new ShopNamingEvent(shop, shopName);
        if (Util.fireCancellableEvent(namingEvent)) {
            Log.debug("Other plugin cancelled shop naming.");
            return;
        }
        shopName = namingEvent.getName();

        if (transaction != null && !transaction.failSafeCommit()) {
            plugin.text().of(sender, "economy-transaction-failed", transaction.getLastError()).send();
            plugin.logger().error("EconomyTransaction Failed, last error: {}", transaction.getLastError());
            return;
        }

        shop.setShopName(shopName);
        plugin.text().of(sender, "shop-name-success", shopName).send();
    }

    @NotNull
    @Override
    public List<String> onTabComplete(@NotNull Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        return parser.getArgs().size() == 1 ? Collections.singletonList(plugin.text().of(sender, "tabcomplete.name").legacy()) : Collections.emptyList();
    }

}
