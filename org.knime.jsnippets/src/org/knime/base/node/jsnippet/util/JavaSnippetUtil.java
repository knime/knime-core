/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   16.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 * Utility method for the Java Snippet nodes.
 * <p>
 * This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class JavaSnippetUtil {

    /* Name of javasnippet jar download cache directories */
    private static final String JAR_CACHE_DIR_NAME = "javasnippet-jar-download-cache";

    private JavaSnippetUtil() {
        // private constructor to prevent instantiation.
    }

    /**
     * Create a temporary directory for all urls from all javasnippets. Downloaded jars are saved in a subfolder named
     * by the hexadecimal of the hashCode of the URL.
     * @throws IOException if the directory for the Jar cache cannot be created
     */
    private static final File getCacheDir() throws IOException {
        final File workflowTempDir = FileUtil.getWorkflowTempDir();

        final File cacheDir = new File(workflowTempDir, JAR_CACHE_DIR_NAME);
        Files.createDirectories(cacheDir.toPath());

        return cacheDir;
    }

    /**
     * Resolve a remote .jar URL (e.g. file relative to "local-copy-from-server" workflow. File still on server.),
     * potentially downloading the file to a temporary directory, if not done so already.
     *
     * @param url URL of the .jar file.
     * @return The downloaded file or file from cache.
     * @throws InvalidSettingsException If the URL is invalid or could not be opened or downloaded.
     * @throws UnsupportedEncodingException
     */
    private static final File resolveRemoteFile(final URL url)
        throws InvalidSettingsException, UnsupportedEncodingException {
        // knime://knime.workflow/../foo/bar.jar -> "/../foo/bar.jar"
        final String file = URLDecoder.decode(url.getFile(), "UTF-8");
        CheckUtils.checkSetting(StringUtils.endsWithIgnoreCase(file, ".jar"), "Not a .jar URL: %s", url.toString());

        final String filename = FilenameUtils.getName(file); // "/../foo/bar.jar" -> "bar.jar"

        File cacheForUrl;
        try {
            cacheForUrl = new File(getCacheDir(), Integer.toString(url.toString().hashCode(), 16));
        } catch (IOException ex) {
            throw new InvalidSettingsException(ex);
        }

        final File jarFile = new File(cacheForUrl, filename);
        if (jarFile.exists()) {
            // already downloaded
            return jarFile;
        }

        /* File not in cache, download it to cache */
        try (InputStream urlIn = FileUtil.openStreamWithTimeout(url)) {
            FileUtils.copyToFile(urlIn, jarFile);
        } catch (IOException e) {
            throw new InvalidSettingsException("Cannot download jar from URL " + url.toString() + ": " + e.getMessage(),
                e);
        }

        return jarFile;
    }

    /**
     * Convert file location to File. Also accepts file in URL format (e.g. local drop files as URL).
     *
     * @param location The location string.
     * @return The file to the location
     * @throws InvalidSettingsException if argument is null, empty or the file does not exist.
     */
    public static final File toFile(final String location) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(location), "Invalid (empty) jar file location");
        File result;
        try {
            final URL url = new URL(location);
            final Path p = FileUtil.resolveToPath(url);

            if (p == null) {
                // not a local path (see javadoc of resolveToPath)
                result = resolveRemoteFile(url);
            } else {
                result = p.toFile();
            }
        } catch (IOException | URISyntaxException mue) {
            // (especially MalformedURLException)
            result = new File(location);
        }
        CheckUtils.checkSetting(result != null && result.exists(), "Can't read file \"%s\"; invalid class path",
            location);

        return result;
    }
}
