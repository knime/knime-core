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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.knime.core.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Manager for profiles that should be applied during startup. This includes custom default preferences and
 * supplementary files such as database drivers. The profiles must be applied as early as possible during startup,
 * ideally as the first command in the application's start method.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class ProfileManager {
    private static final ProfileManager INSTANCE = new ProfileManager();

    /**
     * Returns the singleton instance.
     *
     * @return the singleton, never <code>null</code>
     */
    public static ProfileManager getInstance() {
        return INSTANCE;
    }

    private final IProfileProvider m_provider;

    private ProfileManager() {
        List<Supplier<IProfileProvider>> potentialProviders = Arrays.asList(
            () -> new CommandlineProfileProvider(),
            () -> new WorkspaceProfileProvider(),
            getExtensionPointProviderSupplier());

        m_provider = potentialProviders.stream().map(s -> s.get())
                .filter(p -> !p.getRequestedProfiles().isEmpty())
                .findFirst().orElse(new EmptyProfileProvider());
    }

    private static Supplier<IProfileProvider> getExtensionPointProviderSupplier() {
        return () -> {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint("org.knime.product.profileProvider");

            Optional<IConfigurationElement> extension =
                    Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements())).findFirst();

            IProfileProvider provider = new EmptyProfileProvider();
            if (extension.isPresent()) {
                try {
                    provider = (IProfileProvider)extension.get().createExecutableExtension("class");
                } catch (CoreException ex) {
                    Bundle b = FrameworkUtil.getBundle(ProfileManager.class);
                    Platform.getLog(b).log(new Status(IStatus.ERROR, b.getSymbolicName(),
                        "Could not create profile provider instance from class " + extension.get().getAttribute("class")
                        + ". No profiles will be processed.",
                        ex));
                }
            }
            return provider;
        };
    }

    /**
     * Apply the available profiles to this instance. This includes setting new default preferences and copying
     * supplementary files to instance's configuration area.
     *
     * @throws IOException if an I/O error occurs while processing files
     */
    public void applyProfiles() throws IOException {
        List<Path> localProfiles = fetchProfileContents();
        applyPreferences(localProfiles);
    }


    @SuppressWarnings("restriction")
    private void applyPreferences(final List<Path> profiles) throws IOException {
        if (DefaultPreferences.pluginCustomizationFile != null) {
            return; // plugin customizations are already explicitly provided by someone else
        }

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

        // removed "/instance" prefixes from preferences because otherwise they are not applied as default preferences
        // (because they are instance preferences...)
        for (Object key : new HashSet<>(props.keySet())) {
            if (key.toString().startsWith("/instance/")) {
                Object value = props.remove(key);
                props.put(key.toString().substring("/instance/".length()), value);
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

    private List<Path> fetchProfileContents() throws IOException {
        List<String> profiles = m_provider.getRequestedProfiles();
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }

        URI profileLocation = m_provider.getProfilesLocation();
        Path localProfileLocation;
        if ("file".equalsIgnoreCase(profileLocation.getScheme())) {
            localProfileLocation = Paths.get(profileLocation);
        } else if (profileLocation.getScheme().startsWith("http")) {
            localProfileLocation = downloadProfiles(profileLocation);
        } else {
            throw new IllegalArgumentException("Profile from '" + profileLocation.getScheme() + " are not supported");
        }

        return profiles.stream().map(p -> localProfileLocation.resolve(p).normalize())
                .filter(p -> Files.isDirectory(p))
                .filter(p -> p.startsWith(localProfileLocation)) // remove profiles that are outside the root
                .collect(Collectors.toList());
    }


    private Path downloadProfiles(final URI profileLocation) throws IOException {
        Bundle myself = FrameworkUtil.getBundle(getClass());
        Path stateDir = Platform.getStateLocation(myself).toFile().toPath();
        Files.createDirectories(stateDir);

        Path profileDir = stateDir.resolve("profiles");
        URIBuilder builder = new URIBuilder(profileLocation);
        builder.addParameter("profiles", String.join(",", m_provider.getRequestedProfiles()));
        try {
            URL profileContentUrl = builder.build().toURL();
            Path tempDir = Files.createTempDirectory(stateDir, "profile-download");
            URLConnection conn = profileContentUrl.openConnection();

            int timeout = 2000;
            String to = System.getProperty("knime.url.timeout", Integer.toString(timeout));
            try {
                timeout = Integer.parseInt(to);
            } catch (NumberFormatException ex) {
                Platform.getLog(myself).log(new Status(IStatus.WARNING, myself.getSymbolicName(),
                    "Illegal value for system property knime.url.timeout :" + to, ex));
            }
            conn.setConnectTimeout(timeout);

            try (InputStream is = conn.getInputStream()) {
                PathUtils.unzip(new ZipInputStream(is), tempDir);
            }

            // replace profiles only if new data has been downloaded successfully
            PathUtils.deleteDirectoryIfExists(profileDir);
            Files.move(tempDir, profileDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        } catch (IOException ex) {
            if (Files.isDirectory(profileDir)) {
                // Use existing files for now
                String msg = "Could not download profiles from " + profileLocation + ": " + ex.getMessage() + ". " +
                        "Will use existing but potentially outdated profiles.";
                Platform.getLog(myself).log(new Status(IStatus.ERROR, myself.getSymbolicName(), msg, ex));
            } else {
                throw ex;
            }
        }

        return profileDir;
    }
}
