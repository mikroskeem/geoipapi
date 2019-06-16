/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.impl;


import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import eu.mikroskeem.geoip.GeoIPAPI;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Vainomaa
 */
public class GeoIPAPIImpl implements GeoIPAPI {
    private final static Logger logger = LoggerFactory.getLogger(GeoIPAPIImpl.class);
    private final DatabaseReader dbReader;
    private ExpiringMap<InetAddress, Optional<String>> cache;

    public GeoIPAPIImpl(Path databaseFile, long expires, TimeUnit timeUnit) {
        try {
            this.dbReader = new DatabaseReader.Builder(databaseFile.toFile()).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GeoIP database reader", e);
        }

        cache = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(expires, timeUnit)
                .entryLoader(this::loadCountry)
                .build();
    }

    public GeoIPAPIImpl(Path databaseFile) {
        this(databaseFile, 5, TimeUnit.MINUTES);
    }

    private Optional<String> loadCountry(InetAddress address) {
        try {
            return Optional.of(dbReader.country(address).getCountry().getIsoCode());
        } catch (AddressNotFoundException e) {
            return Optional.empty();
        } catch (IOException | GeoIp2Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public String getCountryByIP(@NonNull InetAddress ipAddress) {
        return cache.get(ipAddress).orElse(null);
    }

    @Nullable
    @Override
    public String getCountryByIP(@NonNull String ipAddress) {
        try {
            return getCountryByIP(InetAddress.getByName(ipAddress));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            dbReader.close();
        } catch (Exception e) {
            logger.warn("Failed to close database reader", e);
        }
    }
}
