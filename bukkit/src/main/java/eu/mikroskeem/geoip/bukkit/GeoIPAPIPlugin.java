/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.bukkit;

import eu.mikroskeem.geoip.common.GeoIPDownloader;
import eu.mikroskeem.geoip.impl.GeoIPAPIImpl;
import eu.mikroskeem.geoip.impl.ImplInjector;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Mark Vainomaa
 */
public final class GeoIPAPIPlugin extends JavaPlugin {
    private boolean shouldEnable = true;
    private GeoIPAPIImpl api = null;

    @Override
    public void onLoad() {
        // Set up database
        getSLF4JLogger().info("Setting up GeoIP database...");
        Path databaseFolder = getDataFolder().toPath().resolve("database");
        Path hashIgnoreFile = getDataFolder().toPath().resolve(".ignorehash");
        boolean ignoreHash = Files.exists(hashIgnoreFile);
        Path databaseFile;
        try {
            if (ignoreHash) {
                getSLF4JLogger().warn("Ignoring GeoIP database checksum mismatch");
            }
            databaseFile = GeoIPDownloader.setupDatabase(null, databaseFolder, !ignoreHash);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to set up GeoIP database! Disabling plugin", e);
            shouldEnable = false;
            return;
        }

        // Set up API
        api = new GeoIPAPIImpl(databaseFile);
        try {
            api.initializeDatabase();
            api.setupUpdater(!ignoreHash, 2, TimeUnit.DAYS);
            ImplInjector.initialize();
            ImplInjector.setApi(api);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to initialize API", e);
            shouldEnable = false;
            return;
        }

        getSLF4JLogger().info("GeoIP API initialized. API data is provided by MaxMind");

        // Register commands
        registerCommand("geoiplookup", GeoIpLookupCommand::new);
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            setEnabled(false);
            return;
        }
    }

    @Override
    public void onDisable() {
        if (!shouldEnable)
            return;

        api.close();
    }

    private void registerCommand(String name, Supplier<CommandExecutor> executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getSLF4JLogger().warn("Command {} is not registered in plugin.yml!", name);
            return;
        }
        CommandExecutor commandExecutor = executor.get();
        command.setExecutor(commandExecutor);
        if (commandExecutor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter) commandExecutor);
        }
    }
}
