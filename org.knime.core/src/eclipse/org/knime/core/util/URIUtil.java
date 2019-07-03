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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.httpclient.URIException;
import org.knime.core.node.NodeLogger;

/**
 * Utility class to create, e.g., encoded URIs.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 * @noreference This class is not intended to be referenced by clients.
 */
public final class URIUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URIUtil.class);

    /**
     * Converts a url-string to an encoded (if not already encoded) URI.
     *
     * @param url the string encode and turn into an URI
     * @return the URI
     */
    public static URI createEncodedURI(final String url) {
        URI uri;
        try {
            if (isURLEncoded(url.toString())) {
                //URL is already encoded
                uri = new URI(url);
            } else {
                //URL is not yet encoded!
                uri = createAndEncodeURI(url.toString());
            }
        } catch (URISyntaxException | URIException | UnsupportedEncodingException e) {
            LOGGER.error("The URL '" + url + "' couldn't be turned into an URI", e);
            return null;
        }
        return uri;
    }

    /**
     * Converts a URL to an encoded (if not already encoded) URI.
     *
     * @param url the string encode and turn into an URI
     * @return the URI
     */
    public static URI createEncodedURI(final URL url) {
        return createEncodedURI(url.toString());
    }

    private static boolean isURLEncoded(final String urlString) throws UnsupportedEncodingException {
        return !URLDecoder.decode(urlString, StandardCharsets.UTF_8.name()).equals(urlString);
    }

    private static URI createAndEncodeURI(final String uri) throws URIException, URISyntaxException {
        return new URI(new org.apache.commons.httpclient.URI(uri, false, StandardCharsets.UTF_8.name()).toString());
    }
}
