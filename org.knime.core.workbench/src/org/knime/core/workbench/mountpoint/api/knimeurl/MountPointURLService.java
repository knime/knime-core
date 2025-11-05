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
 *   Aug 12, 2025 (magnus): created
 */
package org.knime.core.workbench.mountpoint.api.knimeurl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.workbench.mountpoint.api.MountPointProvider;

/**
 * A {@link URLConnection} with provided mount point service for KNIME URLs.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.8
 */
public interface MountPointURLService extends MountPointProvider {

    /**
     * Returns the mount ID of the associated mount point.
     *
     * @return the mount ID of the associated mount point, never {@code null}
     */
    String getMountId();

    /**
     * Creates a new {@link URLConnection} for the given path and version. It's guaranteed that the path is not null
     * and is relative to the mount point root represented by the corresponding mount point state.
     *
     * @param path the path relative to the mount point root, not {@code null}
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @return a new {@link URLConnection} for the given path and version
     * @throws IOException if an I/O error occurs while creating the connection
     */
    KnimeURLConnection newURLConnection(IPath path, ItemVersion version) throws IOException;

    /**
     * Resolves the given path and version into a local file. If the path does not represent a local file
     * (e.g. on hub) it is downloaded first to a temporary directory and then the temporary copy is returned.
     *
     * @param path the path relative to the mount point root, not {@code null}
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @param monitor a progress monitor to report progress, may be {@link NullProgressMonitor}
     * @return a local or temporary file {@link File} represented by the given path and version
     * @throws IOException if an I/O error occurs while resolving the file
     * @throws CancellationException if the operation is cancelled
     */
    File toLocalOrTempFile(IPath path, ItemVersion version, IProgressMonitor monitor)
            throws IOException, CancellationException;

    /**
     * Resolves the given path and version into a local file. If the path does not represent a local file
     * (e.g. on hub) it is downloaded first to a temporary directory and then the temporary copy is returned.
     * <p>
     * In contrast to {@link #toLocalOrTempFile(IPath, ItemVersion, IProgressMonitor)}, this method allows
     * specifying a cut-off date to attach as {@code If-Modified-Since} header to HTTP requests
     * that resolve the item to the {@link File}. This method will return {@code Optional#empty()}
     * if the conditional check determines that no newer version exists after the cut-off date.
     * </p>
     * NOTE: The cut-off date is not used for conditional checks on local files, they will always be
     * returned as a non-empty {@link File} value.
     *
     * @param path the path relative to the mount point root, not {@code null}
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @param ifModifiedSince the if-modified-since date for a conditional request, can be {@code null}
     * @param monitor a progress monitor to report progress, may be {@link NullProgressMonitor}
     * @return a local or temporary file {@link File} represented by the given path and version
     * @throws IOException if an I/O error occurs while resolving the file
     * @throws CancellationException if the operation is cancelled
     * @since 5.8.1
     */
    default Optional<File> toLocalOrTempFileConditional(final IPath path, final ItemVersion version,
        final Instant ifModifiedSince, final IProgressMonitor monitor) throws IOException, CancellationException {
        // technically the `null` check is unnecessary, but we want to be sure here
        return Optional.ofNullable(toLocalOrTempFile(path, version, monitor));
    }

    /**
     * Retrieves all named item versions of the repository item at the given path. This method is
     * only applicable on mount point URL service implementations for a KNIME Hub.
     *
     * @param path the path relative to the mount point root, not {@code null}
     * @return a possibly empty list of {@link NamedItemVersion}
     * @throws IOException if an I/O error occurs while retrieving the named item versions
     * @throws UnsupportedOperationException if the URL service is not associated with a KNIME Hub
     */
    List<NamedItemVersion> getVersions(IPath path) throws UnsupportedOperationException, IOException;

    /**
     * Resolves the given URI to a path relative to the mount point root.
     * This is a no-op for every mount point URL service except for Hub, where item IDs are resolved to paths.
     *
     * @param uri the URI to resolve, not {@code null}
     * @return the path relative to the mount point root, never {@code null}
     * @throws ResourceAccessException if the URI cannot be resolved
     */
    default IPath resolveItemPath(final URI uri) throws ResourceAccessException {
        return IPath.forPosix(uri.getPath() == null ? "/" : uri.getPath());
    }

    /**
     * Information about an item.
     *
     * @param mountId mount ID of the containing mountpoint
     * @param path item path
     * @param id item ID (if applicable)
     * @param version item version (if applicable)
     * @param size item size (in bytes, if applicable)
     * @param isFolder whether or not the item is a folder (space, remote workflow group or local file system folder)
     */
    record ItemInfo(String mountId, IPath path, Optional<String> id, Optional<ItemVersion> version, OptionalLong size,
        boolean isFolder) {
    }

    /**
     * Fetches information about an item.
     *
     * @param uri absolute KNIME URL identifying the item
     * @param monitor a progress monitor to report progress, may be {@code null}
     * @return information about the item if it exists, {@link Optional#empty()} if it doesn't
     * @throws IOException if the information can't be fetched
     */
    default Optional<ItemInfo> fetchItemInfo(final URI uri, final IProgressMonitor monitor) throws IOException {
        final var path = IPath.forPosix(StringUtils.firstNonBlank(uri.getPath(), "/"));
        final var itemVersion = new URIBuilder(uri).getQueryParams().stream() //
            .filter(nv -> "version".equals(nv.getName())) //
            .map(NameValuePair::getValue) //
            .map(ItemVersion::convertToItemVersion) //
            .findFirst() //
            .orElse(null);
        return fetchItemInfo(path, itemVersion, monitor);
    }

    /**
     * Fetches information about an item.
     *
     * @param path the path relative to the mount point root, not {@code null}
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @param monitor a progress monitor to report progress, may be {@code null}
     * @return information about the item if it exists, {@link Optional#empty()} if it doesn't
     * @throws IOException if the information can't be fetched
     */
    Optional<ItemInfo> fetchItemInfo(IPath path, final ItemVersion version, final IProgressMonitor monitor)
        throws IOException;

    /**
     * Creates a {@link MountPointURLService} for the executor described by the given executor and location info if possible.
     *
     * @param execInfo executor info
     * @param restLoc location info
     * @return URL service if it could be created, {@link Optional#empty()} otherwise
     */
    public static Optional<MountPointURLService> getExecutorURLService(final JobExecutorInfo execInfo,
        final RestLocationInfo restLoc) {
        final var collector = MountPointURLServiceFactoryCollector.getInstance();
        return collector.getRegisteredTypeIdentifiers().stream() //
            .flatMap(id -> collector.getMountPointURLServiceFactory(id).stream()) //
            .flatMap(factory -> factory.createExecutorMountPointURLService(execInfo, restLoc).stream()) //
            .findFirst();
    }
}
