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
import java.net.URL;
import java.util.ArrayList;

import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.URIUtil;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.util.URIPathEncoder;
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
    URL resolveMountpointAbsolute(final URL url) throws ResourceAccessException {
        final var mountId = url.getAuthority();
        if (!m_locationInfo.getDefaultMountId().equals(mountId)) {
            // TODO should this be delegated to the `ExplorerMountTable`?
            throw new ResourceAccessException("Unknown Mount ID on Hub Executor in URL '" + url + "'.");
        }

        // we're on a Hub executor, resolve workflow locally via the repository
        final var decodedPath = URIPathEncoder.decodePath(url);
        final var versionInfo = HubItemVersion.of(url);

        final var repoUriBuilder = new URIBuilder(m_locationInfo.getRepositoryAddress());
        final var segments = new ArrayList<>(repoUriBuilder.getPathSegments());
        segments.addAll(new URIBuilder().setPath(decodedPath + ":data").getPathSegments());
        repoUriBuilder.setPathSegments(segments);
        versionInfo.ifPresent(version -> version.addVersionToURI(repoUriBuilder));
        return URLResolverUtil.toURL(repoUriBuilder);
    }

    @Override
    URI resolveMountpointRelative(final String decodedPath, final HubItemVersion version)
            throws ResourceAccessException {
        return resolveSpaceRelative(decodedPath, version);
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        // we're on a Hub executor, resolve workflow locally via the repository
        final var spacePath = m_locationInfo.getSpacePath();
        final var repositoryAddress = m_locationInfo.getRepositoryAddress();
        final var workflowPath = m_locationInfo.getWorkflowPath();
        final var workflowAddress = m_locationInfo.getWorkflowAddress();
        return createSpaceRelativeRepoUri(workflowAddress, workflowPath, decodedPath, repositoryAddress, spacePath,
            version);
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath, final HubItemVersion version) throws ResourceAccessException {
        if (leavesScope(decodedPath)) {
            // we're on a server of hub executor, resolve against the repository
            final var spacePath = m_locationInfo.getSpacePath();
            final var workflowAddress = m_locationInfo.getWorkflowAddress().normalize();
            final var plainUri = URIUtil.append(workflowAddress, decodedPath).normalize();
            final var relativeSpacePath = spacePath.startsWith("/") ? spacePath.substring(1) : spacePath;
            final var spaceUri = URIUtil.append(m_locationInfo.getRepositoryAddress(), relativeSpacePath).normalize();
            if (!isContainedIn(plainUri, spaceUri)) {
                throw new ResourceAccessException("Leaving the Hub space is not allowed for workflow relative URLs: "
                    + decodedPath + " is not in " + spacePath);
            }
            try {
                final var builder = new URIBuilder(URIUtil.append(workflowAddress, decodedPath + ":data"));
                if (version != null) {
                    version.addVersionToURI(builder);
                }
                return builder.build().normalize();
            } catch (URISyntaxException e) {
                throw new ResourceAccessException("Could not build space URI: " + e.getMessage(), e);
            }
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

    /**
     * Creates a space-relative URI.
     *
     * @param workflowAddress space-relative path
     * @param workflowPath space-relative path
     * @param decodedPath space-relative path
     * @param spacePath path to the Hub Space
     * @param spaceRepoUri REST repository address of the Hub Space
     * @param itemVersion item version, may be {@code null}
     * @return resolved URI
     * @throws ResourceAccessException if the URI doesn't stay in its lane
     */
    static URI createSpaceRelativeRepoUri(final URI workflowAddress, final String workflowPath,
        final String decodedPath, final URI repositoryAddress, final String spacePath, final HubItemVersion version)
        throws ResourceAccessException {
        final URI normalizedUri;
        final URI plainUri;
        final URI spaceRepoUri;
        try {
            // this URI does not have a trailing slash because of normalization, safe to append to
            final var spaceRepoUriBuilder = new URIBuilder(repositoryAddress);
            final var segments = new ArrayList<>(spaceRepoUriBuilder.getPathSegments());
            segments.addAll(new URIBuilder().setPath(spacePath).getPathSegments());
            spaceRepoUriBuilder.setPathSegments(segments);
            spaceRepoUri = spaceRepoUriBuilder.build().normalize();

            // omit `:data` and query parameter for the checks below
            plainUri = new URIBuilder(URIUtil.append(spaceRepoUri, decodedPath)).build().normalize();
            final var builder = new URIBuilder(URIUtil.append(spaceRepoUri, decodedPath + ":data"));
            if (version != null) {
                version.addVersionToURI(builder);
            }
            normalizedUri = builder.build().normalize();
        } catch (URISyntaxException e) {
            throw new ResourceAccessException("Could not build space URI: " + e.getMessage(), e);
        }

        if (isContainedIn(plainUri, workflowAddress)) {
            // we could allow this at some point and resolve the URL in the local file system
            throw new ResourceAccessException("Accessing the workflow contents is not allowed for space relative URLs: "
                + "'" + decodedPath + "' points into current workflow " + workflowPath);
        }

        if (!isContainedIn(plainUri, spaceRepoUri)) {
            throw new ResourceAccessException("Leaving the Hub space is not allowed for space relative URLs: "
                + decodedPath + " is not in " + spacePath);
        }

        return normalizedUri;
    }
}
