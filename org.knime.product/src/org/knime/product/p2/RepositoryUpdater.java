/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   21.03.2014 (thor): created
 */
package org.knime.product.p2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Singleton class that updates artifact repository URIs to include the instance ID. The singleton registers itself as a
 * listener to the p2 event bus so that it can react on changes to repository URIs immediately.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
@SuppressWarnings("restriction")
public class RepositoryUpdater implements ProvisioningListener {
    /**
     * Singleton instance.
     */
    public static final RepositoryUpdater INSTANCE = new RepositoryUpdater();

    private RepositoryUpdater() {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<IProvisioningAgent> ref = context.getServiceReference(IProvisioningAgent.class);
        if (ref != null) {
            IProvisioningAgent agent = context.getService(ref);
            try {
                IProvisioningEventBus eventBus =
                    (IProvisioningEventBus)agent.getService(IProvisioningEventBus.SERVICE_NAME);
                if (eventBus != null) {
                    // is null if started from the SDK
                    eventBus.addListener(this);
                }
            } finally {
                context.ungetService(ref);
            }
        }
    }

    /**
     * Updates the URLs of all enabled artifact repository by adding the KNIME ID to them.
     */
    public void updateArtifactRepositoryURLs() {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<IProvisioningAgent> ref = context.getServiceReference(IProvisioningAgent.class);
        if (ref != null) {
            IProvisioningAgent agent = context.getService(ref);
            try {
                IArtifactRepositoryManager repoManager =
                    (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
                if (repoManager != null) {
                    // is null if started from the SDK
                    for (URI uri : repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL)) {
                        updateArtifactRepositoryURL(repoManager, uri, true);
                    }
                }
            } finally {
                context.ungetService(ref);
            }
        }
    }

