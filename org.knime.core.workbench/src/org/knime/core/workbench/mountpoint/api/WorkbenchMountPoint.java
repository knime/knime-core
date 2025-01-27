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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.workbench.mountpoint.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.workbench.preferences.MountSettings;

/**
 * Represents a content tree in the KNIME Explorer.
 *
 * @author ohl, University of Konstanz
 */
public final class WorkbenchMountPoint {

    private final WorkbenchMountPointType m_definition;

    private final MountSettings m_settings;

    private WorkbenchMountPointState m_state;

    private final Map<Class<? extends MountPointProvider>, MountPointProvider> m_contentProviders;

    WorkbenchMountPoint(final WorkbenchMountPointType definition, final MountSettings settings,
        final WorkbenchMountPointState state) {
        m_definition = definition;
        m_settings = settings;
        m_state = state;
        m_contentProviders = new LinkedHashMap<>();
    }

    /**
     * @return the definition
     */
    public WorkbenchMountPointType getDefinition() {
        return m_definition;
    }

    public String getMountID() {
        return m_settings.getMountID();
    }

    public String getDisplayName() {
        return m_state.getDisplayName();
    }

    public WorkbenchMountPointState getState() {
        return m_state;
    }

    public MountSettings getSettings() {
        return m_settings;
    }

    @SuppressWarnings("unchecked")
    public <T extends MountPointProvider, S2 extends WorkbenchMountPointState> T
        getProvider(final Class<T> providerType, final Function<S2, T> providerFactory) {
        return (T)m_contentProviders.computeIfAbsent(providerType, k -> providerFactory.apply((S2)m_state));
    }

    @SuppressWarnings("unchecked")
    public <T extends MountPointProvider> Optional<T> getProvider(final Class<T> providerType) {
        return Optional.ofNullable((T)m_contentProviders.get(providerType));
    }

    public void dispose(final Class<? extends MountPointProvider> cl) {
        Optional.ofNullable(m_contentProviders.remove(cl)).ifPresent(MountPointProvider::dispose);
    }

    public void dispose() {
        m_contentProviders.values().forEach(MountPointProvider::dispose);
        m_contentProviders.clear();
    }
}
