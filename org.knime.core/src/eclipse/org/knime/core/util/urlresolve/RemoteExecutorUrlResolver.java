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
 *   Nov 28, 2022 (leonard.woerteler): created
 */
package org.knime.core.util.urlresolve;

import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.IPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;

/**
 * KNIME URL Resolver for a remote executor in the Remote Workflow Editor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class RemoteExecutorUrlResolver extends KnimeUrlResolver {

    private final String m_localMountId;

    private final URI m_mountpointUri;

    private final HubSpaceLocationInfo m_hubLocationInfo;

    /**
     * @param mountpointUri provides the name under which the remote (server/hub) location is mounted locally
     * @param locationInfo provides the space path or workflow path
     */
    RemoteExecutorUrlResolver(final URI mountpointUri, final HubSpaceLocationInfo locationInfo) {
        m_mountpointUri = CheckUtils.checkArgumentNotNull(mountpointUri, "Remote job doesn't specify mountpoint URL");
        m_localMountId = CheckUtils.checkArgumentNotNull(mountpointUri.getAuthority(),
            "Mountpoint URL '%s' doesn't specify a mountID.", mountpointUri);
        m_hubLocationInfo = locationInfo;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        if (m_hubLocationInfo != null) {
            return Optional.of(new ContextPaths(getSpacePath(m_hubLocationInfo), getWorkflowPath(m_hubLocationInfo)));
        }

        // must be an old KNIME Server
        return Optional.ofNullable(m_mountpointUri).map(uri -> new ContextPaths(EMPTY_POSIX_PATH, getPath(uri)));
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final ItemVersion version) throws ResourceAccessException {

        // we are conservative here and accept the URL as referencing the same mountpoint if the mount ID matches either
        // the mount ID of the workflow in the local AP or the default mount ID of the remote Hub
        // covers server and hub
        final var localIdMatches = m_localMountId.equals(mountId);
        // historically, server resolution did not account for default mount id, that's why we do it only for hub
        final var defaultIdMatches =
            m_hubLocationInfo != null && Objects.equals(m_hubLocationInfo.getDefaultMountId(), mountId);
        if (!(localIdMatches || defaultIdMatches)) {
            throw new ResourceAccessException("Unknown Mount ID on Remote Executor in URL '" + url + "'.");
        }

        // the rest is done by the ExplorerFileStore instance from the ExplorerMountTable
        var canBeRelativized = m_hubLocationInfo == null ||
                (!isHubIdUrl(path) && getSpacePath(m_hubLocationInfo).isPrefixOf(path)
                        && !(version != null && version.isVersioned()));
        final var resourceUrl = createKnimeUrl(m_localMountId, path, version);
        return new ResolvedURL(mountId, path, version, null, resourceUrl, canBeRelativized);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        // Mountpoint and space are synonymous here
        return resolveSpaceRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        final IPath resolvedPath;
        if (m_hubLocationInfo != null) {
            // Hub executor
            final var spacePath = getSpacePath(m_hubLocationInfo);
            resolvedPath = spacePath.append(path);
        } else {
            // server executor, Space == Mountpoint Root and no versions
            if (version != null) {
                throw new ResourceAccessException("KNIME URLs on Server cannot specify a version: " + url);
            }
            resolvedPath = path;
        }

        // we are using the best guess for which mount ID will work remotely
        final var mountId = m_hubLocationInfo != null ? m_hubLocationInfo.getDefaultMountId() : m_localMountId;
        final var resourceUrl = createKnimeUrl(m_localMountId, resolvedPath, version);
        return new ResolvedURL(mountId, resolvedPath, version, null, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        if (!leavesScope(path)) {
            throw new ResourceAccessException(
                "Workflow relative URL points to a resource within a workflow. Not accessible.");
        }

        if (m_hubLocationInfo != null) {
            final var workflowPath = getWorkflowPath(m_hubLocationInfo);
            final var resolvedPath = workflowPath.append(path);
            final var resourceUrl = createKnimeUrl(m_localMountId, resolvedPath, version);
            final var remoteMountId = m_hubLocationInfo.getDefaultMountId();
            return new ResolvedURL(remoteMountId, resolvedPath, version, null, resourceUrl, true);
        }

        // server executor, Space == Mountpoint Root and no versions
        if (version != null) {
            throw new ResourceAccessException("KNIME URLs on Server cannot specify a version.");
        }

        final var workflowPath = getPath(m_mountpointUri);
        final var resolvedPath = workflowPath.append(path);
        final var resourceUrl = createKnimeUrl(m_localMountId, resolvedPath, null);
        return new ResolvedURL(m_localMountId, resolvedPath, null, null, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        throw new ResourceAccessException("Node relative URLs cannot be resolved from within purely remote workflows.");
    }
}
