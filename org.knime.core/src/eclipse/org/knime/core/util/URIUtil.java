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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

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

    private static final Pattern ENCODED_PLUS_SIGN = Pattern.compile("%2B", Pattern.LITERAL);

    /**
     * Converts a url-string (without query parameters!) to an encoded (if not already encoded) URI.
     *
     * @param urlWithoutQueryParams the string to encode and turn into an URI
     * @return the URI or an empty optional if it couldn't be encoded
     */
    public static Optional<URI> createEncodedURI(final String urlWithoutQueryParams) {
        //make sure that no query parameters are contained
        if (urlWithoutQueryParams == null || urlWithoutQueryParams.contains("?")) {
            return Optional.empty();
        }
        URI uri;
        try {
            if (isURLEncoded(urlWithoutQueryParams.toString())) {
                //URL is already encoded
                uri = new URI(urlWithoutQueryParams);
            } else {
                //URL is not yet encoded!
                uri = createAndEncodeURI(urlWithoutQueryParams.toString());
            }
        } catch (URISyntaxException | URIException | UnsupportedEncodingException e) {
            LOGGER.error("The URL '" + urlWithoutQueryParams + "' couldn't be turned into an URI", e);
            return Optional.empty();
        }
        return Optional.ofNullable(uri);
    }

    /**
     * Converts a URL (without query parameters!) to an encoded (if not already encoded) URI.
     *
     * @param url the string encode and turn into an URI
     * @return the URI or an empty optional if it couldn't be encoded (or the passed URL is <code>null</code>)
     */
    public static Optional<URI> createEncodedURI(final URL url) {
        if(url == null) {
            return Optional.empty();
        }
        return createEncodedURI(url.toString());
    }

    private static boolean isURLEncoded(final String urlStringWithoutQueryParams) throws UnsupportedEncodingException {
        //URL is already encoded if it contains a '%' which is not allowed in names of KNIME objects
        //see org.knime.workbench.explorer.filesystem.ExplorerFileSystem.validateFilename(String)
        return urlStringWithoutQueryParams.contains("%");
    }

    private static URI createAndEncodeURI(final String uri) throws URIException, URISyntaxException {
        String encoded = new org.apache.commons.httpclient.URI(uri, false, StandardCharsets.UTF_8.name()).toString();
        //'+' signs are usually taken literally (i.e. not encoded) in the path of an URL.
        //However, the above function encodes it nevertheless and the line below reverts that
        encoded = ENCODED_PLUS_SIGN.matcher(encoded).replaceAll("+");
        return new URI(encoded);
    }
}
