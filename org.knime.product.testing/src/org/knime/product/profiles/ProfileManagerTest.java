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
 *   09.02.2018 (thor): created
 */
package org.knime.product.profiles;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.prefs.Preferences;

/**
 * Basic test for the profile manager.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class ProfileManagerTest {
    /**
     * Sets up the test environment.
     */
    @Before
    public void setup() {
        String app = "Unknown";
        Bundle myself = FrameworkUtil.getBundle(getClass());
        if (myself != null) {
            BundleContext ctx = myself.getBundleContext();
            ServiceReference<ApplicationHandle> ser = ctx.getServiceReference(ApplicationHandle.class);
            if (ser != null) {
                ApplicationHandle appHandle = ctx.getService(ser);
                app = appHandle.getInstanceId();
                ctx.ungetService(ser);
            }
        }
        // Only KNIME applications set new default preferences
        assumeThat("Not started with a KNIME application", app, containsString(".knime."));
    }

    /**
     * Checks if preferences from two profiles have been applied correctly. The profiles are part of this fragment
     * (test-profiles/base and test-profiles/custom).
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testAppliedPreferences() throws Exception {
        IEclipsePreferences productPrefs = DefaultScope.INSTANCE.getNode("org.knime.product");
        assertThat("Unexpected preferences value for 'test-pref'", productPrefs.get("test-pref", ""), is("custom"));

        IEclipsePreferences workbenchPrefs = DefaultScope.INSTANCE.getNode("org.knime.workbench.ui");
        assertThat("Unexpected preferences value", workbenchPrefs.get("knime.gridsize.x", ""), is("3333"));
        assertThat("Unexpected preferences value", workbenchPrefs.get("knime.gridsize.y", ""), is("5555"));
    }

    /**
     * Checks if variables in preference values are replaced correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testVariablesReplacement() throws Exception {
        IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode("org.knime.product");

        String expectedEnv = System.getenv("USER");
        assertThat("Unexpected value for environment variable", prefs.get("environment-variable", "XXX"),
            is(expectedEnv));

        String expectedSysprop = System.getProperty("user.name");
        assertThat("Unexpected value for system property", prefs.get("system-property", "XXX"), is(expectedSysprop));

        assertThat("Unexpected value for profile name", prefs.get("profile-name", "XXX"), is("base"));
        Path profileLocation = Paths.get(prefs.get("profile-location", "XXX"));
        assertThat("Returned profile location " + profileLocation + " does not exist",
            Files.isDirectory(profileLocation), is(true));

        assertThat("Unexpected value for custom variable", prefs.get("custom-variable", "XXX"), is("replaced-value"));

        // unknown variables => no replacement
        assertThat("Unexpected value for unknown environment variable", prefs.get("unknown-environment-variable", "XXX"),
            is("${env:unknown}"));
        assertThat("Unexpected value for unknown system property", prefs.get("unknown-system-property", "XXX"),
            is("${sysprop:unknown}"));
        assertThat("Unexpected value for unknown custom variable", prefs.get("unknown-custom-variable", "XXX"),
            is("${custom:unknown}"));
        assertThat("Unexpected value for unknown profile variable", prefs.get("unknown-profile-variable", "XXX"),
            is("${profile:unknown}"));

        // escaped "variable" (with $$) => no replacement
        assertThat("Unexpected value for escaped variable", prefs.get("non-variable", "XXX"),
            is("bla/${custom:var}/foo"));

        // "origin" variable replacement
        IEclipsePreferences explorerPrefs = DefaultScope.INSTANCE.getNode("org.knime.workbench.explorer.view");
        Preferences mpPrefs = explorerPrefs.node("mountpointNode/test-mountpoint2");
        assertThat("Unexpected value for origin variable", mpPrefs.get("address", "XXX"),
            is("http://localhost:12345/tomee/ejb"));
        assertThat("Unexpected value for origin variable", mpPrefs.get("mountID", "XXX"),
            is("test-mountpoint2"));
    }
}
