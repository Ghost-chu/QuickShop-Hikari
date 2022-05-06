/*
 *  This file is a part of project QuickShop, the name is HikariDatabaseConverter.java
 *  Copyright (C) Ghost_chu and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ghostchu.quickshop.converter;

import cc.carm.lib.easysql.EasySQL;
import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.hikari.HikariConfig;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.database.HikariUtil;
import com.ghostchu.quickshop.database.SimpleDatabaseHelper;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapperManager;
import com.ghostchu.quickshop.util.JsonUtil;
import com.ghostchu.quickshop.util.logger.Log;
import lombok.*;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.h2.Driver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HikariDatabaseConverter implements HikariConverterInterface {

    private final HikariConverter instance;
    private final QuickShop plugin;

    public HikariDatabaseConverter(@NotNull HikariConverter instance) {
        this.instance = instance;
        this.plugin = instance.getPlugin();
        /* Engage HikariCP logging */
        Logger.getLogger("cc.carm.lib.easysql.hikari.pool.PoolBase").setLevel(Level.OFF);
        Logger.getLogger("cc.carm.lib.easysql.hikari.pool.HikariPool").setLevel(Level.OFF);
        Logger.getLogger("cc.carm.lib.easysql.hikari.HikariDataSource").setLevel(Level.OFF);
        Logger.getLogger("cc.carm.lib.easysql.hikari.HikariConfig").setLevel(Level.OFF);
        Logger.getLogger("cc.carm.lib.easysql.hikari.util.DriverDataSource").setLevel(Level.OFF);
    }

    /**
     * Returns empty for ready, any elements inside will mark as not ready and will be post to users.
     *
     * @return The element about not ready.
     * @throws Exception Any exception throws will mark as unready and will show to users.
     */
    @Override
    public @NotNull List<Component> checkReady() throws Exception {
        DatabaseConfig config = getDatabaseConfig();
        List<Component> entries = new ArrayList<>();
        // Initialze drivers
        Driver.load();
        Class.forName("org.sqlite.JDBC");
        try (Connection liveDatabase = getLiveDatabase().getConnection()) {
            if (!config.isMysql()) {
                try (Connection sqliteDatabase = getSQLiteDatabase()) {
                    if (hasTable(config.getPrefix() + "shops", liveDatabase))
                        throw new IllegalStateException("The target database has exists shops data!");
                    if (hasTable(config.getPrefix() + "messages", liveDatabase))
                        throw new IllegalStateException("The target database has exists messages data!");
                    if (hasTable(config.getPrefix() + "external_cache", liveDatabase))
                        throw new IllegalStateException("The target database has external_data data!");
                    if (!hasTable(config.getPrefix() + "shops", sqliteDatabase))
                        throw new IllegalStateException("The sources database had no exists shops data! shops.db file data missing!");
                    if (!hasTable(config.getPrefix() + "messages", sqliteDatabase))
                        throw new IllegalStateException("The sources database had no exists messages data! shops.db file data missing!");
                    if (!hasTable(config.getPrefix() + "external_cache", sqliteDatabase))
                        throw new IllegalStateException("The sources database had no external_data data! shops.db file data missing!");
                    if (!hasColumn(config.getPrefix() + "shops", "owner", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists owner column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "price", sqliteDatabase))
                        throw new IllegalStateException("The sources database has not exists price column!");
                    if (!hasColumn(config.getPrefix() + "shops", "itemConfig", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists itemConfig column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "x", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists x column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "y", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists y column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "z", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists z column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "world", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists world column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "unlimited", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists unlimited column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "type", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists type column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "extra", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists extra column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "disableDisplay", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists disableDisplay column!"));
                    if (!hasColumn(config.getPrefix() + "shops", "taxAccount", sqliteDatabase))
                        entries.add(Component.text("The sources database has not exists taxAccount column!"));
                }
            } else {
                // mysql
                if (!hasColumn(config.getPrefix() + "shops", "owner", liveDatabase))
                    entries.add(Component.text("The sources database has not exists owner column!"));
                if (!hasColumn(config.getPrefix() + "shops", "price", liveDatabase))
                    throw new IllegalStateException("The sources database has not exists price column!");
                if (!hasColumn(config.getPrefix() + "shops", "itemConfig", liveDatabase))
                    entries.add(Component.text("The sources database has not exists itemConfig column!"));
                if (!hasColumn(config.getPrefix() + "shops", "x", liveDatabase))
                    entries.add(Component.text("The sources database has not exists x column!"));
                if (!hasColumn(config.getPrefix() + "shops", "y", liveDatabase))
                    entries.add(Component.text("The sources database has not exists y column!"));
                if (!hasColumn(config.getPrefix() + "shops", "z", liveDatabase))
                    entries.add(Component.text("The sources database has not exists z column!"));
                if (!hasColumn(config.getPrefix() + "shops", "world", liveDatabase))
                    entries.add(Component.text("The sources database has not exists world column!"));
                if (!hasColumn(config.getPrefix() + "shops", "unlimited", liveDatabase))
                    entries.add(Component.text("The sources database has not exists unlimited column!"));
                if (!hasColumn(config.getPrefix() + "shops", "type", liveDatabase))
                    entries.add(Component.text("The sources database has not exists type column!"));
                if (!hasColumn(config.getPrefix() + "shops", "extra", liveDatabase))
                    entries.add(Component.text("The sources database has not exists extra column!"));
                if (!hasColumn(config.getPrefix() + "shops", "disableDisplay", liveDatabase))
                    entries.add(Component.text("The sources database has not exists disableDisplay column!"));
                if (!hasColumn(config.getPrefix() + "shops", "taxAccount", liveDatabase))
                    entries.add(Component.text("The sources database has not exists taxAccount column!"));
            }
        }

        return entries;
    }

    /**
     * Start for backing up
     *
     * @param actionId Action Identifier for this upgrade operation.
     * @param folder   The target folder for backup.
     * @throws Exception Backup fails.
     */
    @Override
    public void backup(@NotNull UUID actionId, @NotNull File folder) throws Exception {
        DatabaseConfig config = getDatabaseConfig();
        if (config.isMysql()) {
            instance.getLogger().warning("ApolloConverter doesn't support for MySQL backup, do it by your self with `mysqldump`!");
        } else {
            Files.copy(new File(plugin.getDataFolder(), "shops.db").toPath(), new File(folder, "shops.db").toPath());
        }
    }

    /**
     * Start the migrating
     *
     * @param actionId Action Identifier for this upgrade operation.
     * @throws IllegalStateException Not ready.
     * @throws Exception             Migrate operation fails.
     */
    @Override
    public void migrate(@NotNull UUID actionId) throws Exception {
        if (!checkReady().isEmpty())
            throw new IllegalStateException("Not ready!");
        DatabaseConfig config = getDatabaseConfig();
        instance.getLogger().info("Renaming tables...");
        String shopsTmpTable = renameTables(actionId, config);
        instance.getLogger().info("Offline Messages and External Caches won't be migrated because they are have totally different syntax and cache need regenerate after migrated.");
        instance.getLogger().info("Downloading old data from database connection...");
        List<ShopStorageUnit> units;
        SQLManager liveDatabase = getLiveDatabase();
        try (Connection liveDatabaseConnection = liveDatabase.getConnection()) {
            if (config.isMysql()) {
                units = pullShops(shopsTmpTable, liveDatabaseConnection);
            } else {
                try (Connection sqliteDatabase = getSQLiteDatabase()) {
                    units = pullShops(shopsTmpTable, sqliteDatabase);
                }
            }
            instance.getLogger().info("Checking and creating for database tables... ");
            // Database Helper will resolve all we need while starting up.
            new SimpleDatabaseHelper(plugin, liveDatabase,config.getPrefix());
            instance.getLogger().info("Migrating old data to new database...");
            pushShops(units, config.getPrefix(), liveDatabase);
        }
        instance.getLogger().info("Database migration completed!");
    }

    /**
     * Rename tables
     *
     * @param actionId ActionID
     * @param config   DatabaseConfig
     * @return The shops table name
     * @throws Exception Any error happens
     */
    @NotNull
    private String renameTables(@NotNull UUID actionId, @NotNull DatabaseConfig config) throws Exception {
        if (config.isMysql()) {
            SQLManager manager = getLiveDatabase();
            manager.alterTable(config.getPrefix() + "shops")
                    .renameTo(config.getPrefix() + "shops_" + actionId.toString().replace("-", ""))
                    .execute();
            try {
                manager.alterTable(config.getPrefix() + "messages")
                        .renameTo(config.getPrefix() + "messages_" + actionId.toString().replace("-", ""))
                        .execute();
                manager.alterTable(config.getPrefix() + "logs")
                        .renameTo(config.getPrefix() + "logs_" + actionId.toString().replace("-", ""))
                        .execute();
                manager.alterTable(config.getPrefix() + "external_cache")
                        .renameTo(config.getPrefix() + "external_cache_" + actionId.toString().replace("-", ""))
                        .execute();
            } catch (SQLException ignored) {
                instance.getLogger().log(Level.INFO, "Error while rename tables! CHECK:Recoverable + Skip-able, Skipping...");
            }
            try (Connection connection = manager.getConnection()) {
                if (!hasTable(config.getPrefix() + "shops_" + actionId.toString().replace("-", ""), connection)) {
                    throw new IllegalStateException("Failed to rename tables!");
                }
            }
            return config.getPrefix() + "shops_" + actionId.toString().replace("-", "");
        } else {
            return config.getPrefix() + "shops";
        }
    }


    /**
     * Returns a valid connection for SQLite database.
     *
     * @return A valid connection for SQLite database.
     * @throws IllegalStateException Something not ready.
     * @throws ConnectException      Connection to SQLite failed.
     */
    @NotNull
    private Connection getSQLiteDatabase() throws IllegalStateException, ConnectException {
        File sqliteFile = new File(plugin.getDataFolder(), "shops.db");
        if (!sqliteFile.exists()) {
            throw new IllegalStateException("SQLite database not found!");
        }
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
            if (!connection.isValid(10)) {
                throw new ConnectException("SQLite database is not valid!");
            }
            return connection;
        } catch (SQLException exception) {
            throw new ConnectException("Failed to connect to SQLite database!" + exception.getMessage());
        }
    }

    /**
     * Returns a valid SQLManager for the live database.
     *
     * @return A valid SQLManager for the live database.
     * @throws IllegalStateException Something not ready.
     * @throws ConnectException      Failed to connect to the database.
     */
    @NotNull
    private SQLManager getLiveDatabase() throws IllegalStateException, ConnectException {
        HikariConfig config = HikariUtil.createHikariConfig();
        SQLManager manager;
        try {
            DatabaseConfig databaseConfig = getDatabaseConfig();
            if (databaseConfig.isMysql()) {
                config.setJdbcUrl("jdbc:mysql://" + databaseConfig.getHost() + ":" + databaseConfig.getPort()
                        + "/" + databaseConfig.getDatabase() + "?useSSL=" + databaseConfig.isUseSSL());
                config.setUsername(databaseConfig.getUser());
                config.setPassword(databaseConfig.getPass());
                manager = EasySQL.createManager(config);
            } else {
                // SQLite database - Doing this handles file creation
                Driver.load();
                config.setJdbcUrl("jdbc:h2:" + new File(plugin.getDataFolder(), "shops").getCanonicalFile().getAbsolutePath() + ";DB_CLOSE_DELAY=-1;MODE=MYSQL");
                manager = EasySQL.createManager(config);
                manager.executeSQL("SET MODE=MYSQL"); // Switch to MySQL mode
            }
            if (!manager.getConnection().isValid(10)) {
                throw new ConnectException("Live database is not valid!");
            }
            return manager;
        } catch (Exception e) {
            throw new ConnectException("Couldn't connect to live database! " + e.getMessage());
        }
    }

    /**
     * Getting the live database configuration.
     *
     * @return The live database configuration.
     * @throws IllegalStateException If any configuration key not set correct.
     */
    @NotNull
    private DatabaseConfig getDatabaseConfig() throws IllegalStateException {
        ConfigurationSection dbCfg = plugin.getConfig().getConfigurationSection("database");
        if (dbCfg == null)
            throw new IllegalStateException("Database configuration section not found!");
        if (!dbCfg.isSet("mysql"))
            throw new IllegalStateException("Database configuration section -> type not set!");
        if (!dbCfg.isSet("prefix"))
            throw new IllegalStateException("Database configuration section -> prefix not set!");
        boolean mysql = dbCfg.getBoolean("mysql");
        if (mysql) {
            if (!dbCfg.isSet("host"))
                throw new IllegalStateException("Database configuration section -> host not set!");
            if (!dbCfg.isSet("port"))
                throw new IllegalStateException("Database configuration section -> port not set!");
            if (!dbCfg.isSet("user"))
                throw new IllegalStateException("Database configuration section -> user not set!");
            if (!dbCfg.isSet("password"))
                throw new IllegalStateException("Database configuration section -> password not set!");
            if (!dbCfg.isSet("database"))
                throw new IllegalStateException("Database configuration section -> name not set!");
            if (!dbCfg.isSet("usessl"))
                throw new IllegalStateException("Database configuration section -> SSL not set!");
        }

        String user = dbCfg.getString("user", "mc");
        String pass = dbCfg.getString("password", "minecraft");
        String host = dbCfg.getString("host", "localhost");
        int port = dbCfg.getInt("port", 3306);
        String database = dbCfg.getString("database", "mc");
        boolean useSSL = dbCfg.getBoolean("usessl", false);
        String dbPrefix = dbCfg.getString("prefix", "");
        if ("none".equals(dbPrefix)) {
            dbPrefix = "";
        }
        return new DatabaseConfig(mysql, host, user, pass, port, database, useSSL, dbPrefix);
    }


    private void pushShops(@NotNull List<ShopStorageUnit> units, @NotNull String prefix, @NotNull SQLManager manager) {
        instance.getLogger().info("Preparing to pushing shops into database...");
        instance.getLogger().info("Statistics: Total " + units.size() + " shops waiting for pushing.");
        instance.getLogger().info("Initializing target database...");
        int count = 0;
        int fails = 0;
        for (ShopStorageUnit unit : units) {
            ++count;
            instance.getLogger().info("Pushing shop " + count + " of " + units.size() + " to target database...");
            try {
                manager.createInsert(prefix + "shops")
                        .setColumnNames("owner", "price", "itemConfig", "x", "y", "z", "world", "unlimited", "type", "extra",
                                "currency", "disableDisplay", "taxAccount", "inventorySymbolLink", "inventoryWrapperName")
                        .setParams(unit.getOwner(), unit.getPrice(), unit.getItemConfig(), unit.getX(), unit.getY(), unit.getZ(), unit.getWorld(), unit.getUnlimited(), unit.getType(), unit.getExtra(),
                                unit.getCurrency(), unit.getDisableDisplay(), unit.getTaxAccount(), unit.getInventorySymbolLink(), unit.getInventoryWrapperName())
                        .execute();
            } catch (Exception e) {
                ++fails;
                instance.getLogger().log(Level.WARNING, "Failed to push shop " + unit + " into database! " + e.getMessage() + ", skipping...", e);
            }
        }
        instance.getLogger().info("Pushed " + count + " shops into database. " + fails + " shops failed to push.");
    }

    @NotNull
    private List<ShopStorageUnit> pullShops(@NotNull String shopsTable, @NotNull Connection connection) throws SQLException {
        instance.getLogger().info("Preparing for pulling shops from database...");
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM " + shopsTable);
        List<ShopStorageUnit> units = new ArrayList<>();
        int count = 0;
        int fails = 0;
        while (resultSet.next()) {
            ++count;
            instance.getLogger().info("Pulling and converting shops from database, downloading: #" + count + "...");
            ShopStorageUnit.ShopStorageUnitBuilder builder = ShopStorageUnit.builder();
            try {
                builder.owner(resultSet.getString("owner"));
                builder.price(resultSet.getDouble("price"));
                builder.itemConfig(resultSet.getString("itemConfig"));
                builder.x(resultSet.getInt("x"));
                builder.y(resultSet.getInt("y"));
                builder.z(resultSet.getInt("z"));
                builder.world(resultSet.getString("world"));
                builder.unlimited(resultSet.getInt("unlimited"));
                builder.type(resultSet.getInt("type"));
                builder.currency(resultSet.getString("currency"));
                builder.extra(resultSet.getString("extra"));
                builder.disableDisplay(resultSet.getInt("disableDisplay"));
                builder.taxAccount(resultSet.getString("taxAccount"));
                units.add(builder.build());
            } catch (SQLException exception) {
                instance.getLogger().log(Level.WARNING, "Error while pulling shop from database: " + exception.getMessage() + ", skipping...", exception);
                ++fails;
            }
        }
        instance.getLogger().info("Completed! Pulled " + (count - fails) + " shops from database! Total " + fails + " fails.");
        return units;

    }

    @AllArgsConstructor
    @Data
    static class DatabaseConfig {
        private final boolean mysql;
        private final String host;
        private final String user;
        private final String pass;
        private final int port;
        private final String database;
        private final boolean useSSL;
        private final String prefix;
    }

    /**
     * Returns true if the table exists
     *
     * @param table The table to check for
     * @return True if the table is found
     * @throws SQLException Throw exception when failed execute somethins on SQL
     */
    public boolean hasTable(@NotNull String table, @NotNull Connection connection) throws SQLException {
        boolean match = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Returns true if the given table has the given column
     *
     * @param table  The table
     * @param column The column
     * @return True if the given table has the given column
     */
    public boolean hasColumn(@NotNull String table, @NotNull String column, @NotNull Connection connection) throws SQLException {
        String query = "SELECT * FROM " + table + " LIMIT 1";
        boolean match = false;
        try (PreparedStatement ps = connection.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (metaData.getColumnLabel(i).equals(column)) {
                    match = true;
                    break;
                }
            }
        }
        return match; // Uh, wtf.
    }

    @Builder
    @AllArgsConstructor
    @Getter
    @ToString
    static class ShopStorageUnit {
        private final String owner;
        private final double price;
        private final String itemConfig;
        private final int x;
        private final int y;
        private final int z;
        private final String world;
        private final int unlimited;
        private final int type;
        private final String extra;
        private final String currency;
        private final int disableDisplay;
        private final String taxAccount;

        @NotNull
        public String getInventoryWrapperName() {
            return QuickShop.getInstance().getDescription().getName();
        }

        @NotNull
        public String getInventorySymbolLink() {
            String holder = JsonUtil.standard().toJson(new BukkitInventoryWrapperManager.BlockHolder(world, x, y, z));
            String link = JsonUtil.standard().toJson(new BukkitInventoryWrapperManager.CommonHolder(BukkitInventoryWrapperManager.HolderType.BLOCK, holder));
            Log.debug("Generating SymbolLink: " + link + ", InventoryHolder: BukkitInventoryWrapper, Holder:" + holder);
            return link;
        }
    }
}
