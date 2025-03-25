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
 *   26 Jun 2023 (carlwitt): created
 */
package org.knime.core.util.hub;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.WWWFormCodec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.util.urlresolve.URLResolverUtil;

/**
 * Reference to a KNIME Hub item version. Provides utility methods for conversion to/from URI query parameters.
 *
 * @param linkType whether this refers to a fixed version, the latest version, or the staging area. Never {@code null}.
 * @param versionNumber only for fixed version: the id of the version
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 * @deprecated Use {@link ItemVersion} for version references and {@link URLResolverUtil} for URL query parameter handling
 * @see ItemVersion
 * @see URLResolverUtil
 */
@Deprecated(since = "5.5", forRemoval = true)
public record HubItemVersion(LinkType linkType, Integer versionNumber) {

    private static final HubItemVersion CURRENT_STATE = new HubItemVersion(LinkType.LATEST_STATE, null);

    private static final HubItemVersion MOST_RECENT = new HubItemVersion(LinkType.LATEST_VERSION, null);

    /**
     * @param linkType whether this refers to a fixed version, the latest version, or the staging area. Never
     *            {@code null}.
     * @param versionNumber only for fixed version: the id of the version
     * @throws IllegalArgumentException if the link type is null or if the link type is not equal to fixed version but a
     *             version number is specified.
     */
    public HubItemVersion {
        CheckUtils.checkArgumentNotNull(linkType);
        if(linkType == LinkType.FIXED_VERSION) {
            CheckUtils.checkNotNull(versionNumber);
            CheckUtils.checkArgument(versionNumber > 0, "Version number must be larger than zero, but was %d.",
                versionNumber);
        } else {
            CheckUtils.checkArgument(versionNumber == null,
                "Version number can only be specified for a fixed version.");
        }
    }

    /**
     * @return {@code false} if this refers to content in the staging area, {@code true} if this refers to content
     *         in a specific version or the latest version.
     */
    public boolean isVersioned() {
        return linkType != LinkType.LATEST_STATE;
    }

    /**
     * @return the value for the version query parameter, e.g., "most-recent" or "4". Empty if this refers to the
     *         staging area.
     */
    public Optional<String> getQueryParameterValue() {
        return Optional.ofNullable(linkType.getParameterString(versionNumber));
    }

