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

import com.google.common.net.InetAddresses;
import eu.mikroskeem.geoip.GeoIPAPI;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Mark Vainomaa
 */
public final class GeoIpLookupCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <online player name or IP address>");
            return true;
        }

        String name = args[0];
        final InetAddress address;
        Player player;
        if ((player = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null)) != null) {
            address = player.getAddress().getAddress(); // TODO: why is this nullable?
        } else {
            try {
                address = InetAddresses.forString(name);
            } catch (Exception e) {
                sender.sendMessage("Failed to parse address: " + e.getMessage());
                return true;
            }
        }

        String readableAddress = address != null ? address.getHostAddress() : "(null?)";
        (address != null ? GeoIPAPI.INSTANCE.getCountryByIPAsync(address) : CompletableFuture.completedFuture("(unknown)")).thenAcceptAsync(countryCode -> {
            sender.sendMessage("Address " + readableAddress + " country: " + countryCode);
        }).exceptionally(throwable -> {
            sender.sendMessage("Failed to get " + readableAddress + " country: " + throwable.getMessage());
            JavaPlugin.getPlugin(GeoIPAPIPlugin.class).getSLF4JLogger().warn("Failed to get {} country", readableAddress, throwable);
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[0], playerNames, new LinkedList<>());
        }
        return Collections.emptyList();
    }
}
