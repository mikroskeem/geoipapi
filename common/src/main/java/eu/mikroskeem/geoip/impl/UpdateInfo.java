/*
 * Copyright (c) 2019-2020 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.geoip.impl;

import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
public class UpdateInfo {
    private final Path updateFile;
    private final Path updateArchiveHashFile;

    public UpdateInfo(Path updateFile, Path updateArchiveHashFile) {
        this.updateFile = updateFile;
        this.updateArchiveHashFile = updateArchiveHashFile;
    }

    public Path getUpdateFile() {
        return updateFile;
    }

    public Path getUpdateArchiveHashFile() {
        return updateArchiveHashFile;
    }
}
