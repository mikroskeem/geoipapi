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
