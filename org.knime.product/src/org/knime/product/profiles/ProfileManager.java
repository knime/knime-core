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
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
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

    private final List<Runnable> m_collectedLogs = new ArrayList<>(2);

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

    private Supplier<IProfileProvider> getExtensionPointProviderSupplier() {
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
                    m_collectedLogs.add(() -> NodeLogger.getLogger(ProfileManager.class).error(
                        "Could not create profile provider instance from class "
                            + extension.get().getAttribute("class") + ". No profiles will be processed.",
                        ex));
                }
            }
            return provider;
        };
    }

    /**
     * Apply the available profiles to this instance. This includes setting new default preferences and copying
     * supplementary files to instance's configuration area.
     */
    public void applyProfiles() {
        List<Path> localProfiles = fetchProfileContents();
        try {
            applyPreferences(localProfiles);
        } catch (IOException ex) {
            m_collectedLogs.add(() -> NodeLogger.getLogger(ProfileManager.class)
                .error("Could not apply preferences from profiles: " + ex.getMessage(), ex));
        }

        m_collectedLogs.stream().forEach(r -> r.run());
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

        Path pluginCustFile = PathUtils.createTempFile("pluginCustomization", ".ini");
        // It's important here to write to a stream and not a reader because when reading the file back in
        // org.eclipse.core.internal.preferences.DefaultPreferences.loadProperties(String) also reads from a stream
        // and therefore assumes it's ISO-8859-1 encoded (with replacement for UTF characters).
        try (OutputStream out = Files.newOutputStream(pluginCustFile)) {
            props.store(out, "");
        }
        DefaultPreferences.pluginCustomizationFile = pluginCustFile.toAbsolutePath().toString();
    }

    private List<Path> fetchProfileContents() {
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
            throw new IllegalArgumentException("Profiles from '" + profileLocation.getScheme() + " are not supported");
        }

        return profiles.stream().map(p -> localProfileLocation.resolve(p).normalize())
                .filter(p -> Files.isDirectory(p))
                // remove profiles that are outside the profile root (e.g. with "../" in their name)
                .filter(p -> p.startsWith(localProfileLocation))
                .collect(Collectors.toList());
    }


    private Path downloadProfiles(final URI profileLocation) {
        Bundle myself = FrameworkUtil.getBundle(getClass());
        Path stateDir = Platform.getStateLocation(myself).toFile().toPath();
        Path profileDir = stateDir.resolve("profiles");

        try {
            Files.createDirectories(stateDir);

            URIBuilder builder = new URIBuilder(profileLocation);
            builder.addParameter("profiles", String.join(",", m_provider.getRequestedProfiles()));
            URI profileUri = builder.build();

            m_collectedLogs
                .add(() -> NodeLogger.getLogger(ProfileManager.class).info("Downloading profiles from " + profileUri));

            // proxies
            HttpHost proxy = ProxySelector.getDefault().select(profileUri).stream()
                    .filter(p -> p.address() != null)
                    .findFirst()
                    .map(p -> new HttpHost(((InetSocketAddress) p.address()).getAddress()))
                    .orElse(null);

            // timeout; we cannot access KNIMEConstants here because that would acccess preferences
            int timeout = 2000;
            String to = System.getProperty("knime.url.timeout", Integer.toString(timeout));
            try {
                timeout = Integer.parseInt(to);
            } catch (NumberFormatException ex) {
                m_collectedLogs.add(() -> NodeLogger.getLogger(ProfileManager.class)
                    .warn("Illegal value for system property knime.url.timeout :" + to, ex));
            }
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setProxy(proxy)
                    .setConnectionRequestTimeout(timeout)
                    .build();


            try (CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setRedirectStrategy(new DefaultRedirectStrategy()).build()) {
                HttpGet get = new HttpGet(profileUri);

                if (Files.isDirectory(profileDir)) {
                    Instant lastModified = Files.getLastModifiedTime(profileDir).toInstant();
                    get.setHeader("If-Modified-Since",
                        DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atZone(ZoneId.of("GMT"))));
                }

                try (CloseableHttpResponse response = client.execute(get)) {
                    int code = response.getStatusLine().getStatusCode();
                    if ((code >= 200) && (code < 300)) {
                        Header ct = response.getFirstHeader("Content-Type");
                        if ((ct == null) || (ct.getValue() == null) || !ct.getValue().startsWith("application/zip")) {
                            // this is a workaround because ZipInputStream doesn't complain when the read contents are
                            // no zip file - it just processes an empty zip
                            throw new IOException("Server did not return a ZIP file containing the selected profiles");
                        }

                        Path tempDir = PathUtils.createTempDir("profile-download", stateDir);
                        try (InputStream is = response.getEntity().getContent()) {
                            PathUtils.unzip(new ZipInputStream(is), tempDir);
                        }

                        // replace profiles only if new data has been downloaded successfully
                        PathUtils.deleteDirectoryIfExists(profileDir);
                        Files.move(tempDir, profileDir, StandardCopyOption.ATOMIC_MOVE);
                    } else if (code != 304) { // 304 = Not Modified
                        HttpEntity body = response.getEntity();
                        String msg;
                        if (body.getContentType().getValue().startsWith("text/")) {
                            byte[] buf = new byte[Math.min(4096, Math.max(4096, (int)body.getContentLength()))];
                            int read = body.getContent().read(buf);
                            msg = new String(buf, 0, read, "US-ASCII").trim();
                        } else if (!response.getStatusLine().getReasonPhrase().isEmpty()) {
                            msg = response.getStatusLine().getReasonPhrase();
                        } else {
                            msg = "Server returned status " + response.getStatusLine().getStatusCode();
                        }

                        throw new IOException(msg);
                    }
                }
            }
        } catch (IOException | URISyntaxException ex) {
            String msg = "Could not download profiles from " + profileLocation + ": " + ex.getMessage() + ". "
                + (Files.isDirectory(profileDir) ? "Will use existing but potentially outdated profiles."
                    : "No profiles will be applied.");
            m_collectedLogs.add(() -> NodeLogger.getLogger(ProfileManager.class).error(msg, ex));
        }

        return profileDir;
    }
}
