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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Mark Vainomaa
 */
public final class GeoIPDownloader {
    private static final Logger logger = LoggerFactory.getLogger(GeoIPDownloader.class);
    private static final MessageDigest md5Digest;

    /* The URL where Geo IP database will be downloaded */
    private static final String DATABASE_URL = "https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz";
    private static final String DATABASE_URL_MD5 = "https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz.md5";
    /* GeoIP database name on local filesystem */
    private static final String DATABASE_FILE_NAME = "geoip-country.db";
    /* Last downloaded archive md5sum */
    public static final String LAST_ARCHIVE_MD5 = "last-downloaded.md5";
    /* GeoIP database name in archive */
    private static final String DATABASE_FILE_NAME_IN_ARCHIVE = "GeoLite2-Country.mmdb";

    /**
     * Sets up database to specified directory
     *
     * @param remoteHash Expected database archive md5 checksum,.
     * @param directory Directory where database should be in
     * @param checkHash Whether checksum should be checked
     * @throws IOException When any I/O error happens
     */
    public static Path setupDatabase(String remoteHash, Path directory, boolean checkHash) throws IOException {
        Files.createDirectories(directory);

        Path databaseFile = directory.resolve(DATABASE_FILE_NAME);
        Path databaseArchiveChecksumFile = directory.resolve(LAST_ARCHIVE_MD5);
        if (Files.exists(databaseFile))
            return databaseFile;
        logger.trace("Database file {} does not exist, downloading from {}", databaseFile, DATABASE_URL);

        // Download md5 checksum if not set
        if (remoteHash == null) {
            remoteHash = getRemoteDatabaseMd5Hash();
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
        // Note: apparently MaxMind's checksum is unreliable, http://web.archive.org/web/20190723150206/https:/twitter.com/_mikroskeem/status/1153673166834864130
        if (!localHash.equalsIgnoreCase(remoteHash)) {
            // Does not match
            String message = String.format("Local database archive checksum does not match remote (%s != %s)!", localHash, remoteHash);
            if (checkHash) {
                throw new IOException(message);
            } else {
                logger.warn(message);
            }
        }

        // Download
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(databaseArchive)))) {
            TarArchiveEntry entry = null;
            while ((entry = tarStream.getNextTarEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith(DATABASE_FILE_NAME_IN_ARCHIVE))
                    break;
            }

            if (entry == null)
                throw new IOException("Could not find " + DATABASE_FILE_NAME_IN_ARCHIVE + " from " + DATABASE_URL);

            try (ReadableByteChannel tarChannel = Channels.newChannel(tarStream)) {
                try (FileChannel fileChannel = new FileOutputStream(databaseFile.toFile()).getChannel()) {
                    fileChannel.transferFrom(tarChannel, 0, Long.MAX_VALUE);
                }
            }
        } catch (ConnectException e) {
            throw new IOException("Failed to connect to " + DATABASE_URL, e);
        }

        // Write database md5sum
        Files.write(databaseArchiveChecksumFile, remoteHash.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        return databaseFile;
    }

    public static String getRemoteDatabaseMd5Hash() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(DATABASE_URL_MD5).openStream()))) {
            return reader.lines().collect(Collectors.joining("\n")).trim();
        } catch (ConnectException e) {
            throw new IOException("Failed to connect to " + DATABASE_URL_MD5, e);
        }
    }

    public static String getLocalDatabaseMd5Hash(Path databaseDirectory) throws IOException {
        Path checksumFile = databaseDirectory.resolve(LAST_ARCHIVE_MD5);
        if (Files.exists(checksumFile)) {
            return new String(Files.readAllBytes(checksumFile));
        }
        return null;
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
