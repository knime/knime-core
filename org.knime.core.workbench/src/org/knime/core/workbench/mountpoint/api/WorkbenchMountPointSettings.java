/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.workbench.mountpoint.api;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;

/**
 * Settings for an entry in the {@link WorkbenchMountTable}.
 *
 * @param mountID The mountpoint's mount ID
 * @param defaultMountID The mountpoint's default mount ID
 * @param factoryID The mountpoint's factory ID
 * @param isActive Whether the mountpoint is active
 * @param mountPointNumber The mountpoint number
 * @param mountPointStateSettings The settings required by the concrete implementation (e.g. incl server address etc)
 *
 * @since 5.5
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public record WorkbenchMountPointSettings(String mountID,
    String defaultMountID, String factoryID, WorkbenchMountPointStateSettings mountPointStateSettings,
    boolean isActive) {

    public WorkbenchMountPointSettings {
        CheckUtils.checkArgumentNotNull(mountID, "mount id must not be null");
        CheckUtils.checkArgumentNotNull(factoryID, "factory id must not be null");
        CheckUtils.checkArgumentNotNull(mountPointStateSettings, "Settings must not be null");
    }
    /**
     * @return the mount point type associated with these settings
     * @throws IllegalStateException if the type is not registered
     */
    public WorkbenchMountPointType getWorkbenchMountPointTypeOrFail() {
        return WorkbenchActivator.getInstance().getMountPointTypeOrFail(factoryID());
    }

    /**
     * A copy of this with a new value for {@link #isActive()}.
     *
     * @param newIsActive the new value
     * @return A clone
     */
    public WorkbenchMountPointSettings withActive(final boolean newIsActive) {
        return new WorkbenchMountPointSettings(mountID, defaultMountID, factoryID, mountPointStateSettings,
            newIsActive);
    }
}
