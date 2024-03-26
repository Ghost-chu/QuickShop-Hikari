package com.ghostchu.quickshop.economy;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.NonSeparateAbstractEconomy;
import com.ghostchu.quickshop.util.economyformatter.BuiltInEconomyFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * @author creatorfromhell
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public class Economy_Reserve extends NonSeparateAbstractEconomy {

    private static final String ERROR_MESSAGE =
            "QuickShop received an error when processing Economy response, THIS NOT A QUICKSHOP FAULT, you might need ask help with your Economy Provider plugin author.";

    private final QuickShop plugin;
    private final BuiltInEconomyFormatter formatter;

    @Getter
    @Setter
    @Nullable
    private EconomyAPI reserve = null;

    /**
     * @param plugin Main instance
     */
    public Economy_Reserve(@NotNull QuickShop plugin) {
        this.plugin = plugin;
        this.formatter = new BuiltInEconomyFormatter(plugin);
        plugin.getReloadManager().register(this);

        setup();
    }

    private void setup() {
        try {
            Reserve re = ((Reserve) Bukkit.getPluginManager().getPlugin("Reserve"));
            if (re.economyProvided()) {
                reserve = re.economy();
            }
        } catch (Exception throwable) {
            reserve = null;
        }
    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-Reserve";
    }

    /**
     * Transfers the given amount of money from Player1 to Player2
     *
     * @param from   The player who is paying money
     * @param to     The player who is receiving money
     * @param amount The amount to transfer
     * @return true if success (Payer had enough cash, receiver was able to receive the funds)
     */
    @Override
    public boolean transfer(@NotNull UUID from, @NotNull UUID to, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).transferHoldings(from, to, BigDecimal.valueOf(amount), world.getName(), currency);
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }

    @Override
    public boolean transfer(@NotNull String from, @NotNull String to, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).transferHoldings(from, to, BigDecimal.valueOf(amount), world.getName(), currency);
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }

    @Override
    public boolean withdraw(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).removeHoldings(name, BigDecimal.valueOf(amount));
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Reserve";
    }

    @Override
    public boolean deposit(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).addHoldings(name, BigDecimal.valueOf(amount), world.getName(), currency);
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }

    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param name   The exact (case insensitive) username to give money to
     * @param amount The amount to give them
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).addHoldings(name, BigDecimal.valueOf(amount), world.getName(), currency);
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }


    @Override
    public boolean deposit(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        return deposit(trader.getUniqueId(), amount, world, currency);
    }

    /**
     * Formats the given number... E.g. 50.5 becomes $50.5 Dollars, or 50 Dollars 5 Cents
     *
     * @param balance The given number
     * @return The balance in human readable text.
     */
    @Override
    public String format(double balance, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).format(BigDecimal.valueOf(balance), world.getName(), currency);
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return formatInternal(balance);
        }
    }

    @Override
    public double getBalance(@NotNull String name, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).getHoldings(name, world.getName(), currency).doubleValue();
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return 0.0;
        }
    }

    private String formatInternal(double balance) {
        if (!isValid()) {
            return "Error";
        }

        return this.formatter.getInternalFormat(balance, null);
    }

    /**
     * Fetches the balance of the given account name
     *
     * @param name The name of the account
     * @return Their current balance.
     */
    @Override
    public double getBalance(@NotNull UUID name, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).getHoldings(name, world.getName(), currency).doubleValue();
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return 0.0;
        }
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player, @NotNull World world, @Nullable String currency) {
        return getBalance(player.getUniqueId(), world, currency);
    }


    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param name   The exact (case insensitive) username to take money from
     * @param amount The amount to take from them
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        try {
            return Objects.requireNonNull(reserve).removeHoldings(name, BigDecimal.valueOf(amount));
        } catch (Exception throwable) {
            if (plugin.getSentryErrorReporter() != null) {
                plugin.getSentryErrorReporter().ignoreThrow();
            }
            plugin.logger().warn(ERROR_MESSAGE, throwable);
            return false;
        }
    }

    @Override
    public boolean withdraw(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        return withdraw(trader.getUniqueId(), amount, world, currency);
    }


    /**
     * Gets the currency does exists
     *
     * @param currency Currency name
     * @return exists
     */
    @Override
    public boolean hasCurrency(@NotNull World world, @NotNull String currency) {
        if (!isValid()) {
            return false;
        }
        return reserve.hasCurrency(currency, world.getName());
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
    public @Nullable String getLastError() {
        return "Cannot provide: Reserve api doesn't support enhanced error tracing.";
    }

    /**
     * Checks that this economy is valid. Returns false if it is not valid.
     *
     * @return True if this economy will work, false if it will not.
     */
    @Override
    public boolean isValid() {
        return reserve != null;
    }


    @Override
    public @NotNull Plugin getPlugin() {
        return plugin.getJavaPlugin();
    }

}
