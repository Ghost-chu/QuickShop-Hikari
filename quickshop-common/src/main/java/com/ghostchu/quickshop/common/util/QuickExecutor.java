package com.ghostchu.quickshop.common.util;

import java.util.concurrent.*;

public class QuickExecutor {

    private static final ExecutorService DATABASE_EXECUTOR = new ThreadPoolExecutor(2, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static final ExecutorService SHOP_SAVE_EXECUTOR = new ThreadPoolExecutor(0, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static final ExecutorService COMMON_EXECUTOR = Executors.newCachedThreadPool();
    private static final ExecutorService PLAYER_USERNAME_UUID_LOOKUP_EXECUTOR = new ThreadPoolExecutor(2, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    private QuickExecutor() {
    }

    public static ExecutorService getCommonExecutor() {
        return COMMON_EXECUTOR;
    }

    public static ExecutorService getDatabaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    public static ExecutorService getShopSaveExecutor() {
        return SHOP_SAVE_EXECUTOR;
    }

    public static ExecutorService getProfileIOExecutor() {
        return PLAYER_USERNAME_UUID_LOOKUP_EXECUTOR;
    }
}
