/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Geo IP API
 *
 * @author Mark Vainomaa
 */
public interface GeoIPAPI {
    /**
     * Gets country by IP
     *
     * @param ipAddress IP address to query
     * @return Country ISO code, or null if no country was found for given address
     */
    @Nullable
    String getCountryByIP(@NonNull InetAddress ipAddress);

    /**
     * Gets country by IP
     *
     * @param ipAddress IP address in a string form to query
     * @return Country ISO code, or null if no country was found for given address
     */
    @Nullable
    String getCountryByIP(@NonNull String ipAddress);

    /**
     * Gets country by IP asynchronously
     *
     * @param ipAddress IP address to query
     * @return Country ISO code, or null if no country was found for given address
     */
    @NonNull
    CompletableFuture<@Nullable String> getCountryByIPAsync(@NonNull InetAddress ipAddress);

    /**
     * Gets country by IP asynchronously
     *
     * @param ipAddress IP address in a string form to query
     * @return Country ISO code, or null if no country was found for given address
     */
    @NonNull
    CompletableFuture<@Nullable String> getCountryByIPAsync(@NonNull String ipAddress);

    /**
     * Instance of {@link GeoIPAPI} - initialized on runtime
     */
    @NonNull
    @Initialized
    GeoIPAPI INSTANCE = null;
}
