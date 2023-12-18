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
import java.net.URL;

import org.eclipse.core.runtime.URIUtil;
import org.knime.core.node.workflow.contextv2.ServerJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

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
    URL resolveMountpointAbsolute(final URL url) throws ResourceAccessException {
        final var mountId = url.getAuthority();
        checkNoVersion(HubItemVersion.of(url).orElse(null));
        if (m_locationInfo.getDefaultMountId().equals(mountId)) {
            final var uri = resolveMountpointRelative(URIPathEncoder.decodePath(url),
                HubItemVersion.of(url).orElse(null));
            return URIPathEncoder.UTF_8.encodePathSegments(URLResolverUtil.toURL(uri));
        }
        // possibly a MountTable is present, which will then resolve the URL (AP-19986)
        return url;
    }

    @Override
    URI resolveMountpointRelative(final String decodedPath, final HubItemVersion version)
            throws ResourceAccessException {
        checkNoVersion(version);

        // legacy Servers don't have spaces or versions, resolve directly against repo root
        final var repositoryAddress = m_locationInfo.getRepositoryAddress().normalize();
        return URIUtil.append(repositoryAddress, decodedPath + ":data").normalize();
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        checkNoVersion(version);

        return resolveMountpointRelative(decodedPath, version);
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        checkNoVersion(version);

        if (leavesScope(decodedPath)) {
            // we're on a server executor, resolve against the repository
            final var repositoryAddress = m_locationInfo.getRepositoryAddress().normalize();
            final var uri =
                URIUtil.append(repositoryAddress, m_locationInfo.getWorkflowPath() + "/" + decodedPath + ":data");
            return uri.normalize();
        }

        // file inside the workflow
        final var currentLocation = m_executorInfo.getLocalWorkflowPath();
        final var resolvedFile = new File(currentLocation.toFile(), decodedPath);
        if (!URLResolverUtil.getCanonicalPath(resolvedFile)
            .startsWith(URLResolverUtil.getCanonicalPath(currentLocation.toFile()))) {
            throw new ResourceAccessException(
                "Path component of workflow relative URLs leaving the workflow must start with " + "'/..', found '"
                    + decodedPath + "'.");
        }
        return resolvedFile.toURI();
    }

    @Override
    URI resolveNodeRelative(final String decodedPath) throws ResourceAccessException {
        return defaultResolveNodeRelative(decodedPath, m_executorInfo.getLocalWorkflowPath());
    }

    private static void checkNoVersion(final HubItemVersion version) throws ResourceAccessException {
        if (version != null) {
            throw new ResourceAccessException("KNIME URLs on a KNIME Server cannot specify an item version.");
        }
    }
}
