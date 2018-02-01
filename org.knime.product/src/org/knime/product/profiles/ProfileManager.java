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
 *   31.01.2018 (thor): created
 */
package org.knime.product.profiles;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class ProfileManager {
    private static final ProfileManager INSTANCE = new ProfileManager();

    public static ProfileManager getInstance() {
        return INSTANCE;
    }

    private final IProfileProvider m_provider;

    private ProfileManager() {
        IProfileProvider provider = new DefaultProfileProvider();
        if (provider.getRequestedProfiles().isEmpty()) {
            // only if no profile arguments have been provided on the command line we check for an extension
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint("org.knime.product.profileProvider");

            Optional<IConfigurationElement> extension =
                    Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements())).findFirst();

            if (extension.isPresent()) {
                try {
                    provider = (IProfileProvider)extension.get().createExecutableExtension("class");
                } catch (CoreException ex) {
                    Bundle b = FrameworkUtil.getBundle(getClass());
                    Platform.getLog(b).log(new Status(IStatus.ERROR, b.getSymbolicName(),
                        "Could not create profile provider instance from class " + extension.get().getAttribute("class")
                        + ". Using the default provider instead.",
                        ex));
                }
            }

        }

        m_provider = provider;
    }

    public void applyProfiles() throws CoreException, IOException {
        List<Path> localProfiles = fetchProfileContents();
        applyPreferences(localProfiles);
    }


    @SuppressWarnings("restriction")
    private void applyPreferences(final List<Path> profiles) throws IOException {
        Properties props = new Properties();
        for (Path dir : profiles) {
            List<Path> prefFiles = Files.walk(dir).filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".epf"))
                .collect(Collectors.toList());
            for (Path f : prefFiles) {
                try (Reader r = Files.newBufferedReader(f, Charset.forName("UTF-8"))) {
                    props.load(r);
                }
            }
        }

        Path pluginCustFile = Files.createTempFile("pluginCustomization", ".ini");
        // It's important here to write to a stream and not a reader because when reading the file back in
        // org.eclipse.core.internal.preferences.DefaultPreferences.loadProperties(String) also reads from a stream
        // and therefore assumes it's ISO-8859-1 encoded (with replacement for UTF characters).
        try (OutputStream out = Files.newOutputStream(pluginCustFile)) {
            props.store(out, "");
        }
        DefaultPreferences.pluginCustomizationFile = pluginCustFile.toAbsolutePath().toString();
    }

    private List<Path> fetchProfileContents() throws CoreException {
        List<String> profiles = m_provider.getRequestedProfiles();
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }

        URI profileLocation = m_provider.getProfilesLocation();
        Path localProfileLocation;
        if ("file".equalsIgnoreCase(profileLocation.getScheme())) {
            localProfileLocation = Paths.get(profileLocation);
        } else if (profileLocation.getScheme().startsWith("http")) {
            localProfileLocation = downloadProfiles();
        } else {
            throw new IllegalArgumentException("Profile from '" + profileLocation.getScheme() + " are not supported");
        }

        return profiles.stream().map(p -> localProfileLocation.resolve(p).normalize())
                .filter(p -> Files.isDirectory(p))
                .filter(p -> p.startsWith(localProfileLocation)) // remove profiles that are outside the root
                .collect(Collectors.toList());
    }


    private Path downloadProfiles() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
