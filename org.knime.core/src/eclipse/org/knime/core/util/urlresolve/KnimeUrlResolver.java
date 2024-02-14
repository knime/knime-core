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

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
 * @since 5.3
 */
public abstract class KnimeUrlResolver {

    /** Empty POSIX path, used as the default space path. */
    static final Path EMPTY_POSIX_PATH = Path.forPosix("");

    /** Pattern for removing leading slashes from paths. */
    static final Pattern LEADING_SLASHES = Pattern.compile("^/+");

    /**
     * Result of a successfully resolved KNIME URL.
     *
     * @param mountID mount ID of the enclosing mountpoint
     * @param path path to the resolved item
     * @param version Hub item version, may be {@code null}
     * @param pathInsideWorkflow path inside the workflow's directory in the executor, may be {@code null}
     * @param resourceURL URL which can be read and (potentially) written to
     * @param cannotBeRelativized flag indicating that the url references a resource not under the current mountpoint
     *      or is a KNIME Hub ID URL, both of which can't be converted to a relative URL
     */
    record ResolvedURL(String mountID, IPath path, HubItemVersion version, IPath pathInsideWorkflow,
        URL resourceURL, boolean cannotBeRelativized) {
    }

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
            final var restInfo = (RestLocationInfo)locationInfo;
            final var workflowPath = getWorkflowPath(restInfo);
            // This is the default mount ID as given by the remote server, might differ from the local mount ID.
            // If the user renames the mountpoint, resolution fails here -- live with.
            final var mountId = restInfo.getDefaultMountId();

            HubItemVersion version = null;
            HubSpaceLocationInfo hubLocationInfo = null;
            if (restInfo instanceof HubSpaceLocationInfo hubInfo) {
                version = hubInfo.getItemVersion().stream().mapToObj(HubItemVersion::of).findAny().orElse(null);
                hubLocationInfo = hubInfo;
            }

