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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.core.workbench.mounts;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.BackingStoreException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.mounts.events.MountPointEvent;
import org.knime.core.workbench.mounts.events.MountPointListener;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.preferences.ExplorerPreferenceInitializer;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class WorkbenchMountTable {
    /**
     * The property for changes on mount points IPropertyChangeListener can
     * register for.
     */
    public static final String MOUNT_POINT_PROPERTY = "MOUNT_POINTS";

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchMountTable.class);

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(WorkbenchMountTable.class).getSymbolicName();

    private static final List<MountPointListener> CHANGE_LISTENER = new CopyOnWriteArrayList<>();


    // TODO move somewhere else
    /**
     * The scheme this file system is registered with (see extension point
     * "org.eclipse.core.filesystem.filesystems").
     */
    public static final String SCHEME = "knime";

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     */
    private static final Pattern MOUNTID_PATTERN = Pattern.compile("^[a-zA-Z](?:[.a-zA-Z0-9-]*[a-zA-Z0-9])?$");

    private WorkbenchMountTable() {
        // hiding constructor of utility class
    }

    /**
     * Keeps all currently mounted content with the mountID (provided by the user).
     */
    private static final Map<String, WorkbenchMountPoint> MOUNTED = new LinkedHashMap<>();

    /*---------------------------------------------------------------*/
    /**
     * Adds a listener for mount point changes.
     *
     * @param listener ...
     */
    public static void addListener(final MountPointListener listener) {
        CHANGE_LISTENER.add(listener); // NOSONAR (no performance hotspot)
    }

    /**
     * Removes the given listener.
     *
     * @param listener ...
     */
    public static void removeListener(final MountPointListener listener) {
        CHANGE_LISTENER.remove(listener); // NOSONAR (no performance hotspot)
    }

    private static void notifyListeners(final MountPointEvent event) {
        CHANGE_LISTENER.forEach(listener -> listener.mountPointAdded(event));
    }

    /**
     * Creates a new instance of the specified content provider. May open a user
     * dialog to get parameters needed by the provider factory. Returns null, if
     * the user canceled.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static WorkbenchMountPoint mount(final String mountID, final String providerID) throws IOException {
        return mountOrRestore(mountID, providerID, null);
    }

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     *
     * @param id the id to test
     * @return true if the id is valid (in terms of contained characters)
     *         independent of it may already be in use.
     */
    public static boolean isValidMountID(final String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (id.startsWith("knime.") || !MOUNTID_PATTERN.matcher(id).find()) {
            return false;
        }
        try {
            // this is the way we build URIs to reference server items - this must not choke.
            new URI(SCHEME, id, "/test/path", null);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Throws an exception, if the specified id is not a valid mount id (in
     * terms of contained characters). (@see {@link #isValidMountID(String)})
     *
     * @param id to test
     * @throws IllegalArgumentException if the specified id is not a valid mount
     *             id (in terms of contained characters). (@see
     *             {@link #isValidMountID(String)})
     */
    public static void checkMountID(final String id)
            throws IllegalArgumentException {
        if (!isValidMountID(id)) {
            throw new IllegalArgumentException(id);
        }
    }

    /**
     * Sorts the mount points in the same way as in the passed list. It makes
     * sure that if two entries A and B of the list are mounted and A comes
     * before B, the mount point A will appear before mount point B in the mount
     * table.
     *
     * @param mountIDs a list of mount ids
     */
    public static void setMountOrder(final List<String> mountIDs) {
        if (!compareSortOrder(mountIDs)) {
            synchronized (MOUNTED) {
                for (String mountID : mountIDs) {
                    WorkbenchMountPoint workbenchMountPoint = MOUNTED.get(mountID);
                    if (workbenchMountPoint != null) {
                        /*
                         * Remove the mount point and insert it again immediately to
                         * get the same order as in the mount id list.
                         */
                        MOUNTED.remove(mountID);
                        notifyListeners(new PropertyChangeEvent(workbenchMountPoint, MOUNT_POINT_PROPERTY,
                            mountID, null));
                        MOUNTED.put(mountID, workbenchMountPoint);
                        notifyListeners(new PropertyChangeEvent(workbenchMountPoint,
                            MOUNT_POINT_PROPERTY, null, mountID));
                    }
                }
            }
        }
    }

    /**
     * @param mountIDs a list of mount IDs in the expected order
     * @return if the sort order of the passed list of mount IDs is the same as in the mount table
     */
    private static boolean compareSortOrder(final List<String> mountIDs) {
        synchronized (MOUNTED) {
            // sanity check
            if (mountIDs.size() != MOUNTED.size()) {
                return false;
            }
            // compare entry set with mount ID list
            Iterator<Entry<String, WorkbenchMountPoint>> iterator = MOUNTED.entrySet().iterator();
            for (int i = 0; i < mountIDs.size(); i++) {
                if (!mountIDs.get(i).equals(iterator.next().getKey())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Creates a new instance of the specified content provider.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @param storage the stored data of the content provider
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static WorkbenchMountPoint mount(final String mountID,
            final String providerID, final String storage) throws IOException {
        return mountOrRestore(mountID, providerID, storage);
    }

    /**
     * Mounts a new content with the specified mountID and from the specified
     * provider factory - and initializes it from the specified storage, if that
     * is not null.
     *
     * @param mountID
     * @param typeIdentifier
     * @param storage A lengthy text block that was persisted to the eclipse preferences
     * @param active
     * @return
     */
    private static WorkbenchMountPoint mountOrRestore(final String mountID,
            final String typeIdentifier, final String storage) throws IOException {
        checkMountID(mountID);
        synchronized (MOUNTED) {
            // can't mount different providers with the same ID
            WorkbenchMountPoint existMp = MOUNTED.get(mountID);
            if (existMp != null) {
                if (!Objects.equals(existMp.getDefinition().getTypeIdentifier(), typeIdentifier)) {
                    LOGGER.debug("The mount point definition with the specified type ({}) is already mounted with "
                            + "requested ID ({}).", typeIdentifier, mountID);
                    return existMp;
                }
                throw new IOException(String.format("There is a different content mounted with the same mountID "
                    + "\"%s\" but different type: \"%s\" vs \"%s\"",
                    mountID, typeIdentifier, existMp.getDefinition().getTypeIdentifier()));
            }

            // TODO review checked exception type
            WorkbenchMountPointDefinition def = WorkbenchActivator.getInstance() //
                .getMountPointDefinition(typeIdentifier) //
                .orElseThrow(() -> new IOException(
                    "The mount point definition with the specified id (" + typeIdentifier + ") is not available."));
            if (!def.supportsMultipleInstances() && isMounted(typeIdentifier)) {
                // should be prevented by the UI, hence unchecked exception
                throw new IllegalStateException("Cannot mount " + def.getTypeIdentifier() + " multiple times.");
            }

            final WorkbenchMountPoint newProvider = def.createMountPoint(mountID, storage);
            MOUNTED.put(mountID, newProvider);
            notifyListeners(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY, null, mp.getMountID()));
            return newProvider;
        }
    }

    /**
     * @param mountID the id to unmount
     * @return true if unmounting was successful, false otherwise
     */
    public static synchronized boolean unmount(final String mountID) {
        synchronized (MOUNTED) {
            WorkbenchMountPoint mp = MOUNTED.remove(mountID);
            if (mp == null) {
                return false;
            }
            mp.dispose();
            notifyListeners(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY,
                    mp.getMountID(), null));
            return true;
        }
    }

    /**
     * Unmounts all MountPoints.
     */
    public static void unmountAll() {
        synchronized (MOUNTED) {
            List<String> ids = getAllMountedIDs();
            for (String id : ids) {
                unmount(id);
            }
        }
    }

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not yet mounted). The list contains the
     * factory objects. Their toString method should return a useful name.
     *
     * @return a map of available content providers (key = name, value = ID).
     */
    public static List<WorkbenchMountPointDefinition> getAddableContentProviders() {
        var result = new LinkedList<WorkbenchMountPointDefinition>();
        synchronized (MOUNTED) {
            return WorkbenchActivator.getInstance().getMountPointDefinitions().stream() //
                .filter(mpDef -> mpDef.supportsMultipleInstances() || !(isMounted(mpDef.getTypeIdentifier()))) //
                .toList();
        }
    }

    /**
     * @return a list of all mount IDs currently in use.
     * @since 6.4
     */
    public static List<String> getAllMountedIDs() {
        synchronized (MOUNTED) {
            return new ArrayList<String>(MOUNTED.keySet());
        }
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param mountID the mount point
     * @return null, if no content is mounted with the specified ID
     */
    public static WorkbenchMountPoint getMountPoint(final String mountID) {
        synchronized (MOUNTED) {
            return MOUNTED.get(mountID);
        }
    }

    /**
     * Applies the given function on a collection of all currently mounted mount points.
     * The function application is thread-safe, because no mountpoints are mounted or unmounted by other threads
     * during the course of the function application.
     *
     * @param <T> return type of function application
     * @param mountedFn function to apply on collection
     * @return result of function
     * @throws E exception thrown by function
     */
    public static <T, E extends Throwable> T
            withMounted(final FailableFunction<Collection<WorkbenchMountPoint>, T, E> mountedFn) throws E {
        synchronized (MOUNTED) {
            return mountedFn.apply(MOUNTED.values());
        }
    }


    /**
     * Checks whether an instance created by the specified factory is already
     * mounted.
     *
     *
     * @param providerID the id of the provider factory
     * @return true, if an instance created by the specified factory exists
     *         already, false, if not.
     */
    public static boolean isMounted(final String providerID) {
        synchronized (MOUNTED) {
            return MOUNTED.values().stream().anyMatch(mp -> mp.getDefinition().getTypeIdentifier().equals(providerID));
        }
    }

    /**
     * Initializes the explorer mount table based on the preferences of the
     * plugin's preference store.
     */
    public static void init() {
        unmountAll();
        mountTempSpace();
        synchronized (MOUNTED) {
            for (MountSettings ms : getMountSettings()) {
                // ignore inactive
                if (!ms.isActive()) {
                    continue;
                }
                String mountID = ms.getMountID();
                String storage = ms.getContent();
                if (storage == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }
                String factID = ms.getFactoryID();
                if (factID == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }

                try {
                    if (mountOrRestore(mountID, factID, storage)
                            == null) {
                        LOGGER.error("Unable to restore mount point '"
                                + mountID + "' (from " + factID
                                + ": returned null).");
                    }
                } catch (Throwable t) {
                    String msg = t.getMessage();
                    if (msg == null || msg.isEmpty()) {
                        msg = "<no details>";
                    }
                    LOGGER.error("Unable to restore mount point '" + mountID
                            + "' (from " + factID + "): " + msg, t);
                }
            }
        }

    }

    private static List<MountSettings> getMountSettings() {
        // AP-8989 switching to IEclipsePreferences
        List<MountSettings> mountSettings = new ArrayList<>();

        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation());
        String[] childrenNames = null;
        try {
            childrenNames = mountPointNode.childrenNames();
        } catch (BackingStoreException e) {
            LOGGER.error("Unabled to read mount point preferences: " + e.getMessage(), e);
        }

        if (!ArrayUtils.isEmpty(childrenNames)) {
            mountSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        } else {
            IPreferenceStore pStore = ExplorerActivator.getDefault().getPreferenceStore();
            String mpSettings;
            if (ExplorerPreferenceInitializer.existsMountPreferencesXML()) {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            } else {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT);
            }
            if (StringUtils.isEmpty(mpSettings)) {
                ExplorerPreferenceInitializer.loadDefaultMountPoints();
                mpSettings = pStore.getDefaultString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            }
             mountSettings = MountSettings.parseSettings(mpSettings, true);
             mountSettings.addAll(MountSettings.loadSortedMountSettingsFromDefaultPreferenceNode());
        }

        return mountSettings;
    }

    /* Mounts all hidden spaces that provide a default mount id. */
    private static void mountTempSpace() {
        List<AbstractContentProviderFactory> contentProviders = getAddableContentProviders();
        for (AbstractContentProviderFactory fac : contentProviders) {
            String mountID = fac.getDefaultMountID();
            if (fac.isTempSpace() && mountID != null) {
                try {
                    mountOrRestore(mountID, fac.getID(), null);
                    LOGGER.info("Mounted Explorer Temp Space '" + mountID + "' - " + fac.getID());
                    return; // mounting only one temp space
                } catch (IOException e) {
                    LOGGER.error("Unable to mount the temp space '" + mountID + "' - " + fac.getID(), e);
                }
            }
        }
        LOGGER.debug("No Explorer Temp Space available.");
    }

    /**
     * Updates the settings of all providers in the preferences. Some providers may get additional attributes once they
     * are used (e.g. the REST address for server mount points) which should be persisted.
     * @since 7.3
     */
    public static void updateProviderSettings() {
        // AP-8989 switching to IEclipsePreferences
        Map<String, AbstractContentProvider> mountedContent = getMountedContent();
        List<MountSettings> mountSettingsToSave = new ArrayList<>();

        for (MountSettings ms : getMountSettings()) {
            if (mountedContent.containsKey(ms.getMountID())) {
                mountSettingsToSave.add(new MountSettings(mountedContent.get(ms.getMountID())));
            } else {
                mountSettingsToSave.add(ms);
            }
        }

        MountSettings.saveMountSettings(mountSettingsToSave);

    }

    /**
     * Returns the collected ContentProviderFactories.
     *
     * @return the collected ContentProviderFactories
     * @since 8.2
     */
    public static TreeMap<String, AbstractContentProviderFactory> getContentProviderFactories() {
        return CONTENT_FACTORIES;
    }
}
