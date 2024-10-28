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
 *   Oct 28, 2024 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.file.PathUtils;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;

/**
 * An {@linkplain IEarlyStartup early startup} instance that checks if the system supports path lengths longer than 260
 * characters. By default, on Windows, the longest path supported is 260 characters, but longer paths can be enabled via
 * a registry key. The class tests this property by creating a short-living file with a long path, logging an error
 * if necessary.
 *
 * For details see the
 * <a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file#maximum-path-length-limitation"> Windows
 * documentation</a> and the KNIME FAQ.
 *
 * This class was added as part of AP-23487.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class LongPathCheckerEarlyStartup implements IEarlyStartup {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LongPathCheckerEarlyStartup.class);

    private static final int WINDOWS_MAX_PATH_LENGTH = 260;

    private static Boolean longPathsSupported;

    @Override
    public void run() {
        if (longPathsSupported == null) {
            final var areLongPathsSupported = checkLongPathTempFolder();
            LOGGER.debugWithFormat("Checked for long path support: %s", (areLongPathsSupported ? "yes" : "no"));
            if (!areLongPathsSupported) {
                LOGGER.error("""
                        The system does not support long paths (longer than %d characters). \
                        You will most likely run into some issues, especially with Python-based extensions. \
                        Refer to the KNIME documentation for more information: %s"""//
                    .formatted(WINDOWS_MAX_PATH_LENGTH, ExternalLinks.LONG_PATH_FAQ_URL));
            }
            longPathsSupported = areLongPathsSupported; // NOSONAR setting static variable
        }
    }

    /**
     * Tests for long path support by creating nested directories until the path length exceeds
     * {@value #WINDOWS_MAX_PATH_LENGTH} characters.
     *
     * @return {@code true} if long paths are supported, {@code false} otherwise.
     */
    private static boolean checkLongPathTempFolder() {
        Path problematicPath = null;
        Path tempFolder = null;
        try {
            tempFolder = Files.createTempDirectory("knime-long-path-check"); // NOSONAR (public folder)
            final var readme = tempFolder.resolve("readme.txt");
            Files.writeString(readme, """
                    This is a test folder created by KNIME Analytics Platform %s to check for long path support.
                    File was created on %s. This folder is short-living, it can be safely deleted by the user.
                    Learn more at %s
                    """.formatted(KNIMEConstants.VERSION,
                ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME), ExternalLinks.LONG_PATH_FAQ_URL));
            // Create folders until the path length exceeds WINDOWS_MAX_PATH_LENGTH characters.
            var longPath = tempFolder;
            do {
                longPath = longPath.resolve("long-folder-name");
                problematicPath = longPath;
                Files.createDirectories(longPath);
            } while (longPath.toString().length() < WINDOWS_MAX_PATH_LENGTH + "long-folder-name".length() + 20);
        } catch (IOException e) {
            if (problematicPath != null
                && problematicPath.toAbsolutePath().toString().length() > WINDOWS_MAX_PATH_LENGTH) {
                return false;
            }
            LOGGER.error("Could not create long path test folder: " + e.getMessage(), e);
            return false;
        } finally {
            try {
                PathUtils.deleteDirectory(tempFolder);
            } catch (IOException e) {
                LOGGER.error("Could not delete test folder: " + e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Returns whether the system supports long paths.
     *
     * @return {@code true} if long paths are supported, {@code false} otherwise.
     */
    public static boolean isLongPathSupported() {
        if (longPathsSupported == null) {
            checkLongPathTempFolder();
        }
        return longPathsSupported.booleanValue();
    }

}
