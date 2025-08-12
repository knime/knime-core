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
 *   Nov 3, 2022 (leonard.woerteler): created
 */
package org.knime.core.util;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;

/**
 * A KNIME URL uses the {@code knime:} protocol and is used to identify resources inside a KNIME process.
 * While all KNIME URIs are technically absolute URLs, the {@link URL#getAuthority() Authority} component can contain
 * <ul>
 *   <li>either the Mount ID of a Mountpoint inside an Analytics Platform (e.g. {@code LOCAL} or
 *       {@code MegaCorp-Business-Hub})</li>
 *   <li>or a {@code knime.<xyz>} signifier which makes the URL relative to the current {@code <xyz>} location of the
 *       code the URI is resolved in.</li>
 * </ul>
 *
 * @author Leonard Wörteler, KNIME GmbH
 */
public enum KnimeUrlType {

    /**
     * A URL that identifies resources by their location relative to the current node, e.g.
     * {@code knime://knime.node/<???>}.
     */
    NODE_RELATIVE(CoreConstants.NODE_RELATIVE),

    /**
     * A URL that locates resources by relative to the current workflow's root directory, e.g.
     * {@code knime://knime.workflow/data/subdir/measurements.csv} or
     * {@code knime://knime.workflow/../neighbor-workflow}.
     */
    WORKFLOW_RELATIVE(CoreConstants.WORKFLOW_RELATIVE),

    /**
     * A URL that locates resources relative to the current Hub space, e.g.
     * {@code knime://knime.space/some_group/workflow17}.
     */
    HUB_SPACE_RELATIVE(CoreConstants.SPACE_RELATIVE),

    /**
     * A URL that locates resources relative to the current mountpoint, e.g.
     * {@code knime://knime.mountpoint/some_group/workflow17}.
     */
    MOUNTPOINT_RELATIVE(CoreConstants.MOUNTPOINT_RELATIVE),

    /**
     * An absolute mountpoint URL with a mount ID as its {@link URL#getAuthority() authority}, e.g.
     * {@code knime://LOCAL/some%20group/some_workflow/workflow.knime} or
     * {@code knime://My-Business-Hub/Users/john.doe/project123-space/some-component}
     */
    MOUNTPOINT_ABSOLUTE(null);

    /**
     * Checks whether or not the given URL is a KNIME URL (under the {@code knime:} scheme) and returns the URL type
     * if it is.
     *
     * @param url URL to get the type of
     * @return KNIME URL type if applicable, {@code null} otherwise
     * @throws IllegalArgumentException if the KNIME URL is malformed (missing {@link URL#getAuthority()
     *         authority component})
     */
    public static Optional<KnimeUrlType> getType(final URL url) {
        if (!SCHEME.equalsIgnoreCase(url.getProtocol())) {
            return Optional.empty();
        }
        final var authority = CheckUtils.checkArgumentNotNull(url.getAuthority(),
            "KNIME URLs must contain an Authority, fount '%s'", url);
        for (final KnimeUrlType type : values()) {
            if (type.m_authority == null || type.m_authority.equalsIgnoreCase(authority)) {
                return Optional.of(type);
            }
        }
        throw new IllegalStateException(MOUNTPOINT_ABSOLUTE.name() + " should be the fallback.");
    }

    /**
     * Checks whether or not the URL represented by the given URI is a KNIME URL (under the {@code knime:} scheme) and
     * returns the URL type if it is.
     *
     * @param uri URI to get the type of
     * @return KNIME URL type if applicable, {@code null} otherwise
     * @throws IllegalArgumentException if the KNIME URL is malformed (missing {@link URI#getAuthority()
     *         authority component})
     */
    public static Optional<KnimeUrlType> getType(final URI uri) {
        if (!SCHEME.equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }
        final var authority = CheckUtils.checkArgumentNotNull(uri.getAuthority(),
            "KNIME URIs must contain an Authority, fount '%s'", uri);
        for (final KnimeUrlType type : values()) {
            if (type.m_authority == null || type.m_authority.equalsIgnoreCase(authority)) {
                return Optional.of(type);
            }
        }
        throw new IllegalStateException(MOUNTPOINT_ABSOLUTE.name() + " should be the fallback.");
    }

    /** URI scheme of KNIME URLs. */
    public static final String SCHEME = "knime";

    /**
     * Authority of the corresponding URLs, {@code null} for {@link #MOUNTPOINT_ABSOLUTE}.
     */
    private final String m_authority;

    KnimeUrlType(final String authority) {
        m_authority = authority;
    }

    /**
     * The {@link URL#getAuthority() authority component} of this KNIME URL type, {@code null} for
     * {{@link #MOUNTPOINT_ABSOLUTE}}.
     *
     * @return authority string, e.g. {@code "knime.workflow"}
     */
    public String getAuthority() {
        return m_authority;
    }

    /**
     * Checks whether this URL type is relative (i.e., not {@link #MOUNTPOINT_ABSOLUTE mountpoint absolute}).
     *
     * @return {@code true} if the URL type is relative, {@code false} otherwise
     */
    public boolean isRelative() {
        return m_authority != null;
    }
}
