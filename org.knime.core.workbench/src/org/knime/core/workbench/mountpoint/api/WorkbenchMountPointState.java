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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.workbench.preferences.MountPointsPreferencesUtil;
import org.slf4j.LoggerFactory;

/**
 * A state represents the content of a mount point, stuff that any of the providers (MUI, CUI, ...) use commonly, e.g.
 * log-in information for hub connection.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public interface WorkbenchMountPointState {

    /**
     * Static singleton - instance with no settings. Returning this field instead of {@link Optional#empty()}
     * for {@link WorkbenchMountPointType#getDefaultSettings()} has the effect of loading it as default
     * mount point in the initialization phase (loading from preferences using {@link MountPointsPreferencesUtil}).
     */
    WorkbenchMountPointStateSettings EMPTY_SETTINGS = new WorkbenchMountPointStateSettings(Map.of());

    /** @return The type the state is associated with. This is immutable and often hard-coded. */
    WorkbenchMountPointType getType();

    /** @return If this mount point is a server or hub mount point. */
    default boolean isRemote() {
        return false;
    }

    /**
     * Save the current state of this mount point to a new {@link WorkbenchMountPointStateSettings} instance.
     * Default implementation redirects to the corresponding factory.
     * @return a new instance representing the current state.
     */
    @SuppressWarnings("unchecked")
    default WorkbenchMountPointStateSettings getCurrentSettings() {
        WorkbenchMountPointStateFactory<WorkbenchMountPointState> stateFactory;
        try {
            stateFactory =
                (WorkbenchMountPointStateFactory<WorkbenchMountPointState>)getType().instantiateStateFactory();
        } catch (WorkbenchMountException ex) {
            LoggerFactory.getLogger(WorkbenchMountPointState.class) //
                .atError().setCause(ex).log("Unable to save mount point state for, failed to instantiate factory "
                    + "for mount point type: \"{}\"", getType());
            return EMPTY_SETTINGS;
        }
        return stateFactory.getCurrentSettings(this);
    }

    /**
     * Settings for the mount point state, wraps a read-only map of key-value pairs (both String).
     *
     * @param props the properties for the mount point state, must not be null
     */
    public record WorkbenchMountPointStateSettings(Map<String, String> props) {

        /**
         * Checks the Settings argument is not null, otherwise fails.
         *
         * @param props the settings for the mount point state, must not be null
         */
        public WorkbenchMountPointStateSettings {
            props = new LinkedHashMap<>(CheckUtils.checkNotNull(props, "Properties must not be null"));
        }

    }}