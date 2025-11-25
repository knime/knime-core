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
 *   9 Dec 2022 (leon.wenzler): created
 */
package org.knime.core.util.urlresolve;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.WWWFormCodec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.pathresolve.URIToFileResolve;

/**
 * Utility class for the KNIME URL resolving. Includes utilities for the item versions and conversion to URL.
 * Introduced to narrow Exception types from IOException to ResourceAccessException.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @since 5.3
 */
public final class URLResolverUtil {

    /**
     * Converts the URI builder for the space URI to a URL. Used in KnimeUrlResolvers.
     *
     * @param uriBuilder the repository URI builder
     * @return built URL
     * @throws ResourceAccessException
     */
    static URL toURL(final URIBuilder uriBuilder) throws ResourceAccessException {
        try {
            return toURL(uriBuilder.build().normalize());
        } catch (URISyntaxException ex) {
            throw new ResourceAccessException("Cannot build URL: " + ex.getMessage(), ex);
        }
    }

    /**
     * Wraps the URL to URI conversion into a {@link ResourceAccessException}.
     *
     * @param url URL to convert
     * @return converted URI
     * @throws ResourceAccessException in case the URL cannot be converted to a URI
     */
    public static URI toURI(final URL url) throws ResourceAccessException {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new ResourceAccessException("Cannot build URI: " + ex.getMessage(), ex);
        }
    }

    /**
     * Wraps the URI to URL conversion into a {@link ResourceAccessException}, used e.g. in {@link URIToFileResolve}.
     *
     * @param uri input URI
     * @return converted URL
     * @throws ResourceAccessException in case the URI cannot be converted to a URL
     */
    public static URL toURL(final URI uri) throws ResourceAccessException {
        try {
            return URIPathEncoder.UTF_8.encodePathSegments(uri.toURL());
        } catch (MalformedURLException ex) {
            throw new ResourceAccessException("Cannot convert URI to URL: " + ex.getMessage(), ex);
        }
    }

    /**
     * Converts the given path to a URL.
     * <ul>
     *   <li>Path segments are encoded: {@code "file://tmp/ä/Ä.txt"} -> {@code "file://tmp/%C3%A4/%C3%84.txt"}</li>
     *   <li>Windows UNC paths are represented with four forward slashes: {@code file:////UncHost/path/to/file.csv}</li>
     * </ul>
     * @param path
     * @return converted URL
     * @throws ResourceAccessException
     */
    static URL toURL(final Path path) throws ResourceAccessException {
        return toURL(path.toFile().toURI());
    }

    /**
     * Hides constructor.
     */
    private URLResolverUtil() {
    }

    // from core LinkType.LATEST_VERSION.getIdentifier()
    private static final String MOST_RECENT_IDENTIFIER = "most-recent";

    // from core LinkType.VERSION_QUERY_PARAM
    private static final String VERSION_QUERY_PARAM_NAME = "version";

    // from core LinkType.SPACE_VERSION_QUERY_PARAM, needed for backwards compatibility
    private static final String SPACE_VERSION_QUERY_PARAM_NAME = "spaceVersion";

    /**
     * Applies the given {@link ItemVersion} to the given query parameters by adding/replacing the version. In case the
     * given version is not {@link ItemVersion#isVersioned}, the parameter is removed.
     *
     * @param version to apply to the URI
     * @param queryParams the params to apply the version to
     * @return the query parameters with the version applied
     * @since 5.5
     */
    public static List<NameValuePair> applyTo(final ItemVersion version, final List<NameValuePair> queryParams) {
        return URLQueryParamUtil.replaceParameterValue(queryParams, VERSION_QUERY_PARAM_NAME,
            getQueryParameterValue(version).orElse(null), NameValuePair::getName, NameValuePair::getValue,
            BasicNameValuePair::new);
    }

    /**
     * Calls {@link #applyTo(ItemVersion, List)} on the given {@link URI} and replaces its
     * query parameters using the {@link URIBuilder} from Apache HC5.
     *
     * @param version to apply to the URI
     * @param uri the {@link URI} to process
     * @return the modified URI
     * @throws URISyntaxException if the URI with the applied {@link ItemVersion} cannot be parsed
     * @since 5.10
     */
    public static URI applyTo(final ItemVersion version, final URI uri) throws URISyntaxException {
        if (version == null || uri == null) {
            return uri;
        }
        final var builder = new URIBuilder(uri);
        builder.setParameters(applyTo(version, builder.getQueryParams()));
        return builder.build();
    }

    /**
     * Adds the version information to the query parameters consumer if the given non-{@code null} version is
     * {@link ItemVersion#isVersioned}.
     *
     * @param version non-{@code null} version to add to parameter consumer
     * @param parameterConsumer consumer for version key-value pair
     * @since 5.5
     */
    public static void addVersionQueryParameter(final ItemVersion version,
        final BiConsumer<String, String> parameterConsumer) {
        getQueryParameterValue(version) //
            .ifPresent(value -> parameterConsumer.accept(VERSION_QUERY_PARAM_NAME, value));
    }

    /**
     * Tries to parse the {@link ItemVersion} from the given nullable query parameter string.
     *
     * <br>
     * Note: this method supports parsing the legacy space version parameter as well
     *
     * @param queryParams nullable query parameter string to parse the version from
     * @return the parsed {@link ItemVersion} or {@link Optional#empty()} if no version parameter is present
     * @throws IllegalArgumentException if conflicting or unparsable version parameters are present
     * @since 5.5
     */
    public static Optional<ItemVersion> parseVersion(final String queryParams) {
        final var params = parseQuery(queryParams);
        return parseVersion(params);
    }

    private static Optional<ItemVersion> parseVersion(final Collection<NameValuePair> queryParams) {
        return fromQueryParameters(queryParams, VERSION_QUERY_PARAM_NAME, SPACE_VERSION_QUERY_PARAM_NAME,
            queryParams.toString());
    }

    private static List<NameValuePair> parseQuery(final String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }
        return WWWFormCodec.parse(query, StandardCharsets.UTF_8);
    }

    /**
     * Migrates query parameters that might contain a "Space Version" query parameter to item versioning.
     *
     * @param queryParams nullable query parameters that might contain the legacy parameter query parameter
     * @return query parameters that has no spaceVersion parameter. If the input has a spaceVersion=X, the output has
     *         version=X. If given {@code null}, {@code null} is returned.
     * @since 5.5
     */
    // necessary for backwards compatibility
    public static String migrateFromSpaceVersion(final String queryParams) {
        final var params = parseQuery(queryParams);
        return parseVersion(params).map(v -> applyTo(v, params))
            .map(p -> WWWFormCodec.format(p, StandardCharsets.UTF_8)).orElse(queryParams);
    }

    // Helper methods

    // old HubItemVersion#fromQuery (more or less)
    private static Optional<ItemVersion> fromQueryParameters(final Collection<NameValuePair> queryParams,
        final String versionParam, final String spaceVersionParam, final String queryForLogging) {
        if (queryParams == null || queryParams.isEmpty()) {
            return Optional.empty();
        }
        ItemVersion found = null;
        for (final var param : queryParams) {
            final var name = param.getName();
            final var isItemVersion = versionParam.equals(name);
            boolean isLegacySpaceVersion = spaceVersionParam.equals(name);
            if (isItemVersion || isLegacySpaceVersion) {
                final var value = CheckUtils.checkArgumentNotNull(param.getValue(),
                    "Version parameter value for \"%s\" cannot be empty in query parameters \"%s\"", name,
                    queryForLogging);
                final var versionHere = ItemVersion.convertToItemVersion(value);
                if (found != null && !found.equals(versionHere)) {
                    throw new IllegalArgumentException(
                        "Conflicting version parameters in query parameters \"%s\": \"%s\" vs. \"%s\""
                            .formatted(queryForLogging, found, versionHere));
                }
                found = versionHere;
            }
        }
        return Optional.ofNullable(found);
    }

    /**
     * Gets the non-{@code null} version as a query parameter.
     *
     * @param version the non-{@code null} version to get the query parameter for
     * @return the value for the version query parameter, e.g., "most-recent" or "4". Empty if this refers to the
     *         current state.
     */
    private static Optional<String> getQueryParameterValue(final ItemVersion version) {
        CheckUtils.checkArgumentNotNull(version, "Version cannot be null");
        return version.match( //
            Optional::empty, // nothing to return for current-state, like the old method
            () -> Optional.of(MOST_RECENT_IDENTIFIER), //
            v -> Optional.of(Integer.toString(v)) //
        );
    }

}
