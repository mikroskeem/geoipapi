/*
 * Copyright (c) 2019-2020 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.bungee;

import eu.mikroskeem.geoip.GeoIPAPI;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * @author Mark Vainomaa
 */
public final class PlayerConnectionListener implements Listener {
    @EventHandler
    public void on(PreLoginEvent event) {
        final String username = event.getConnection().getName();
        GeoIPAPI.INSTANCE.getCountryByIPAsync(event.getConnection().getAddress().getAddress()).thenAccept((country) -> {
            GeoIPAPIPlugin.getInstance().getSLF4JLogger().info("Player {} is connecting from {}", username, country);
        });
    }
}
