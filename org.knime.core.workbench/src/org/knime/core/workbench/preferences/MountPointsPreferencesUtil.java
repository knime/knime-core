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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.util.CoreConstants;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.osgi.service.prefs.BackingStoreException;
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

    private static final String MOUNTPOINT_PREFERENCE_LOCATION =
        WORKBENCH_PREFERENCES_PLUGIN_ID + "/defaultMountpoint";

    private static final String DEFAULT_MOUNTPOINTS_LIST = "defaultMountpoints";

    private static final String ENFORCE_EXCLUSION = "enforceExclusion";

    private MountPointsPreferencesUtil() {
    }

    static List<MountSettings> loadDefaultMountPoints() {
        final List<MountSettings> defaultMountPoints = new ArrayList<>(getIncludedDefaultMountPoints());

        // sort the default mount points so that the KNIME Hub mount point is always at the top
        defaultMountPoints.sort(Comparator.<MountSettings, Boolean> comparing( //
            e -> CoreConstants.KNIME_HUB_MOUNT_ID.equals(e.getDefaultMountID())) //
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
    private static List<MountSettings> getIncludedDefaultMountPoints() {
        final String mpSetting =
            DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).get(DEFAULT_MOUNTPOINTS_LIST, null);

        final Predicate<String> isADefaultMountPointPredicate;
        if (mpSetting == null) {
            // the enforce exclusion flag is only evaluated if no default mount points are set
            final boolean enforceExclusion =
                    DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false);

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
                        return Optional.<MountSettings> empty();
                    }
                }) //
                .flatMap(Optional::stream) //
                .filter(e -> isADefaultMountPointPredicate.test(e.getDefaultMountID())) //
                .toList();
    }

    /**
     * @return a list with all default mount point IDs that shall be excluded.
     */
    static List<String> getExcludedDefaultMountIDs() {
        if (DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false)) {
            final List<String> includedMountPoints = getIncludedDefaultMountPoints().stream() //
                    .map(MountSettings::getDefaultMountID) //
                    .toList();

            return WorkbenchActivator.getInstance().getMountPointTypes().stream() //
                    .map(e -> e.getDefaultMountID()) //
                    .flatMap(Optional::stream) //
                    .filter(e -> !StringUtils.isEmpty(e) && !includedMountPoints.contains(e)) //
                    .toList();
        }

        return Collections.emptyList();
    }
}
