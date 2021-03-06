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

import eu.mikroskeem.geoip.common.GeoIPDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Vainomaa
 */
public class DatabaseUpdaterTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdaterTask.class);
    private final String licenseKey;
    private final boolean checkHash;
    private final long updateCheckInterval;
    private final GeoIPAPIImpl apiImpl;

    public DatabaseUpdaterTask(GeoIPAPIImpl apiImpl, String licenseKey, boolean checkHash, long interval, TimeUnit timeUnit) {
        this.licenseKey = licenseKey;
        this.checkHash = checkHash;
        this.updateCheckInterval = timeUnit.toMillis(interval);
        this.apiImpl = apiImpl;
    }

    @Override
    public void run() {
        // Do update check
        logger.info("Checking for database update...");

        // Get local database hash
        Path databaseFile = this.apiImpl.getDatabaseFile();
        Path databaseUpdateFile;
        String localDatabaseHash;
        String remoteDatabaseHash;

        try {
            localDatabaseHash = GeoIPDownloader.getLocalDatabaseMd5Hash(databaseFile.getParent());
        } catch (IOException e) {
            logger.warn("Failed to check for database update: unable to get local database md5sum", e);
            return;
        }

        // Get remote database hash
        try {
            remoteDatabaseHash = GeoIPDownloader.getRemoteDatabaseMd5Hash(licenseKey);
        } catch (IOException e) {
            logger.warn("Failed to check for database update: unable to get remote database md5sum", e);
            return;
        }

        if (localDatabaseHash != null && localDatabaseHash.equalsIgnoreCase(remoteDatabaseHash)) {
            // No updates
            logger.info("No update available");
            return;
        }

        // Update is available, download
        logger.info("GeoIP database update is available, downloading...");
        try {
            Path tempDirectory = Files.createTempDirectory("geoipapi-dbupdate-");
            databaseUpdateFile = GeoIPDownloader.setupDatabase(remoteDatabaseHash, tempDirectory, checkHash, licenseKey);
            logger.info("Done! Updating database shortly...");
        } catch (IOException e) {
            logger.warn("Failed to download update!", e);
            return;
        }

        // Do the actual update now
        apiImpl.updateDatabase(updateInfo -> {
            Path updatePath = updateInfo.getUpdateFile();
            Path updateArchiveChecksumPath = updateInfo.getUpdateArchiveHashFile();
            Files.createDirectories(updatePath.getParent());
            Files.copy(databaseUpdateFile, updatePath, StandardCopyOption.REPLACE_EXISTING);
            Files.write(updateArchiveChecksumPath, remoteDatabaseHash.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return true;
        });

        // Delete update
        try {
            Files.deleteIfExists(databaseUpdateFile);
        } catch (Exception e) {
            logger.warn("Failed to delete update file", e);
        }

        // Pause
        try {
            Thread.sleep(this.updateCheckInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
