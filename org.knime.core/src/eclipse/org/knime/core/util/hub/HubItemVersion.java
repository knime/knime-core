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
import java.util.ArrayList;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;

/**
 * Reference to a KNIME Hub item version. Provides utility methods for conversion to/from URI query parameters.
 *
 * @param linkType whether this refers to a fixed version, the latest version, or the staging area
 * @param versionNumber only for fixed version: the id of the version
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public record HubItemVersion(LinkType linkType, Integer versionNumber) {
    /**
     * @param linkType whether this refers to a fixed version, the latest version, or the staging area
     * @param versionNumber only for fixed version: the id of the version
     */
    public HubItemVersion {
        CheckUtils.checkArgumentNotNull(linkType);
        if(linkType == LinkType.FIXED_VERSION) {
            CheckUtils.checkNotNull(versionNumber);
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
     * @param uri to derive new URI from. Non-null.
     * @return source URI with Hub item version set
     */
    public URI applyTo(final URI uri) {
        CheckUtils.checkArgumentNotNull(uri);
        return replaceParam(uri, LinkType.VERSION_QUERY_PARAM, getQueryParameterValue().orElse(null));
    }

    /**
     * @return a to the staging area state
     */
    public static HubItemVersion currentState() {
        return new HubItemVersion(LinkType.LATEST_STATE, null);
    }

    /**
     * @return a reference to the newest version state
     */
    public static HubItemVersion latestVersion() {
        return new HubItemVersion(LinkType.LATEST_VERSION, null);
    }

    /**
     * @param versionNumber id of the version
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
     * @param knimeUri KNIME URI. Non-null.
     * @return the link type and item version. Item version is {@code null} for link types ≠ FIXED_VERSION
     * @throws IllegalArgumentException if the given URI is null.
     */
    public static HubItemVersion of(final URI knimeUri) {
        CheckUtils.checkArgumentNotNull(knimeUri);
        final var queryParams = new URIBuilder(knimeUri).getQueryParams();
        final var versionParam = queryParams.stream()//
            .filter(nvp -> nvp.getName().equals(LinkType.VERSION_QUERY_PARAM))//
            .findFirst();
        if (versionParam.isPresent()) {
            final var itemVersion = versionParam.get().getValue();
            return switch (itemVersion) {
                case "current-state" -> new HubItemVersion(LinkType.LATEST_STATE, null);
                case "most-recent" -> new HubItemVersion(LinkType.LATEST_VERSION, null);
                default -> new HubItemVersion(LinkType.FIXED_VERSION, Integer.parseInt(itemVersion));
            };
        } else {
            return new HubItemVersion(LinkType.LATEST_STATE, null);
        }
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
        var offset = params.stream().map(NameValuePair::getName).toList().indexOf(oldName);
        var insertAt = offset == -1 ? params.size() : offset;

        if(offset != -1) {
            params.remove(offset);
        }

        Optional.ofNullable(value)//
            .map(v -> new BasicNameValuePair(newName, v))//
            .ifPresent(pair -> params.add(insertAt, pair));

        try {
            return builder.setParameters(params).build();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
        //        // org.apache.hc.core5.net.URIBuilder encodes params=apple,banana into params=apple%2Cbanana that's why I use the UriBuilder here.
//        var builder = UriBuilder.fromUri(uri);
//        if(!Objects.equals(oldName, newName)) {
//            builder.replaceQueryParam(oldName, (Object) null);
//        }
//        return builder.replaceQueryParam(newName, value).build();
    }
}