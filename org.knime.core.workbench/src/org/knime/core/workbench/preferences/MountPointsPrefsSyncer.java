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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that previously lived the the old classic UI code - whenever the preference change, it will sync it
 * with the mount table.
 *
 * @author Bernd Wiswedel
 * @since 5.5
 */
public final class MountPointsPrefsSyncer implements IPreferenceChangeListener, INodeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountPointsPrefsSyncer.class);

    private List<WorkbenchMountPointSettings> m_previousValues;

    private static final AtomicBoolean m_isInstalled = new AtomicBoolean();

    /**
     * Creates a new preference syncer.
     */
    public MountPointsPrefsSyncer() {
        m_previousValues = getUserOrDefaultValue();
    }

    /**
     * Installs the prefs syncer once. The return value indicates if this call installed the prefs syncer or not.
     * {@code false} does not mean no prefs syncer is installed.
     *
     * @return {@code true} if this call installed the prefs syncer, {@code false} otherwise
     * @since 5.9
     */
    public static boolean install() {
        final var wasInstalled = m_isInstalled.getAndSet(true);
        if (wasInstalled) {
            return false;
        }
        // AP-8989 switching to IEclipsePreferences
        final var prefsSyncer = new MountPointsPrefsSyncer();

        final IEclipsePreferences defaultPrefs =
            DefaultScope.INSTANCE.getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation());
        defaultPrefs.addPreferenceChangeListener(prefsSyncer);

        final IEclipsePreferences preferences =
            InstanceScope.INSTANCE.getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation());
        preferences.addNodeChangeListener(prefsSyncer);
        preferences.addPreferenceChangeListener(prefsSyncer);
        try {
            for (String childName : preferences.childrenNames()) {
                IEclipsePreferences childPreference = (IEclipsePreferences)preferences.node(childName);
                childPreference.addNodeChangeListener(prefsSyncer);
                childPreference.addPreferenceChangeListener(prefsSyncer);
            }
        } catch (BackingStoreException e) {
            LoggerFactory.getLogger(MountPointsPrefsSyncer.class).atWarn().setCause(e)
                .log("Unable to access preferences");
        }
        return true;
    }

    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        if (InstanceScope.INSTANCE.getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation())
            .equals(event.getNode().parent())) {
            Object eventValue = event.getNewValue();
            if (eventValue != null) {
                // if the eventValue is null, then the preference was removed
                List<WorkbenchMountPointSettings> newValue = getUserOrDefaultValue();
                updateSettings(m_previousValues, newValue);
                m_previousValues = newValue;
            }
        }
    }

    private static void updateSettings(final List<WorkbenchMountPointSettings> oldValues,
        final List<WorkbenchMountPointSettings> newValues) {
        if (Objects.equals(oldValues, newValues)) {
            return;
        }

        Set<WorkbenchMountPointSettings> oldSettings = new LinkedHashSet<>(oldValues);
        oldSettings.removeAll(newValues);

        Set<WorkbenchMountPointSettings> newSettings = new LinkedHashSet<>(newValues);
        // leave unchanged values untouched
        newSettings.removeAll(oldValues);

        // remove deleted mount points
        for (WorkbenchMountPointSettings ms : oldSettings) {
            boolean successful = WorkbenchMountTable.unmount(ms.mountID());
            if (!successful) {
                // most likely mount point was not present to begin with
                LOGGER.debug("Mount point \"{}\" could not be unmounted.", ms.mountID());
            }
        }

        // add all new mount points
        for (WorkbenchMountPointSettings ms : newSettings) {
            if (!ms.isActive()) {
                continue;
            }
            try {
                WorkbenchMountTable.mount(ms);
            } catch (WorkbenchMountException ex) {
                LOGGER.atError().setCause(ex).log("Mount point \"{}\" could not be mounted.", ms.mountID());
            }
        }

        // sync the ordering of the mount points
        List<String> newMountIds = new ArrayList<>();
        for (WorkbenchMountPointSettings ms : newValues) {
            newMountIds.add(ms.mountID());
        }
        WorkbenchMountTable.setMountOrder(newMountIds);
    }

    private static List<WorkbenchMountPointSettings> getUserOrDefaultValue() {
        return MountPointsPreferencesUtil.loadSortedMountSettingsFromPreferenceNode();
    }


    @Override
    public void added(final NodeChangeEvent event) {
        // AP-8989 switching to IEclipsePreferences
        if (InstanceScope.INSTANCE.getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation())
            .equals(event.getParent())) {
            IEclipsePreferences childNode = InstanceScope.INSTANCE
                .getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation() + "/" + event.getChild().name());
            childNode.addPreferenceChangeListener(this);
        }
    }

    @Override
    public void removed(final NodeChangeEvent event) {
        // AP-8989 switching to IEclipsePreferences
        if (InstanceScope.INSTANCE.getNode(MountPointsPreferencesUtil.getMountpointPreferenceLocation())
            .equals(event.getParent())) {
            List<WorkbenchMountPointSettings> newValue = getUserOrDefaultValue();
            updateSettings(m_previousValues, newValue);
        }
    }
}
