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
 *   May 28, 2019 (hornm): created
 */
package org.knime.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Utility class to deal with URIs referencing KNIME-specific resources (e.g. hub) potentially with some additional
 * query parameters. Those query parameters are helpful, e.g., to transfer the information describing a KNIME entity
 * (e.g., a component, extensions or workflow) between the browser and the AP (e.g., via drag'n'drop).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 * @noreference This class is not intended to be referenced by clients.
 */
public final class KnimeURIUtil {

    private static final int NODE_FAC_PATH_IDX = 4;

    private static final int EXT_ID_PATH_IDX = 2;

    private static final Pattern HUB_HOST = Pattern.compile("(hub|hubdev)\\.knime\\.com");

    /**
     * Type of a KNIME entity specified as a query parameter.
     */
    public static enum HubEntityType {

            /**
             * An object from a space (either component, workflow group, workflow or file).
             */
            // matches <not-empty>/space(/<not-empty>)+ (with optional / at the beginning)
            OBJECT("/+[^/]++/space/([^/]++/)*[^/]++"),

            /**
             * A KNIME node
             */
            // matches <not-empty>/extensions/<not-empty>/<not-empty>/<not-empty> (with optional / at the beginning)
            NODE("/+[^/]++/extensions/([^/]++/){2}[^/]++"),

            /**
             * A KNIME extension
             */
            // matches <not-empty>/extensions/<not-empty> and <not-empty>/extensions/<not-empty>/<not-empty>
            // (with optional / at the beginning)
            EXTENSION("/+[^/]++/extensions/([^/]++/){0,1}[^/]++"),

            /**
             * If the type couldn't be determined or is not known.
             */
            // IMPORTANT: has to be last, since it matches everything.
            UNKNOWN(".*");

        /** The pattern specified w.r.t. https://knime-com.atlassian.net/wiki/spaces/SPECS/pages/577404968/URL+Scheme */
        private final Pattern m_pattern;

        private HubEntityType(final String regex) {
            m_pattern = Pattern.compile(regex);
        }

        private boolean matches(final String path) {
            return m_pattern.matcher(path).matches();
        }

        /**
         * Returns the type of the entity given the URL.
         *
         * Implemented according to URL Scheme
         *
         * @see <a href="https://knime-com.atlassian.net/wiki/spaces/SPECS/pages/577404968/URL+Scheme">URL scheme
         *      (06/12/19)</a>
         *
         * @param knimeURI the URI to extract the type from
         *
         * @return the type
         */
        public static HubEntityType getHubEntityType(final URI knimeURI) {
            if (!isHubURI(knimeURI)) {
                return HubEntityType.UNKNOWN;
            }
            final String path = knimeURI.getPath();
            for (final HubEntityType t : HubEntityType.values()) {
                if (t.matches(path)) {
                    return t;
                }
            }
            // dead code since unkown matches everything
            return HubEntityType.UNKNOWN;
        }

    }

    private KnimeURIUtil() {
        //utility class
    }

    /**
     * @return canonical path without leading "/".
     */
    private static String[] splitPath(final URI knimeURI) {
        final String path = knimeURI.getPath();
        return (path.startsWith("/") ? path.substring(1, path.length()) : path).split("/");
    }

    /**
     * Returns the object entity endpoint URI.
     *
     * @param knimeURI the knime URI
     * @return the object entity endpoint's URI
     */
    public static URI getObjectEntityEndpointURI(final URI knimeURI) {
        if (!isHubURI(knimeURI)) {
            return knimeURI;
        }
        checkURIValidity(knimeURI, HubEntityType.OBJECT);
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI,
            "/knime/rest/v4/repository/Users" + knimeURI.getPath().replaceFirst("/space/", "/"));
    }

    /**
     * Returns the node entities endpoint URI.
     *
     * @param knimeURI the knime URI
     * @return the node entity endpoint's URI
     */
    public static URI getNodeEndpointURI(final URI knimeURI) {
        if (!isHubURI(knimeURI)) {
            return knimeURI;
        }
        checkURIValidity(knimeURI, HubEntityType.NODE);
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI, "/nodes/" + splitPath(knimeURI)[NODE_FAC_PATH_IDX]);
    }

    /**
     * Returns the extensions endpoint URI
     *
     * @param knimeURI the knime URI
     * @return the extensions endpoint's URI
     */
    public static URI getExtensionEndpointURI(final URI knimeURI) {
        if (!isHubURI(knimeURI)) {
            return knimeURI;
        }
        checkURIValidity(knimeURI, HubEntityType.EXTENSION);
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI, "/extensions/" + splitPath(knimeURI)[EXT_ID_PATH_IDX]);
    }

    /**
     * Determines whether it a URI referencing a resource on the KNIME Community Workflow Hub.
     *
     * @param knimeURI the URI to check
     *
     * @return <code>true</code> if it's a 'hub'-URI
     */
    public static boolean isHubURI(final URI knimeURI) {
        return HUB_HOST.matcher(knimeURI.getHost()).matches();
    }

    private static void checkURIValidity(final URI knimeURI, final HubEntityType expectedType)
        throws IllegalArgumentException {
        if (!expectedType.matches(knimeURI.getPath())) {
            throw new IllegalArgumentException(
                "The provided URI does not match the expected type of " + expectedType.name());
        }
    }

    private static URI getHubEndpoint(final URI knimeURI, final String path) {
        final String scheme = knimeURI.getScheme();
        final String host = "api." + knimeURI.getHost();
        try {
            return new URI(scheme, host, path, null);
        } catch (URISyntaxException ex) {
            //should never happen -> implementation problem
            throw new RuntimeException(ex);
        }
    }

}
