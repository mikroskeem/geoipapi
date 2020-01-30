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

        String readableAddress = address != null ? address.getHostAddress() : "(null?)";
        (address != null ? GeoIPAPI.INSTANCE.getCountryByIPAsync(address) : CompletableFuture.completedFuture("(unknown)")).thenAcceptAsync(countryCode -> {
            sender.sendMessage(new TextComponent("Address " + readableAddress + " country: " + countryCode));
        }).exceptionally(throwable -> {
            sender.sendMessage(new TextComponent("Failed to get " + readableAddress + " country: " + throwable.getMessage()));
            ProxyServer.getInstance().getPluginManager().getPlugin("GeoIPAPI").getSLF4JLogger()
                    .warn("Failed to get {} country", readableAddress, throwable);
            return null;
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
