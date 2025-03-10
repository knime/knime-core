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
 *   Oct 30, 2024 (wiswedel): created
 */
package org.knime.core.workbench;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Constants for the KNIME Workbench plug-in.
 *
 * @since 5.5
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class WorkbenchConstants {

    /** The plug-in which historically has hosted the preferences. Preferences are read/written under this root. */
    public static final String WORKBENCH_PREFERENCES_PLUGIN_ID = "org.knime.workbench.explorer.view";

    /** The special "temp space" (used, e.g. for yellow bar editors) */
    public static final String TYPE_IDENTIFIER_TEMP_SPACE = "com.knime.explorer.tempspace";

    /** Preference constant for mount points for the Explorer (xml format). */
    public static final String P_EXPLORER_MOUNT_POINT_XML = "knime.explorer.mountpoint.xml";

    /**
     * Pref constant to link the original meta node to a newly defined template.
     */
    public static final String P_EXPLORER_LINK_ON_NEW_TEMPLATE = "knime.explorer.link_on_new_template";

    /** Preference constant for showing a warning dialog when connecting to an older server */
    public static final String P_SHOW_OLDER_SERVER_WARNING_DIALOG = "knime.explorer.show_older_server_warning";
    /** The default value for whether a warning dialog should appear when connecting to an older server or not */
    public static final boolean P_DEFAULT_SHOW_OLDER_SERVER_WARNING_DIALOG = true;

    /**
     * The scheme this file system is registered with (see extension point
     * "org.eclipse.core.filesystem.filesystems").
     */
    public static final String SCHEME = "knime";

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     */
    @SuppressWarnings("java:S5867") // Unicode character classes are explicitly not wanted here
    private static final Pattern MOUNTID_PATTERN = Pattern.compile("^[a-zA-Z](?:[.a-zA-Z0-9-]*[a-zA-Z0-9])?$");

    private WorkbenchConstants() {
    }

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     *
     * @param id the id to test
     * @return true if the id is valid (in terms of contained characters)
     *         independent of it may already be in use.
     */
    @SuppressWarnings("java:S1166") // the exception is expected control flow
    public static boolean isValidMountID(final String id) {
        if (id == null || id.isEmpty() || id.startsWith("knime.") || !MOUNTID_PATTERN.matcher(id).find()) {
            return false;
        }

        try {
            // this is the way we build URIs to reference server items - this must not choke.
            @SuppressWarnings("unused")
            final var ignored = new URI(SCHEME, id, "/test/path", null);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

}
