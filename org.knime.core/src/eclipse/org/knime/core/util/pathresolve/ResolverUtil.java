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
 * History
 *   18.07.2012 (meinl): created
 */
package org.knime.core.util.pathresolve;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.internal.knimeurl.ExplorerURLStreamHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService.ItemInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility class for resolving URIs (e.g. on a server inside a KNIME TeamSpace) into local files.
 *
 * @see URIToFileResolve
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public final class ResolverUtil {

    /** A location within the workflow directory where some "KNIME native" nodes put temporary data. Currently used by
     * "Create Temp Directory" and "File Upload (Widget)" nodes to cache temporary data created or received from a
     * webportal application. The files will be deleted when the workflow is discarded (deleted by the nodes itself).
     *
     * This field will likely change in future versions (and also the concept where to put such temporary data) and
     * hence should not be relied on by 3rd party node implementations.
     *
     * @noreference This field is not intended to be referenced by clients.
     * @since 4.2
     */
    public static final String IN_WORKFLOW_TEMP_DIR = "tmp";

    private static final ServiceTracker<URIToFileResolve, URIToFileResolve> SERVICE_TRACKER;

    static {
        Bundle coreBundle = FrameworkUtil.getBundle(ResolverUtil.class);
        if (coreBundle != null) {
            SERVICE_TRACKER = new ServiceTracker<>(coreBundle.getBundleContext(), URIToFileResolve.class, null);
            SERVICE_TRACKER.open();
        } else {
            SERVICE_TRACKER = null;
        }
    }

    /**
     * Fetches a service implementing the {@link URIToFileResolve} interface and
     * returns the resolved file.
     *
     * @param uri The URI to resolve
     * @return The local file underlying the URI (if any)
     * @throws ResourceAccessException If no service is registered or the URI can't be
     *             resolved.
     */
    public static File resolveURItoLocalFile(final URI uri) throws ResourceAccessException {
        return resolveURItoLocalFile(uri, new NullProgressMonitor());
    }

    /**
     * Fetches a service implementing the {@link URIToFileResolve} interface and
     * returns the resolved file.
     *
     * @param uri The URI to resolve
     * @param monitor a progress monitor, must not be <code>null</code>
     * @return The local file underlying the URI (if any), or <code>null</code> if the URI does not denote a local file
     * @throws ResourceAccessException If no service is registered or the URI can't be
     *             resolved.
     */
    public static File resolveURItoLocalFile(final URI uri, final IProgressMonitor monitor)
        throws ResourceAccessException {
        if (uri == null) {
            throw new ResourceAccessException("Can't resolve null URI to file");
        }
        // try resolving file-URIs without helper
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\": it does not have a scheme");
        }
        if (scheme.equalsIgnoreCase("file")) {
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new ResourceAccessException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        }
        if (SERVICE_TRACKER == null) {
            throw new ResourceAccessException("Core bundle is not active, can't resolve URI \"" + uri + "\"");
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\"; no URI resolve service registered");
        }
        return res.resolveToFile(uri);
    }

    /**
     * Fetches a service implementing the {@link URIToFileResolve} interface and
     * returns the resolved file.
     *
     * @param uri The URI to resolve
     * @return The local file or temporary copy of a remote file underlying the
     *         URI (if any)
     * @throws ResourceAccessException If no service is registered or the URI can't be
     *             resolved.
     */
    public static File resolveURItoLocalOrTempFile(final URI uri) throws ResourceAccessException {
        return resolveURItoLocalOrTempFile(uri, new NullProgressMonitor());
    }

    /**
     * Fetches a service implementing the {@link URIToFileResolve} interface and
     * returns the resolved file.
     *
     * @param uri The URI to resolve
     * @param monitor a progress monitor, must not be <code>null</code>
     * @return The local file or temporary copy of a remote file underlying the
     *         URI (if any)
     * @throws ResourceAccessException If no service is registered or the URI can't be
     *             resolved.
     */
    public static File resolveURItoLocalOrTempFile(final URI uri, final IProgressMonitor monitor)
        throws ResourceAccessException {
        File localFile = resolveURItoLocalFile(uri, monitor);
        if (localFile != null) {
            return localFile;
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\"; no URI resolve service registered");
        }
        return res.resolveToLocalOrTempFile(uri, monitor);
    }

    /**
     * Fetches a service implementing the {@link URIToFileResolve} interface and
     * returns the resolved file.
     *
     * @param uri The URI to resolve
     * @param monitor a progress monitor, must not be <code>null</code>
     * @param ifModifiedSince the if-modified-since date for a conditional request; can be <code>null</code>
     * @return The local file or temporary copy of a remote file underlying the
     *         URI (if any)
     * @throws ResourceAccessException If no service is registered or the URI can't be
     *             resolved.
     * @since 4.3
     */
    public static Optional<File> resolveURItoLocalOrTempFileConditional(final URI uri, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        File localFile = resolveURItoLocalFile(uri, monitor);
        if (localFile != null) {
            return Optional.of(localFile);
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\"; no URI resolve service registered");
        }
        return res.resolveToLocalOrTempFileConditional(uri, monitor, ifModifiedSince);
    }

    /**
     * Checks whether the passed URI is a mountpoint relative URI.
     * @param uri to check
     * @return true, if URI has KNIME FS scheme and is a mount point relative URI
     * @throws ResourceAccessException if things went bad.
     * @since 2.8
     */
    public static boolean isMountpointRelativeURL(final URI uri) throws ResourceAccessException {
        if (uri == null) {
            throw new ResourceAccessException("Can't check null URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new ResourceAccessException("Can't check URI \"" + uri + "\": it does not have a scheme");
        }
        if (scheme.equalsIgnoreCase("file")) {
            return false;
        }
        if (SERVICE_TRACKER == null) {
            throw new ResourceAccessException("Core bundle is not active, can't resolve URI \"" + uri + "\"");
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\"; no URI resolve service registered");
        }
        return res.isMountpointRelative(uri);
    }

    /**
     * Checks whether the passed URI is a workflow relative URI.
     * @param uri to check
     * @return true, if URI has KNIME FS scheme and is a workflow relative URI
     * @throws ResourceAccessException if things went bad.
     * @since 2.8
     */
    public static boolean isWorkflowRelativeURL(final URI uri) throws ResourceAccessException {
        if (uri == null) {
            throw new ResourceAccessException("Can't check null URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new ResourceAccessException("Can't check URI \"" + uri + "\": it does not have a scheme");
        }
        if (scheme.equalsIgnoreCase("file")) {
            return false;
        }
        if (SERVICE_TRACKER == null) {
            throw new ResourceAccessException("Core bundle is not active, can't resolve URI \"" + uri + "\"");
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI \"" + uri + "\"; no URI resolve service registered");
        }
        return res.isWorkflowRelative(uri);
    }

    /**
     * Retrieves the {@link KNIMEURIDescription} for a given URI. The description is useful for displaying the mountpoint and
     * the actual file path to the user in e.g. config dialogs. Especially with new id-based Hub-URIs, it otherwise
     * would very difficult for the user to tell where the URI actually points to.
     *
     * @param uri the URI to be resolved
     * @param monitor ProgressMonitor
     * @return Optional of a KNIMEURLDescription
     * @since 4.7
     */
    public static Optional<KNIMEURIDescription> toDescription(final URI uri, final IProgressMonitor monitor) {
        if (SERVICE_TRACKER != null) {
            var resolver = SERVICE_TRACKER.getService();
            if (resolver != null) {
                return resolver.toDescription(uri, monitor);
            }
        }
        return Optional.empty();
    }

    /**
     * @deprecated use {@link ResolverUtil#getHubItemVersions(URI)}
     */
    @Deprecated(since = "5.1.0")
    public static Optional<List<SpaceVersion>> getSpaceVersions(final URI uri, final IProgressMonitor monitor)
        throws Exception {
        if (SERVICE_TRACKER != null) {
            var resolver = SERVICE_TRACKER.getService();
            if (resolver != null) {
                return resolver.getSpaceVersions(uri);
            }
        }
        return Optional.empty();
    }

    /**
     * @param uri to an item (workflow, workflow project, shared component)
     * @return the version history of the given item
     * @deprecated use {@link ResolverUtil#getHubItemVersionList(URI)}
     * @since 5.1
     */
    @Deprecated(since = "5.4")
    public static List<NamedItemVersion> getHubItemVersions(final URI uri) {
        try {
            return getHubItemVersionList(uri);
        } catch (ResourceAccessException ex) {
            throw ExceptionUtils.asRuntimeException(ex);
        }
    }

    /**
     * @param uri to an item (workflow, workflow project, shared component)
     * @return the version history of the given item
     * @throws ResourceAccessException
     * @since 5.4
     */
    public static List<NamedItemVersion> getHubItemVersionList(final URI uri) throws ResourceAccessException {
        CheckUtils.checkState(SERVICE_TRACKER != null, "No service available to resolve uri: %s", uri);
        var resolver = SERVICE_TRACKER.getService();
        CheckUtils.checkState(resolver != null, "No resolver available to resolve uri: %s", uri);
        return resolver.getHubItemVersionList(uri);
    }

    /**
     * Resolved the correct {@link MountPointURLService} for the given KNIME URL.
     *
     * @param knimeUrl KNIME URL (may be relative)
     * @return resolved URL service if found, {@link Optional#empty()} otherwise
     * @since 5.9
     */
    public static Optional<MountPointURLService> getURLService(final URI knimeUrl) {
        CheckUtils.checkState(SERVICE_TRACKER != null, "No service available to resolve uri: %s", knimeUrl);
        var resolver = SERVICE_TRACKER.getService();
        CheckUtils.checkState(resolver != null, "No resolver available to resolve uri: %s", knimeUrl);
        return resolver.getURLService(knimeUrl);
    }

    /**
     * Fetches information about an item referenced by a KNIME URL.
     *
     * @param url KNIME URL referencing the item
     * @param monitor progress monitor for cancellation
     * @return item info if it could be resolved, {@link Optional#empty()} otherwise
     * @throws IOException if an error occurred during resolution
     * @since 5.9
     */
    public static Optional<ItemInfo> fetchItemInfo(final URL url, final IProgressMonitor monitor) throws IOException {
        final var absoluteUrl = ExplorerURLStreamHandler.resolveKNIMEURLToAbsolute(url);
        if (absoluteUrl.isEmpty()) {
            return Optional.empty();
        }

        try {
            final var absoluteUri = absoluteUrl.get().toURI();
            final var mpUrlService = ResolverUtil.getURLService(absoluteUri);
            if (mpUrlService.isPresent()) {
                return mpUrlService.get().fetchItemInfo(absoluteUri, monitor);
            }
        } catch (URISyntaxException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return Optional.empty();
    }
}
