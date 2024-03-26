package com.ghostchu.quickshop.api.economy;

import com.ghostchu.quickshop.api.obj.QUser;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class NonSeparateAbstractEconomy extends AbstractEconomy {


    @Override
    public boolean withdraw(@NotNull QUser qUser, double amount, @NotNull World world, @Nullable String currency) {
        // Handle UUID - Player
        UUID uuid = qUser.getUniqueIdIfRealPlayer().orElse(null);
        if (uuid != null) {
            return withdraw(uuid, amount, world, currency);
        }
        // Handle Username - System or Player
        if (qUser.getUsernameOptional().isPresent()) {
            return withdraw(qUser.getUsernameOptional().get(), amount, world, currency);
        }
        // Handle UUID - System
        if (qUser.getUniqueIdOptional().isPresent()) {
            return withdraw(qUser.getUniqueIdOptional().get(), amount, world, currency);
        }
        // All failure
        throw new IllegalArgumentException("Failed to determine the type of this QUser: " + qUser.serialize());
    }

    @Override
    public boolean deposit(@NotNull QUser qUser, double amount, @NotNull World world, @Nullable String currency) {
        // Handle UUID - Player
        UUID uuid = qUser.getUniqueIdIfRealPlayer().orElse(null);
        if (uuid != null) {
            return deposit(uuid, amount, world, currency);
        }
        // Handle Username - System or Player
        if (qUser.getUsernameOptional().isPresent()) {
            return deposit(qUser.getUsernameOptional().get(), amount, world, currency);
        }
        // Handle UUID - System
        if (qUser.getUniqueIdOptional().isPresent()) {
            return deposit(qUser.getUniqueIdOptional().get(), amount, world, currency);
        }
        // All failure
        throw new IllegalArgumentException("Failed to determine the type of this QUser: " + qUser.serialize());
    }

    @Override
    public double getBalance(@NotNull QUser qUser, @NotNull World world, @Nullable String currency) {
        // Handle UUID - Player
        UUID uuid = qUser.getUniqueIdIfRealPlayer().orElse(null);
        if (uuid != null) {
            return getBalance(uuid, world, currency);
        }
        // Handle Username - System or Player
        if (qUser.getUsernameOptional().isPresent()) {
            return getBalance(qUser.getUsernameOptional().get(), world, currency);
        }
        // Handle UUID - System
        if (qUser.getUniqueIdOptional().isPresent()) {
            return getBalance(qUser.getUniqueIdOptional().get(), world, currency);
        }
        // All failure
        throw new IllegalArgumentException("Failed to determine the type of this QUser: " + qUser.serialize());
    }


    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param name     The exact (case insensitive) username to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success (Should be almost always)
     */
    public abstract boolean deposit(@NotNull String name, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param name     The exact (case insensitive) username to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success (Should be almost always)
     */
    public abstract boolean deposit(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param trader   The player to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success (Should be almost always)
     */
    public abstract boolean deposit(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Fetches the balance of the given account name
     *
     * @param name     The uuid of the account
     * @param currency The currency name
     * @param world    The transaction world
     * @return Their current balance.
     */
    public abstract double getBalance(@NotNull String name, @NotNull World world, @Nullable String currency);

    /**
     * Fetches the balance of the given account name
     *
     * @param uuid     The uuid of the account
     * @param currency The currency name
     * @param world    The transaction world
     * @return Their current balance.
     */
    public abstract double getBalance(@NotNull UUID uuid, @NotNull World world, @Nullable String currency);

    /**
     * Fetches the balance of the given player
     *
     * @param player   The name of the account
     * @param currency The currency name
     * @param world    The transaction world
     * @return Their current balance.
     */
    public abstract double getBalance(@NotNull OfflinePlayer player, @NotNull World world, @Nullable String currency);

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param name     The exact (case insensitive) username to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success, false if they didn't have enough cash
     */
    public abstract boolean withdraw(@NotNull String name, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param name     The exact (case insensitive) username to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success, false if they didn't have enough cash
     */
    public abstract boolean withdraw(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param trader   The player to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success, false if they didn't have enough cash
     */
    public abstract boolean withdraw(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Transfer specific amount of currency from A to B
     * (Developer: This is low layer of Economy System, use EconomyTransaction if possible)
     *
     * @param from     The player who is paying money
     * @param to       The player who is receiving money
     * @param amount   The amount to transfer
     * @param world    The transaction world
     * @param currency The currency name
     * @return successed
     */
    public boolean transfer(@NotNull UUID from, @NotNull UUID to, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        if (this.getBalance(from, world, currency) >= amount) {
            if (this.withdraw(from, amount, world, currency)) {
                if (this.deposit(to, amount, world, currency)) {
                    this.deposit(from, amount, world, currency);
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean transfer(@NotNull String from, @NotNull String to, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        if (this.getBalance(from, world, currency) >= amount) {
            if (this.withdraw(from, amount, world, currency)) {
                if (this.deposit(to, amount, world, currency)) {
                    this.deposit(from, amount, world, currency);
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

}
