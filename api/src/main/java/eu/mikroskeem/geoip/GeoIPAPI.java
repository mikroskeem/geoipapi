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
