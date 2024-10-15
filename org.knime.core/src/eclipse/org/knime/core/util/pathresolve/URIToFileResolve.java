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
 *
 */
package org.knime.core.util.pathresolve;

import java.io.File;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.NamedItemVersion;

/**
 * A service interface to convert a URI into a local file. The URI is usually (always?) either a file URI or a URI
 * pointing into the KNIME TeamSpace (also file based), e.g. "knime:/MOUNT_ID/some/path/workflow.knime".
 *
 * <p>
 * This interface is used to resolve URIs that are stored as part of referenced metanode templates. It is not meant to
 * be implemented by third-party plug-ins.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noimplement
 */
public interface URIToFileResolve {
    /**
     * Resolves the given URI into a local file. If the URI doesn't denote a local file, <code>null</code> is returned.
     *
     * @param uri The URI, e.g. "knime:/MOUNT_ID/some/path/workflow.knime"
     * @return the local file represented by the URI or <code>null</code>
     * @throws ResourceAccessException If the URI can't be resolved
     */
    File resolveToFile(URI uri) throws ResourceAccessException;

    /**
     * Resolves the given URI into a local file. If the URI doesn't denote a local file, <code>null</code> is returned.
     *
     * @param uri The URI, e.g. "knime:/MOUNT_ID/some/path/workflow.knime"
     * @param monitor a progress monitor, must not be <code>null</code>
     * @return the local file represented by the URI or <code>null</code>
     * @throws ResourceAccessException If the URI can't be resolved
     * @since 2.6
     */
    File resolveToFile(URI uri, IProgressMonitor monitor) throws ResourceAccessException;

    /**
     * Resolves the given URI into a local file. If the URI does not represent a local file (e.g. a remote file on a
     * server) it is downloaded first to a temporary directory and the the temporary copy is returned. If it represents
     * a local file the behavior is the same as in {@link #resolveToFile(URI)}.
     *
     * @param uri The URI, e.g. "knime:/MOUNT_ID/some/path/workflow.knime"
     * @return the file represented by the URI or a temporary copy of that file if it represents a remote file
     * @throws ResourceAccessException If the URI can't be resolved
     */
    File resolveToLocalOrTempFile(URI uri) throws ResourceAccessException;

    /**
     * Resolves the given URI into a local file. If the URI does not represent a local file (e.g. a remote file on a
     * server) it is downloaded first to a temporary directory and the the temporary copy is returned. If it represents
     * a local file the behavior is the same as in {@link #resolveToFile(URI)}.
     *
     * @param uri The URI, e.g. "knime:/MOUNT_ID/some/path/workflow.knime"
     * @param monitor a progress monitor, must not be <code>null</code>
     * @return the file represented by the URI or a temporary copy of that file if it represents a remote file
     * @throws ResourceAccessException If the URI can't be resolved
     * @since 2.6
     */
    File resolveToLocalOrTempFile(URI uri, IProgressMonitor monitor) throws ResourceAccessException;

    /**
     * Attempts to extract some information about the argument URI, especially attempts to resolve the full path for
     * id-based KNIME URIs. For instance, a URL such as "knime://KNIME-Community-Hub/*39189vdd9d" will extract its path
     * and capture this information in the returned object.
     *
     * Non-KNIME URIs (or null) will result in an empty optional. Failures to resolve the URI -- which requires API
     * calls to be made -- will (currently) return bogus result objects (e.g. the path remains "*39189vdd9d").
     *
     * @param uri The argument URI
     * @param monitor A progress monitor tracking the progress of the (async) call
     * @return Such a description.
     * @since 4.7
     */
    default Optional<KNIMEURIDescription> toDescription(final URI uri, final IProgressMonitor monitor) {
        return Optional.of(new KNIMEURIDescription(uri.getHost(), uri.getPath(), FilenameUtils.getName(uri.getPath())));
    }

    /**
     * @param uri of a shared component
     * @return this used to return all space versions this item existed in. With item-level versioning, space versions
     *         are replaced with workflow project item versions.
     * @throws Exception
     * @deprecated use {@link #getHubItemVersions(URI)}
     */
    @Deprecated(since = "5.1.0")
    default Optional<List<SpaceVersion>> getSpaceVersions(final URI uri) throws Exception {
        return Optional.empty();
    }

