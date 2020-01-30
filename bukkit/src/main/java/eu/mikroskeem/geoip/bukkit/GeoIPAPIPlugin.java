/*
 * This file is part of project GeoIPAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2019-2020 Mark Vainomaa <mikroskeem@mikroskeem.eu>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
        // Load configuration
        saveDefaultConfig();
        String licenseKey = getConfig().getString("geolite_license_key");
        if (licenseKey == null || licenseKey.isEmpty()) {
            getSLF4JLogger().error("License key is not set, unable to download the database from MaxMind!");
            shouldEnable = false;
            return;
        }

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
            databaseFile = GeoIPDownloader.setupDatabase(null, databaseFolder, !ignoreHash, licenseKey);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to set up GeoIP database! Disabling plugin", e);
            shouldEnable = false;
            return;
        }

        // Set up API
        api = new GeoIPAPIImpl(databaseFile, licenseKey);
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
