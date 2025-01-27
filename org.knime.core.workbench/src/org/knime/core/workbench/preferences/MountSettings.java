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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointType;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MountSettings {

    /** The preference key used to store the MountSettings as XML in the IEclipsePreference nodes */
    private static final String MOUNTPOINT_PREFERENCE_KEY = "mountpoint";

    /** Location for the MountSettings preference node. */
    private static final String MOUNTPOINT_PREFERENCE_LOCATION =
            WorkbenchConstants.WORKBENCH_PREFERENCES_PLUGIN_ID + "/mountpointNode";

    /** Used for separating multiple mount settings in the preferences. */
    private static final String SETTINGS_SEPARATOR = "\n";

    private static final String VISIBILITY_SEPARATOR = "\t";

    /** Used for separating the different setting elements. */
    private static final String ELEMENTS_SEPARATOR = ":";

    private static final Logger LOGGER = LoggerFactory.getLogger(MountSettings.class);

    /**
     * The workspace version for which KNIME Hub has been added. If the version is less than this number then the KNIME
     * Hub will be added automatically and the EXAMPLES will be renamed, as probably an AP update occurred.
     */
    private static final int HUB_WORKSPACE_VERSION = 20190627;

    private String m_displayName;

    private String m_mountID;

    private String m_defaultMountID;

    private String m_factoryID;

    private Map<String, String> m_additionalSettings;

    private boolean m_active;

    private int m_mountPointNumber;

//    /**
//     * Creates a new mount settings object based on the passed settings string.
//     *
//     * @param settings a settings string
//     */
//    @Deprecated
//    public MountSettings(final String settings) {
//        parse(settings);
//    }

//    /**
//     * Creates a new mount settings object based on the passed NodeSettings object.
//     *
//     * @param settings a NodeSettings object
//     * @throws InvalidSettingsException if settings can't be retrieved
//     * @since 6.0
//     */
//    public MountSettings(final ConfigBaseRO settings) throws InvalidSettingsException {
//        m_mountID = settings.getString("mountID");
//        m_displayName = settings.getString("displayName");
//        m_factoryID = settings.getString("factoryID");
//        m_content = new Storage(settings.getString("content"));
//        m_defaultMountID = settings.getString("defaultMountID");
//        m_active = settings.getBoolean("active");
//        if (settings.containsKey("mountPointNumber")) {
//            m_mountPointNumber = settings.getInt("mountPointNumber");
//        }
//
//    }

//    /**
//     * Creates a new mount settings object for the content provider.
//     *
//     * @param cp the content provider to create mount settings for
//     */
//    public MountSettings(final AbstractContentProvider cp) {
//        m_mountID = cp.getMountID();
//        m_displayName = m_mountID + " (" + cp.toString() + ")";
//        m_factoryID = cp.getFactory().getID();
//        m_content = cp.saveState();
//        m_defaultMountID = cp.getFactory().getDefaultMountID();
//        m_active = true;
//
//        String[] splitContent = m_content.storageString().split(";");
//        m_useRest = splitContent.length >= 5 ? splitContent[4].equals("true") : false;
//        // New Mount Points Are always at the top of the table.
//        m_mountPointNumber = 0;
//    }

    /**
     * Creates a new mount settings object for the content provider.
     *
     * @param cp the content provider to create mount settings for
     */
    public MountSettings(final WorkbenchMountPoint cp) {
        m_mountID = cp.getMountID();
        m_displayName = cp.getDisplayName();
        final WorkbenchMountPointType definition = cp.getDefinition();
        m_factoryID = definition.getTypeIdentifier();
        m_defaultMountID = definition.getDefaultMountID().orElse(null);
        m_active = true;

        // New Mount Points Are always at the top of the table.
        m_mountPointNumber = 0;
    }

    /**
     * Creates a new mount settings object from the given parameters.
     *
     * @param mountID The mountpoint's mount ID
     * @param displayName The mountpoint's display name
     * @param factoryID The mountpoint's factory ID
     * @param content The mountpoint's content
     * @param defaultMountID The mountpoint's default mount ID
     * @param active Whether the mountpoint is active
     * @param mountPointNumber The mountpoint number
     * @since 8.2
     */
    public MountSettings(final String mountID, final String factoryID, final String defaultMountID,
            final Boolean active, final int mountPointNumber, final Map<String, String> additionalSettings) {
        m_mountID = mountID;
        m_displayName = null;
        m_factoryID = factoryID;
        m_defaultMountID = defaultMountID;
        m_active = active;
        m_mountPointNumber = mountPointNumber;
        m_additionalSettings = additionalSettings;
    }

    /**
     * @return the name to be displayed for this mount settings
     */
    public String getDisplayName() {
        return m_displayName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * @return the mountID
     */
    public String getMountID() {
        return m_mountID;
    }

    /**
     * @return the defaultMountID
     * @since 6.0
     */
    public String getDefaultMountID() {
        return m_defaultMountID;
    }

//    /**
//     * @param defaultMountID the defaultMountID to set
//     * @since 6.0
//     */
//    public void setDefaultMountID(final String defaultMountID) {
//        if ((m_defaultMountID == null && defaultMountID != null) || (m_defaultMountID != null && defaultMountID == null)
//            || (m_defaultMountID != null && defaultMountID != null && !defaultMountID.equals(m_defaultMountID))) {
//            m_state = null;
//        }
//        m_defaultMountID = defaultMountID;
//    }

    /**
     * @return the factoryID
     */
    public String getFactoryID() {
        return m_factoryID;
    }

    /**
     * @return the state of the content provider stored as string
     */
//    public String getContent() {
//        return m_content.storageString();
//    }

    /**
     * @return the active
     * @since 6.0
     */
    public boolean isActive() {
        return m_active;
    }

    /**
     * @param active the active to set
     * @since 6.0
     */
    public void setActive(final boolean active) {
        m_active = active;
    }

    /**
     * Returns the mount point's number according to the mount points' ordering.
     *
     * @return The mount point number
     * @since 8.2
     */
    public int getMountPointNumber() {
        return m_mountPointNumber;
    }

//    /**
//     * @param config the NodeSettings to save to
//     */
//    private void saveToNodeSettings(final ConfigBaseWO config) {
//        config.addString("mountID", m_mountID);
//        config.addString("displayName", m_displayName);
//        config.addString("factoryID", m_factoryID);
//        config.addString("content", m_content.storageString());
//        config.addString("defaultMountID", m_defaultMountID);
//        config.addBoolean("active", m_active);
//        config.addBoolean("useRest", m_useRest);
//    }
//
//    /**
//     * @return the state of this mount settings as preference string
//     */
//    public String getSettingsString() {
//        if (m_state == null) {
//            m_state = getDisplayName() + VISIBILITY_SEPARATOR + m_mountID + ELEMENTS_SEPARATOR + m_factoryID
//                + ELEMENTS_SEPARATOR + Boolean.toString(m_active) + ELEMENTS_SEPARATOR
//                + (m_defaultMountID == null ? "" : m_defaultMountID) + ELEMENTS_SEPARATOR + m_content
//                + ELEMENTS_SEPARATOR + m_useRest;
//        }
//        return m_state;
//    }

//    /**
//     * Parses a settings string containing one or multiple settings in XML form or separated by
//     * {@link MountSettings#SETTINGS_SEPARATOR}.
//     *
//     * @param settings the preference string to parse
//     * @param excludeUnknownContentProviders true if resulting list should only contain displayable settings
//     * @return the parsed list of mount settings
//     * @since 6.2
//     */
//    public static List<MountSettings> parseSettings(final String settings,
//        final boolean excludeUnknownContentProviders) {
//        List<MountSettings> ms = new ArrayList<MountSettings>();
//        if (settings == null || settings.isEmpty()) {
//            return ms;
//        }
//        if (settings.startsWith("<?xml")) {
//            try {
//                SimpleConfig nodeSettings = new SimpleConfig("mount-settings");
//                nodeSettings.load(new ByteArrayInputStream(settings.getBytes()));
//                int numSettings = nodeSettings.getInt("numSettings");
//                for (int i = 0; i < numSettings; i++) {
//                    ConfigBaseRO singleSettings = nodeSettings.getConfigBase("mountSettings_" + i);
//                    MountSettings singleMountSettings = new MountSettings(singleSettings);
//                    if (!excludeUnknownContentProviders || isMountSettingsAddable(singleMountSettings)) {
//                        ms.add(singleMountSettings);
//                    }
//                }
//            } catch (Exception e) {
//                throw new IllegalArgumentException("Error parsing mount settings. ", e);
//            }
//        } else {
//            String[] split = settings.split(SETTINGS_SEPARATOR);
//            for (String setting : split) {
//                MountSettings singleMountSettings = new MountSettings(setting);
//                if (!excludeUnknownContentProviders || isMountSettingsAddable(singleMountSettings)) {
//                    ms.add(singleMountSettings);
//                }
//            }
//        }
//        return ms;
//    }

    /**
     * Checks if a given MountSettings object can be displayed.
     *
     * @param mountSettings the settings to check
     * @return True, if the ContenProviderFactory of the given mountSettings is available.
     * @since 6.2
     */
    public static boolean isMountSettingsAddable(final MountSettings mountSettings) {
        final String factoryID = mountSettings.getFactoryID();
        return WorkbenchMountTable.getAddableMountPointDefinitions().contains(factoryID); // NOSONAR short list
    }
//
//    /**
//     * @param mountSettings a list of MountSettings
//     * @return an XML string representing the given list of MountSettings
//     * @since 6.0
//     */
//    public static String getSettingsString(final List<MountSettings> mountSettings) {
//        SimpleConfig nodeSettings = new SimpleConfig("mountSettings");
//        for (int i = 0; i < mountSettings.size(); i++) {
//            ConfigBaseWO singleSettings = nodeSettings.addConfigBase("mountSettings_" + i);
//            mountSettings.get(i).saveToNodeSettings(singleSettings);
//        }
//        nodeSettings.addInt("numSettings", mountSettings.size());
//        final ByteArrayOutputStream out = new ByteArrayOutputStream();
//        try {
//            nodeSettings.saveToXML(out);
//        } catch (IOException e) {
//            throw new IllegalArgumentException("Error while saving mount settings to XML.", e);
//        }
//        return out.toString();
//    }

//    @Override
//    public boolean equals(final Object obj) {
//        if (!(obj instanceof MountSettings)) {
//            return false;
//        }
//        return getSettingsString().equals(((MountSettings)obj).getSettingsString());
//    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MountSettings oms) {
            return new EqualsBuilder() //
                    .append(m_displayName, oms.m_displayName) //
                    .append(m_mountID, oms.m_mountID) //
                    .append(m_defaultMountID, oms.m_defaultMountID) //
                    .append(m_factoryID, oms.m_factoryID) //
                    .append(m_additionalSettings, oms.m_additionalSettings) //
                    .append(m_active, oms.m_active) //
                    .append(m_mountPointNumber, oms.m_mountPointNumber) //
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
            .append(m_displayName) //
            .append(m_mountID) //
            .append(m_defaultMountID) //
            .append(m_factoryID) //
            .append(m_additionalSettings) //
            .append(m_active) //
            .append(m_mountPointNumber) //
            .toHashCode();
    }

    /**
     * Loads the MountSettings from the {@link WorkbenchConstants#WORKBENCH_PREFERENCES_PLUGIN_ID} preference node.
     *
     * @return The MountSettings, never <code>null</code>
     * @since 8.2
     */
    public static List<MountSettings> loadSortedMountSettingsFromPreferenceNode() {
        // AP-8989 Switching to IEclipsePreferences
        final List<MountSettings> mountSettings = new ArrayList<MountSettings>();
        try {
            final List<MountSettings> defaultMountSettingsList = loadSortedMountSettingsFromDefaultPreferenceNode();
            final List<MountSettings> instanceMountSettingsList = new ArrayList<>();

            final List<MountSettings> loadedSettingsList =
                getSortedMountSettingsFromNode(getInstanceMountPointParentNode());

            instanceMountSettingsList.addAll(loadedSettingsList);

            final List<String> instanceMountIDs =
                instanceMountSettingsList.stream().map(ms -> ms.getMountID()).collect(Collectors.toList());

            for (Iterator<MountSettings> iterator = defaultMountSettingsList.iterator(); iterator.hasNext();) {
                MountSettings defaultSetting = iterator.next();
                String nextMountID = defaultSetting.getMountID();
                boolean doAdd = true;
                for (String instanceMountId : instanceMountIDs) {
                    if (nextMountID.equals(instanceMountId)) {
                        doAdd = false;
                    }
                }
                if (doAdd) {
                    mountSettings.add(defaultSetting);
                }
            }

            mountSettings.addAll(instanceMountSettingsList);
        } catch (BackingStoreException e) {
            // ignore, return an empty list
        }

        // exclude default mps that are not part of the preference if enforce exclusion is enabled.
        final List<String> excludedDefaultMPs = ExplorerPreferenceInitializer.getExcludedDefaultMountPoints();

        return mountSettings.stream().filter(e -> !excludedDefaultMPs.contains(e.getDefaultMountID()))
            .collect(Collectors.toList());
    }

    private static List<MountSettings> getSortedMountSettingsFromNode(final IEclipsePreferences preferenceNode)
        throws BackingStoreException {
        final List<MountSettings> mountSettings = new ArrayList<>();
        final String[] instanceChildNodes = preferenceNode.childrenNames();
        for (final String mountPointNodeName : instanceChildNodes) {
            final Preferences childMountPointNode = preferenceNode.node(mountPointNodeName);
            final MountSettings ms = loadMountSettingsFromNode(childMountPointNode);
            if (ms != null) {
                mountSettings.add(ms);
            }
        }

        // Sort ascending by mountPointNumber
        mountSettings.sort((o1, o2) -> o1.getMountPointNumber() - o2.getMountPointNumber());

        return mountSettings;
    }

    /**
     * Loads the MountSettings from the DefaultInstance {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @return The MountSettings read from the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @since 8.2
     */
    public static List<MountSettings> loadSortedMountSettingsFromDefaultPreferenceNode() {
        final List<MountSettings> defaultMountSettings = new ArrayList<>();
        try {
            defaultMountSettings.addAll(getSortedMountSettingsFromNode(getDefaultMountPointParentNode()));
        } catch (BackingStoreException e) {
            // ignore, return an empty list
        }

        return defaultMountSettings;
    }

    /**
     * Loads the MountSettings from either the {@link ExplorerActivator#PLUGIN_ID} preference node, or from the
     * PreferenceStore, this ensures backwards compatibility.
     *
     * @param loadDefaultsIfEmpty If <code>true</code> and there were no prior settings, then the default mount
     *            settings are stored into the instance preference store.
     *
     * @return The MountSettings read from the {@link ExplorerActivator#PLUGIN_ID} preference node
     */
    public static List<MountSettings> loadSortedMountSettingsFromPreferences(final boolean loadDefaultsIfEmpty) {
        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        String[] childNodes = null;
        try {
            childNodes = mountPointsNode.childrenNames();
        } catch (BackingStoreException ex) {
            LOGGER.atError().setCause(ex).log("Unable to read mount point preferences: {}", ex.getMessage());
        }
        if ((childNodes == null || childNodes.length == 0) && loadDefaultsIfEmpty) {
            ExplorerPreferenceInitializer.loadDefaultMountPoints();
        }
        return loadSortedMountSettingsFromPreferenceNode();
    }

    /**
     * Saves the given mountSettings to the {@link ExplorerActivator#PLUGIN_ID) preference node. The preferences are
     * saved in the mount point ordering of the given List.
     *
     * @param mountSettings The MountSettings to be saved to the preference node
     * @since 8.2
     */
    public static void saveMountSettings(final List<MountSettings> mountSettings) {
        // AP-8989 Switching to IEclipsePreferences
        List<MountSettings> defaultMountSettings = MountSettings.loadSortedMountSettingsFromDefaultPreferenceNode();
        mountSettings.removeAll(defaultMountSettings);

        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        for (int i = 0; i < mountSettings.size(); i++) {
            MountSettings ms = mountSettings.get(i);
            IEclipsePreferences mountPointChildNode = (IEclipsePreferences)mountPointsNode.node(ms.getMountID());
            saveMountSettingsToNode(ms, mountPointChildNode, i);
        }

        MOUNT_SETTINGS_SAVED_LISTENERS.forEach(Runnable::run);
    }

    private static final List<Runnable> MOUNT_SETTINGS_SAVED_LISTENERS =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * @param listener called whenever the mount settings change
     * @since 8.12
     */
    public static void addMountSettingsSavedListener(final Runnable listener) {
        MOUNT_SETTINGS_SAVED_LISTENERS.add(listener);
    }

    private static final String MOUNT_ID = "mountID";

    private static final String FACTORY_ID = "factoryID";

    private static final String DEFAULT_MOUNT_ID = "defaultMountID";

    private static final String ACTIVE = "active";

    private static final String MOUNTPOINT_NUMBER = "mountpointNumber";

    private static final List<String> m_necessaryKeys = Arrays.asList(MOUNT_ID, FACTORY_ID);

    private static final Set<String> RESERVED_KEYS =
            Set.of(MOUNT_ID, FACTORY_ID, DEFAULT_MOUNT_ID, ACTIVE, MOUNTPOINT_NUMBER);

    private static void saveMountSettingsToNode(final MountSettings settings, final IEclipsePreferences node,
            final int mountPointNumber) {
        String defaultMountID = settings.getDefaultMountID();
        if (!StringUtils.isEmpty(defaultMountID)) {
            node.put(DEFAULT_MOUNT_ID, defaultMountID);
        }
        node.putBoolean(ACTIVE, settings.isActive());
        node.putInt(MOUNTPOINT_NUMBER, mountPointNumber);

        // The factoryID and mountID are saved last, this makes sure that the settings do not get loaded prematurely
        // from a triggered preferenceChange event.
        node.put(FACTORY_ID, settings.getFactoryID());
        node.put(MOUNT_ID, settings.getMountID());

        settings.m_additionalSettings.forEach(node::put);
    }

    private static MountSettings loadMountSettingsFromNode(final Preferences node) throws BackingStoreException {
        // Preference nodes must contain the factoryID and the mountID, otherwise they cannot be loaded.
        List<String> nodeKeys = Arrays.asList(node.keys());
        if (nodeKeys.containsAll(m_necessaryKeys)) {
            try {
                String mountID = node.get(MOUNT_ID, "");
                String factoryID = node.get(FACTORY_ID, "");
                String defaultMountID = node.get(DEFAULT_MOUNT_ID, "");
                boolean active = node.getBoolean(ACTIVE, true);
                int mountPointNumber = node.getInt(MOUNTPOINT_NUMBER, 0);

                final LinkedHashMap<String, String> additionalSettings = Arrays.stream(node.childrenNames()) //
                    .filter(c -> !RESERVED_KEYS.contains(c)) //
                    .collect(Collectors.toMap( //
                        Function.identity(), //
                        c -> node.get(c, null), //
                        (a, b) -> { throw new IllegalStateException("Duplicate key"); }, //
                        LinkedHashMap<String, String>::new));

                return new MountSettings(mountID, factoryID, defaultMountID, active, mountPointNumber,
                    additionalSettings);
            } catch (Exception ex) {
                LoggerFactory.getLogger(MountSettings.class).atError().setCause(ex).log(
                    "Could not load mount point settings from node {}: {}", node.absolutePath(), ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Removes the given MountSettings from the {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @param mountSettings The mounSettings to be removed from the preference node
     * @throws BackingStoreException if there is a failure in the backing store
     * @since 8.2
     */
    public static void removeMountSettings(final Iterable<String> mountSettings) throws BackingStoreException {
        // AP-8989 Switching to IEclipsePreferences
        for (String ms : mountSettings) {
            IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(getMountpointPreferenceLocation());
            mountPointNode.node(ms).removeNode();
        }
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
     * @since 8.2
     */
    public static String getMountpointPreferenceLocation() {
        return MOUNTPOINT_PREFERENCE_LOCATION;
    }

}
