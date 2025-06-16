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
package org.knime.core.workbench.mountpoint.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;
import org.knime.core.workbench.mountpoint.api.events.MountPointEvent;
import org.knime.core.workbench.mountpoint.api.events.MountPointListener;
import org.knime.core.workbench.preferences.MountPointsPreferencesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static, AP-wide mount table.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class WorkbenchMountTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchMountTable.class);

    private static final List<MountPointListener> CHANGE_LISTENER = new CopyOnWriteArrayList<>();

    /**
     * Helps to sort mount point types in the mount point definition dialog.
     */
    private static final Comparator<WorkbenchMountPointType> DESC_PRIO =
        Comparator.comparingInt(WorkbenchMountPointType::getSortPriority).reversed();

    private WorkbenchMountTable() {
        // hiding constructor of utility class
    }

    /**
     * Keeps all currently mounted content with the mountID (provided by the user).
     */
    private static final Map<String, WorkbenchMountPoint> MOUNTED = new LinkedHashMap<>();

    /**
     * Static filter for mount point hosts, specifically for {@link WorkbenchMountPointSettings}.
     * This field is only set once, during {@link #init(WorkbenchMountPointHostFilter)}.
     */
    private static WorkbenchMountPointHostFilter hostFilter = WorkbenchMountPointHostFilter.ALLOW_ALL;

    /*---------------------------------------------------------------*/

    /**
     * Adds a listener for mount point changes.
     *
     * @param listener listener to add
     */
    public static void addListener(final MountPointListener listener) {
        CHANGE_LISTENER.add(listener); // NOSONAR (no performance hotspot)
    }

    /**
     * Removes the given listener.
     *
     * @param listener listener to remove
     */
    public static void removeListener(final MountPointListener listener) {
        CHANGE_LISTENER.remove(listener); // NOSONAR (no performance hotspot)
    }

    private static void notifyMountPointAdded(final MountPointEvent event) {
        CHANGE_LISTENER.forEach(listener -> listener.mountPointAdded(event));
    }

    private static void notifyMountPointRemoved(final MountPointEvent event) {
        CHANGE_LISTENER.forEach(listener -> listener.mountPointRemoved(event));
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
                    final var wmp = MOUNTED.get(mountID); // NOSONAR
                    if (wmp != null) {
                        MOUNTED.remove(mountID);
                        notifyMountPointRemoved(new MountPointEvent(wmp));
                        MOUNTED.put(mountID, wmp);
                        notifyMountPointAdded(new MountPointEvent(wmp));
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
            for (var i = 0; i < mountIDs.size(); i++) {
                if (!mountIDs.get(i).equals(iterator.next().getKey())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Creates a mount point from the given settings.
     *
     * @param mountSettings mount settings
     * @return created mount point
     * @throws WorkbenchMountException if the mount point couldn't be created
     */
    public static WorkbenchMountPoint mount(final WorkbenchMountPointSettings mountSettings)
        throws WorkbenchMountException {
        final String factoryID = mountSettings.factoryID();
        WorkbenchMountPointType type =
            WorkbenchActivator.getInstance().getMountPointType(factoryID)
                .orElseThrow(() -> new WorkbenchMountException("No mount point definition found for " + factoryID));
        return mountOrRestore(type, mountSettings);
    }

    /**
     * Mounts an instance of the given type with the given settings.
     */
    private static WorkbenchMountPoint mountOrRestore(final WorkbenchMountPointType type,
        final WorkbenchMountPointSettings settings) throws WorkbenchMountException {
        final String mountID = settings.mountID();
        CheckUtils.check(WorkbenchConstants.isValidMountID(mountID), WorkbenchMountException::new,
            "Mount ID %s is invalid.", mountID);

        WorkbenchMountPoint mp;
        synchronized (MOUNTED) {
            // can't mount disallowed settings, e.g. filtered server addresses if remote
            if (!isAllowed(settings)) {
                throw new WorkbenchMountException(
                    String.format("The mount point with the mountID \"%s\" cannot be mounted, "
                        + "its server address is disallowed via customization.", mountID));
            }

            // can't mount different providers with the same ID
            WorkbenchMountPoint existingMountPoint = MOUNTED.get(mountID);
            if (existingMountPoint != null) {
                final WorkbenchMountPointType existingMountPointType = existingMountPoint.getType();
                if (Objects.equals(existingMountPointType, type)) {
                    LOGGER.debug("The mount point definition with the specified type ({}) is already mounted with "
                            + "requested ID ({}).", type.getTypeIdentifier(), mountID);
                    return existingMountPoint;
                }
                throw new WorkbenchMountException(String.format("There is a different content mounted with the "
                    + "same mountID \"%s\" but different type: \"%s\" vs \"%s\"",
                    mountID, type.getTypeIdentifier(), existingMountPointType.getTypeIdentifier()));
            }

            // can't mount "singleton type" multiple times
            if (!type.supportsMultipleInstances() && isMounted(type.getTypeIdentifier())) {
                throw new WorkbenchMountException(
                    String.format("Cannot mount %s multiple times.", type.getTypeIdentifier()));
            }
            if ((mp = type.createMountPoint(settings)) == null) {
                return null;
            }

            // can't allow a remote mount point to not have a remote address
            if (mp.getState().isRemote() && type.getRemoteAdress(settings.mountPointStateSettings()).isEmpty()) {
                throw new WorkbenchMountException(
                    String.format("The *remote* mount point with the mountID \"%s\" cannot be mounted "
                        + "since its server address cannot be found.", mountID));
            }

            MOUNTED.put(mountID, mp);
        }
        notifyMountPointAdded(new MountPointEvent(mp));
        return mp;
    }

    /**
     * Unmount mount point specified by given mount ID, notifying all listeners.
     *
     * @param mountID the ID of the mount point to unmount
     * @return {@code true} if unmounting was successful, {@code false} otherwise
     */
    public static synchronized boolean unmount(final String mountID) {
        WorkbenchMountPoint mp;
        synchronized (MOUNTED) {
            mp = MOUNTED.remove(mountID);
            if (mp == null) {
                return false;
            }
        }
        notifyMountPointRemoved(new MountPointEvent(mp));
        mp.dispose();
        return true;
    }

    /**
     * @return all mount point types for which mount points can be added
     */
    public static List<WorkbenchMountPointType> getAddableMountPointDefinitions() {
        synchronized (MOUNTED) {
            return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                .filter(mpDef -> mpDef.supportsMultipleInstances() || !(isMounted(mpDef.getTypeIdentifier()))) //
                .sorted(DESC_PRIO) //
                .toList();
        }
    }

    /**
     * @return a list of all mount IDs currently in use.
     */
    public static List<String> getAllMountedIDs() {
        synchronized (MOUNTED) {
            return new ArrayList<>(MOUNTED.keySet());
        }
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param mountID the mount point
     * @return The mount point or an empty Optional if not mounted or unknown.
     */
    public static Optional<WorkbenchMountPoint> getMountPoint(final String mountID) {
        synchronized (MOUNTED) {
            return Optional.ofNullable(MOUNTED.get(mountID));
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
            return MOUNTED.values().stream().anyMatch(mp -> mp.getType().getTypeIdentifier().equals(providerID));
        }
    }

    /**
     * Uses the {@link WorkbenchMountPointHostFilter} instance currently-set in the mount table,
     * to check whether individual {@link WorkbenchMountPointSettings} are allowed.
     *
     * @param mountSettings the {@link WorkbenchMountPointSettings} instance
     * @return {@code true} if settings are allowed, {@code false} otherwise
     * @see WorkbenchMountPointHostFilter
     */
    public static boolean isAllowed(final WorkbenchMountPointSettings mountSettings) {
        final WorkbenchMountPointType type = WorkbenchActivator.getInstance() //
            .getMountPointType(mountSettings.factoryID()) //
            .orElse(null);
        try {
            return isAllowed(type, mountSettings.mountPointStateSettings());
        } catch (WorkbenchMountException e) {
            LOGGER.atError().setCause(e).log(
                "Unable to determine for mount type \"{}\" whether the server address of the "
                    + "mount point \"{}\" is allowed, defaulting to false.",
                mountSettings.factoryID(), mountSettings.mountID());
            return false;
        }
    }

    private static boolean isAllowed(final WorkbenchMountPointType type,
        final WorkbenchMountPointStateSettings settings) throws WorkbenchMountException {
        // do not allow MP types that cannot be found, we need the type to resolve
        // to the individual remote address of the mount point
        if (type == null) {
            return false;
        }

        UnaryOperator<String> hostMapper = address -> {
            try {
                // Check if the address is in URI form...
                final var uri = new URI(address.trim());
                if (uri.getHost() != null) {
                    return uri.getHost();
                }
            } catch (URISyntaxException ex) { // NOSONAR
            }
            // ...otherwise assume a pure host name here, and use as is.
            return address;
        };

        // if settings contain a remote address, match against filter
        return type.getRemoteAdress(settings) //
            .filter(StringUtils::isNotBlank) //
            .map(hostMapper::apply) //
            .map(hostFilter::test) //
            .orElse(true);
    }

    /**
     * Initializes the explorer mount table based on the preferences of the plugin's preference store.
     *
     * @param filter based on AP customization
     */
    public static void init(final WorkbenchMountPointHostFilter filter) {
        synchronized (MOUNTED) {
            // unmount all mount points
            List<String> ids = getAllMountedIDs();
            for (String id : ids) {
                unmount(id);
            }
            // initialize the static state
            hostFilter = Objects.requireNonNull(filter);
            mountTempSpace();
            for (WorkbenchMountPointSettings settings : MountPointsPreferencesUtil.
                    loadSortedMountSettingsFromPreferences(true)) {
                if (!settings.isActive()) {
                    continue;
                }
                final WorkbenchMountPointType type =
                    WorkbenchActivator.getInstance().getMountPointType(settings.factoryID()).orElse(null);
                if (type != null) {
                    tryToRestore(type, settings);
                } else {
                    LOGGER.error("Unknown mount type \"{}\" stored, can't restore mount point \"{}\".",
                        settings.factoryID(), settings.mountID());
                }
            }
        }
    }

    @SuppressWarnings("java:S1181") // we run code from unknown sources, so we have to catch all `Error`s
    private static void tryToRestore(final WorkbenchMountPointType type, final WorkbenchMountPointSettings settings) {
        try {
            if (mountOrRestore(type, settings) == null) {
                LOGGER.error("Unable to restore mount point '{}' (from {}: returned null).", settings.mountID(),
                    settings.factoryID());
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError oome) {
                throw oome;
            }
            LOGGER.atError().setCause(t).log("Unable to restore mount point '{}' (from {}): {}", settings.mountID(),
                settings.factoryID(), StringUtils.defaultIfEmpty(t.getMessage(), "<no details>"));
        }
    }

    /** Mounts all hidden spaces that provide a default mount id. */
    private static void mountTempSpace() {
        final var tempDefOptional =
                WorkbenchActivator.getInstance().getMountPointType(WorkbenchConstants.TYPE_IDENTIFIER_TEMP_SPACE);
        if (tempDefOptional.isEmpty()) {
            LOGGER.error("No mount point definition for temp space found.");
            return;
        }
        final WorkbenchMountPointType tempDefType = tempDefOptional.get();
        String mountID = tempDefType.getDefaultMountID().orElse(null);
        if (tempDefType.isTemporaryMountPoint() && mountID != null) {
            try {
                final WorkbenchMountPointSettings defaultTempSettings =
                    tempDefType.getDefaultSettings().orElseThrow(() -> new WorkbenchMountException(
                        "Temporary mount point definition does not provide default settings."));
                mountOrRestore(tempDefType, defaultTempSettings);
                LOGGER.debug("Mounted Explorer Temp Space '{}' - {}", mountID, tempDefType.getTypeIdentifier());
            } catch (WorkbenchMountException e) {
                LOGGER.atError().setCause(e).log("Unable to mount the temp space '{}' - {}", mountID,
                    tempDefType.getTypeIdentifier());
            }
        }
    }

    /**
     * Updates the settings of all providers in the preferences. Some providers may get additional attributes once they
     * are used (e.g. the REST address for server mount points) which should be persisted. (e.g. user name)
     */
    public static void updateProviderSettings() {
        final LinkedHashMap<String, WorkbenchMountPointSettings> mountPointMap =
            MountPointsPreferencesUtil.loadSortedMountSettingsFromPreferences(false).stream() //
                .collect(Collectors.toMap(s -> s.mountID(), Function.identity(), (a, b) -> {
                    throw new IllegalStateException("Duplicate key detected: " + a.mountID());
                }, LinkedHashMap::new));
        synchronized (MOUNTED) {
            // settings in the current mount list overwrite the definitions in the preferences
            MOUNTED.values().stream() //
                .filter(mp -> !mp.getType().isTemporaryMountPoint()) // skip temporary mount points
                .forEach(mp -> mountPointMap.put(mp.getMountID(), mp.toWorkbenchMountPointSettings()));
        }
        MountPointsPreferencesUtil.saveMountSettings(new ArrayList<>(mountPointMap.values()));
    }
}
