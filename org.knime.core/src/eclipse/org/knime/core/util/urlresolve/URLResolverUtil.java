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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.URIQueryParamUtil;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.CurrentState;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.MostRecent;
import org.knime.core.util.hub.SpecificVersion;
import org.knime.core.util.pathresolve.URIToFileResolve;

/**
 * Utility class for the KNIME URI resolving. Includes utilities for the item versions and conversion to URL.
 * Introduced to narrow Exception types from IOException to ResourceAccessException.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @since 5.3
 */
public final class URLResolverUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URLResolverUtil.class);

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

    // utilities to work with item version

    // from core LinkType.LATEST_VERSION.getIdentifier()
    private static final String MOST_RECENT_IDENTIFIER = "most-recent";

    // from core LinkType.LATEST_STATE.getIdentifier()
    private static final String CURRENT_STATE_IDENTIFIER = "current-state";

    // from core LinkType.VERSION_QUERY_PARAM
    private static final String VERSION_QUERY_PARAM_NAME = "version";

    // from core LinkType.SPACE_VERSION_QUERY_PARAM, needed for backwards compatibility
    private static final String SPACE_VERSION_QUERY_PARAM_NAME = "spaceVersion";

    // these methods formerly lived in the HubItemVersion class

    /**
     * Applies the given {@link ItemVersion} to the given URI by adding/replacing the version.
     * In case the given version is not {@link ItemVersion#isVersioned}, the parameter is removed.
     *
     * @param version to apply to the URI
     * @param uri non-{@code null} URI to apply the version
     * @return the URI with the version applied
     * @since 5.5
     */
    public static URI applyTo(final ItemVersion version, final URI uri) {
        return URIQueryParamUtil.applyTo(uri, VERSION_QUERY_PARAM_NAME, getQueryParameterValue(version).orElse(null));
    }

    /**
     * Adds the version information to the query parameters consumer if the given version is
     * {@link ItemVersion#isVersioned}.
     *
     * @param version version to add to parameter consumer
     * @param parameterConsumer consumer for version key-value pair
     * @since 5.5
     */
    public static void addVersionQueryParameter(final ItemVersion version,
        final BiConsumer<String, String> parameterConsumer) {
        match(version, //
            c -> { }, // nothing to do for current-state, like the old method
            m -> parameterConsumer.accept(VERSION_QUERY_PARAM_NAME, MOST_RECENT_IDENTIFIER), //
            v -> parameterConsumer.accept(VERSION_QUERY_PARAM_NAME, Integer.toString(v.version())) //
        );
    }

    /**
     * Tries to parse the {@link ItemVersion} from the given non-{@code null} URI.
     *
     * @param uri to parse the version from
     * @return the parsed {@link ItemVersion} or {@link Optional#empty()} if the version could not be parsed
     * @since 5.5
     */
    public static Optional<ItemVersion> parseVersion(final URI uri) {
        // the old method did not care about the scheme (that it is a knime:// URL), so we don't either
        CheckUtils.checkArgumentNotNull(uri);
        return fromQueryParameters(VERSION_QUERY_PARAM_NAME, new URIBuilder(uri).getQueryParams(), uri::toString);
    }

    /**
     * Migrates an URI that might contain a "Space Version" query parameter to item versioning.
     *
     * @param uri nullable URI that might contain the legacy parameter query parameter
     * @return URI that has no spaceVersion parameter. If the input has a spaceVersion=X, the output has version=X. If
     *         given {@code null}, {@code null} is returned.
     * @since 5.5
     */
    // necessary for backwards compatibility
    public static URI migrateFromSpaceVersion(final URI uri) {
        if (uri == null) {
            return null;
        }
        return new URIBuilder(uri).getQueryParams().stream()
            .filter((final NameValuePair nvp) -> nvp.getName().equals(SPACE_VERSION_QUERY_PARAM_NAME)).findFirst()
            .map(NameValuePair::getValue) //
            .map((final String versionValue) -> URIQueryParamUtil.applyTo(uri, SPACE_VERSION_QUERY_PARAM_NAME, VERSION_QUERY_PARAM_NAME,
                versionValue))
            .orElse(uri);
    }

    // Helper methods

    /**
     * Parses the first occurrence of the "version" query parameter from the given query string into an ItemVersion.
     *
     * @param query query string to parse ItemVersion from
     * @return
     */
    private static Optional<ItemVersion> fromQueryParameters(final String versionQueryParam,
        final Collection<NameValuePair> queryParams, final Supplier<String> uriForLogging) {
        if (queryParams != null && !queryParams.isEmpty()) {
            // parse "version" parameter from query string and parse its value into an ItemVersion
            final var versionParams = queryParams.stream()
                .filter(p -> versionQueryParam.equals(p.getName())).toList();
            if (versionParams.size() > 1) {
                // TODO exception? debug? try first successful one?
                // HubItemVersion method threw IllegalArgumentException in case of multiple inconsistent _values_
                LOGGER.warn(() ->
                    "Multiple \"version\" query parameters found in URI query string \"%s\", taking first one"
                    .formatted(uriForLogging.get()));
            }
            for (final var param : versionParams) {
                if (versionQueryParam.equals(param.getName())) {
                    final var value = param.getValue();
                    CheckUtils.check(value != null, IllegalArgumentException::new,
                        () -> "\"%s\" parameter cannot be empty in query parameters of \"%s\""
                            .formatted(versionQueryParam, uriForLogging.get()));
                    return match(value);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the version as a query parameter.
     *
     * @param version the version to get the query parameter for
     * @return the value for the version query parameter, e.g., "most-recent" or "4". Empty if this refers to the
     *         current state.
     */
    private static Optional<String> getQueryParameterValue(final ItemVersion version) {
        return matchFn(version, //
            c -> Optional.empty(), //
            m -> Optional.of(MOST_RECENT_IDENTIFIER), //
            v -> Optional.of(Integer.toString(v.version())) //
        );
    }

    /**
     * Matches one of the functions with the given version and applies it.
     *
     * @param <T> return type of the functions
     * @param version version to apply functions to
     * @param currentStateFn function to apply if given current-state version
     * @param mostRecentFn function to apply if given most-recent version
     * @param versionFn function to apply if given fixed version number
     * @return function return value
     */
    private static <T> T matchFn(final ItemVersion version, final Function<CurrentState, T> currentStateFn,
        final Function<MostRecent, T> mostRecentFn, final Function<SpecificVersion, T> versionFn) {
        // good candidate for pattern-matching switch in next Java version (preview in Java 17)
        if (version instanceof CurrentState cs) {
            return currentStateFn.apply(cs);
        }
        if (version instanceof MostRecent mr) {
            return mostRecentFn.apply(mr);
        }
        return versionFn.apply((SpecificVersion)version);
    }

    private static void match(final ItemVersion version, final Consumer<CurrentState> currentStateFn,
        final Consumer<MostRecent> mostRecentFn, final Consumer<SpecificVersion> versionFn) {
        // good candidate for pattern-matching switch in next Java version (preview in Java 17)
        if (version instanceof CurrentState cs) {
            currentStateFn.accept(cs);
        } else if (version instanceof MostRecent mr) {
            mostRecentFn.accept(mr);
        } else {
            versionFn.accept((SpecificVersion)version);
        }
    }

    /**
     * Matches the given query parameter value potentially representing an {@link ItemVersion} to the proper
     * {@link ItemVersion} if possible.
     *
     * @param itemVersionParamValue query parameter value to match to {@link ItemVersion}
     * @return {@link ItemVersion} if it can be matched (parsed), otherwise {@link Optional#empty()}
     */
    private static Optional<ItemVersion> match(final String itemVersionParamValue) {
        if (CURRENT_STATE_IDENTIFIER.equals(itemVersionParamValue)) {
            return Optional.of(new CurrentState());
        }
        if (MOST_RECENT_IDENTIFIER.equals(itemVersionParamValue)) {
            return Optional.of(new MostRecent());
        }
        try {
            return Optional.of(new SpecificVersion(Integer.parseUnsignedInt(itemVersionParamValue)));
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }
}
