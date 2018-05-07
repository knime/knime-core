/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 26, 2018 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Utility class to check integrity of Eclipse Configuration area. For details see AP-9165; the configuration area
 * needs to be 'private' to the user as it otherwise may cause trouble in multi-user setups.
 *
 * @author Bernd Wiswedel
 * @since 3.6
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ConfigurationAreaChecker {

    private static final String ERROR_APPENDIX = "This may lead to undesired effects in multi-user setups, see "
        + "KNIME FAQs: https://www.knime.com/faq#q34.";

    private ConfigurationAreaChecker() {
    }

    /**
     * @return the path of the eclipse config area as per {@link Platform#getConfigurationLocation()} or an empty
     *         optional if it can't be determined.
     */
    public static Optional<Path> getConfigurationLocationPath() {
        Location configLocation = Platform.getConfigurationLocation();
        if (configLocation != null) {
            URL configURL = configLocation.getURL();
            if (configURL != null) {
                String path = configURL.getPath();
                if (Platform.OS_WIN32.equals(Platform.getOS()) && path.matches("^/[a-zA-Z]:/.*")) {
                    // Windows path with drive letter => remove first slash
                    path = path.substring(1);
                }
                return Optional.of(Paths.get(path));
            }
        }
        return Optional.empty();
    }

    /**
     * Queues a scan of the config area of KNIME/Eclipse and checks if files are owned by the current user and writable,
     * or not owned by the current user and not writable. If there is anything suspicious it will print an error to the
     * logger.
     */
    public static void scheduleIntegrityCheck() {
        Thread thread = new Thread(() -> {
            Path configLocationPath = getConfigurationLocationPath().orElse(null);
            try {
                Thread.sleep(60 * 1000); // some delay to make sure log messages occur in console if run in UI
                long start = System.currentTimeMillis();
                String currentUser = System.getProperty("user.name");
                if (configLocationPath == null) {
                    getLogger().warnWithFormat(
                        "Path to configuration area could not be determined (location URL is \"%s\")",
                        Platform.getConfigurationLocation().getURL());
                } else {
                    getLogger().infoWithFormat("Configuration area is under %s", configLocationPath);

                    final UserPrincipal configLocationOwner = Files.getOwner(configLocationPath);
                    if (!Objects.equals(currentUser, configLocationOwner.getName())) {
                        getLogger().errorWithFormat(
                            "Configuration area (\"%s\") is not owned by current user \"%s\" (owned by \"%s\"). %s",
                            configLocationPath, currentUser, configLocationOwner.getName(), ERROR_APPENDIX);
                    } else {
                        List<Path> suspiciousFiles = findFilesNotOwnedByUser(configLocationPath);
                        if (!suspiciousFiles.isEmpty()) {
                            getLogger().errorWithFormat(
                                "Configuration area (\"%s\") contains files not owned by current user \"%s\". %s",
                                configLocationPath, currentUser, ERROR_APPENDIX);

                            getLogger().errorWithFormat("Suspicious files: %s",
                                ConvenienceMethods.getShortStringFrom(suspiciousFiles.stream()
                                    .map(ConfigurationAreaChecker::formatFileAndOwner).iterator(),
                                    suspiciousFiles.size(), 3));
                        }
                    }
                }

                long end = System.currentTimeMillis();
                getLogger().debugWithFormat("Configuration area check completed in %.1fs", (end - start) / 1000.0);
            } catch (InterruptedException ie) {
                // ignore
            } catch (IOException ioe) {
                getLogger().error(String.format("Can't check integrity of configuration area (\"%s\"): %s",
                    configLocationPath, ioe.getMessage()), ioe);
            } catch (UnsupportedOperationException uoe) {
                getLogger().debugWithFormat(
                    "File ownerships in configuration area (\"%s\") can't be checked -- file system does not support "
                        + "file ownerships: %s",
                    configLocationPath, ExceptionUtils.getRootCauseMessage(uoe));
            }
        }, "KNIME-ConfigurationArea-Checker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Utility function to string-format a file system path with the file owner.
     *
     * @param file A {@link Path} that points to a file.
     * @return the file's path and the file owner in a formatted string.
     */
    private static String formatFileAndOwner(final Path file) {
        try {
            return String.format("%s (owned by %s)", file, Files.getOwner(file));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Find a list of files in configuration area not owned by current user.
     */
    private static List<Path> findFilesNotOwnedByUser(final Path configLocationPath) throws IOException {
        final UserPrincipal configLocationOwner = Files.getOwner(configLocationPath);
        Set<Path> suspiciousFiles = new LinkedHashSet<>();

        // visits all files + dirs in the config location, skips problematic folders to avoid superfluous warnings
        FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                checkPath(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                throws IOException {
                if (checkPath(dir)) {
                    // all files in the subfolder will be problematic, too. Skip extra warnings.
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            /**
             * Checks file ownership. If not owned by current user, adds it to suspectFiles variable and returns true.
             */
            private boolean checkPath(final Path file) throws IOException {
                UserPrincipal owner = Files.getOwner(file);
                // writable property changes in sub directory or file ownership changes in directory
                if (!Objects.equals(configLocationOwner, owner)) {
                    suspiciousFiles.add(file);
                    return true;
                }
                return false;
            }
        };
        Files.walkFileTree(configLocationPath, fileVisitor);
        return new ArrayList<>(suspiciousFiles);
    }

    /**
     * Lazy getter of logger as code in this class might be called from static initializer in NodeLogger and/or
     * KNIMEConstants.
     *
     * @return logger instance
     */
    private static final NodeLogger getLogger() {
        return NodeLogger.getLogger(ConfigurationAreaChecker.class);
    }

}
