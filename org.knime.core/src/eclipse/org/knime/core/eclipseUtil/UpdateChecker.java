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
 * ---------------------------------------------------------------------
 *
 * Created on 31.03.2014 by thor
 */
package org.knime.core.eclipseUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.Platform;
import org.knime.core.util.ThreadLocalHTTPAuthenticator;
import org.knime.core.util.proxy.URLConnectionFactory;

/**
 * Checks for updates to new feature releases. The information is extract from a file "newRelease.txt" on the update
 * site (e.g. http://update.knime.com/analytics-platform/3.4/newRelease.txt). The contents of the file are as follows:
 *
 * <pre>
 * http://update.knime.org/analytics-platform/3.5   linux   x86_64   true   KNIME 3.5
 * http://update.knime.org/analytics-platform/3.5   macosx  x86      true   KNIME 3.5
 * http://update.knime.org/analytics-platform/3.5   win32   x86      false  KNIME 3.5
 *  ...
 * </pre>
 *
 * Each line contains a URL for a specific combination of OS and architecture. The boolean value indicates whether
 * a direct update is possible or not. The last entry lists a name for the update. Note that the delimiter between
 * the five columns <b>must</b> be a tab and not spaces. This also means the name must not contain tabs, only spaces.
 *
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.2
 */
public class UpdateChecker {
    private static final int URL_INDEX = 0;

    private static final int OS_INDEX = 1;

    private static final int ARCH_INDEX = 2;

    private static final int POSSIBLE_INDEX = 3;

    private static final int NAME_INDEX = 4;

    private static final int SHORT_NAME_INDEX = 5;

    /**
     * Information about an available update.
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     */
    public static class UpdateInfo {
        private final URI m_uri;

        private final String m_name;

        private final String m_shortName;

        private final boolean m_updatePossible;

        /**
         * Creates a new update info.
         *
         * @param uri the URI to the new update site, never <code>null</code>
         * @param name a name for the update, never <code>null</code>
         * @param shortName a short name for the update (e.g. version number)
         * @param updatePossible <code>true</code> if an direct update is possible, <code>false</code> otherwise
         * @since 4.0
         */
        public UpdateInfo(final URI uri, final String name, final String shortName, final boolean updatePossible) {
            m_uri = uri;
            m_name = name;
            m_shortName = shortName;
            m_updatePossible = updatePossible;
        }

        /**
         * Returns the URI for the new update site.
         *
         * @return an update site URI, never <code>null</code>
         */
        public URI getUri() {
            return m_uri;
        }

        /**
         * Returns the name for the update, e.g. "KNIME Analytics Platform 4.1".
         *
         * @return a name for the update, never <code>null</code>
         */
        public String getName() {
            return m_name;
        }

        /**
         * Returns a short name for the update, e.g. "4.1"
         *
         * @return a short name for the update
         * @since 4.0
         */
        public String getShortName() {
            return m_shortName;
        }

        /**
         * Returns whether a direct update is possible or not.
         *
         * @return <code>true</code> if a direct update is possible, <code>false</code> otherwise
         */
        public boolean isUpdatePossible() {
            return m_updatePossible;
        }
    }

    private UpdateChecker() {

    }

    /**
     * Checks the given existing update site if it contains the newRelease.txt and returns the new release information.
     * If no new release is available, <code>null</code> is returned.
     *
     * @param updateURI an URI of an existing update site
     * @return the update information or <code>null</code>
     * @throws IOException if an I/O error occurs while reading data from the server
     * @throws URISyntaxException if the new update site URI is invalid
     */
    public static UpdateInfo checkForNewRelease(final URI updateURI) throws IOException, URISyntaxException {
        final var nextVersionURL = new URL(updateURI.toString() + "/newRelease.txt");
        HttpURLConnection conn = null;
        try (final var c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            conn = (HttpURLConnection)URLConnectionFactory.getConnection(nextVersionURL);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (final var in =
                new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    final var parts = line.split("\t");
                    if (parts.length >= 6 && Platform.getOS().equals(parts[OS_INDEX])
                        && Platform.getOSArch().equals(parts[ARCH_INDEX])) {
                        return new UpdateInfo(new URI(parts[URL_INDEX]), parts[NAME_INDEX], parts[SHORT_NAME_INDEX],
                            Boolean.parseBoolean(parts[POSSIBLE_INDEX]));
                    }
                }
            }
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
