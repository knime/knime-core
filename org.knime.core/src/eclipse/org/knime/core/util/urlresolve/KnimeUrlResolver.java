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
import java.util.function.Function;
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
import org.knime.core.node.workflow.virtual.VirtualNodeContext;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;

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
     * Almost identical to {@link KnimeUrlType}, but distinguishes between path-based and ID-based absolute URLs.
     */
    public enum KnimeUrlVariant {
        /** Node-relative KNIME URL. */
        NODE_RELATIVE(KnimeUrlType.NODE_RELATIVE, "node-relative"),
        /** Workflow-relative KNIME URL. */
        WORKFLOW_RELATIVE(KnimeUrlType.WORKFLOW_RELATIVE, "workflow-relative"),
        /** Space-relative KNIME URL. */
        SPACE_RELATIVE(KnimeUrlType.HUB_SPACE_RELATIVE, "space-relative"),
        /** Mountpoint-relative KNIME URL. */
        MOUNTPOINT_RELATIVE(KnimeUrlType.MOUNTPOINT_RELATIVE, "mountpoint-relative"),
        /** Mountpoint-absolute, path-based KNIME URL. */
        MOUNTPOINT_ABSOLUTE_PATH(KnimeUrlType.MOUNTPOINT_ABSOLUTE, "mountpoint-absolute"),
        /** Mountpoint-absolute, Hub-ID-based KNIME URL. */
        MOUNTPOINT_ABSOLUTE_ID(KnimeUrlType.MOUNTPOINT_ABSOLUTE, "mountpoint-absolute (ID-based)");

        private final KnimeUrlType m_type;
        private final String m_desc;

        KnimeUrlVariant(final KnimeUrlType type, final String desc) {
            m_type = type;
            m_desc = desc;
        }

        /**
         * @return description for the variant, like {@code "workflow-relative"} or
         *         {@code "mountpoint-absolute (ID-based)"}
         */
        public String getDescription() {
            return m_desc;
        }

        /**
         * @return the base URL type (generalizing {@link #MOUNTPOINT_ABSOLUTE_PATH} and
         *         {@link #MOUNTPOINT_ABSOLUTE_ID})
         */
        public KnimeUrlType getType() {
            return m_type;
        }

        /**
         * Checks whether or not the given URL is a KNIME URL (under the {@code knime:} scheme) and returns the URL
         * variant if it is.
         *
         * @param url URL to get the type of
         * @return KNIME URL type if applicable, {@code null} otherwise
         * @throws IllegalArgumentException if the KNIME URL is malformed (missing {@link URL#getAuthority()
         *         authority component})
         */
        public static Optional<KnimeUrlVariant> getVariant(final URL url) {
            final var urlType = KnimeUrlType.getType(url);
            return urlType.isEmpty() ? Optional.empty() : Optional.of(switch (urlType.get()) {
                case NODE_RELATIVE -> KnimeUrlVariant.NODE_RELATIVE;
                case WORKFLOW_RELATIVE -> KnimeUrlVariant.WORKFLOW_RELATIVE;
                case HUB_SPACE_RELATIVE -> KnimeUrlVariant.SPACE_RELATIVE;
                case MOUNTPOINT_RELATIVE -> KnimeUrlVariant.MOUNTPOINT_RELATIVE;
                case MOUNTPOINT_ABSOLUTE -> isHubIdUrl(getPath(url)) ? KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_ID // NOSONAR
                    : KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_PATH;
            });
        }

        /**
         * Checks whether or not the URL represented by the given URI is a KNIME URL (under the {@code knime:} scheme)
         * and returns the URL variant if it is.
         *
         * @param uri URI to get the type of
         * @return KNIME URL variant if applicable, {@code null} otherwise
         * @throws IllegalArgumentException if the KNIME URL is malformed (missing {@link URI#getAuthority()
         *         authority component})
         */
        public static Optional<KnimeUrlVariant> getVariant(final URI uri) {
            final var urlType = KnimeUrlType.getType(uri);
            return urlType.isEmpty() ? Optional.empty() : Optional.of(switch (urlType.get()) {
                case NODE_RELATIVE -> KnimeUrlVariant.NODE_RELATIVE;
                case WORKFLOW_RELATIVE -> KnimeUrlVariant.WORKFLOW_RELATIVE;
                case HUB_SPACE_RELATIVE -> KnimeUrlVariant.SPACE_RELATIVE;
                case MOUNTPOINT_RELATIVE -> KnimeUrlVariant.MOUNTPOINT_RELATIVE;
                case MOUNTPOINT_ABSOLUTE -> isHubIdUrl(getPath(uri)) ? KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_ID // NOSONAR
                    : KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_PATH;
            });
        }
    }

    /**
     * Result of a successfully resolved KNIME URL.
     *
     * @param mountID mount ID of the enclosing mountpoint
     * @param path path to the resolved item
     * @param version Hub item version, may be {@code null}
     * @param pathInsideWorkflow path inside the workflow's directory in the executor, may be {@code null}
     * @param resourceURL URL which can be read and (potentially) written to
     * @param canBeRelativized flag indicating that the URL can be represented as a relative URL
     */
    record ResolvedURL(String mountID, IPath path, ItemVersion version, IPath pathInsideWorkflow,
        URL resourceURL, boolean canBeRelativized) {
        ResolvedURL withPath(final IPath newPath) {
            return new ResolvedURL(mountID, newPath, version, pathInsideWorkflow, resourceURL, canBeRelativized);
        }
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

            ItemVersion version = null;
            HubSpaceLocationInfo hubLocationInfo = null;
            if (restInfo instanceof HubSpaceLocationInfo hubInfo) {
                version = hubInfo.getItemVersion().stream().mapToObj(ItemVersion::of).findAny().orElse(null);
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
        if (KnimeUrlType.getType(url).orElse(null) == KnimeUrlType.MOUNTPOINT_ABSOLUTE) {
            // nothing to do
            return Optional.of(url);
        }

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
    public Map<KnimeUrlVariant, URL> changeLinkType(final URL url) throws ResourceAccessException {
        return changeLinkType(url, null);
    }

    /**
     * Result of the translation between path-based and ID-based KNIME Hub URLs.
     *
     * @param itemId Hub item ID
     * @param itemPath item path
     */
    public record IdAndPath(String itemId, IPath itemPath) {}

    /**
     * Computes alternative representations of the same KNIME URL.
     *
     * @param url initial URL
     * @param hubUrlTranslator translator between path-based and ID-based Hub URLs, may be {@code null}
     * @return mapping from available URL variants to the respective URL
     * @throws ResourceAccessException if the URL could not be resolved
     */
    public Map<KnimeUrlVariant, URL> changeLinkType(final URL url, // NOSONAR
            final Function<URL, Optional<IdAndPath>> hubUrlTranslator) throws ResourceAccessException {
        final var optResolved = resolveInternal(url);
        if (optResolved.isEmpty()) {
            return Map.of();
        }

        var resolved = optResolved.get();
        final var originalVariant = KnimeUrlVariant.getVariant(url).orElseThrow();
        IdAndPath idAndPath = null;
        var translationRequested = false;
        if (hubUrlTranslator != null && originalVariant == KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_ID) {
            // We need to call `resolveInternal(...)` with a path-based URL so the we can check all whether or not the
            // URL can be rewritten to a relative one (must be in same the space). If we can't determine the path, no
            // relative paths will be available in the output.
            // We also cache the result of the translation request because it may require a REST request.
            idAndPath = hubUrlTranslator.apply(url).orElse(null);
            translationRequested = true;

            if (idAndPath != null) {
                final var pathUrl = createKnimeUrl(resolved.mountID, idAndPath.itemPath(), resolved.version);
                resolved = resolveInternal(pathUrl).orElseThrow();
            }
        }

        final var out = new EnumMap<KnimeUrlVariant, URL>(KnimeUrlVariant.class);

        if (resolved.mountID != null && resolved.pathInsideWorkflow == null) {
            final var absoluteUrl = createKnimeUrl(resolved.mountID, resolved.path, resolved.version);
            if (hubUrlTranslator != null && !translationRequested) {
                // if the input is a path-based URL, translate it to an ID-based one (other direction was handled above)
                idAndPath = hubUrlTranslator.apply(absoluteUrl).orElse(null);
            }

            if (idAndPath != null) {
                out.put(KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_ID,
                    createKnimeUrl(resolved.mountID, Path.forPosix(idAndPath.itemId()), resolved.version));
                out.put(KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_PATH,
                    createKnimeUrl(resolved.mountID, idAndPath.itemPath(), resolved.version));
            } else if (isHubIdUrl(resolved.path)) {
                out.put(KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_ID, absoluteUrl);
            } else {
                out.put(KnimeUrlVariant.MOUNTPOINT_ABSOLUTE_PATH, absoluteUrl);
            }
        }

        if (!resolved.canBeRelativized()) {
            // preserve original URL as-is, without normalization in the dialog,
            // e.g. `knime://Mount-ID/foo/../bar?spaceVersion=3` instead of `knime://Mount-ID//bar?version=3`
            out.put(originalVariant, url);
            return out;
        }

        final var optContextPaths = getContextPaths();
        if (resolved.pathInsideWorkflow != null) {
            // workflow-relative URL into the workflow
            out.put(KnimeUrlVariant.WORKFLOW_RELATIVE, createKnimeUrl(KnimeUrlType.WORKFLOW_RELATIVE.getAuthority(),
                resolved.pathInsideWorkflow, null));

        } else if (optContextPaths.isPresent()) {
            final var contextPaths = optContextPaths.get();

            // workflow-relative URL outside the workflow
            final var pathRelativeToWorkflow = resolved.path.makeRelativeTo(contextPaths.workflowPath);
            out.put(KnimeUrlVariant.WORKFLOW_RELATIVE, createKnimeUrl(KnimeUrlType.WORKFLOW_RELATIVE.getAuthority(),
                pathRelativeToWorkflow, null));

            // mountpoint-relative and space-relative links are synonymous
            final var pathInsideSpace = resolved.path.makeRelativeTo(contextPaths.spacePath);
            for (final var variant : Set.of(KnimeUrlVariant.SPACE_RELATIVE, KnimeUrlVariant.MOUNTPOINT_RELATIVE)) {
                out.put(variant, createKnimeUrl(variant.getType().getAuthority(), pathInsideSpace, null));
            }
        }

        // preserve original URL as-is, without normalization in the dialog,
        // e.g. `knime://Mount-ID/foo/../bar?spaceVersion=3` instead of `knime://Mount-ID//bar?version=3`
        out.put(originalVariant, url);
        return out;
    }

    Optional<ResolvedURL> resolveInternal(final URL url) throws ResourceAccessException {
        final var optCurrentType = KnimeUrlType.getType(url);
        if (optCurrentType.isEmpty()) {
            return Optional.empty();
        }

        final var currrentType = optCurrentType.get();
        final var path = getPath(url);
        final var version = URLResolverUtil.parseVersion(url.getQuery()).orElse(null);
        final var resolved = switch (currrentType) {
            case MOUNTPOINT_ABSOLUTE -> resolveMountpointAbsolute(url, url.getAuthority(), path, version);
            case MOUNTPOINT_RELATIVE -> resolveMountpointRelative(url, path, version);
            case HUB_SPACE_RELATIVE  -> resolveSpaceRelative(url, path, version);
            case WORKFLOW_RELATIVE   -> resolveWorkflowRelativeInternal(url, path, version);
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

    private ResolvedURL resolveWorkflowRelativeInternal(final URL url, final IPath path, final ItemVersion version)
        throws ResourceAccessException {
        final var resolved = resolveWorkflowRelative(url, path, version);
        var virtualNodeContext = VirtualNodeContext.getContext().orElse(null);
        if (virtualNodeContext == null) {
            // normal operation, no restrictions
            return resolved;
        }

        if (resolved.pathInsideWorkflow != null && "data".equals(resolved.pathInsideWorkflow.segment(0))) {
            // data-area access in virtual context
            final var virtualDataAreaPath = virtualNodeContext.getVirtualDataAreaPath().orElse(null);
            if (virtualDataAreaPath == null) {
                if (virtualNodeContext.hasRestriction(Restriction.WORKFLOW_DATA_AREA_ACCESS)) {
                    throw new ResourceAccessException("Node is not allowed to access workflow data area "
                        + "because it's executed within in a restricted (virtual) scope.");
                }
                return resolved;
            }

            final var pathInsideDataArea = resolved.pathInsideWorkflow.removeFirstSegments(1);
            final var resolvedInVirtualArea = virtualDataAreaPath.resolve(pathInsideDataArea.toOSString()).normalize();
            return new ResolvedURL(resolved.mountID(), resolved.path(), resolved.version(),
                resolved.pathInsideWorkflow(), URLResolverUtil.toURL(resolvedInVirtualArea),
                resolved.canBeRelativized());
        } else {
            if (virtualNodeContext.hasRestriction(Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS)) {
                // not allowed to access workflow-relative resources at all
                throw new ResourceAccessException("Node is not allowed to access workflow-relative resources "
                    + "because it's executed within in a restricted (virtual) scope.");
            }
            return resolved;
        }
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
     * @param url to resolve
     * @param mountId host component of the url to resolve
     * @param path path component of the url to resolve
     * @param version item version stated by url to resolve
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveMountpointAbsolute(URL url, String mountId, IPath path, ItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a mountpoint relative URL in this resolver's scope.
     *
     * @param url to resolve
     * @param path path component of the url to resolve
     * @param version item version stated by url to resolve
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveMountpointRelative(URL url, IPath path, ItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a space relative URL in this resolver's scope.
     *
     * @param url to resolve
     * @param path path component of the url to resolve
     * @param version item version stated by url to resolve
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveSpaceRelative(URL url, IPath path, ItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a workflow relative URL in this resolver's scope.
     *
     * @param url to resolve
     * @param path path component of the url to resolve
     * @param version item version stated by url to resolve
     * @throws ResourceAccessException if the URL could not be resolved
     */
    abstract ResolvedURL resolveWorkflowRelative(URL url, IPath path, ItemVersion version)
            throws ResourceAccessException;

    /**
     * Resolves a node relative URL in this resolver's scope.
     *
     * @param url to resolve
     * @param path path component of the url to resolve
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
        return new ResolvedURL(mountId, pathToWorkflow, null, pathInsideWorkflow, resourceUrl, true);
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
        final IPath pathInWorkflow, final ItemVersion version, final java.nio.file.Path localWorkflowPath)
            throws ResourceAccessException {
        if (version != null) {
            throw new ResourceAccessException("Workflow relative URLs accessing workflow contents cannot specify a "
                    + "version: '" + url + "'.");
        }

        final var resolvedPath = localWorkflowPath.resolve(pathInWorkflow.toOSString());
        final var resourceUrl = URLResolverUtil.toURL(resolvedPath);
        return new ResolvedURL(mountId, workflowPath, version, pathInWorkflow, resourceUrl, true);
    }

    static URL createKnimeUrl(final String mountId, final IPath path, final ItemVersion version)
            throws ResourceAccessException {
        final var builder = new URIBuilder() //
                .setScheme(KnimeUrlType.SCHEME) //
                .setHost(mountId) //
                .setPathSegments(Arrays.asList(path.segments()));
        if (version != null) {
            URLResolverUtil.addVersionQueryParameter(version, builder::addParameter);
        }
        return URLResolverUtil.toURL(builder);
    }

    /**
     * Checks whether the given path component of a KNIME URL looks like a Hub ID URL.
     * ID URLs have a single path segment which starts with an asterisk:
     * <ul>
     *   <li>{@code knime://My-KNIME-Hub/*a1b2c3d4}</li>
     *   <li>{@code knime://ACMECorp-Hub/*d34db33f?itemVersion=5}</li>
     * </ul>
     *
     * @param path the URL's parsed path component
     * @return {@code true} if the path looks like a Hub item ID, {@code false} otherwise
     */
    static boolean isHubIdUrl(final IPath path) {
        return path.segmentCount() == 1 && path.segment(0).startsWith("*");
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