    /**
     * @param uri KNIME URI of an item (workflow, shared component, etc.)
     * @return the version history of that item on a KNIME Hub
     * @throws ResourceAccessException
     * @throws IllegalArgumentException if the given URI does not use have the knime scheme.
     * @since 5.1
     */
    List<NamedItemVersion> getHubItemVersions(final URI uri) throws ResourceAccessException;

    /**
     * Resolves the given URI into a local file. If the URI does not represent a local file (e.g. a remote file on a
     * server) it is downloaded first to a temporary directory and the temporary copy is returned. If a
     * 'ifModifiedSince' date is provided, it will only be downloaded and returned if the file on the server has been
     * modified after the provided date.
     *
     * If it represents a local file the behavior is the same as in {@link #resolveToFile(URI)}.
     *
     * @param uri The URI, e.g. "knime:/MOUNT_ID/some/path/workflow.knime"
     * @param monitor a progress monitor, must not be <code>null</code>
     * @param ifModifiedSince the if-modified-since date for a conditional request; can be <code>null</code> to not
     *            request it conditionally
     * @return the file represented by the URI or a temporary copy of that file if it represents a remote file; or an
     *         empty optional if the file hasn't been modified after the provided date
     * @throws ResourceAccessException
     * @since 4.3
     */
    Optional<File> resolveToLocalOrTempFileConditional(URI uri, IProgressMonitor monitor, ZonedDateTime ifModifiedSince)
            throws ResourceAccessException;

    /**
     * Returns <code>true</code>, if this is a URI that is relative to the current mountpoint (of the flow it is used
     * in). It can only be resolved in the context of a flow. Contains the corresponding keyword as host.
     *
     * @param uri to check
     * @return <code>true</code> if argument URI is mount point relative, <code>false</code> if not.
     * @since 2.8
     */
    boolean isMountpointRelative(URI uri);

    /**
     * Returns <code>true</code>, if this is a URI that is relative to the workflow it is used in. It can only be
     * resolved in the context of a flow. Contains the corresponding keyword as host.
     *
     * @param uri to check
     * @return <code>true</code> if argument URI is workflow relative, <code>false</code> if not.
     * @since 2.8
     */
    boolean isWorkflowRelative(URI uri);

    /**
     * Returns <code>true</code>, if this is a URI that is relative to the node it is used in. It can only be resolved
     * in the context of a flow. Contains the corresponding keyword as host.
     *
     * @param uri to check
     * @return <code>true</code> if argument URI is node relative, <code>false</code> if not.
     * @since 2.10
     */
    boolean isNodeRelative(URI uri);

    /**
     * Returns <code>true</code>, if this is a URI that is relative to the space it is used in. It can only be
     * resolved in the context of a flow. Contains the corresponding keyword as host.
     *
     * @param uri to check
     * @return <code>true</code> if argument URI is space relative, <code>false</code> if not.
     * @since 4.7
     */
    boolean isSpaceRelative(URI uri);

    /**
     * Return type of {@link URIToFileResolve#toDescription(URI, IProgressMonitor)}. This class should not be used to
     * extract information from it except for string representations shown in config/info dialogs.
     */
    public static final class KNIMEURIDescription {

        private final String m_pathOrId;

        private final String m_mountpointName;

        private final String m_name;

        /**
         * @param mountpointName
         * @param pathOrId
         * @param name
         * @noreference This constructor is not intended to be referenced by clients.
         */
        public KNIMEURIDescription(final String mountpointName, final String pathOrId, final String name) {
            m_pathOrId = pathOrId;
            m_mountpointName = mountpointName;
            m_name = name;
        }

        /** @return A string that can be used in labels etc . */
        public String toDisplayString() {
            return String.format("%s: %s (%s)", m_mountpointName, m_name, m_pathOrId);
        }

        /**
         * @return the path
         * @since 5.0
         */
        public String getPathOrId() {
            return m_pathOrId;
        }

        /**
         * @return the name
         * @since 5.0
         */
        public String getName() {
            return m_name;
        }

        /**
         * @return the mountpointName
         */
        public String getMountpointName() {
            return m_mountpointName;
        }
    }
}
