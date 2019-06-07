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

    /**
     * Type of a KNIME entity specified as a query parameter.
     */
    public static enum Type {
            /**
             * An object from a space (either component, workflow group, workflow or file).
             */
            OBJECT,

            /**
             * A KNIME node
             */
            NODE,

            /**
             * A KNIME extension
             */
            EXTENSION,

            /**
             * If the type couldn't be determined or is not known.
             */
            UNKNOWN;
    }

    private KnimeURIUtil() {
        //utility class
    }

    /**
     * Guesses the type of the entity given the URL.
     *
     * Implemented according to URL Scheme https://knime-com.atlassian.net/wiki/spaces/SPECS/pages/577404968/URL+Scheme
     * (06/12/19)
     *
     * @param knimeURI the URI to extract the type from
     *
     * @return the type
     */
    public static Type guessEntityType(final URI knimeURI) {
        if (!isHubURI(knimeURI)) {
            return Type.UNKNOWN;
        }
        final String[] split = splitPath(knimeURI);
        if (split.length > 2) {
            // handle extensions and nodes
            if (split[1].equalsIgnoreCase("extensions")) {
                // user/extension/extId and user/extension/extId/version
                if (split.length == 3 || split.length == 4) {
                    return Type.EXTENSION;
                } else /* user/extension/extId/version/nodeFactory */ if (split.length == 5) {
                    return Type.NODE;
                }
            }
            // handle space objects.
            else if (split[1].equalsIgnoreCase("space")) {
                return Type.OBJECT;
            }
        }

        return Type.UNKNOWN;
    }

    /**
     * @return canonical path without leading "/".
     */
    private static String[] splitPath(final URI knimeURI) {
        final String path = knimeURI.getPath();
        return (path.startsWith("/") ? path.substring(1, path.length()) : path).split("/");
    }

    public static URI getSpaceEntityEndpointURI(final URI knimeURI, final boolean download) {
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI, "/knime/rest/v4/repository/Users"
            + knimeURI.getPath().replaceFirst("/space", "") + (download ? ":data" : ""));
    }

    public static URI getNodeEndpointURI(final URI knimeURI) {
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI, "/nodes/" + splitPath(knimeURI)[4]);
    }

    public static URI getExtensionEndpointURI(final URI knimeURI) {
        // TODO needs fix if we change backend API
        return getHubEndpoint(knimeURI, "/extensions/" + splitPath(knimeURI)[2]);
    }

    /**
     * Determines whether it a URI referencing a resource on the KNIME Community Workflow Hub.
     *
     * @param knimeURI the URI to check
     *
     * @return <code>true</code> if it's a 'hub'-URI
     */
    public static boolean isHubURI(final URI knimeURI) {
        return knimeURI.getHost().matches("(hub|hubdev)\\.knime\\.com");
    }

    private static URI getHubEndpoint(final URI knimeURI, final String path) {
        if (isHubURI(knimeURI)) {
            String scheme = knimeURI.getScheme();
            String host = "api." + knimeURI.getHost();
            try {
                return new URI(scheme, host, path, null);
            } catch (URISyntaxException ex) {
                //should never happen -> implementation problem
                throw new RuntimeException(ex);
            }
        } else {
            return knimeURI;
        }
    }

}
