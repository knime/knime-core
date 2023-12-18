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

import java.io.File;
import java.net.URI;

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
    URI resolveMountpointRelative(final String decodedPath, final HubItemVersion version)
            throws ResourceAccessException {
        final var mountpointRoot = m_executorInfo.getMountpoint().map(Pair::getSecond)
            .orElseThrow(() -> new ResourceAccessException("Mountpoint or space relative URL needs a mountpoint."));
        final var resolvedFile = new File(mountpointRoot.toFile(), decodedPath);
        final var normalizedPath = resolvedFile.toPath().normalize().toUri();
        final var normalizedRoot = mountpointRoot.normalize().toUri();
        if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
            throw new ResourceAccessException("Leaving the mount point is not allowed for relative URLs: "
                + resolvedFile.getAbsolutePath() + " is not in " + mountpointRoot.toFile().getAbsolutePath());
        }
        return resolvedFile.toURI();
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        return resolveMountpointRelative(decodedPath, version);
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        // in local application or a file inside the workflow
        final var currentLocation = m_executorInfo.getLocalWorkflowPath();
        final var resolvedFile = new File(currentLocation.toFile(), decodedPath);

        // if resolved path is outside the workflow, check whether it is still inside the mountpoint
        final var mountpoint = m_executorInfo.getMountpoint();

        if (!URLResolverUtil.getCanonicalPath(resolvedFile)
            .startsWith(URLResolverUtil.getCanonicalPath(currentLocation.toFile())) && mountpoint.isPresent()) {
            final var mountpointRoot = mountpoint.get().getSecond();
            final var normalizedRoot = mountpointRoot.normalize().toUri();
            final var normalizedPath = resolvedFile.toPath().normalize().toUri();

            if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                throw new ResourceAccessException("Leaving the mount point is not allowed for workflow relative URLs: "
                    + resolvedFile.getAbsolutePath() + " is not in " + mountpointRoot.toAbsolutePath());
            }
        }
        return resolvedFile.toURI();
    }

    @Override
    URI resolveNodeRelative(final String decodedPath) throws ResourceAccessException {
        return defaultResolveNodeRelative(decodedPath, m_executorInfo.getLocalWorkflowPath());
    }
}
