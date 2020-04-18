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

import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.mikroskeem.geoip.GeoIPAPI;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Mark Vainomaa
 */
public final class GeoIpLookupCommand implements Command {
    @Inject private Logger logger;
    @Inject private ProxyServer proxy;

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(TextComponent.of("Usage: /geoiplookup <online player name or IP address>"));
            return;
        }

        String name = args[0];
        final InetAddress address;
        Player player;
        if ((player = proxy.getAllPlayers().stream().filter(p -> p.getUsername().equals(name)).findFirst().orElse(null)) != null) {
            address = player.getRemoteAddress().getAddress(); // TODO: why is this nullable?
        } else {
            try {
                address = InetAddresses.forString(name);
            } catch (Exception e) {
                sender.sendMessage(TextComponent
                        .builder("Failed to parse address: ")
                        .append(e.getMessage(), TextColor.RED)
                        .build()
                );
                return;
            }
        }

        String readableAddress = address != null ? address.getHostAddress() : "(null?)";
        (address != null ? GeoIPAPI.INSTANCE.getCountryByIPAsync(address) : CompletableFuture.completedFuture("(unknown)")).thenAcceptAsync(countryCode -> {
            sender.sendMessage(TextComponent
                    .builder("Address ")
                    .append(readableAddress)
                    .append(" country: ")
                    .append(countryCode)
                    .build()
            );
        }).exceptionally(throwable -> {
            sender.sendMessage(TextComponent
                    .builder("Failed to get ")
                    .append(readableAddress)
                    .append(" country: ")
                    .append(throwable.getMessage(), TextColor.RED)
                    .build()
            );
            logger.warn("Failed to get {} country", readableAddress, throwable);
            return null;
        });
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] args) {
        if (args.length == 1) {
            String token = args[0];
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.length() >= token.length() && name.regionMatches(true, 0, token, 0, token.length()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("geoipapi.geoiplookup");
    }
}
