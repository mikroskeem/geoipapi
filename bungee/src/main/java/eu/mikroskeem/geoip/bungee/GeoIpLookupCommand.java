/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.bungee;

import com.google.common.net.InetAddresses;
import eu.mikroskeem.geoip.GeoIPAPI;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Mark Vainomaa
 */
public final class GeoIpLookupCommand extends Command implements TabExecutor {
    public GeoIpLookupCommand() {
        super("geoiplookup", "geoipapi.geoiplookup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("Usage: /" + getName() + " <online player name or IP address>"));
            return;
        }

        String name = args[0];
        final InetAddress address;
        ProxiedPlayer player;
        if ((player = ProxyServer.getInstance().getPlayers().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null)) != null) {
            address = player.getAddress().getAddress(); // TODO: why is this nullable?
        } else {
            try {
                address = InetAddresses.forString(name);
            } catch (Exception e) {
                sender.sendMessage(new TextComponent("Failed to parse address: " + e.getMessage()));
                return;
            }
        }

        (address != null ? GeoIPAPI.INSTANCE.getCountryByIPAsync(address) : CompletableFuture.completedFuture("(unknown)")).thenAcceptAsync(countryCode -> {
            sender.sendMessage(new TextComponent("Address " + (address != null ? address.getHostAddress() : "(null?)") + " country: " + countryCode));
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String token = args[0];
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.length() >= token.length() && name.regionMatches(true, 0, token, 0, token.length()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
