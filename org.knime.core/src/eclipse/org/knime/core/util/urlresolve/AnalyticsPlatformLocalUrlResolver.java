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
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.util.Pair;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * KNIME URL Resolver for an Analytics Platform with a workflow that comes from the local file system.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class AnalyticsPlatformLocalUrlResolver extends KnimeUrlResolver {

    private final AnalyticsPlatformExecutorInfo m_executorInfo;

    AnalyticsPlatformLocalUrlResolver(final AnalyticsPlatformExecutorInfo executorInfo) {
        m_executorInfo = executorInfo;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        final var mountpoint = m_executorInfo.getMountpoint();
        if (!mountpoint.isPresent()) {
            return Optional.empty();
        }

        final var localRootPath = mountpoint.get().getSecond().normalize();
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath().normalize();
        if (!localWorkflowPath.startsWith(localRootPath)) {
            return Optional.empty();
        }

        final var pathToWorkflow = Path.fromOSString(localRootPath.relativize(localWorkflowPath).toString());
        return Optional.of(new ContextPaths(EMPTY_POSIX_PATH, pathToWorkflow));
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final HubItemVersion version) throws ResourceAccessException {
        final var sameMountpoint = m_executorInfo.getMountpoint() //
                .map(Pair::getFirst) //
                .filter(uri -> Objects.equals(uri.getAuthority(), mountId)) //
                .isPresent();
        final var isVersioned = version != null && version.isVersioned();
        return new ResolvedURL(mountId, path, version, null, url, sameMountpoint && !isVersioned);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        final var mountpoint = m_executorInfo.getMountpoint()
            .orElseThrow(() -> new ResourceAccessException("Mountpoint or space relative URL needs a mountpoint."));

        final var mountpointRoot = mountpoint.getSecond().normalize();
        final var resolved = mountpointRoot.resolve(path.toOSString()).normalize();
        if (!resolved.startsWith(mountpointRoot)) {
            throw new ResourceAccessException("Leaving the mount point is not allowed for relative URLs: "
                + resolved + " is not in " + mountpointRoot);
        }

        final var mountId = mountpoint.getFirst().getAuthority();
        final var workflowPath = m_executorInfo.getLocalWorkflowPath().normalize();

        final IPath pathInWorkspace;
        final IPath pathInWorkflow;
        if (resolved.startsWith(workflowPath)) {
            // This use case (accessing the contents of one's own workflow using a mountpoint-relative URL) is quite
            // strange, but we allow it for backwards compatibility:
            //     `knime://knime.mountpoint/path/to/CurrentWorkflow/data/file.csv`
            pathInWorkspace = Path.fromOSString(mountpointRoot.relativize(workflowPath).toString());
            pathInWorkflow = Path.fromOSString(workflowPath.relativize(resolved).toString());
        } else {
            pathInWorkspace = Path.fromOSString(mountpointRoot.relativize(resolved).toString());
            pathInWorkflow = null;
        }

        final var resourceUrl = URLResolverUtil.toURL(resolved);
        return new ResolvedURL(mountId, pathInWorkspace, version, pathInWorkflow, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        return resolveMountpointRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        // in local application or a file inside the workflow
        final var workflowPath = m_executorInfo.getLocalWorkflowPath().normalize();
        final var resolved = workflowPath.resolve(path.toOSString()).normalize();
        final var isInsideWorkflow = resolved.startsWith(workflowPath);


        final String mountId;
        final IPath pathInWorkspace;
        final var mountpoint = m_executorInfo.getMountpoint().orElse(null);
        if (mountpoint != null) {
            mountId = mountpoint.getFirst().getAuthority();

            // if resolved path is outside the workflow, check whether it is still inside the mountpoint
            final var normalizedRoot = mountpoint.getSecond().normalize();
            if (!resolved.startsWith(normalizedRoot)) {
                throw new ResourceAccessException("Leaving the mount point is not allowed for workflow relative URLs: "
                        + resolved + " is not in " + normalizedRoot);
            }

            final var pathUntilWorkflow = isInsideWorkflow ? workflowPath : resolved;
            pathInWorkspace = Path.fromOSString(normalizedRoot.relativize(pathUntilWorkflow).toString());
        } else {
            if (!isInsideWorkflow) {
                throw new ResourceAccessException(
                    "Cannot access items outside the workflow because it is not imported into a space: '" + url + "'");
            }
            mountId = null;
            pathInWorkspace = null;
        }

        IPath pathInWorkflow = null;
        if (isInsideWorkflow) {
            final var belowWorkflow = Path.fromOSString(workflowPath.relativize(resolved).toString());
            // If a workflow references itself by going outside and then in again, it is resolved from the outside:
            //  - `knime://knime.workflow/../<Workflow Name>`  => `pathInWorkflow == null`
            //  - `knime://knime.workflow`                     => `pathInWorkflow != null`
            //  - `knime://knime.workflow/data/file.txt`       => `pathInWorkflow != null`
            if (belowWorkflow.segmentCount() > 0 || !leavesScope(path)) {
                pathInWorkflow = belowWorkflow;
            }
        }

        final var resourceUrl = URLResolverUtil.toURL(resolved);
        return new ResolvedURL(mountId, pathInWorkspace, version, pathInWorkflow, resourceUrl, true);
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        final var localWorkflowPath = m_executorInfo.getLocalWorkflowPath();

        String mountId = null;
        IPath pathToWorkflow = null;
        final var mountpoint = m_executorInfo.getMountpoint().orElse(null);
        if (mountpoint != null) {
            mountId = mountpoint.getFirst().getAuthority();
            pathToWorkflow = Path.fromOSString(mountpoint.getSecond().relativize(localWorkflowPath).toString());
        }

        return resolveNodeRelative(mountId, pathToWorkflow, localWorkflowPath, path);
    }
}
