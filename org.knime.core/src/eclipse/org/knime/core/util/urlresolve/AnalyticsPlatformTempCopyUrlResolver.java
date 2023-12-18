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
import java.net.URISyntaxException;

import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.URIUtil;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
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
    URI resolveMountpointRelative(final String decodedPath, final HubItemVersion version)
            throws ResourceAccessException {
        return resolveSpaceRelative(decodedPath, version);
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        // we are mounted in the Analytics Platform, make the ExplorerMountTable sort it out

        // set path to `null` on legacy Servers because the whole directory tree is treated as a single space
        final var spacePath = ClassUtils.castOptional(HubSpaceLocationInfo.class, m_locationInfo)
                .map(HubSpaceLocationInfo::getSpacePath).orElse(null);

        final URI spaceUri;
        final URI resolvedUri;
        try {
            final var spaceUriBuilder = new URIBuilder(m_mountpointURI).setPath(spacePath).removeQuery();
            if (version != null) {
                version.addVersionToURI(spaceUriBuilder);
            }
            spaceUri = spaceUriBuilder.build();
            resolvedUri = URIUtil.append(spaceUri, decodedPath);
        } catch (URISyntaxException e) {
            throw new ResourceAccessException("Could not build space URI: " + e.getMessage(), e);
        }

        if (isContainedIn(resolvedUri, m_mountpointURI)) {
            // we could allow this at some point and resolve the URL in the local file system
            throw new ResourceAccessException(
                "Accessing the current workflow's contents is not allowed for space relative URLs: " + "'knime://"
                    + decodedPath + "' points into current workflow " + m_locationInfo.getWorkflowPath());
        }
        if (!isContainedIn(resolvedUri, spaceUri)) {
            throw new ResourceAccessException("Leaving the Hub space is not allowed for space relative URLs: "
                + resolvedUri + " is not in " + spaceUri);
        }
        return resolvedUri;
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        if (leavesScope(decodedPath)) {
            try {
                // remote REST location, access via mountpoint-absolute URL
                final var uriBuilder = new URIBuilder(URIUtil.append(m_mountpointURI, decodedPath)).removeQuery();
                if (version != null) {
                    version.addVersionToURI(uriBuilder);
                }
                return uriBuilder.build();
            } catch (URISyntaxException e) {
                throw new ResourceAccessException("Could not build space URI: " + e.getMessage(), e);
            }
        }

        // a file inside the workflow
        final var currentLocation = m_executorInfo.getLocalWorkflowPath();
        final var resolvedFile = new File(currentLocation.toFile(), decodedPath);

        // if resolved path is outside the workflow, check whether it is still inside the mountpoint
        if (!URLResolverUtil.getCanonicalPath(resolvedFile)
                .startsWith(URLResolverUtil.getCanonicalPath(currentLocation.toFile()))) {
            throw new ResourceAccessException(
                "Path component of workflow relative URLs leaving the workflow must start with " + "'/..', found '"
                    + decodedPath + "'.");
        }

        if (version != null) {
            throw new ResourceAccessException("Workflow relative URLs accessing workflow contents cannot specify a "
                    + "version: 'knime://workflow.knime" + decodedPath + "?version="
                    + version.getQueryParameterValue().orElse(LinkType.LATEST_STATE.getIdentifier()) + "'.");
        }

        return resolvedFile.toURI();
    }

    @Override
    URI resolveNodeRelative(final String decodedPath) throws ResourceAccessException {
        return defaultResolveNodeRelative(decodedPath, m_executorInfo.getLocalWorkflowPath());
    }
}
