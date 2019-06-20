/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
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

        (address != null ? GeoIPAPI.INSTANCE.getCountryByIPAsync(address) : CompletableFuture.completedFuture("(unknown)")).thenAcceptAsync(countryCode -> {
            sender.sendMessage("Address " + (address != null ? address.getHostAddress() : "(null?)") + " country: " + countryCode);
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
