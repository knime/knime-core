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

import static org.knime.core.workbench.WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.util.CoreConstants;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preference initializer for everything that relates to mount points etc.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class MountPointsPreferencesUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountPointsPreferencesUtil.class);

    private static final String DEFAULT_MOUNTPOINT_PREFERENCE_LOCATION =
        WORKBENCH_PREFERENCES_PLUGIN_ID + "/defaultMountpoint";

    /** Location for the MountSettings preference node. */
    private static final String MOUNTPOINT_PREFERENCE_LOCATION = WORKBENCH_PREFERENCES_PLUGIN_ID + "/mountpointNode";

    private static final String DEFAULT_MOUNTPOINTS_LIST = "defaultMountpoints";

    private static final String ENFORCE_EXCLUSION = "enforceExclusion";

    private MountPointsPreferencesUtil() {
    }

    static List<WorkbenchMountPointSettings> loadDefaultMountPoints() {
        final List<WorkbenchMountPointSettings> defaultMountPoints = new ArrayList<>(getIncludedDefaultMountPoints());

        // sort the default mount points so that the KNIME Hub mount point is always at the top
        defaultMountPoints.sort(Comparator.<WorkbenchMountPointSettings, Boolean> comparing( //
            e -> CoreConstants.KNIME_HUB_MOUNT_ID.equals(e.defaultMountID())) //
            .reversed());
        return defaultMountPoints;
    }

    /**
     * @return whether there are MountSettings.
     */
    public static boolean existMountPointPreferenceNodes() {
        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(WORKBENCH_PREFERENCES_PLUGIN_ID);
        try {
            return mountPointNode.childrenNames().length > 0;
        } catch (BackingStoreException e) { // NOSONAR
            // No settings to be found.
            return false;
        }
    }

    /**
     * @return List with all default mount points that shall be added.
     */
    private static List<WorkbenchMountPointSettings> getIncludedDefaultMountPoints() {
        final String mpSetting =
            DefaultScope.INSTANCE.getNode(DEFAULT_MOUNTPOINT_PREFERENCE_LOCATION).get(DEFAULT_MOUNTPOINTS_LIST, null);

        final Predicate<String> isADefaultMountPointPredicate;
        if (mpSetting == null) {
            // the enforce exclusion flag is only evaluated if no default mount points are set
            final boolean enforceExclusion = DefaultScope.INSTANCE.getNode(DEFAULT_MOUNTPOINT_PREFERENCE_LOCATION)
                .getBoolean(ENFORCE_EXCLUSION, false);

            // hard to read? If enforceExclusion is true, we want to filter/exclude all mount points
            isADefaultMountPointPredicate = mountID -> !enforceExclusion;
        } else {
            // this ignores the enforceExclusion flag, it's evaluated later when populating the mount table
            final Set<String> defaultMountPoints = Arrays.stream(mpSetting.split("\\,")) //
                    .map(String::trim) //
                    .filter(StringUtils::isNotEmpty) //
                    .collect(Collectors.toSet());
            isADefaultMountPointPredicate = defaultMountPoints::contains;
        }

        return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                .map(type -> {
                    try {
                        return type.getDefaultSettings();
                    } catch (WorkbenchMountException e) {
                        LOGGER.atError().setCause(e).log(
                            "Failed to create mount point for default mount point with id '{}'",
                            type.getDefaultMountID().orElse("<unknown>"));
                        return Optional.<WorkbenchMountPointSettings> empty();
                    }
                }) //
                .flatMap(Optional::stream) //
                .filter(e -> isADefaultMountPointPredicate.test(e.defaultMountID())) //
                .toList();
    }

    /**
     * @return a list with all default mount point IDs that shall be excluded.
     */
    static List<String> getExcludedDefaultMountIDs() {
        if (DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false)) {
            final List<String> includedMountPoints = getIncludedDefaultMountPoints().stream() //
                    .map(WorkbenchMountPointSettings::defaultMountID) //
                    .toList();

            return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                    .map(e -> e.getDefaultMountID()) //
                    .flatMap(Optional::stream) //
                    .filter(e -> !StringUtils.isEmpty(e) && !includedMountPoints.contains(e)) //
                    .toList();
        }

        return Collections.emptyList();
    }

    /**
     * Loads the MountSettings from the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference node.
     *
     * @return The MountSettings, never <code>null</code>. List is writable.
     * @since 8.2
     */
    public static List<WorkbenchMountPointSettings> loadSortedMountSettingsFromPreferenceNode() {
        // AP-8989 Switching to IEclipsePreferences
        final List<WorkbenchMountPointSettings> mountSettings = new ArrayList<>();
        try {
            final List<WorkbenchMountPointSettings> defaultMountSettingsList = loadSortedMountSettingsFromDefaultPreferenceNode();
            final List<WorkbenchMountPointSettings> instanceMountSettingsList = new ArrayList<>();

            final List<WorkbenchMountPointSettings> loadedSettingsList =
                getSortedMountSettingsFromNode(getInstanceMountPointParentNode());

            instanceMountSettingsList.addAll(loadedSettingsList);

            final List<String> instanceMountIDs =
                    instanceMountSettingsList.stream().map(WorkbenchMountPointSettings::mountID).toList();

            for (Iterator<WorkbenchMountPointSettings> iterator = defaultMountSettingsList.iterator(); iterator.hasNext();) {
                WorkbenchMountPointSettings defaultSetting = iterator.next();
                final String nextMountID = defaultSetting.mountID();
                if (!instanceMountIDs.contains(nextMountID)) {
                    mountSettings.add(defaultSetting);
                }
            }

            mountSettings.addAll(instanceMountSettingsList);
        } catch (BackingStoreException e) { // NOSONAR
            // ignore, return an empty list
        }

        // exclude default mps that are not part of the preference if enforce exclusion is enabled.
        final List<String> excludedDefaultMPs = MountPointsPreferencesUtil.getExcludedDefaultMountIDs();

        return mountSettings.stream()
                .filter(e -> !excludedDefaultMPs.contains(e.defaultMountID())) //
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<WorkbenchMountPointSettings> getSortedMountSettingsFromNode(final IEclipsePreferences preferenceNode)
        throws BackingStoreException {
        final List<WorkbenchMountPointSettings> mountSettings = new ArrayList<>();
        final String[] instanceChildNodes = preferenceNode.childrenNames();
        for (final String mountPointNodeName : instanceChildNodes) {
            final Preferences childMountPointNode = preferenceNode.node(mountPointNodeName);
            final WorkbenchMountPointSettings ms = loadMountSettingsFromNode(childMountPointNode);
            if (ms != null) {
                mountSettings.add(ms);
            }
        }

        // Sort ascending by mountPointNumber
        mountSettings.sort((o1, o2) -> o1.mountPointNumber() - o2.mountPointNumber());

        return mountSettings;
    }

    /**
     * Loads the MountSettings from the DefaultInstance {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID}
     * preference node.
     *
     * @return The MountSettings read from the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID}
     * preference node
     * @since 8.2
     */
    public static List<WorkbenchMountPointSettings> loadSortedMountSettingsFromDefaultPreferenceNode() {
        final List<WorkbenchMountPointSettings> defaultMountSettings = new ArrayList<>();
        try {
            defaultMountSettings.addAll(getSortedMountSettingsFromNode(getDefaultMountPointParentNode()));
        } catch (BackingStoreException e) { // NOSONAR
            // ignore, return an empty list
        }

        return defaultMountSettings;
    }

    /**
     * Loads the MountSettings from either the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference
     * node, or from the PreferenceStore, this ensures backwards compatibility.
     *
     * @param loadDefaultsIfEmpty If <code>true</code> and there were no prior settings, then the default mount
     *            settings are stored into the instance preference store.
     *
     * @return The MountSettings read from the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID}
     * preference node
     */
    public static List<WorkbenchMountPointSettings> loadSortedMountSettingsFromPreferences(final boolean loadDefaultsIfEmpty) {
        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        String[] childNodes = null;
        try {
            childNodes = mountPointsNode.childrenNames();
        } catch (BackingStoreException ex) {
            LOGGER.atError().setCause(ex).log("Unable to read mount point preferences: {}", ex.getMessage());
        }
        if ((childNodes == null || childNodes.length == 0) && loadDefaultsIfEmpty) {
            return MountPointsPreferencesUtil.loadDefaultMountPoints();
        }
        return loadSortedMountSettingsFromPreferenceNode();
    }

    /**
     * Saves the given mountSettings to the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference node.
     * The preferences are saved in the mount point ordering of the given List.
     *
     * @param mountSettings The MountSettings to be saved to the preference node
     * @since 8.2
     */
    public static void saveMountSettings(final List<WorkbenchMountPointSettings> mountSettings) {
        List<WorkbenchMountPointSettings> defaultMountSettings = loadSortedMountSettingsFromDefaultPreferenceNode();
        final List<WorkbenchMountPointSettings> differentThanDefaultSettings = new ArrayList<>(mountSettings);
        differentThanDefaultSettings.removeAll(defaultMountSettings);

        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        for (int i = 0; i < differentThanDefaultSettings.size(); i++) {
            WorkbenchMountPointSettings ms = differentThanDefaultSettings.get(i);
            IEclipsePreferences mountPointChildNode = (IEclipsePreferences)mountPointsNode.node(ms.mountID());
            saveMountSettingsToNode(ms, mountPointChildNode, i);
        }

        MOUNT_SETTINGS_SAVED_LISTENERS.forEach(Runnable::run);
    }

    private static final List<Runnable> MOUNT_SETTINGS_SAVED_LISTENERS =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * Add a (static) listener that is called whenever the mount settings are saved.
     * @param listener called whenever the mount settings change
     */
    public static void addMountSettingsSavedListener(final Runnable listener) {
        MOUNT_SETTINGS_SAVED_LISTENERS.add(listener);
    }

    /**
     * Removes the given MountSettings from the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference
     * node.
     *
     * @param mountSettings The mounSettings to be removed from the preference node
     * @throws BackingStoreException if there is a failure in the backing store
     */
    public static void removeMountSettings(final Iterable<String> mountSettings) throws BackingStoreException {
        // AP-8989 Switching to IEclipsePreferences
        for (String ms : mountSettings) {
            IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(getMountpointPreferenceLocation());
            mountPointNode.node(ms).removeNode();
        }
    }

    private static final String MOUNT_ID = "mountID";

    private static final String FACTORY_ID = "factoryID";

    private static final String DEFAULT_MOUNT_ID = "defaultMountID";

    private static final String ACTIVE = "active";

    private static final String MOUNTPOINT_NUMBER = "mountpointNumber";

    private static final List<String> m_necessaryKeys = Arrays.asList(MOUNT_ID, FACTORY_ID);

    private static final Set<String> RESERVED_KEYS =
            Set.of(MOUNT_ID, FACTORY_ID, DEFAULT_MOUNT_ID, ACTIVE, MOUNTPOINT_NUMBER);

    private static void saveMountSettingsToNode(final WorkbenchMountPointSettings settings, final IEclipsePreferences node,
            final int mountPointNumber) {
        String defaultMountID = settings.defaultMountID();
        if (!StringUtils.isEmpty(defaultMountID)) {
            node.put(DEFAULT_MOUNT_ID, defaultMountID);
        }
        node.putBoolean(ACTIVE, settings.isActive());
        node.putInt(MOUNTPOINT_NUMBER, mountPointNumber);

        // The factoryID and mountID are saved last, this makes sure that the settings do not get loaded prematurely
        // from a triggered preferenceChange event.
        node.put(FACTORY_ID, settings.factoryID());
        node.put(MOUNT_ID, settings.mountID());

        settings.mountPointStateSettings().props().forEach(node::put);
    }

    private static WorkbenchMountPointSettings loadMountSettingsFromNode(final Preferences node) throws BackingStoreException {
        // Preference nodes must contain the factoryID and the mountID, otherwise they cannot be loaded.
        List<String> nodeKeys = Arrays.asList(node.keys());
        if (nodeKeys.containsAll(m_necessaryKeys)) {
            try {
                final LinkedHashMap<String, String> map = Arrays.stream(node.keys()) //
                    .filter(c -> !RESERVED_KEYS.contains(c)) //
                    .collect(Collectors.toMap( //
                        Function.identity(), //
                        c -> node.get(c, null), //
                        (a, b) -> { throw new IllegalStateException("Duplicate key"); }, //
                        LinkedHashMap<String, String>::new));

                String mountID = node.get(MOUNT_ID, "");
                String factoryID = node.get(FACTORY_ID, "");
                String defaultMountID = node.get(DEFAULT_MOUNT_ID, "");
                boolean active = node.getBoolean(ACTIVE, true);
                int mountPointNumber = node.getInt(MOUNTPOINT_NUMBER, 0);
                return new WorkbenchMountPointSettings(mountID, defaultMountID, factoryID,
                    new WorkbenchMountPointStateSettings(map), active, mountPointNumber);
            } catch (Exception ex) {
                LoggerFactory.getLogger(WorkbenchMountPointSettings.class).atError().setCause(ex).log(
                    "Could not load mount point settings from node {}: {}", node.absolutePath(), ex.getMessage());
            }
        }
        return null;
    }

    private static IEclipsePreferences getInstanceMountPointParentNode() {
        return InstanceScope.INSTANCE.getNode(getMountpointPreferenceLocation());
    }

    private static IEclipsePreferences getDefaultMountPointParentNode() {
        return DefaultScope.INSTANCE.getNode(getMountpointPreferenceLocation());
    }

    /**
     * Returns the location for the mountpoint preferences
     *
     * @return the mountpointPreferenceLocation
     */
    public static String getMountpointPreferenceLocation() {
        return MOUNTPOINT_PREFERENCE_LOCATION;
    }

}