    /**
     * Adds default KNIME repositories if they don't already exist.
     */
    public void addDefaultRepositories() {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<IProvisioningAgent> ref = context.getServiceReference(IProvisioningAgent.class);
        if (ref != null) {
            IProvisioningAgent agent = context.getService(ref);
            try {
                IMetadataRepositoryManager metadataManager =
                    (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
                URL usFileUrl = FrameworkUtil.getBundle(getClass()).getEntry("/update-sites.txt");

                if ((metadataManager != null) && (usFileUrl != null)) {
                    processDefaultRepositories(metadataManager, usFileUrl);
                }
            } finally {
                context.ungetService(ref);
            }
        }
    }

    private void processDefaultRepositories(final IMetadataRepositoryManager repoManager, final URL usFileUrl) {
        Set<URI> knownRepositories =
            new HashSet<>(Arrays.asList(repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL
                | IRepositoryManager.REPOSITORIES_DISABLED)));

        IEclipsePreferences preferences =
            InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(usFileUrl.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",");
                URI uri = new URI(parts[0]);

                String oldPrefName = uri.toString(); // preference key until 2.11
                String newPrefName = "us_info-" + uri; // preference key since 2.12

                // Only make any modification if this is the first time. We store the information and if the user
                // has re-added or removed an entry himself, we don't try it a second time
                if ("remove".equals(parts[1]) && !preferences.getBoolean(oldPrefName + "-removed", false)
                    && !preferences.getBoolean(newPrefName + "-removed", false)) {
                    repoManager.removeRepository(uri);
                    preferences.putBoolean(newPrefName + "-removed", true);
                } else if ("add".equals(parts[1])
                    && (knownRepositories.isEmpty() || (!knownRepositories.contains(uri)
                        && !preferences.getBoolean(oldPrefName + "-added", false) && !preferences.getBoolean(
                        newPrefName + "-added", false)))) {
                    repoManager.addRepository(uri);
                    repoManager.setEnabled(uri, (parts.length > 2) && "enabled".equals(parts[2]));
                    if (parts.length > 3) {
                        repoManager.setRepositoryProperty(uri, IRepository.PROP_NAME, parts[3]);
                    }

                    preferences.putBoolean(newPrefName + "-added", true);
                }
            }
        } catch (IOException | URISyntaxException ex) {
            NodeLogger.getLogger(getClass()).error("Error while adding KNIME Update Sites: " + ex.getMessage(), ex);
        }
    }

    /**
     * Updates the URI of a single repository. An update is only performed if the URI does not already contains the
     * KNIME ID (see {@link #urlContainsID(URI)}). Only HTTP(S) URIs from knime.org or knime.com hosts are updated.
     *
     * @param repoManager an artifact repository manager
     * @param uri a URI to a repository, must be known by the repository manager
     * @param enabled <code>true</code> if the corresponding metadata repository is enabled, <code>false</code>
     *            otherwise; the artifact repository gets the same state
     */
    private void updateArtifactRepositoryURL(final IArtifactRepositoryManager repoManager, final URI uri,
        final boolean enabled) {
        if (uri.getScheme().startsWith("http") && isKnimeURI(uri)) {
            if (!urlContainsID(uri)) {
                String knidPath =
                    (uri.getPath().endsWith("/") ? "" : "/") + "knid=" + KNIMEConstants.getKNIMEInstanceID() + "/";
                try {
                    URI newUri =
                        new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath()
                            + knidPath, uri.getQuery(), uri.getFragment());
                    repoManager.addRepository(newUri);
                    repoManager.setEnabled(newUri, enabled);

                    repoManager.removeRepository(uri);
                } catch (URISyntaxException ex) {
                    NodeLogger
                        .getLogger(getClass())
                        .error(
                            "Error while updating artifact repository URI '" + uri.toString() + "': " + ex.getMessage(),
                            ex);
                }
            } else {
                repoManager.setEnabled(uri, enabled);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notify(final EventObject o) {
        if (o instanceof RepositoryEvent) {
            RepositoryEvent event = (RepositoryEvent)o;
            if ((event.getKind() == RepositoryEvent.ADDED) && !urlContainsID(event.getRepositoryLocation())) {
                // Update/add artifact repository location when a new repository is added. Even though Eclipse
                // adds the corresponding artifact repository by itself, reloading an existing repository, will only
                // remove and add the metadata repository. Therefore we need to re-add the artifact repository in such
                // cases "manually".
                updateArtifactRepository(event);
            } else if ((event.getKind() == RepositoryEvent.REMOVED)
                && (event.getRepositoryType() == IRepository.TYPE_METADATA)
                && isKnimeURI(event.getRepositoryLocation())) {

                // remove modified artifact repository when the corresponding metadata repository is removed
                removeOutdatedArtifactRepository(event);
            } else if ((event.getKind() == RepositoryEvent.ENABLEMENT)
                && (event.getRepositoryType() == IRepository.TYPE_METADATA)
                && isKnimeURI(event.getRepositoryLocation())) {
                // en- or disable the corresponding artifact repository
                updateArtifactRepository(event);
            }
        }
    }

    private void removeOutdatedArtifactRepository(final RepositoryEvent event) {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<IProvisioningAgent> ref = context.getServiceReference(IProvisioningAgent.class);
        if (ref != null) {
            IProvisioningAgent agent = context.getService(ref);
            try {
                IArtifactRepositoryManager repoManager =
                    (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

                if (repoManager != null) {
                    URI removedMetadatalocation = event.getRepositoryLocation();
                    String correspondingArtifactRepoPrefix = removedMetadatalocation.toString();
                    if (!correspondingArtifactRepoPrefix.endsWith("/")) {
                        correspondingArtifactRepoPrefix += "/";
                    }
                    correspondingArtifactRepoPrefix += "knid=";

                    for (URI uri : repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL)) {
                        if (urlContainsID(uri) && uri.toString().startsWith(correspondingArtifactRepoPrefix)) {
                            repoManager.removeRepository(uri);
                        }
                    }
                    for (URI uri : repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)) {
                        if (urlContainsID(uri) && uri.toString().startsWith(correspondingArtifactRepoPrefix)) {
                            repoManager.removeRepository(uri);
                        }
                    }
                }
            } finally {
                context.ungetService(ref);
            }
        }
    }

    private void updateArtifactRepository(final RepositoryEvent event) {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<IProvisioningAgent> ref = context.getServiceReference(IProvisioningAgent.class);
        if (ref != null) {
            IProvisioningAgent agent = context.getService(ref);
            try {
                IArtifactRepositoryManager repoManager =
                    (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
                if (repoManager != null) {
                    updateArtifactRepositoryURL(repoManager, event.getRepositoryLocation(), event.isRepositoryEnabled());
                }
            } finally {
                context.ungetService(ref);
            }
        }
    }

    private static final Pattern KNID_PATTERN = Pattern
        .compile("/knid=[0-9a-fA-F]{2,2}-[0-9a-fA-F]{16,16}(?:-[0-9a-fA-F]+){0,}/");

    private static boolean urlContainsID(final URI uri) {
        return (uri.getPath() != null) && KNID_PATTERN.matcher(uri.getPath()).find();
    }

    private static final Pattern KNIME_HOST_PATTERN = Pattern.compile("^(?:www|tech|update)\\.knime\\.(?:org|com)$");

    private static boolean isKnimeURI(final URI uri) {
        return (uri.getHost() != null) && KNIME_HOST_PATTERN.matcher(uri.getHost()).matches();
    }
}
