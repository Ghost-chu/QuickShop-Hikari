package com.ghostchu.quickshop.api.economy;

import org.jetbrains.annotations.NotNull;

public enum EconomyType {
    /*
     * UNKNOWN = FALLBACK TO VAULT
     * VAULT = USE VAULT API
     * RESERVE = USE RESERVE API
     * */
    UNKNOWN(-1),
    VAULT(0),
    RESERVE(1),
    //MIXED(2),
    GEMS_ECONOMY(3),
    TNE(4),
    COINS_ENGINE(5),
    TREASURY(6);

    private final int id;

    EconomyType(int id) {
        this.id = id;
    }

    @NotNull
    public static EconomyType fromID(int id) {
        for (EconomyType type : EconomyType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static int toID(@NotNull EconomyType economyType) {
        return economyType.id;
    }

    public int toID() {
        return id;
    }
}
