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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.util.Pair;
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
@SuppressWarnings("java:S6212") // `var` may hurt readability
public final class MountPointsPreferencesUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountPointsPreferencesUtil.class);

    private static final String DEFAULT_MOUNTPOINT_PREFERENCE_LOCATION =
        WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID + "/defaultMountpoint";

    /** Location for the MountSettings preference node. */
    private static final String MOUNTPOINT_PREFERENCE_LOCATION =
        WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID + "/mountpointNode";

    private static final String DEFAULT_MOUNTPOINTS_LIST = "defaultMountpoints";

    private static final String ENFORCE_EXCLUSION = "enforceExclusion";

    private static final String MOUNT_ID = "mountID";

    private static final String FACTORY_ID = "factoryID";

    private static final String DEFAULT_MOUNT_ID = "defaultMountID";

    private static final String ACTIVE = "active";

    private static final String MOUNTPOINT_NUMBER = "mountpointNumber";

    private static final List<String> NECESSARY_KEYS = Arrays.asList(MOUNT_ID, FACTORY_ID);

    private static final Set<String> RESERVED_KEYS =
            Set.of(MOUNT_ID, FACTORY_ID, DEFAULT_MOUNT_ID, ACTIVE, MOUNTPOINT_NUMBER);

    private MountPointsPreferencesUtil() {
    }

    /**
     * Loads the default mount points, i.e. those from the default preference (customization profiles), if any, and
     * installation defaults (e.g. LOCAL).
     *
     * @return the list of mount point settings, never <code>null</code>.
     */
    public static List<WorkbenchMountPointSettings> loadDefaultMountPoints() {
        Set<String> uniqueMountPointNameSet = new HashSet<>();
        return Stream
                // default preferences take precedence over the installation defaults
            .concat(loadSortedMountSettingsFromDefaultPreferenceNode().stream(),
                loadDefaultMountPointsFromInstallation().stream())
            .filter(e -> uniqueMountPointNameSet.add(e.mountID())) //
            .map(mp -> new Pair<>(mp.getWorkbenchMountPointTypeOrFail().getSortPriority(), mp)) //
            // sort the default mount points according to type's priority and default ID
            .sorted(Comparator //
                .comparing((final Pair<Integer, WorkbenchMountPointSettings> p) -> -p.getFirst()) // NOSONAR type inference
                .thenComparing(e -> e.getSecond().mountID())) //
            .map(Pair::getSecond) //
            .toList();
    }

    /**
     * @return whether there are MountSettings.
     */
    public static boolean existMountPointPreferenceNodes() {
        IEclipsePreferences mountPointNode =
            InstanceScope.INSTANCE.getNode(WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID);
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

        return loadDefaultMountPointsFromInstallation().stream() //
                .filter(e -> isADefaultMountPointPredicate.test(e.defaultMountID())) //
                .toList();
    }

    /**
     * Retrieves the installation default mount point settings for all non-temporary mount point types (currently
     * "My-KNIME-Hub", "EXAMPLES", and "LOCAL".
     *
     * @return A stream of installation default mount point settings.
     */
    private static List<WorkbenchMountPointSettings> loadDefaultMountPointsFromInstallation() {
        return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
            .filter(type -> !type.isTemporaryMountPoint()) //
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
            .toList();
    }

    /**
     * @return a list with all default mount point IDs that shall be excluded.
     */
    private static List<String> getExcludedDefaultMountIDs() {
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
     */
    public static List<WorkbenchMountPointSettings> loadSortedMountSettingsFromPreferenceNode() {
        // AP-8989 Switching to IEclipsePreferences
        final List<WorkbenchMountPointSettings> mountSettings = new ArrayList<>();
        try {
            final List<WorkbenchMountPointSettings> defaultSettings =
                loadSortedMountSettingsFromDefaultPreferenceNode();
            final List<WorkbenchMountPointSettings> instanceSettings =
                new ArrayList<>(getSortedMountSettingsFromNode(getInstanceMountPointParentNode()));

            final Set<String> instanceMountIDs = new HashSet<>();
            instanceSettings.stream().forEach(inst -> instanceMountIDs.add(inst.mountID()));

            for (WorkbenchMountPointSettings defaultSetting : defaultSettings) {
                if (instanceMountIDs.add(defaultSetting.mountID())) {
                    // mount ID wasn't in the set before, add the default setting
                    mountSettings.add(defaultSetting);
                }
            }

            mountSettings.addAll(instanceSettings);
        } catch (BackingStoreException e) { // NOSONAR
            // ignore, return an empty list
        }

        // exclude default mps that are not part of the preference if enforce exclusion is enabled.
        final List<String> excludedDefaultMPs = MountPointsPreferencesUtil.getExcludedDefaultMountIDs();

        return mountSettings.stream()
                .filter(e -> !excludedDefaultMPs.contains(e.defaultMountID())) //
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Collection<WorkbenchMountPointSettings> getSortedMountSettingsFromNode(
        final IEclipsePreferences preferenceNode)
        throws BackingStoreException {
        final TreeSet<WorkbenchMountPointSettingsAndNumber> mountSettings = new TreeSet<>();
        final String[] instanceChildNodes = preferenceNode.childrenNames();
        for (final String mountPointNodeName : instanceChildNodes) {
            final Preferences childMountPointNode = preferenceNode.node(mountPointNodeName);
            final WorkbenchMountPointSettingsAndNumber ms = loadMountSettingsFromNode(childMountPointNode);
            if (ms != null) {
                mountSettings.add(ms);
            }
        }

        return mountSettings.stream().map(WorkbenchMountPointSettingsAndNumber::settings).toList();
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
    public static List<WorkbenchMountPointSettings>
        loadSortedMountSettingsFromPreferences(final boolean loadDefaultsIfEmpty) {

        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        String[] childNodes = null;
        try {
            childNodes = mountPointsNode.childrenNames();
        } catch (BackingStoreException ex) {
            LOGGER.atError().setCause(ex).log("Unable to read mount point preferences: {}", ex.getMessage());
        }
        if (ArrayUtils.isEmpty(childNodes) && loadDefaultsIfEmpty) {
            return MountPointsPreferencesUtil.loadDefaultMountPoints();
        }
        return loadSortedMountSettingsFromPreferenceNode();
    }

    /**
     * Saves the given mountSettings to the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference node.
     * The preferences are saved in the mount point ordering of the given List.
     *
     * @param newSettings The MountSettings to be saved to the preference node
     */
    public static void saveMountSettings(final List<WorkbenchMountPointSettings> newSettings) {
        final Map<String, WorkbenchMountPointSettingsAndNumber> newSettingsMap = new LinkedHashMap<>();
        for (int i = 0; i < newSettings.size(); i++) {
            final var numbered = new WorkbenchMountPointSettingsAndNumber(newSettings.get(i), i);
            if (newSettingsMap.put(newSettings.get(i).mountID(), numbered) != null) {
                throw new IllegalStateException("Duplicate mount ID '%s'".formatted(newSettings.get(i).mountID()));
            }
        }
        try {
            writeMountSettings(newSettingsMap);
        } catch (BackingStoreException ex) {
            LOGGER.atError().setCause(ex).log();
        }

        MOUNT_SETTINGS_SAVED_LISTENERS.forEach(Runnable::run);
    }

    private static void writeMountSettings(final Map<String, WorkbenchMountPointSettingsAndNumber> newSettings)
        throws BackingStoreException {

        final IEclipsePreferences instanceParent = getInstanceMountPointParentNode();
        final Map<String, WorkbenchMountPointSettingsAndNumber> oldSettings = loadRawSettings(instanceParent);

        // collect the union of all old and new keys, then insert/delete/update as needed
        final Set<String> allKeys = new HashSet<>(oldSettings.keySet());
        allKeys.addAll(newSettings.keySet());

        for (final String key : allKeys) {
            final WorkbenchMountPointSettingsAndNumber oldEntry = oldSettings.get(key);
            final WorkbenchMountPointSettingsAndNumber newEntry = newSettings.get(key);
            if (Objects.equals(newEntry, oldEntry)) {
                // nothing to do
                continue;
            }

            if (oldEntry != null) {
                // remove always, re-add maybe
                instanceParent.node(key).removeNode();
            }

            if (newEntry != null) {
                writeNode((IEclipsePreferences)instanceParent.node(key), newEntry.settings(),
                    newEntry.mountPointNumber());
            }
        }
    }

    private static void writeNode(final IEclipsePreferences node, final WorkbenchMountPointSettings settings,
        final int mountPointNumber) {

        node.putBoolean(ACTIVE, settings.isActive());
        node.putInt(MOUNTPOINT_NUMBER, mountPointNumber);

        final String defaultMountID = settings.defaultMountID();
        if (!StringUtils.isEmpty(defaultMountID)) {
            node.put(DEFAULT_MOUNT_ID, defaultMountID);
        }

        for (final Entry<String, String> stateProp : settings.mountPointStateSettings().props().entrySet()) {
            node.put(stateProp.getKey(), stateProp.getValue());
        }

        // The factoryID and mountID are saved last, this makes sure that the settings do not get loaded prematurely
        // from a triggered preferenceChange event.
        node.put(FACTORY_ID, settings.factoryID());
        node.put(MOUNT_ID, settings.mountID());
    }

    private static Map<String, WorkbenchMountPointSettingsAndNumber>
        loadRawSettings(final IEclipsePreferences preferenceNode) throws BackingStoreException {

        final Map<String, WorkbenchMountPointSettingsAndNumber> mountSettings = new HashMap<>();
        for (final String mountPointNodeName : preferenceNode.childrenNames()) {
            final Preferences childMountPointNode = preferenceNode.node(mountPointNodeName);
            final WorkbenchMountPointSettingsAndNumber ms = loadMountSettingsFromNode(childMountPointNode);
            if (ms != null) {
                mountSettings.put(mountPointNodeName, ms);
            }
        }
        return mountSettings;
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

    /** record to hold the settings and the mount point number, sortable by that number, then mount id. */
    private record WorkbenchMountPointSettingsAndNumber(WorkbenchMountPointSettings settings, int mountPointNumber)
        implements Comparable<WorkbenchMountPointSettingsAndNumber> {

        @Override
        public int compareTo(final WorkbenchMountPointSettingsAndNumber o) {
            final var numberCmp = Integer.compare(this.mountPointNumber, o.mountPointNumber);
            return numberCmp != 0 ? numberCmp : settings.mountID().compareTo(o.settings.mountID());
        }
    }

    private static WorkbenchMountPointSettingsAndNumber loadMountSettingsFromNode(final Preferences node)
        throws BackingStoreException {
        // Preference nodes must contain the factoryID and the mountID, otherwise they cannot be loaded.
        List<String> nodeKeys = Arrays.asList(node.keys());
        if (nodeKeys.containsAll(NECESSARY_KEYS)) {
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
                return new WorkbenchMountPointSettingsAndNumber(//
                    new WorkbenchMountPointSettings(mountID, defaultMountID, factoryID,
                        new WorkbenchMountPointStateSettings(map), active),
                    mountPointNumber);
            } catch (Exception ex) { // NOSONAR
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
