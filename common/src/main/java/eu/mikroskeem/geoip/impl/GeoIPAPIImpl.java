/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.impl;


import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.pivovarit.function.ThrowingFunction;
import eu.mikroskeem.geoip.GeoIPAPI;
import eu.mikroskeem.geoip.common.GeoIPDownloader;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Mark Vainomaa
 */
public class GeoIPAPIImpl implements GeoIPAPI {
    private static final Logger logger = LoggerFactory.getLogger(GeoIPAPIImpl.class);
    private final ExecutorService executorService = new ForkJoinPool();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path databaseFile;
    private final ExpiringMap<InetAddress, Optional<String>> cache;
    private boolean initialized = false;
    private DatabaseReader dbReader = null;
    private Thread updaterThread = null;

    public GeoIPAPIImpl(Path databaseFile, long expires, TimeUnit timeUnit) {
        this.databaseFile = databaseFile;
        this.cache = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(expires, timeUnit)
                .entryLoader(this::loadCountry)
                .build();
    }

    public GeoIPAPIImpl(Path databaseFile) {
        this(databaseFile, 5, TimeUnit.MINUTES);
    }

    private Optional<String> loadCountry(InetAddress address) {
        if (!this.initialized) {
            throw new IllegalStateException("API is not initialized!");
        }

        try {
            lock.readLock().lock();
            return Optional.ofNullable(dbReader.country(address).getCountry().getIsoCode());
        } catch (AddressNotFoundException e) {
            return Optional.empty();
        } catch (IOException | GeoIp2Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private DatabaseReader initializeReader() {
        logger.debug("Initializing database reader");
        try {
            return new DatabaseReader.Builder(databaseFile.toFile()).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GeoIP database reader", e);
        }
    }

    public void initializeDatabase() {
        if (this.initialized) {
            return;
        }

        this.dbReader = initializeReader();
        this.initialized = true;
    }

    public void setupUpdater(boolean checkHash, long interval, TimeUnit timeUnit) {
        if (this.updaterThread != null) {
            return;
        }

        this.updaterThread = new UpdaterThread(this, checkHash, interval, timeUnit);
        this.updaterThread.start();
    }

    public void updateDatabase(ThrowingFunction<UpdateInfo, Boolean, Exception> databaseUpdater) {
        DatabaseReader oldReader = this.dbReader;
        boolean locked = false;
        boolean updated = false;
        try {
            Path updateFile = databaseFile.getParent().resolve("update.db");
            Path updateArchiveFileMd5 = databaseFile.getParent().resolve("update-archive.md5");

            try {
                // Run update function
                boolean success = databaseUpdater.apply(new UpdateInfo(updateFile, updateArchiveFileMd5));
                if (!success) {
                    return;
                }

                if (Files.notExists(updateFile) || Files.notExists(updateArchiveFileMd5)) {
                    throw new IllegalStateException("Database update file or checksum does not exist!");
                }
            } catch (Exception e) {
                // Update failed
                logger.warn("Database update failed", e);
                try {
                    Files.deleteIfExists(updateFile);
                } catch (Exception ignored) {}
                return;
            }

            // Close database reader, update file and reinitialize reader
            logger.debug("Locking database access for update...");
            lock.writeLock().lock();
            locked = true;
            try {
                Files.move(updateFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.warn("Failed to replace {} with {}", this.databaseFile, updateFile, e);
                return;
            }

            Path lastDownloadedArchiveMd5 = databaseFile.getParent().resolve(GeoIPDownloader.LAST_ARCHIVE_MD5);
            try {
                Files.move(updateArchiveFileMd5, lastDownloadedArchiveMd5, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.warn("Failed to copy last downloaded database archive checksum from {} to {}", lastDownloadedArchiveMd5, updateArchiveFileMd5, e);
            }

            // Move succeeded, replace reader and clear cache
            try {
                this.dbReader = initializeReader();
            } catch (Exception e) {
                this.dbReader = oldReader;
                logger.error("Failed to replace GeoIP database reader, using old reader", e);
                return;
            }

            // Clear cache and mark update done
            this.cache.clear();
            updated = true;
        } finally {
            if (locked) {
                logger.debug("Unlocking database after update");
                lock.writeLock().unlock();
            }
            if (updated) {
                executorService.submit(() -> {
                    try {
                        oldReader.close();
                    } catch (Exception e) {
                        logger.warn("Failed to close old database reader", e);
                    }
                });
                logger.info("Database updated successfully");
            }
        }
    }

    @Nullable
    @Override
    public String getCountryByIP(@NonNull InetAddress ipAddress) {
        try {
            return getCountryByIPAsync(ipAddress).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public String getCountryByIP(@NonNull String ipAddress) {
        try {
            return getCountryByIPAsync(ipAddress).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public CompletableFuture<@Nullable String> getCountryByIPAsync(@NonNull InetAddress ipAddress) {
        return CompletableFuture.supplyAsync(() -> cache.get(ipAddress).orElse(null), executorService);
    }

    @NonNull
    @Override
    public CompletableFuture<@Nullable String> getCountryByIPAsync(@NonNull String ipAddress) {
        return CompletableFuture
                .supplyAsync(() -> this.getInetAddressByName(ipAddress), executorService)
                .thenApplyAsync(this::getCountryByIP, executorService);
    }

    public void close() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.warn("Failed to shut down database thread pool", e);
        }
        try {
            dbReader.close();
        } catch (Exception e) {
            logger.warn("Failed to close database reader", e);
        }
    }

    private InetAddress getInetAddressByName(String rawAddress) {
        try {
            return InetAddress.getByName(rawAddress);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Path getDatabaseFile() {
        return databaseFile;
    }
}
