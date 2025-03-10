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
 *   Jun 18, 2019 (Moritz Heine, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.workbench.util;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workspace utility class that can be used to set and get the workspace version.
 * <p/>
 * This version can be used to determine if an update of AP occurred or if an older workspace has been opened with a
 * newer AP version. The version is usually in the format 'yyyyMMdd'.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public final class KNIMEWorkspaceUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(KNIMEWorkspaceUtil.class);

    // TODO change
    private static final String PLUGIN_ID = "org.knime.workbench.core";

    private static final String WORKSPACE_VERSION = "knime.workspace.version";

    private KNIMEWorkspaceUtil() {
    }

    /**
     * Returns the currently version of the workspace, or -1 if the version does not exist yet.
     * <p/>
     * Versions are usually in the format 'yyyyMMdd'.
     *
     * @return The version.
     */
    public synchronized static int getVersion() {
        final int version = DefaultScope.INSTANCE.getNode(PLUGIN_ID).getInt(WORKSPACE_VERSION, -1);

        if (version != -1) {
            setVersion(version);
            return version;
        }

        final IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(PLUGIN_ID);

        return pref.getInt(WORKSPACE_VERSION, -1);
    }

    /**
     * Sets the version that shall be persisted for the workspace.
     *
     * @param version The version in format 'yyyyMMdd'.
     */
    public synchronized static void setVersion(final int version) {
        final IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(PLUGIN_ID);

        pref.putInt(WORKSPACE_VERSION, version);
        try {
            pref.flush();
        } catch (BackingStoreException e) {
            LOGGER.info("Couldn't write version into preference file", e);
        }
    }

}
