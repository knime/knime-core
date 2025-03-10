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
 *   Apr 11, 2018 (oole): created
 */
package org.knime.core.workbench;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.knime.core.workbench.preferences.MountPointsPreferencesUtil.loadSortedMountSettingsFromPreferenceNode;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.BatchExecutor;
import org.knime.core.util.CoreConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;
import org.knime.core.workbench.preferences.MountPointsPreferencesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final class MountPointPreferencesTest {

    /**
     * Loads mount point preferences before the tests.
     *
     * @throws Exception if an error occurs
     */
    @BeforeAll
    static void loadPreferences() throws Exception {
        Bundle myself = FrameworkUtil.getBundle(MountPointPreferencesTest.class);
        URL url = FileLocator.find(myself, new Path("/files/testing.epf"), null);
        URL fileUrl = FileLocator.toFileURL(url);
        BatchExecutor.setPreferences(new File(fileUrl.getFile()));
    }

    /**
     * Testcase for AP-8989
     *
     * Loads all MountSettings, adds a new MountSetting and checks if they are all saved and loaded correctly after that.
     *
     * @throws Exception if an errors occurs
     */
    @Test
    void testMountPointLoading() throws Exception {
        List<WorkbenchMountPointSettings> initialSettings =
            loadSortedMountSettingsFromPreferenceNode();

        // required when running tests inside Eclipse with server space available
        initialSettings.removeIf(m -> CoreConstants.KNIME_HUB_MOUNT_ID.equals(m.mountID()));

        int numberOfSettings = initialSettings.size();

        List<String> initialMountIDs = initialSettings.stream().map(ms -> ms.mountID()).toList();

        assertThat(initialMountIDs, Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2"));

        String mountID = "new-mountpoint";
        String defaultMountID = "test-mountpoint";
        String factoryID = TestMountPointState.TYPE.getTypeIdentifier();
        boolean active = true;

        WorkbenchMountPointStateSettings settings = new WorkbenchMountPointStateSettings(
            Map.of("user", "oole", "address", "https://testing.knime.org/tomee/ejb"));
        WorkbenchMountPointSettings newMountSettings =
            new WorkbenchMountPointSettings(mountID, defaultMountID, factoryID, settings, active);

        initialSettings.add(newMountSettings);

        MountPointsPreferencesUtil.saveMountSettings(initialSettings);

        List<WorkbenchMountPointSettings> modifiedSettings = loadSortedMountSettingsFromPreferenceNode();
        // required when running tests inside Eclipse with server space available
        modifiedSettings.removeIf(m -> CoreConstants.KNIME_HUB_MOUNT_ID.equals(m.mountID()));


        List<String> newMountIDs= modifiedSettings.stream().map(ms -> ms.mountID()).toList();

        assertThat(modifiedSettings.size(), Matchers.is(numberOfSettings + 1));
        assertThat(newMountIDs,
            Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2", newMountSettings.mountID()));
    }

    /**
     * Testcase for AP-8989
     *
     * Loads all MountSettings, overwrites one of the default MountSettings and checks that the default settings is hidden/overwritten.
     *
     * @throws Exception if an errors occurs
     */
    @Test
    void testDefaultOverwrite() throws Exception {
        List<WorkbenchMountPointSettings> initialSettings = loadSortedMountSettingsFromPreferenceNode();
        int numberOfSettings = initialSettings.size();

        Optional<WorkbenchMountPointSettings> optMS =
            initialSettings.stream().filter(ms -> ms.mountID().equals("test-mountpoint1")).findFirst();

        WorkbenchMountPointSettings oldMountSettings = optMS.orElse(null);
        assertThat(oldMountSettings, Matchers.notNullValue());

        String mountID = "test-mountpoint1";
        String defaultMountID = "test-mountpoint";

        assertThat(oldMountSettings.mountID(), Matchers.equalTo(mountID));
        assertThat(oldMountSettings.mountPointStateSettings().props().get("user"), Matchers.equalTo("knuser1"));

        String factoryID = TestMountPointState.TYPE.getTypeIdentifier();
        boolean active = true;

        WorkbenchMountPointStateSettings settings = new WorkbenchMountPointStateSettings(
            Map.of("user", "oole", "address", "https://testing.knime.org/tomee/ejb"));
        WorkbenchMountPointSettings newMountSettings =
            new WorkbenchMountPointSettings(mountID, defaultMountID, factoryID, settings, active);

        // replace the old mount settings with the new one
        initialSettings.removeIf(ms -> ms.mountID().equals(mountID));
        initialSettings.add(newMountSettings);

        MountPointsPreferencesUtil.saveMountSettings(initialSettings);

        List<WorkbenchMountPointSettings> modifiedSettings = loadSortedMountSettingsFromPreferenceNode();

        assertThat(modifiedSettings.size(), Matchers.equalTo(numberOfSettings));

        Optional<WorkbenchMountPointSettings> optoverwritteMS =
            modifiedSettings.stream().filter(ms -> ms.mountID().equals("test-mountpoint1")).findFirst();

        WorkbenchMountPointSettings overwrittenMountSettings = optoverwritteMS.orElse(null);

        assertThat(overwrittenMountSettings, Matchers.notNullValue());
        assertThat(oldMountSettings, Matchers.not(Matchers.equalTo(overwrittenMountSettings)));
        assertThat(overwrittenMountSettings, Matchers.equalTo(newMountSettings));
    }
}
