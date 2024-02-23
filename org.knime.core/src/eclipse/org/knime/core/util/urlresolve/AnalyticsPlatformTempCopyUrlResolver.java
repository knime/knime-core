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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.util.Pair;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * KNIME URL Resolver for an Analytics Platform with a workflow that comes from a REST location.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class AnalyticsPlatformTempCopyUrlResolver extends KnimeUrlResolver {

    private final AnalyticsPlatformExecutorInfo m_executorInfo;

    private final RestLocationInfo m_locationInfo;

    private final URI m_mountpointURI;

    AnalyticsPlatformTempCopyUrlResolver(final AnalyticsPlatformExecutorInfo executorInfo,
        final RestLocationInfo locationInfo, final URI mountpointURI) {
        m_executorInfo = executorInfo;
        m_locationInfo = locationInfo;
        m_mountpointURI = mountpointURI;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        return Optional.of(new ContextPaths(getSpacePath(m_locationInfo), getWorkflowPath(m_locationInfo)));
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final HubItemVersion version) throws ResourceAccessException {

        if (m_locationInfo instanceof HubSpaceLocationInfo
                && path.segmentCount() == 1 && path.segment(0).startsWith("*")) {
            // cannot relativize ID URLs (for now)
            return new ResolvedURL(mountId, path, version, null, url, false);
        }

        // we are conservative here and accept the URL as referencing the same mountpoint if the mount ID matches either
        // the mount ID of the workflow in the local AP or the default mount ID of the remote Hub/server
        final var defaultMountId = m_locationInfo.getDefaultMountId();
        final var candidates = m_executorInfo.getMountpoint() //
                .map(Pair::getFirst) //
                .map(URI::getAuthority) //
                .<Set<String>>map(id -> new HashSet<>(List.of(id, defaultMountId))) // `Set.of(X,Y)` hates duplicates
                .orElseGet(() -> Set.of(defaultMountId));

        final var canBeRelativized = candidates.contains(mountId) && getSpacePath(m_locationInfo).isPrefixOf(path);
        return new ResolvedURL(mountId, path, version, null, url, canBeRelativized);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        return resolveSpaceRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        // we are mounted in the Analytics Platform, make the ExplorerMountTable sort it out

        final IPath resolvedPath;
        if (m_locationInfo instanceof HubSpaceLocationInfo hubLocationInfo) {
            final var spacePath = getSpacePath(hubLocationInfo);
            resolvedPath = spacePath.append(path);
            if (!spacePath.isPrefixOf(resolvedPath)) {
                throw new ResourceAccessException("Leaving the Hub space is not allowed for space relative URLs: "
                        + resolvedPath + " is not in " + spacePath);
            }
        } else {
            resolvedPath = path;
        }

        final IPath workflowPath = getWorkflowPath(m_locationInfo);
        if (workflowPath.isPrefixOf(resolvedPath) && resolvedPath.segmentCount() > workflowPath.segmentCount()) {
            // we could allow this at some point and resolve the URL in the local file system
            throw new ResourceAccessException(
                "Accessing the current workflow's contents is not allowed for space relative URLs: '" + url
                    + "' points into current workflow " + workflowPath);
        }

        final var localMountId = m_mountpointURI.getAuthority();
        final var remoteMountId = m_locationInfo.getDefaultMountId();
        final var resourceUrl = createKnimeUrl(localMountId, resolvedPath, version);
        return new ResolvedURL(remoteMountId, resolvedPath, version, null, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        final var localMountId = m_mountpointURI.getAuthority();
        final var contextPaths = getContextPaths().orElseThrow();
        final var workflowPath = contextPaths.workflowPath();

        if (leavesScope(path)) {
            // we are on a local AP executor, resolve against the mount table
            final var resolvedPath = workflowPath.append(path);

            CheckUtils.check(contextPaths.spacePath().isPrefixOf(resolvedPath), ResourceAccessException::new,
                () -> "Leaving the Hub space is not allowed for workflow relative URLs: "
                        + resolvedPath + " is not in " + contextPaths.spacePath());

            final var remoteMountId = m_locationInfo.getDefaultMountId();
            final var resourceUrl = createKnimeUrl(localMountId, resolvedPath, version);
            return new ResolvedURL(remoteMountId, resolvedPath, version, null, resourceUrl, true);
        }

        // a file inside the workflow
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath().toAbsolutePath();
        return resolveInExecutorWorkflowDir(url, localMountId, workflowPath, path, version, localWorkflowPath);
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        final var localMountId = m_mountpointURI.getAuthority();
        final var pathToWorkflow = getPath(m_mountpointURI);
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath();
        return resolveNodeRelative(localMountId, pathToWorkflow, localWorkflowPath, path);
    }
}
