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
package org.knime.core.workbench.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.util.CoreConstants;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointType;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.LoggerFactory;

public class ExplorerPreferenceInitializer extends AbstractPreferenceInitializer {

    private static final String MOUNTPOINT_PREFERENCE_LOCATION =
        WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID + "/defaultMountpoint";

    private static final String DEFAULT_MOUNTPOINTS_LIST = "defaultMountpoints";

    private static final String ENFORCE_EXCLUSION = "enforceExclusion";

    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences defaultPrefStore = DefaultScope.INSTANCE.getNode(WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID);

        // Set the default behavior of "Do you want to link this metanode".
        defaultPrefStore.put(WorkbenchConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE, WorkbenchConstants.P_DEFAULT_PROMPT_ACTION);

        // Set the default behavior of "Should a warning dialog appear when you connect to an older server".
        defaultPrefStore.putBoolean(WorkbenchConstants.P_SHOW_OLDER_SERVER_WARNING_DIALOG,
            WorkbenchConstants.P_DEFAULT_SHOW_OLDER_SERVER_WARNING_DIALOG);

        // EJB support removed in 5.4
    }

    public static void loadDefaultMountPoints() {
        List<WorkbenchMountPointType> addableDefs = WorkbenchMountTable.getAddableMountPointDefinitions();
        // Set the default mount points
        final List<MountSettings> settingsList = new ArrayList<>();
        final List<String> include = getIncludedDefaultMountPoints();

        for (WorkbenchMountPointType fac : addableDefs) {
            try {
                fac.getDefaultSettings().ifPresent(settingsList::add);
            } catch (WorkbenchMountException ioe) {
                LoggerFactory.getLogger(ExplorerPreferenceInitializer.class).atError().setCause(ioe).log(
                    "Failed to create mount point for default mount point with id '{}'", fac.getDefaultMountID());
            }
            if (fac.getDefaultMountID().filter(include::contains).isPresent()) {
                // TODO?
            }
        }
        // Sort Mount Points in such a way that the KNIME Hub is on top (SRV-2308)
        settingsList.sort((e1, e2) -> CoreConstants.KNIME_HUB_MOUNT_ID.equals(e1.getDefaultMountID()) ? -1
            : CoreConstants.KNIME_HUB_MOUNT_ID.equals(e2.getDefaultMountID()) ? 1 : 0);

        MountSettings.saveMountSettings(settingsList);
    }

    /**
     * @return true, if mount settings have been stored in XML yet
     * @since 6.2
     */
    public static boolean existsMountPreferencesXML() {
        IEclipsePreferences preferences =
                InstanceScope.INSTANCE.getNode(WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID);
        String mpSettings = preferences.get(WorkbenchConstants.P_EXPLORER_MOUNT_POINT_XML, null);
        return StringUtils.isNotEmpty(mpSettings);
    }

    /**
     * Returns whether there are MountSettings stored in the {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @return Whether there are MountSettingsStored in the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @since 8.2
     */
    public static boolean existMountPointPreferenceNodes() {
        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID);
        try {
            return mountPointNode.childrenNames().length > 0;
        } catch (BackingStoreException e) {
            // No settings to be found.
            return false;
        }
    }

    /**
     * Returns a list with all default mount points that shall be added.
     *
     * @return List with all default mount points that shall be added.
     * @since 8.5
     */
    public static List<String> getIncludedDefaultMountPoints() {
        final String mpSetting =
            DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).get(DEFAULT_MOUNTPOINTS_LIST, null);

        final boolean enforceExclusion =
            DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false);

        if (mpSetting == null) {
            return enforceExclusion ? Collections.emptyList()
                : WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                    .map(e -> e.getDefaultMountID()) //
                    .flatMap(Optional::stream) //
                    .filter(e -> !StringUtils.isEmpty(e)) //
                    .collect(Collectors.toList());
        }

        final String[] mps = mpSetting.split("\\,");

        return Arrays.stream(mps).map(e -> e.trim()).filter(e -> !StringUtils.isEmpty(e)).collect(Collectors.toList());
    }

    /**
     * Returns a list with all default mount points that shall be excluded.
     *
     * @return List with all default mount points that shall be excluded.
     * @since 8.5
     */
    public static List<String> getExcludedDefaultMountPoints() {
        if (DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false)) {
            final List<String> includedMountPoints = getIncludedDefaultMountPoints();

            return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                    .map(e -> e.getDefaultMountID()) //
                    .flatMap(Optional::stream) //
                    .filter(e -> !StringUtils.isEmpty(e) && !includedMountPoints.contains(e)) //
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
