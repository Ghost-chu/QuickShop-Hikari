package com.ghostchu.quickshop.api.economy;

import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import com.ghostchu.simplereloadlib.Reloadable;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract Economy Core
 */
public abstract class AbstractEconomy implements EconomyCore, Reloadable {

    protected AbstractEconomy() {
    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-Economy Processor";
    }

    @Override
    public abstract boolean withdraw(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency);

    @Override
    public abstract boolean deposit(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency);

    @Override
    public abstract double getBalance(@NotNull QUser obj, @NotNull World world, @Nullable String currency);
    @Override
    public boolean transfer(@NotNull QUser from, @NotNull QUser to, double amount, @NotNull World world, @Nullable String currency) {
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

    public abstract String getProviderName();

    /**
     * Callback for reloading
     *
     * @return Reloading success
     */
    @Override
    public ReloadResult reloadModule() {
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }

    @Override
    public abstract String toString();
}
