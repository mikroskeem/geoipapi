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

package eu.mikroskeem.geoipapi.velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.mikroskeem.geoip.GeoIPAPI;
import eu.mikroskeem.geoip.common.GeoIPDownloader;
import eu.mikroskeem.geoip.impl.GeoIPAPIImpl;
import eu.mikroskeem.implinjector.ImplInjector;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(id = "geoipapi", name = "GeoIPAPI", version = "0.0.1-SNAPSHOT", /* TODO: keep in sync */
        description = "Provides GeoIP database access to plugins",
        authors = {"mikroskeem"})
public final class GeoIPAPIPlugin {
    @Inject private Logger logger;
    @Inject private ProxyServer proxy;
    @Inject private Injector injector;
    @Inject @DataDirectory private Path dataFolder;

    private GeoIPAPIImpl api;

    @Subscribe
    public void on(ProxyInitializeEvent event) {
        // Load configuration
        Path configFile = saveDefaultConfig();
        YAMLConfigurationLoader configLoader = YAMLConfigurationLoader.builder()
                .setFile(configFile.toFile())
                .build();
        ConfigurationNode config;

        try {
            config = configLoader.load();
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return;
        }

        String licenseKey = config.getNode("geolite_license_key").getString();
        if (licenseKey == null || licenseKey.isEmpty()) {
            logger.error("License key is not set, unable to download the database from MaxMind!");
            return;
        }

        // Set up database
        logger.info("Setting up GeoIP database...");
        Path databaseFolder = dataFolder.resolve("database");
        Path hashIgnoreFile = dataFolder.resolve(".ignorehash");
        boolean ignoreHash = Files.exists(hashIgnoreFile);
        Path databaseFile;
        try {
            if (ignoreHash) {
                logger.warn("Ignoring GeoIP database checksum mismatch");
            }
            databaseFile = GeoIPDownloader.setupDatabase(null, databaseFolder, !ignoreHash, licenseKey);
        } catch (Exception e) {
            logger.error("Failed to set up GeoIP database! Disabling plugin", e);
            return;
        }

        // Set up API
        api = new GeoIPAPIImpl(databaseFile, licenseKey);
        try {
            api.initializeDatabase();
            api.setupUpdater(!ignoreHash, 2, TimeUnit.DAYS);
            ImplInjector.inject(GeoIPAPI.class, "INSTANCE", api);
        } catch (Exception e) {
            logger.error("Failed to initialize API", e);
            return;
        }

        logger.info("GeoIP API initialized. API data is provided by MaxMind");
        proxy.getEventManager().register(this, injector.getInstance(PlayerConnectionListener.class));
        proxy.getCommandManager().register("geoiplookup", injector.getInstance(GeoIpLookupCommand.class));
    }

    private Path saveDefaultConfig() {
        Path configFile = dataFolder.resolve("config.yml");
        try (InputStream defaultResource = GeoIPAPIPlugin.class.getResourceAsStream("/config.yml")) {
            if (Files.notExists(configFile) && defaultResource != null) {
                Files.createDirectories(configFile.getParent());
                Files.copy(defaultResource, configFile);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return configFile;
    }
}
