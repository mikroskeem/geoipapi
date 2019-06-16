/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.impl;

import eu.mikroskeem.geoip.GeoIPAPI;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Mark Vainomaa
 */
public final class ImplInjector {
    private static boolean initialized;
    private static Field apiInstanceField = null;

    public static void initialize() throws Exception {
        if (initialized)
            return;

        apiInstanceField = GeoIPAPI.class.getField("INSTANCE");
        apiInstanceField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(apiInstanceField, apiInstanceField.getModifiers() & ~Modifier.FINAL);
        initialized = true;
    }

    public static void setApi(GeoIPAPI api) {
        if (!initialized) {
            try {
                initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (GeoIPAPI.INSTANCE != null)
            throw new RuntimeException("API is already initialized!");

        try {
            apiInstanceField.set(null, api);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
