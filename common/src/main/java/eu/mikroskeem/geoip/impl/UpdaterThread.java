/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
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
public class UpdaterThread extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(UpdaterThread.class);
    private final long updateCheckInterval;
    private final GeoIPAPIImpl apiImpl;

    public UpdaterThread(GeoIPAPIImpl apiImpl, long interval, TimeUnit timeUnit) {
        super("GeoIPAPI database updater");
        this.updateCheckInterval = timeUnit.toMillis(interval);
        this.apiImpl = apiImpl;
    }

    @Override
    public void run() {
        while (true) {
            // Do update check
            logger.info("Checking for database update...");
            update: {
                // Get local database hash
                Path databaseFile = this.apiImpl.getDatabaseFile();
                Path databaseUpdateFile;
                String localDatabaseHash;
                String remoteDatabaseHash;

                try {
                    localDatabaseHash = GeoIPDownloader.getLocalDatabaseMd5Hash(databaseFile.getParent());
                } catch (IOException e) {
                    logger.warn("Failed to check for database update: unable to get local database md5sum", e);
                    break update;
                }

                // Get remote database hash
                try {
                    remoteDatabaseHash = GeoIPDownloader.getRemoteDatabaseMd5Hash();
                } catch (IOException e) {
                    logger.warn("Failed to check for database update: unable to get remote database md5sum", e);
                    break update;
                }

                if (localDatabaseHash != null && localDatabaseHash.equalsIgnoreCase(remoteDatabaseHash)) {
                    // No updates
                    logger.info("No update available");
                    break update;
                }

                // Update is available, download
                logger.info("GeoIP database update is available, downloading...");
                try {
                    Path tempDirectory = Files.createTempDirectory("geoipapi-dbupdate-");
                    databaseUpdateFile = GeoIPDownloader.setupDatabase(remoteDatabaseHash, tempDirectory);
                    logger.info("Done! Updating database shortly...");
                } catch (IOException e) {
                    logger.warn("Failed to download update!", e);
                    break update;
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
            }

            // Pause
            try {
                Thread.sleep(this.updateCheckInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
