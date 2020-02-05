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

package eu.mikroskeem.geoip.bungee;

import eu.mikroskeem.geoip.GeoIPAPI;
import eu.mikroskeem.geoip.common.GeoIPDownloader;
import eu.mikroskeem.geoip.impl.GeoIPAPIImpl;
import eu.mikroskeem.implinjector.ImplInjector;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Vainomaa
 */
public final class GeoIPAPIPlugin extends Plugin {
    private boolean shouldEnable = true;
    private GeoIPAPIImpl api = null;

    @Override
    public void onLoad() {
        // Load configuration
        saveDefaultConfig();
        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to load configuration", e);
            shouldEnable = false;
            return;
        }

        String licenseKey = config.getString("geolite_license_key");
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
            ImplInjector.inject(GeoIPAPI.class, "INSTANCE", api);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to initialize API", e);
            shouldEnable = false;
            return;
        }

        getSLF4JLogger().info("GeoIP API initialized. API data is provided by MaxMind");

        // Register commands and event handlers
        getProxy().getPluginManager().registerCommand(this, new GeoIpLookupCommand());
        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener());
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            getSLF4JLogger().error("As this plugin failed to initialize before, then GeoIP API is not available.");
            return;
        }
    }

    @Override
    public void onDisable() {
        if (!shouldEnable)
            return;

        api.close();
    }

    public static GeoIPAPIPlugin getInstance() {
        return (GeoIPAPIPlugin) ProxyServer.getInstance().getPluginManager().getPlugin("GeoIPAPI");
    }

    private void saveDefaultConfig() {
        try (InputStream defaultResource = getResourceAsStream("config.yml")) {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists() && defaultResource != null) {
                configFile.getParentFile().mkdirs();
                Files.copy(defaultResource, configFile.toPath());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
