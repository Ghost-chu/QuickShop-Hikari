package com.ghostchu.quickshop.economy;


import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.AbstractEconomy;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.util.economyformatter.BuiltInEconomyFormatter;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import lombok.Getter;
import lombok.ToString;
import me.lokka30.treasury.api.common.Cause;
import me.lokka30.treasury.api.common.NamespacedKey;
import me.lokka30.treasury.api.common.service.Service;
import me.lokka30.treasury.api.common.service.ServiceRegistry;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.Account;
import me.lokka30.treasury.api.economy.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@ToString
public class Economy_Treasury extends AbstractEconomy implements Listener {

    private static final String ERROR_MESSAGE =
            "QuickShop received an error when processing Economy response, THIS NOT A QUICKSHOP FAULT, you might need ask help with your Economy Provider plugin (%s) author.";
    private final QuickShop plugin;
    private final BuiltInEconomyFormatter formatter;
    private boolean allowLoan;
    @Nullable
    private String lastError = null;
    @Getter
    private Service<EconomyProvider> service;


    public Economy_Treasury(@NotNull QuickShop plugin) {
        super();
        this.plugin = plugin;
        this.formatter = new BuiltInEconomyFormatter(plugin);
        plugin.getReloadManager().register(this);
        init();
        setupEconomy();
    }

    private void init() {
        this.allowLoan = plugin.getConfig().getBoolean("shop.allow-economy-loan");
    }

    private boolean setupEconomy() {
        // Try to find an economy provider service registration.
        final Optional<Service<EconomyProvider>> serviceOpt = ServiceRegistry.INSTANCE.serviceFor(EconomyProvider.class);

        // Require an economy provider to be registered as a service before continuing.
        if (serviceOpt.isEmpty()) {
            plugin.logger().error("Expected an Economy Provider to be registered through Treasury; found none");
            return false;
        }
        this.service = serviceOpt.get();
        plugin.logger().info("Using economy system: " + this.service.registrarName());
        Bukkit.getPluginManager().registerEvents(this, plugin.getJavaPlugin());
        Log.debug("Economy service listener was registered.");
        return true;
    }

    @Override
    public String format(double balance, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return "Error";
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return formatInternal(balance);
        }
        try {
            return cur.format(BigDecimal.valueOf(balance), Locale.ROOT);
        } catch (Exception e) {
            return formatInternal(balance);
        }
    }

    @Nullable
    private Currency getCurrency(@NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return null;
        }
        if (currency == null) {
            return service.get().getPrimaryCurrency();
        }
        return service.get().findCurrency(currency).orElse(null);
    }


    private String formatInternal(double balance) {
        if (!isValid()) {
            return "Error";
        }
        return this.formatter.getInternalFormat(balance, null);
    }


    @Override
    public @Nullable String getLastError() {
        return this.lastError;
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin.getJavaPlugin();
    }

    /**
     * Gets the currency does exists
     *
     * @param currency Currency name
     * @return exists
     */
    @Override
    public boolean hasCurrency(@NotNull World world, @NotNull String currency) {
        return getCurrency(world, currency) != null;
    }

    @Override
    public boolean isValid() {
        return this.service != null;
    }

    /**
     * Gets currency supports status
     *
     * @return true if supports
     */
    @Override
    public boolean supportCurrency() {
        return true;
    }


    @Override
    public @NotNull String getName() {
        return "BuiltIn-Treasury (experimental)";
    }

    @Override
    public boolean withdraw(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency) {
        Account account;
        if (obj.isRealPlayer()) {
            account = service.get().accountAccessor().player().withUniqueId(obj.getUniqueId()).get().join();
        } else {
            account = service.get().accountAccessor().nonPlayer().withName(obj.getUsername()).get().join();
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        account.withdrawBalance(BigDecimal.valueOf(amount), Cause.plugin(NamespacedKey.of(plugin.getJavaPlugin().getName(), "economy-withdraw")), cur);
        return false;
    }

    @Override
    public boolean deposit(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency) {
        Account account;
        if (obj.isRealPlayer()) {
            account = service.get().accountAccessor().player().withUniqueId(obj.getUniqueId()).get().join();
        } else {
            account = service.get().accountAccessor().nonPlayer().withName(obj.getUsername()).get().join();
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        account.depositBalance(BigDecimal.valueOf(amount), Cause.plugin(NamespacedKey.of(plugin.getJavaPlugin().getName(), "economy-withdraw")), cur);
        return false;
    }

    @Override
    public double getBalance(@NotNull QUser obj, @NotNull World world, @Nullable String currency) {
        Account account;
        if (obj.isRealPlayer()) {
            account = service.get().accountAccessor().player().withUniqueId(obj.getUniqueId()).get().join();
        } else {
            account = service.get().accountAccessor().nonPlayer().withName(obj.getUsername()).get().join();
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return 0.0;
        }
        return account.retrieveBalance(cur).join().doubleValue();
    }

    @Override
    public String getProviderName() {
        if (this.service == null) {
            return "Provider not found.";
        }
        return this.service.registrarName();
    }

    /**
     * Callback for reloading
     *
     * @return Reloading success
     */
    @Override
    public ReloadResult reloadModule() {
        init();
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }
}
