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
package org.knime.core.workbench.mountpoint.api;

import java.util.Optional;

import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;

/**
 * Factory for mount point states, registered via extension point.
 *
 * @param <T> type of the mount point state
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public interface WorkbenchMountPointStateFactory<T extends WorkbenchMountPointState> {

    /**
     * Mount point types contributing a default mount ID will also implement this and return the settings of an instance
     * that would be created using no further configuration.
     *
     * @return a default instance for "default mount points", this implementation returns an empty optional.
     */
    default Optional<WorkbenchMountPointStateSettings> getDefaultStateSettings() {
        return Optional.empty();
    }

    /**
     * Create the mountpoint state for the given mount settings.
     *
     * @param settings mount settings
     * @return mountpoint state instance
     * @throws WorkbenchMountException if no instance can be created
     */
    T newInstance(WorkbenchMountPointSettings settings) throws WorkbenchMountException;

    /**
     * Creates a display string for the mountpoint represented by the given mount settings.
     *
     * @param mountSettings settings
     * @return display string
     * @throws WorkbenchMountException if the mount settings are incompatible
     */
    String getContentDisplayString(WorkbenchMountPointSettings mountSettings) throws WorkbenchMountException;

    /**
     * Returns the name of the mount point type.
     *
     * @return mount point type name
     */
    String getDisplayName();

    /**
     * @param workbenchMountPointState
     * @return
     */
    WorkbenchMountPointStateSettings getCurrentSettings(T workbenchMountPointState);

}
