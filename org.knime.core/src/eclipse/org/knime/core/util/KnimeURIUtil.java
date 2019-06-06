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
import java.util.Optional;

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
     * Parameter determining the entity type, such as node, component etc.
     */
    public static final String ENTITY_TYPE_QUERY_PARAM = "knimeEntityType";

    /**
     * Parameter that specifies the node factory, e.g. to be dropped onto the workbench.
     */
    public static final String NODE_FACTORY_PARAM = "knimeNodeFactory";

    /**
     * Parameter that specifies the symbolic name of a bundle (i.e. as KNIME extension).
     */
    public static final String BUNDLE_SYMBOLIC_NAME_PARAM = "knimeBundleSymbolicName";

    /**
     * Parameter that specifies the symbolic name of a feature (i.e. as KNIME extension).
     */
    public static final String FEATURE_SYMBOLIC_NAME_PARAM = "knimeFeatureSymbolicName";

    /**
     * Parameter that specifies the name of a KNIME extension
     */
    public static final String BUNDLE_NAME_PARAM = "knimeBundleName";

    /**
     * Type of a KNIME entity specified as a query parameter.
     */
    public static enum Type {
            /**
             * A KNIME component (aka wrapped metanode).
             */
            COMPONENT,

            /**
             * A KNIME node
             */
            NODE,

            /**
             * If the type couldn't be determined or is not known.
             */
            UNKNOWN;
    }

    private KnimeURIUtil() {
        //utility class
    }

    /**
     * Extracts the entity type (e.g. component) from the URI's query parameters.
     *
     * @param knimeURI the URI to extract the type from
     *
     * @return the type
     */
    public static Type getEntityType(final URI knimeURI) {
        String p = getQueryParamValue(knimeURI, ENTITY_TYPE_QUERY_PARAM);
        if (p != null) {
            if (p.equalsIgnoreCase(Type.COMPONENT.name())) {
                return Type.COMPONENT;
            } else if (p.equalsIgnoreCase(Type.NODE.name())) {
                return Type.NODE;
            }
        }
        return Type.UNKNOWN;
    }

    /**
     * Extracts a value for a knime specific parameter from the URI.
     *
     * @param knimeURI the knime URI to extract the value from
     * @param key the key of the parameter
     * @return the value or an empty optional if there is no such parameter
     */
    public static Optional<String> getKnimeParamValue(final URI knimeURI, final String key) {
        String p = getQueryParamValue(knimeURI, key);
        return Optional.ofNullable(p);
    }

    /**
     * Extracts a URI from the original URI that represents the download link of the respective knime entity (e.g. a
     * component).
     *
     * @param knimeURI the URL to transform
     * @return the download URI or the passed URI itself if it's not referencing the hub
     */
    public static URI getDownloadURI(final URI knimeURI) {
        if (isHubURI(knimeURI)) {
            String scheme = knimeURI.getScheme();
            String host = "api." + knimeURI.getHost();
            String path = "/knime/rest/v4/repository/Users" + knimeURI.getPath().replaceFirst("/space", "") + ":data";
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

    private static String getQueryParamValue(final URI knimeURI, final String key) {
        String query = knimeURI.getQuery();
        if (query != null) {
            int index = query.indexOf(key);
            if (index >= 0) {
                index = query.indexOf("=", index);
                if (index >= 0) {
                    int indexAnd = query.indexOf("&", index);
                    if (indexAnd >= 0) {
                        return query.substring(index + 1, indexAnd);
                    } else {
                        return query.substring(index + 1);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param knimeURI the URI to transform
     * @return the URI with KNIME specific query parameters removed
     */
    public static URI getBaseURI(final URI knimeURI) {
        return removeQueryParams(knimeURI, ENTITY_TYPE_QUERY_PARAM, NODE_FACTORY_PARAM, BUNDLE_SYMBOLIC_NAME_PARAM,
            FEATURE_SYMBOLIC_NAME_PARAM, BUNDLE_NAME_PARAM);
    }

    private static URI removeQueryParams(final URI uri, final String... keys) {
        String uriString = uri.toString() + "&";
        for (String key : keys) {
            //removes e.g. "knimeEntityType=component&" from the URI
            //adding a '?' on a quantifier (?, * or +) makes it non-greedy,
            //i.e. matching as few characters as possible
            //here: match the very first '&' after the 'key'
            uriString = uriString.replaceFirst(key + ".*?&", "");
        }
        try {
            return new URI(uriString.substring(0, uriString.length() - 1));
        } catch (URISyntaxException ex) {
            //should never happen
            //otherwise something is wrong with the implementation
            throw new RuntimeException();
        }
    }

}
