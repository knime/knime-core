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
 *   10 Mar 2025 (leonard.woerteler): created
 */
package org.knime.core.internal;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointHostFilter;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.knime.core.workbench.preferences.MountPointsPrefsSyncer;

/**
 * Initializes the mountpoints after the profiles have been loaded.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class MountTableInitializerEarlyStartup implements IEarlyStartup {

    @Override
    public void run() {
        // mount point filter from AP customization, default to allow all if absent
        final var filter = CorePlugin.getInstance().getCustomizationService() //
            .map(x -> (WorkbenchMountPointHostFilter)x.getCustomization().mountpoint()) //
            .orElse(WorkbenchMountPointHostFilter.ALLOW_ALL);

        // AP-25290: install the prefs syncer before initializing the mount table, but after instance location is set
        if (!MountPointsPrefsSyncer.install()) {
            // we expect to be the first and only ones to add the syncer
            // if it is added before the instance location is set, it will lead to accidental setting of the
            // workspace to the default location
            NodeLogger.getLogger(getClass()).coding("MountPointPrefsSyncer was already installed");
        }
        WorkbenchMountTable.init(filter);
    }
}
