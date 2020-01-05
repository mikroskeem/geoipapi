/*
 * Copyright (c) 2019-2020 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.bungee;

import eu.mikroskeem.geoip.common.GeoIPDownloader;
import eu.mikroskeem.geoip.impl.GeoIPAPIImpl;
import eu.mikroskeem.geoip.impl.ImplInjector;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

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
}
