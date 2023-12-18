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
 *   Nov 27, 2022 (leonard.woerteler): created
 */
package org.knime.core.util.urlresolve;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.LocationType;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * Resolves a KNIME URL in a specified environment specified by a {@link WorkflowContextV2}.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public abstract class KnimeUrlResolver {

    /** Regular expression pattern matching either the string {@code /..} or any string starting with {@code "/../"}. */
    private static final Pattern LEAVING_SCOPE_PATTERN = Pattern.compile("^/\\.\\.(?:/.*|$)");

    /**
     * Creates a KNIME URL resolver for the given context.
     *
     * @param workflowContext context of the workflow
     * @return URL resolver
     */
    public static KnimeUrlResolver getResolver(final WorkflowContextV2 workflowContext) {
        if (workflowContext == null) {
            return ContextlessUrlResolver.INSTANCE;
        }

        final var executorInfo = workflowContext.getExecutorInfo();
        final var locationInfo = workflowContext.getLocationInfo();

        if (executorInfo instanceof JobExecutorInfo jobExec && jobExec.isRemote()) {
            return new RemoteExecutorUrlResolver((RestLocationInfo)locationInfo);
        }

        switch (executorInfo.getType()) {
            case ANALYTICS_PLATFORM:
                final var apExecInfo = (AnalyticsPlatformExecutorInfo)executorInfo;
                return locationInfo.getType() == LocationType.LOCAL
                    ? new AnalyticsPlatformLocalUrlResolver(apExecInfo)
                    : new AnalyticsPlatformTempCopyUrlResolver(apExecInfo, (RestLocationInfo)locationInfo,
                        workflowContext.getMountpointURI().orElseThrow());
            case HUB_EXECUTOR:
                return new HubExecutorUrlResolver((HubJobExecutorInfo)executorInfo,
                    (HubSpaceLocationInfo)locationInfo);
            case SERVER_EXECUTOR:
                return new ServerExecutorUrlResolver((ServerJobExecutorInfo)executorInfo,
                    (ServerLocationInfo)locationInfo);
            default:
                throw new IllegalStateException("Unknown executor for URL resolution: " + executorInfo.getType());
        }
    }

    /**
     * Resolves a given KNIME URL to a URL than can be used by the executor to access the referenced resources. This
     * method may return
     * <ul>
     *     <li>mountpoint-absolute {@code knime://} URLs that can be opened in AP via the mount table,</li>
     *     <li>local {@code file://} URLs accessing the executor's file system, and</li>
     *     <li>{@code https://} REST URLs that point to the repository the workflow belongs to.</li>
     * </ul>
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if the URL could not be resolved
     */
    public URL resolve(final URL url) throws ResourceAccessException {
        switch (KnimeUrlType.getType(url)
            .orElseThrow(() -> new ResourceAccessException("Failed to resolve URL, is not present"))) {
            case MOUNTPOINT_ABSOLUTE:
                return resolveMountpointAbsolute(url);
            case MOUNTPOINT_RELATIVE:
                return resolveMountpointRelative(url);
            case HUB_SPACE_RELATIVE:
                return resolveSpaceRelative(url);
            case WORKFLOW_RELATIVE:
                return resolveWorkflowRelative(url);
            case NODE_RELATIVE:
                return resolveNodeRelative(url);
            default:
                throw new IllegalStateException("Unhandled KNIME URL type: " + url);
        }
    }

    /**
     * Resolves a mountpoint absolute KNIME URL against this resolver's context.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if resolution was not possible
     */
    URL resolveMountpointAbsolute(final URL url) throws ResourceAccessException {
        return url;
    }

    /**
     * Resolves a mountpoint relative KNIME URL against this resolver's context.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if resolution was not possible
     */
    final URL resolveMountpointRelative(final URL url) throws ResourceAccessException {
        final var resolvedUri = resolveMountpointRelative(URIPathEncoder.decodePath(url),
            HubItemVersion.of(url).orElse(null));
        return URLResolverUtil.toURL(resolvedUri);
    }

    /**
     * Resolves a space relative KNIME URL against this resolver's context.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if resolution was not possible
     */
    final URL resolveSpaceRelative(final URL url) throws ResourceAccessException {
        final var resolvedUri = resolveSpaceRelative(URIPathEncoder.decodePath(url),
            HubItemVersion.of(url).orElse(null));
        return URLResolverUtil.toURL(resolvedUri);
    }

    /**
     * Resolves a workflow relative KNIME URL against this resolver's context.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if resolution was not possible
     */
    final URL resolveWorkflowRelative(final URL url) throws ResourceAccessException {
        final var resolvedUri = resolveWorkflowRelative(URIPathEncoder.decodePath(url),
            HubItemVersion.of(url).orElse(null));
        return URLResolverUtil.toURL(resolvedUri);
    }

    /**
     * Resolves a node relative KNIME URL against this resolver's context.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if resolution was not possible
     */
    final URL resolveNodeRelative(final URL url) throws ResourceAccessException {
        if (HubItemVersion.of(url).isPresent()) {
            throw new ResourceAccessException("Node-relative KNIME URLs cannot specify an item version: " + url);
        }
        final var resolvedUri = resolveNodeRelative(URIPathEncoder.decodePath(url));
        return URLResolverUtil.toURL(resolvedUri);
    }

    /**
     * Default implementation for node relative URL resolution.
     *
     * @param decodedPath decoded URL path component
     * @param localWorkflowPath local workflow path
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    URI defaultResolveNodeRelative(final String decodedPath, final Path localWorkflowPath)
            throws ResourceAccessException {
        ReferencedFile nodeDirectoryRef = NodeContext.getContext().getNodeContainer().getNodeContainerDirectory();
        if (nodeDirectoryRef == null) {
            throw new ResourceAccessException("Workflow must be saved before node-relative URLs can be used");
        }

        // check if resolved path leaves the workflow
        final var resolvedPath = new File(nodeDirectoryRef.getFile().getAbsolutePath(), decodedPath);

        final var currentLocation = localWorkflowPath.toFile();
        String resolved = URLResolverUtil.getCanonicalPath(resolvedPath);
        String workflow = URLResolverUtil.getCanonicalPath(currentLocation);

        if (!resolved.startsWith(workflow)) {
            throw new ResourceAccessException(
                "Leaving the workflow is not allowed for node-relative URLs: " + resolved + " is not in " + workflow);
        }
        return resolvedPath.toURI();
    }

    /**
     * Resolves a mountpoint relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract URI resolveMountpointRelative(String decodedPath, HubItemVersion version) throws ResourceAccessException;

    /**
     * Resolves a space relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract URI resolveSpaceRelative(String decodedPath, HubItemVersion version) throws ResourceAccessException;

    /**
     * Resolves a workflow relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract URI resolveWorkflowRelative(String decodedPath, HubItemVersion version) throws ResourceAccessException;

    /**
     * Resolves a node relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract URI resolveNodeRelative(String decodedPath) throws ResourceAccessException;

    /**
     * Checks if the first given URI if contained in the second one, more specifically that it can be addressed via a
     * relative path from the second one.
     *
     * @param inner inner address
     * @param outer outer address
     * @return {@code true} if the first argument is contained in the second, {@code false} otherwise
     */
    static final boolean isContainedIn(final URI inner, final URI outer) {
        return !outer.relativize(inner).isAbsolute();
    }

    /**
     * Checks if the given path starts with {@code /../}, which signals that it is supposed to escape the current scope.
     * Note that no normalization takes place, so the path could try to escape the scope later. THe resolver has to
     * verify that this does not happen and that URLs <i>not</i> starting with {@code /../} stay in their scope.
     *
     * @param decodedPath path to analyze
     * @return {@code true} if the path signals that it leaves the scope, {@code false} otherwise
     */
    static final boolean leavesScope(final String decodedPath) {
        return LEAVING_SCOPE_PATTERN.matcher(decodedPath).matches();
    }
}
