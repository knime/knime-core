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
 *   02.02.2018 (thor): created
 */
package org.knime.product.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Implementation of a profile provider that reads the required parameters from a properties file in the workspace. This
 * file is created by the profile preference page and is expected to contain two properties <tt>profileList</tt> and
 * <tt>profileLocation</tt>. The list should be a comma- or colon-separated list of strings whereas the location should
 * a URI (or an absolute file system path).
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class WorkspaceProfileProvider implements IProfileProvider {
    /**
     * The file that contains the workspace settings. May not exist.
     */
    public static final Path SETTINGS_FILE;

    /**
     * Constant for the name of the profile location property: {@value}.
     */
    public static final String PROFILE_LOCATION = "profileLocation";

    /**
     * Constant for the name of the profile list property: {@value}.
     */
    public static final String PROFILE_LIST = "profileList";

    /**
     * Constant for the name of the property that tells whether profiles are enabled or not: {@value}.
     */
    public static final String PROFILES_ENABLED = "profilesEnabled";


    static {
        Bundle myself = FrameworkUtil.getBundle(WorkspaceProfileProvider.class);
        Path stateDir = Platform.getStateLocation(myself).toFile().toPath();
        SETTINGS_FILE = stateDir.resolve("profile-settings.ini");
    }

    private List<String> m_requestedProfiles = Collections.emptyList();

    private URI m_profilesLocation;

    /**
     * Creates a new profile provider.
     */
    public WorkspaceProfileProvider() {
        try {
            readWorkspaceSettings();
        } catch (IOException | URISyntaxException ex) {
            Bundle myself = FrameworkUtil.getBundle(getClass());
            Platform.getLog(myself).log(new Status(IStatus.ERROR, myself.getSymbolicName(),
                "Could not read profile settings from workspace: " + ex.getMessage(), ex));
        }
    }

    private void readWorkspaceSettings() throws IOException, URISyntaxException {
        if (Files.isRegularFile(SETTINGS_FILE)) {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(SETTINGS_FILE)) {
                props.load(is);
            }

            boolean enabled = Boolean.valueOf(props.getProperty(PROFILES_ENABLED, "false"));
            String profileLocation = props.getProperty(PROFILE_LOCATION);
            if (enabled && (profileLocation != null)) {
                m_profilesLocation = new URI(profileLocation);
                String requestedProfiles = props.getProperty(PROFILE_LIST, "");
                m_requestedProfiles = Arrays.asList(requestedProfiles.split("[,;:]"));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRequestedProfiles() {
        return m_requestedProfiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getProfilesLocation() {
        if (m_profilesLocation == null) {
            throw new IllegalArgumentException("No profile location was provided");
        }
        return m_profilesLocation;
    }
}
