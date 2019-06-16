/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.bukkit;

import eu.mikroskeem.geoip.common.GeoIPDownloader;
import eu.mikroskeem.geoip.impl.GeoIPAPIImpl;
import eu.mikroskeem.geoip.impl.ImplInjector;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

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
        Path databaseFile;
        try {
            databaseFile = GeoIPDownloader.setupDatabase(databaseFolder);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to set up GeoIP database! Disabling plugin", e);
            shouldEnable = false;
            return;
        }

        // Set up API
        api = new GeoIPAPIImpl(databaseFile);
        try {
            ImplInjector.initialize();
            ImplInjector.setApi(api);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to initialize API", e);
            shouldEnable = false;
            return;
        }

        getSLF4JLogger().info("GeoIP API initialized. API data is provided by MaxMind");
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
}
