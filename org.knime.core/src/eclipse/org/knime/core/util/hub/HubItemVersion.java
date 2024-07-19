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
import java.util.ArrayList;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.WWWFormCodec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;

/**
 * Reference to a KNIME Hub item version. Provides utility methods for conversion to/from URI query parameters.
 *
 * @param linkType whether this refers to a fixed version, the latest version, or the staging area. Never {@code null}.
 * @param versionNumber only for fixed version: the id of the version
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
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
     */
    public URI applyTo(final URI uri) {
        CheckUtils.checkArgumentNotNull(uri);
        return replaceParam(uri, LinkType.VERSION_QUERY_PARAM, getQueryParameterValue().orElse(null));
    }

    /**
     * @return a to the staging area state
     */
    public static HubItemVersion currentState() {
        return CURRENT_STATE;
    }

    /**
     * @return a reference to the newest version state
     */
    public static HubItemVersion latestVersion() {
        return MOST_RECENT;
    }

    /**
     * @param versionNumber id of the version, larger than zero
     * @return a reference to a fixed version state
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
            boolean isLegacySpaceVersion = LinkType.LEGACY_SPACE_VERSION_QUERY_PARAM.equals(param.getName());
            if (isItemVersion || isLegacySpaceVersion) {
                final var value = CheckUtils.checkArgumentNotNull(param.getValue(),
                    "version parameter can't be empty in URL '%s'", url);

                final HubItemVersion versionHere;
                if (value.equals(isItemVersion ? LinkType.LATEST_STATE.getIdentifier()
                        : LinkType.LATEST_STATE.getLegacyIdentifier())) {
                    versionHere = CURRENT_STATE;
                } else if (value.equals(isItemVersion ? LinkType.LATEST_VERSION.getIdentifier()
                        : LinkType.LATEST_VERSION.getLegacyIdentifier())) {
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
     * @param uri URI that might contain a spaceVersion query parameter. Nullable.
     * @return URI that has no spaceVersion parameter. If the input has a spaceVersion=X, the output has version=X. If
     *         given {@code null}, {@code null} is returned.
     */
    public static URI migrateFromSpaceVersion(final URI uri) {
        if (uri == null) {
            return null;
        }
        var spaceVersion = new URIBuilder(uri).getQueryParams().stream()
            .filter(nvp -> nvp.getName().equals(LinkType.LEGACY_SPACE_VERSION_QUERY_PARAM)).findFirst()
            .map(NameValuePair::getValue);
        if (spaceVersion.isPresent()) {
            return replaceParam(uri, LinkType.LEGACY_SPACE_VERSION_QUERY_PARAM, LinkType.VERSION_QUERY_PARAM,
                spaceVersion.get());
        } else {
            return uri;
        }
    }

    private static URI replaceParam(final URI uri, final String name, final String value) {
        return replaceParam(uri, name, name, value);
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
     * Adds both the legacy {@code spaceVersion=[...]} and the new {@code version=[...]} query parameter to the given
     * builder if this version needs them.
     *
     * @param uriBuilder URI builder
     */
    public void addVersionToURI(final URIBuilder uriBuilder) {
        addVersionToURI(uriBuilder, true);
    }

    /**
     *
     * @param uri to derive new URI from. Non-null.
     * @param oldName Parameter to remove or replace. Non-null.
     * @param newName Parameter to hold the value if it is not null. Non-null.
     * @param value new value or null if the parameter is to be removed
     * @return new URI with the modified query parameter
     */
    private static URI replaceParam(final URI uri, final String oldName, final String newName, final String value) {
        CheckUtils.checkArgumentNotNull(uri);
        CheckUtils.checkArgumentNotNull(oldName);
        CheckUtils.checkArgumentNotNull(newName);

        var builder = new URIBuilder(uri);
        var params = new ArrayList<>(builder.getQueryParams());

        var multiValueParameter = params.stream().filter(nvp -> nvp.getValue().contains(",")).findAny();
        if (multiValueParameter.isPresent()) {
            throw new IllegalArgumentException("Cannot handle multi-valued query parameters. "
                + "Commas will be URL encoded, which means the parameter is interpreted as a single value parameter.");
        }

        var offset = params.stream().map(NameValuePair::getName).toList().indexOf(oldName);
        if (offset != -1) {
            params.remove(offset);
        }

        var insertAt = offset == -1 ? params.size() : offset;
        Optional.ofNullable(value)//
            .map(v -> new BasicNameValuePair(newName, v))//
            .ifPresent(pair -> params.add(insertAt, pair));

        try {
            return builder.setParameters(params).build();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}