            try {
                final var mountpointUri = URLResolverUtil.toURI(createKnimeUrl(mountId, workflowPath, version));
                return new RemoteExecutorUrlResolver(mountpointUri, hubLocationInfo);
            } catch (ResourceAccessException ex) {
                throw new IllegalStateException("Cannot create mountpoint URL: " + ex.getMessage(), ex);
            }
        }

        return switch (executorInfo.getType()) {
            case ANALYTICS_PLATFORM -> { // NOSONAR
                final var apExecInfo = (AnalyticsPlatformExecutorInfo)executorInfo;
                yield locationInfo.getType() == LocationType.LOCAL
                    ? new AnalyticsPlatformLocalUrlResolver(apExecInfo)
                    : new AnalyticsPlatformTempCopyUrlResolver(apExecInfo, (RestLocationInfo)locationInfo,
                        workflowContext.getMountpointURI().orElseThrow());
            }
            case HUB_EXECUTOR -> new HubExecutorUrlResolver((HubJobExecutorInfo)executorInfo,
                    (HubSpaceLocationInfo)locationInfo);
            case SERVER_EXECUTOR -> new ServerExecutorUrlResolver((ServerJobExecutorInfo)executorInfo,
                    (ServerLocationInfo)locationInfo);
        };
    }

    /**
     * Creates a KNIME URL resolver for the local dialogs of a remotely executed workflow.
     *
     * @param mountpointUri mountpoint URI of the workflow
     * @param workflowContext context of the workflow
     * @return URL resolver
     */
    public static KnimeUrlResolver getRemoteWorkflowResolver(final URI mountpointUri,
        final WorkflowContextV2 workflowContext) {
        return new RemoteExecutorUrlResolver(mountpointUri, workflowContext != null
                && workflowContext.getLocationInfo() instanceof HubSpaceLocationInfo hubInfo ? hubInfo : null);
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
        return resolveInternal(url) //
                .map(ResolvedURL::resourceURL) //
                .orElseThrow(() -> new ResourceAccessException("Failed to resolve, not a valid KNIME URL: " + url));
    }

    /**
     * Resolves a given KNIME URL to a mountpoint-absolute KNIME URL if possible.
     *
     * @param uri URI representing the URL to resolve
     * @return resolved URI
     * @throws ResourceAccessException if the URI could not be resolved
     */
    public Optional<URI> resolveToAbsolute(final URI uri) throws ResourceAccessException {
        final var result = resolveToAbsolute(URLResolverUtil.toURL(uri));
        return result.isEmpty() ? Optional.empty() : Optional.of(URLResolverUtil.toURI(result.get()));
    }

    /**
     * Resolves a given KNIME URL to a mountpoint-absolute KNIME URL if possible.
     *
     * @param url URL to resolve
     * @return resolved URL
     * @throws ResourceAccessException if the URL could not be resolved
     */
    public Optional<URL> resolveToAbsolute(final URL url) throws ResourceAccessException {
        final var optResolved = resolveInternal(url);
        if (optResolved.isEmpty()) {
            return Optional.empty();
        }

        final var resolved = optResolved.get();
        if (KnimeUrlType.getType(resolved.resourceURL()) //
                .filter(t -> t == KnimeUrlType.MOUNTPOINT_ABSOLUTE).isPresent()) {
            return Optional.of(resolved.resourceURL());
        }

        final var notInsideWorkflow = resolved.pathInsideWorkflow == null || resolved.pathInsideWorkflow.isEmpty();
        return resolved.mountID != null && notInsideWorkflow
                ? Optional.of(createKnimeUrl(resolved.mountID, resolved.path, resolved.version)) : Optional.empty();
    }

    /**
     * Computes alternative representations of the same KNIME URL.
     *
     * @param url initial URL
     * @return mapping from available URL types to the respective URL
     * @throws ResourceAccessException if the URL could not be resolved
     */
    public Map<KnimeUrlType, URL> changeLinkType(final URL url) throws ResourceAccessException { //NOSONAR too complex
        final var optResolved = resolveInternal(url);
        if (optResolved.isEmpty()) {
            return Map.of();
        }

        final var resolved = optResolved.get();
        final var out = new EnumMap<KnimeUrlType, URL>(KnimeUrlType.class);


        if (resolved.mountID != null && resolved.pathInsideWorkflow == null) {
            out.put(KnimeUrlType.MOUNTPOINT_ABSOLUTE,
                createKnimeUrl(resolved.mountID, resolved.path, resolved.version));
        }

        if (resolved.cannotBeRelativized()) {
            // only absolute URLs can reference resources across mountpoints
            return out;
        }

        if (KnimeUrlType.NODE_RELATIVE.getAuthority().equals(url.getAuthority())) {
            // we only offer node-relative URLs if that's what came in
            out.put(KnimeUrlType.NODE_RELATIVE, url);
        }

        final var optContextPaths = getContextPaths();
        if (resolved.pathInsideWorkflow != null) {
            // workflow-relative URL into the workflow
            out.put(KnimeUrlType.WORKFLOW_RELATIVE, createKnimeUrl(KnimeUrlType.WORKFLOW_RELATIVE.getAuthority(),
                resolved.pathInsideWorkflow, null));

        } else if (optContextPaths.isPresent()) {
            final var contextPaths = optContextPaths.get();

            // workflow-relative URL outside the workflow
            final var pathRelativeToWorkflow = resolved.path.makeRelativeTo(contextPaths.workflowPath);
            out.put(KnimeUrlType.WORKFLOW_RELATIVE, createKnimeUrl(KnimeUrlType.WORKFLOW_RELATIVE.getAuthority(),
                pathRelativeToWorkflow, resolved.version));

            // mountpoint-relative and space-relative links are synonymous
            final var pathInsideSpace = resolved.path.makeRelativeTo(contextPaths.spacePath);
            for (final var type : Set.of(KnimeUrlType.HUB_SPACE_RELATIVE, KnimeUrlType.MOUNTPOINT_RELATIVE)) {
                out.put(type, createKnimeUrl(type.getAuthority(), pathInsideSpace, resolved.version));
            }
        }

        return out;
    }

    Optional<ResolvedURL> resolveInternal(final URL url) throws ResourceAccessException {
        final var optCurrentType = KnimeUrlType.getType(url);
        if (optCurrentType.isEmpty()) {
            return Optional.empty();
        }

        final var currrentType = optCurrentType.get();
        final var path = getPath(url);
        final var version = HubItemVersion.of(url).orElse(null);
        final var resolved = switch (currrentType) {
            case MOUNTPOINT_ABSOLUTE -> resolveMountpointAbsolute(url, url.getAuthority(), path, version);
            case MOUNTPOINT_RELATIVE -> resolveMountpointRelative(url, path, version);
            case HUB_SPACE_RELATIVE  -> resolveSpaceRelative(url, path, version);
            case WORKFLOW_RELATIVE   -> resolveWorkflowRelative(url, path, version);
            case NODE_RELATIVE       -> { //NOSONAR one line too long
                if (version != null) {
                    throw new ResourceAccessException(
                        "Node-relative KNIME URLs cannot specify an item version: " + url);
                }
                yield resolveNodeRelative(url, path);
            }
        };

        return Optional.of(resolved);
    }

    /**
     * Paths to the root of the space and workflow of the current context.
     *
     * @param spacePath relative path from the root of the mountpoint to the root of the space
     * @param workflowPath relative path from the root of the mountpoint to the root of the workflow
     */
    record ContextPaths(IPath spacePath, IPath workflowPath) {}

    /**
     * Paths to the root of the space and workflow of the current context.
     *
     * @return context paths if known, {@link Optional#empty()} otherwise
     */
    abstract Optional<ContextPaths> getContextPaths();

    /**
     * Resolves a mountpoint absolute URL in this resolver's scope.
     *
     * @param mountId mount ID
     * @param decodedPath URI's decoded path component
     * @param version item version
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveMountpointAbsolute(URL url, String mountId, IPath path, HubItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a mountpoint relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @param version item version
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveMountpointRelative(URL url, IPath path, HubItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a space relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @param version item version
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveSpaceRelative(URL url, IPath path, HubItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a workflow relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @param version item version
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveWorkflowRelative(URL url, IPath path, HubItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a node relative URL in this resolver's scope.
     *
     * @param decodedPath URI's decoded path component
     * @return resolved URI
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveNodeRelative(URL url, IPath path) throws ResourceAccessException;

    /**
     * Resolves the given relative path relative to the current node's directory on the executor.
     *
     * @param mountId mount ID of the mountpoint in the local AP or on the stand-alone executor
     * @param pathToWorkflow relative path from the root of the space to the workflow
     * @param localWorkflowPath location of the current workflow's root directory in the executor's file system
     * @param pathFromNodeDir path relative to the current node's directory
     * @return resolved URL components
     * @throws ResourceAccessException if resolution fails
     */
    static ResolvedURL resolveNodeRelative(final String mountId, final IPath pathToWorkflow,
        final java.nio.file.Path localWorkflowPath, final IPath pathFromNodeDir) throws ResourceAccessException {

        final var nodeDirectoryRef = NodeContext.getContext().getNodeContainer().getNodeContainerDirectory();
        if (nodeDirectoryRef == null) {
            throw new ResourceAccessException("Workflow must be saved before node-relative URLs can be used");
        }

        final var nodeDir = nodeDirectoryRef.getFile().toPath().toAbsolutePath();
        final var resolvedPath = nodeDir.resolve(pathFromNodeDir.toOSString()).normalize();
        final var workflowPath = localWorkflowPath.toAbsolutePath();

        // check if resolved path leaves the workflow
        if (!resolvedPath.startsWith(workflowPath)) {
            throw new ResourceAccessException(
                "Leaving the workflow is not allowed for node-relative URLs: '"
                        + resolvedPath + "' is not in '" + workflowPath + "'");
        }

        final var pathInsideWorkflow = Path.fromOSString(workflowPath.relativize(resolvedPath).toString());
        final var resourceUrl = URLResolverUtil.toURL(resolvedPath);
        return new ResolvedURL(mountId, pathToWorkflow, null, pathInsideWorkflow, resourceUrl, false);
    }

    /**
     * Resolves the given relative path relative to the current workflow's directory on the executor.
     *
     * @param url original KNIME URL
     * @param mountId mount ID of the mountpoint in the local AP or on the stand-alone executor
     * @param workflowPath relative path from the root of the space to the workflow
     * @param pathInWorkflow relative space from the root of the workflow to the referenced item,
     *      must not contain {@code ".."} steps
     * @param version version for sanity check
     * @param localWorkflowPath location of the current workflow's root directory in the executor's file system
     * @return resolved URL components
     * @throws ResourceAccessException if a version was specified
     */
    static ResolvedURL resolveInExecutorWorkflowDir(final URL url, final String mountId, final IPath workflowPath,
        final IPath pathInWorkflow, final HubItemVersion version, final java.nio.file.Path localWorkflowPath)
            throws ResourceAccessException {
        if (version != null) {
            throw new ResourceAccessException("Workflow relative URLs accessing workflow contents cannot specify a "
                    + "version: '" + url + "'.");
        }

        final var resolvedPath = localWorkflowPath.resolve(pathInWorkflow.toOSString());
        final var resourceUrl = URLResolverUtil.toURL(resolvedPath);
        return new ResolvedURL(mountId, workflowPath, version, pathInWorkflow, resourceUrl, false);
    }

    static URL createKnimeUrl(final String mountId, final IPath path, final HubItemVersion version)
            throws ResourceAccessException {
        final var builder = new URIBuilder() //
                .setScheme(KnimeUrlType.SCHEME) //
                .setHost(mountId) //
                .setPathSegments(Arrays.asList(path.segments()));
        if (version != null) {
            version.addVersionToURI(builder);
        }
        return URLResolverUtil.toURL(builder);
    }

    /**
     * Checks if the given path starts with {@code /../}, which signals that it is supposed to escape the current scope.
     * Note that no normalization takes place, so the path could try to escape the scope later. The resolver has to
     * verify that this does not happen and that URLs <i>not</i> starting with {@code /../} stay in their scope.
     *
     * @param decodedPath path to analyze
     * @return {@code true} if the path signals that it leaves the scope, {@code false} otherwise
     */
    static final boolean leavesScope(final IPath path) {
        return path.segmentCount() > 0 && "..".equals(path.segment(0));
    }

    static final IPath getPath(final URL url) {
        return toRelativeIPath(URIPathEncoder.decodePath(url));
    }

    static final IPath getPath(final URI uri) {
        return Optional.ofNullable(uri.getPath()).map(KnimeUrlResolver::toRelativeIPath).orElse(EMPTY_POSIX_PATH);
    }

    static final IPath getWorkflowPath(final RestLocationInfo restInfo) {
        return toRelativeIPath(restInfo.getWorkflowPath());
    }

    static final IPath getSpacePath(final RestLocationInfo restInfo) {
        return restInfo instanceof HubSpaceLocationInfo hubInfo ? toRelativeIPath(hubInfo.getSpacePath())
            : EMPTY_POSIX_PATH;
    }

    static final IPath toRelativeIPath(final String posixPath) {
        return Path.forPosix(LEADING_SLASHES.matcher(posixPath).replaceFirst("")).makeRelative();
    }
}
