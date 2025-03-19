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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.workflow.contextv2.ServerJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;

/**
 * KNIME URL Resolver for a Server executor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class ServerExecutorUrlResolver extends KnimeUrlResolver {

    private final ServerJobExecutorInfo m_executorInfo;

    private final ServerLocationInfo m_locationInfo;

    ServerExecutorUrlResolver(final ServerJobExecutorInfo executorInfo, final ServerLocationInfo locationInfo) {
        m_executorInfo = executorInfo;
        m_locationInfo = locationInfo;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        return Optional.of(new ContextPaths(EMPTY_POSIX_PATH, getWorkflowPath(m_locationInfo)));
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final ItemVersion version) throws ResourceAccessException {
        checkNoVersion(version);

        if (!m_locationInfo.getDefaultMountId().equals(mountId)) {
            // possibly a MountTable is present, which will then resolve the URL (AP-19986)
            return new ResolvedURL(mountId, path, null, null, url, false);
        }

        return resolveMountpointRelative(url, path, null);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        checkNoVersion(version);

        final var workflowPath = getWorkflowPath(m_locationInfo);
        IPath pathToItem;
        IPath pathInsideWorkflow = null;
        if (workflowPath.isPrefixOf(path)) {
            pathToItem = workflowPath;
            final var subPath = path.makeRelativeTo(workflowPath);
            if (subPath.segmentCount() > 0) {
                // this is weird, but we keep it for backwards compatibility
                pathInsideWorkflow = subPath;
            }
        } else {
            pathToItem = path;
        }

        final var mountId = m_locationInfo.getDefaultMountId();
        return new ResolvedURL(mountId, pathToItem, null, pathInsideWorkflow, createRepoUrl(path), true);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        return resolveMountpointRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        checkNoVersion(version);

        final var workflowPath = getWorkflowPath(m_locationInfo);
        final var resolvedPath = workflowPath.append(path);

        final IPath pathInsideMountpoint;
        final IPath pathInsideWorkflow;
        final URL resourceUrl;
        if (leavesScope(path)) {
            // we're on a server executor, resolve against the repository
            pathInsideMountpoint = resolvedPath;
            pathInsideWorkflow = null;
            resourceUrl = createRepoUrl(resolvedPath);
        } else {
            // file inside the workflow
            final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath();
            final var resolvedFile = localWorkflowPath.resolve(path.toOSString());
            pathInsideMountpoint = workflowPath;
            pathInsideWorkflow = Path.fromOSString(localWorkflowPath.relativize(resolvedFile).toString());
            resourceUrl = URLResolverUtil.toURL(resolvedFile);
        }

        final var mountId = m_locationInfo.getDefaultMountId();
        return new ResolvedURL(mountId, pathInsideMountpoint, null, pathInsideWorkflow, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath();
        final var pathToWorkflow = getWorkflowPath(m_locationInfo);
        return resolveNodeRelative(m_locationInfo.getDefaultMountId(), pathToWorkflow, localWorkflowPath, path);
    }

    private URL createRepoUrl(final IPath path) throws ResourceAccessException {
        // legacy Servers don't have spaces or versions, resolve directly against repo root
        final var repositoryAddress = m_locationInfo.getRepositoryAddress().normalize();
        final var uriBuilder = new URIBuilder(repositoryAddress);
        final var segments = new ArrayList<>(uriBuilder.getPathSegments());
        final var additionalSegments = Arrays.asList(path.segments());
        if (!additionalSegments.isEmpty()) {
            segments.addAll(additionalSegments);
        } else if (segments.isEmpty() || !StringUtils.isEmpty(segments.get(segments.size() - 1))) {
            // make sure that the repository root has a trailing slash (required by KNIME Server)
            segments.add("");
        }
        HubExecutorUrlResolver.addDataSuffix(segments);
        uriBuilder.setPathSegments(segments);
        return URLResolverUtil.toURL(uriBuilder);
    }

    private static void checkNoVersion(final ItemVersion version) throws ResourceAccessException {
        if (version != null) {
            throw new ResourceAccessException("KNIME URLs on a KNIME Server cannot specify an item version.");
        }
    }
}
