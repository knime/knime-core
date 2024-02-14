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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * KNIME URL Resolver for a Hub executor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class HubExecutorUrlResolver extends KnimeUrlResolver {

    private final HubJobExecutorInfo m_executorInfo;

    private final HubSpaceLocationInfo m_locationInfo;

    HubExecutorUrlResolver(final HubJobExecutorInfo executorInfo, final HubSpaceLocationInfo locationInfo) {
        m_executorInfo = executorInfo;
        m_locationInfo = locationInfo;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        return Optional.of(new ContextPaths(getSpacePath(m_locationInfo), getWorkflowPath(m_locationInfo)));
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final HubItemVersion version) throws ResourceAccessException {
        if (!m_locationInfo.getDefaultMountId().equals(mountId)) {
            // possibly a MountTable is present, which will then resolve the URL (AP-19986)
            return new ResolvedURL(mountId, path, null, null, url, true);
        }

        // we're on a Hub executor, resolve workflow locally via the repository

        final var isHubIdUrl = path.segmentCount() == 1 && path.segment(0).startsWith("*");
        final var versionInfo = HubItemVersion.of(url).orElse(null);
        final var resourceUrl = createRepoUrl(m_locationInfo.getRepositoryAddress(), versionInfo, path);
        return new ResolvedURL(mountId, path, version, null, resourceUrl, isHubIdUrl);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        return resolveSpaceRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        // we're on a Hub executor, resolve workflow locally via the repository
        return resolveRelativeToHubSpace(m_locationInfo, path, version);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        final var mountId = m_locationInfo.getDefaultMountId();
        final var workflowPath = getWorkflowPath(m_locationInfo);

        if (leavesScope(path)) {
            // we're on a hub executor, resolve against the repository
            final var referencedItemPath = workflowPath.append(path);

            final var spacePath = getSpacePath(m_locationInfo);
            if (!spacePath.isPrefixOf(referencedItemPath)) {
                throw new ResourceAccessException("Leaving the Hub space is not allowed for workflow relative URLs: "
                        + path + " is not in " + spacePath);
            }

            final var resourceUrl = createRepoUrl(m_locationInfo.getRepositoryAddress(), version, referencedItemPath);
            return new ResolvedURL(mountId, referencedItemPath, version, null, resourceUrl, false);
        }

        // file inside the workflow
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath().toAbsolutePath().normalize();
        return resolveInExecutorWorkflowDir(url, mountId, workflowPath, path, version, localWorkflowPath);
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        final var mountId = m_locationInfo.getDefaultMountId();
        final var pathToWorkflow = getWorkflowPath(m_locationInfo);
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath().toAbsolutePath().normalize();
        return resolveNodeRelative(mountId, pathToWorkflow, localWorkflowPath, path);
    }

    /**
     * Resolves the given path relative to the workflow's hub space root.
     *
     * @param locationInfo hub space location info
     * @param path path relative to the space
     * @param version hub item version
     * @return resolved URL components
     * @throws ResourceAccessException if resolution fails
     */
    static ResolvedURL resolveRelativeToHubSpace(final HubSpaceLocationInfo locationInfo, final IPath path,
        final HubItemVersion version) throws ResourceAccessException {
        final var spacePath = getSpacePath(locationInfo);
        final var resolvedPath = spacePath.append(path);

        if (!spacePath.isPrefixOf(resolvedPath)) {
            throw new ResourceAccessException("Leaving the Hub space is not allowed for space relative URLs: "
                + path + " is not in " + spacePath);
        }

        final var workflowPath = getWorkflowPath(locationInfo);
        if (workflowPath.isPrefixOf(resolvedPath)) {
            // we could allow this at some point and resolve the URL in the local file system
            throw new ResourceAccessException("Accessing the workflow contents is not allowed for space relative URLs: "
                + "'" + path + "' points into current workflow " + workflowPath);
        }

        final var resourceUrl = createRepoUrl(locationInfo.getRepositoryAddress(), version, resolvedPath);
        return new ResolvedURL(locationInfo.getDefaultMountId(), resolvedPath, version, null, resourceUrl, false);
    }

    private static URL createRepoUrl(final URI repositoryAddress, final HubItemVersion version,
        final IPath referencedItemPath) throws ResourceAccessException {

        final var builder = new URIBuilder(repositoryAddress.normalize());
        final var pathSegments = new ArrayList<>(builder.getPathSegments());
        pathSegments.addAll(Arrays.asList(referencedItemPath.segments()));
        addDataSuffix(pathSegments);
        builder.setPathSegments(pathSegments);

        if (version != null) {
            version.addVersionToURI(builder);
        }

        return URLResolverUtil.toURL(builder);
    }

    static void addDataSuffix(final List<String> pathSegments) {
        pathSegments.removeIf(String::isEmpty);
        if (pathSegments.isEmpty()) {
            pathSegments.add(":data");
        } else {
            final var last = pathSegments.size() - 1;
            pathSegments.set(last, pathSegments.get(last) + ":data");
        }
    }
}
