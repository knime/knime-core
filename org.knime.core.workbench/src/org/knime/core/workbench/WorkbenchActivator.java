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
 *   Oct 29, 2024 (wiswedel): created
 */
package org.knime.core.workbench;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointType;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for the Core Workbench plugin, access point for the {@link WorkbenchMountPointType} registry.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
@SuppressWarnings({
    "java:S2696", // assignment to `instance` in instance methods
})
public final class WorkbenchActivator implements BundleActivator {

    private static WorkbenchActivator instance;

    private Map<String, WorkbenchMountPointType> m_mountPointTypeMap;

    /**
     * @return the activator instance of the currently activated plugin
     * @throws NullPointerException if the plugin isn't activated
     */
    public static WorkbenchActivator getInstance() {
        return Objects.requireNonNull(instance);
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        instance = this;
        m_mountPointTypeMap = WorkbenchMountPointType.collectMountPointTypes();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        m_mountPointTypeMap = null;
        instance = null;
    }

    /**
     * Returns all available mount point types.
     *
     * @return immutable collection of mount point types
     */
    public Collection<WorkbenchMountPointType> getMountPointTypes() {
        // the map is already immutable, no need to do anything here
        return m_mountPointTypeMap.values();
    }

    /**
     * @param typeIdentifier mount point type identifier
     * @return mount point definition for a type registered in an extension point
     */
    public Optional<WorkbenchMountPointType> getMountPointType(final String typeIdentifier) {
        return Optional.ofNullable(m_mountPointTypeMap.get(typeIdentifier));
    }

    /**
     * @param typeIdentifier identifier of the mount point type to find
     * @return corresponding {@link WorkbenchMountPointType}
     * @throws IllegalStateException if the mount point type isn't known
     */
    public WorkbenchMountPointType getMountPointTypeOrFail(final String typeIdentifier) {
        return getMountPointType(typeIdentifier)
            .orElseThrow(() -> new IllegalStateException(
                String.format("No mount point definition found for \"%s\"", typeIdentifier)));
    }
}
