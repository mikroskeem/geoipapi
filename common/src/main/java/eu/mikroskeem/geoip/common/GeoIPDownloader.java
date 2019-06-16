/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.common;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Mark Vainomaa
 */
public final class GeoIPDownloader {
    private final static Logger logger = LoggerFactory.getLogger(GeoIPDownloader.class);
    private final static MessageDigest md5Digest;

    /* The URL where Geo IP database will be downloaded */
    private final static String DATABASE_URL = "https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz";
    private final static String DATABASE_URL_MD5 = "https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz.md5";
    /* GeoIP database name on local filesystem */
    private final static String DATABASE_FILE_NAME = "geoip-country.db";
    /* GeoIP database name in archive */
    private final static String DATABASE_FILE_NAME_IN_ARCHIVE = "GeoLite2-Country.mmdb";

    /**
     * Sets up database to specified directory
     *
     * @param directory Directory where database should be in
     * @throws IOException When any I/O error happens
     */
    public static Path setupDatabase(Path directory) throws IOException {
        Files.createDirectories(directory);

        Path databaseFile = directory.resolve(DATABASE_FILE_NAME);
        if (Files.exists(databaseFile))
            return databaseFile;
        logger.trace("Database file {} does not exist, downloading from {}", databaseFile, DATABASE_URL);

        String remoteHash;
        // Download md5 checksum
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(DATABASE_URL_MD5).openStream()))) {
            remoteHash = reader.lines().collect(Collectors.joining("\n"));
        } catch (ConnectException e) {
            throw new IOException("Failed to connect to " + DATABASE_URL_MD5, e);
        }

        // Download database
        byte[] databaseArchive;
        try (InputStream download = new URL(DATABASE_URL).openStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = download.read(buf)) > 0;) {
                baos.write(buf, 0, n);
            }
            databaseArchive = baos.toByteArray();
        }

        // Calculate md5 hash of database file
        byte[] localHashRaw = md5Digest.digest(databaseArchive);
        String localHash = toHexString(localHashRaw);

        // Compare
        if (!localHash.equalsIgnoreCase(remoteHash)) {
            // Does not match, error out
            throw new IOException(String.format("Local database archive checksum does not match remote (%s != %s)!", localHash, remoteHash));
        }

        // Download
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(databaseArchive)))) {
            TarArchiveEntry entry = null;
            while ((entry = tarStream.getNextTarEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith("GeoLite2-Country.mmdb"))
                    break;
            }

            if (entry == null)
                throw new IOException("Could not find " + DATABASE_FILE_NAME_IN_ARCHIVE + " from " + DATABASE_URL);

            ReadableByteChannel channel = Channels.newChannel(tarStream);
            try (FileChannel fileChannel = new FileOutputStream(databaseFile.toFile()).getChannel()) {
                fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
            }
        } catch (ConnectException e) {
            throw new IOException("Failed to connect to " + DATABASE_URL, e);
        }

        return databaseFile;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(Integer.toString((b & 0xFF) + 0x100, 16).substring(1));
        }
        return hexString.toString();
    }

    static {
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new AssertionError("Failed to get MD5 digest!", e);
        }
    }
}