    /**
     * Adds the item version query parameter to the given uri or removes it if this represents an item's current state.
     *
     * @param uri to derive new URI from. Non-null.
     * @return source URI, possibly with item version query parameter.
     * @see URLResolverUtil#applyTo(ItemVersion, java.util.List)
     */
    public URI applyTo(final URI uri) {
        CheckUtils.checkArgumentNotNull(uri);
        final var builder = new URIBuilder(uri);
        final var params = builder.getQueryParams();
        final var replaced = URLResolverUtil.applyTo(convert(this), params);
        try {
            return builder.setParameters(replaced).build();
        } catch (final URISyntaxException e) {
            // we just modify the query parameters and these should not cause a URISyntaxException
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return a to the staging area state
     * @see ItemVersion#currentState()
     */
    public static HubItemVersion currentState() {
        return CURRENT_STATE;
    }

    /**
     * @return a reference to the newest version state
     * @see ItemVersion#mostRecent()
     */
    public static HubItemVersion latestVersion() {
        return MOST_RECENT;
    }

    /**
     * @param versionNumber id of the version, larger than zero
     * @return a reference to a fixed version state
     * @see SpecificVersion#SpecificVersion(int)
     */
    public static HubItemVersion of(final int versionNumber) {
        return new HubItemVersion(LinkType.FIXED_VERSION, versionNumber);
    }

    /**
     * Extracts the item version from a URI.
     *
     * For instance
     * <ul>
     * <li>"knime://My-KNIME-Hub/*Rllck6Bn2-EaOR6d?version=3" -> (FIXED_VERSION, 3)</li>
     * <li>"knime://My-KNIME-Hub/*Rllck6Bn2-EaOR6d?version=most-recent" -> (LATEST_VERSION, null)</li>
     * </ul>
     *
     * @param knimeUrl KNIME URL. Non-null.
     * @return the link type and item version. Item version is {@code null} for link types ≠ FIXED_VERSION
     * @throws IllegalArgumentException if the given URL is null or the version cannot be determined
     * @see URLResolverUtil#parseVersion(URI)
     */
    public static Optional<HubItemVersion> of(final URI knimeUrl) {
        CheckUtils.checkArgumentNotNull(knimeUrl);
        return fromQuery(knimeUrl.getQuery(), knimeUrl.toString());
    }

    /**
     * Extracts the item version from a URL.
     *
     * For instance
     * <ul>
     * <li>"knime://My-KNIME-Hub/*Rllck6Bn2-EaOR6d?version=3" -> (FIXED_VERSION, 3)</li>
     * <li>"knime://My-KNIME-Hub/*Rllck6Bn2-EaOR6d?version=most-recent" -> (LATEST_VERSION, null)</li>
     * </ul>
     *
     * @param knimeUrl KNIME URL. Non-null.
     * @return the link type and item version. Item version is {@code null} for link types ≠ FIXED_VERSION
     * @throws IllegalArgumentException if the given URI is null or the version cannot be determined
     * @see URLResolverUtil#parseVersion(URI)
     */
    public static Optional<HubItemVersion> of(final URL knimeUrl) {
        CheckUtils.checkArgumentNotNull(knimeUrl);
        return fromQuery(knimeUrl.getQuery(), knimeUrl.toString());
    }

    private static Optional<HubItemVersion> fromQuery(final String query, final String url) { // NOSONAR
        if (query == null || query.isEmpty()) {
            return Optional.empty();
        }

        HubItemVersion found = null;
        for (final var param : WWWFormCodec.parse(query, StandardCharsets.UTF_8)) {
            final var isItemVersion = LinkType.VERSION_QUERY_PARAM.equals(param.getName());
            if (isItemVersion) {
                final var value = CheckUtils.checkArgumentNotNull(param.getValue(),
                    "version parameter can't be empty in URL '%s'", url);

                final HubItemVersion versionHere;
                if (value.equals(LinkType.LATEST_STATE.getIdentifier())) {
                    versionHere = CURRENT_STATE;
                } else if (value.equals(LinkType.LATEST_VERSION.getIdentifier())) {
                    versionHere = MOST_RECENT;
                } else {
                    try { // NOSONAR
                        versionHere = HubItemVersion.of(Integer.parseInt(value));
                    } catch (final NumberFormatException nfe) {
                        throw new IllegalArgumentException("Version specifier '" + value + "' not recognized in URL '"
                            + url + "'.", nfe);
                    }
                }

                if (found != null && !found.equals(versionHere)) {
                    throw new IllegalArgumentException("Conflicting version parameters in URL " + url);
                }
                found = versionHere;
            }
        }

        return Optional.ofNullable(found);
    }

    /**
     * Migrates an URI that might contain a "Space Version" query parameter to item versioning.
     *
     * @param uri URI that might contain a spaceVersion query parameter. Nullable.
     * @return URI that has no spaceVersion parameter. If the input has a spaceVersion=X, the output has version=X. If
     *         given {@code null}, {@code null} is returned.
     */
    // kept for backwards compatibility
    public static URI migrateFromSpaceVersion(final URI uri) {
        final var builder = new URIBuilder(uri);
        final var params = URLResolverUtil.migrateFromSpaceVersion(uri.getQuery());
        builder.setCustomQuery(params);
        try {
            return builder.build();
        } catch (final URISyntaxException e) {
            // we just modify the query parameters and these should not cause a URISyntaxException
            throw new IllegalStateException(e);
        }
    }

    /**
     * Adds the {@code version=[...]} query parameter to the given builder if this version needs them.
     *
     * @param uriBuilder URI builder
     * @since 5.0
     */
    public void addVersionToURI(final URIBuilder uriBuilder) {
        addVersionToURI(uriBuilder, false);
    }

    /**
     * Adds the new {@code version=[...]} query parameter and optionally the legacy {@code spaceVersion=[...]} to the
     * given builder if this version needs them.
     *
     * @param uriBuilder URI builder
     * @param addSpaceVersion if {@code true}, the {@code spaceVersion=[...]} is also being added
     * @since 5.3
     */
    public void addVersionToURI(final URIBuilder uriBuilder, final boolean addSpaceVersion) {
        if (isVersioned()) {
            if (linkType == LinkType.LATEST_VERSION) {
                uriBuilder.addParameter(LinkType.VERSION_QUERY_PARAM, LinkType.LATEST_VERSION.getIdentifier());
                if (addSpaceVersion) {
                    uriBuilder.addParameter(LinkType.LEGACY_SPACE_VERSION_QUERY_PARAM,
                        LinkType.LATEST_VERSION.getLegacyIdentifier());
                }
            } else {
                final var versionNo = versionNumber.toString();
                uriBuilder.addParameter(LinkType.VERSION_QUERY_PARAM, versionNo);
                if (addSpaceVersion) {
                    uriBuilder.addParameter(LinkType.LEGACY_SPACE_VERSION_QUERY_PARAM, versionNo);
                }
            }
        }
    }

    /**
     * Converts a {@link HubItemVersion} to an {@link ItemVersion}.
     *
     * @param version the version
     * @return the corresponding {@link ItemVersion}
     * @since 5.5
     */
    public static ItemVersion convert(final HubItemVersion version) {
        if (version == null) {
            return null;
        }
        return switch (version.linkType) {
            case LATEST_STATE -> ItemVersion.currentState();
            case LATEST_VERSION -> ItemVersion.mostRecent();
            case FIXED_VERSION -> new SpecificVersion(version.versionNumber());
        };
    }

    /**
     * Converts an {@link ItemVersion} to a {@link HubItemVersion}.
     *
     * @param version the version
     * @return the corresponding {@link HubItemVersion}
     * @since 5.5
     */
    public static HubItemVersion convert(final ItemVersion version) {
        if (version == null) {
            return null;
        }
        return version.match(cs -> currentState(), mr -> latestVersion(), sv -> of(sv.version()));
    }
}