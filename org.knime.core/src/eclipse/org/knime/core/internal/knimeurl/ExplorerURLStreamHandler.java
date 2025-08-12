/*
 * ------------------------------------------------------------------------
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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.core.internal.knimeurl;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.virtual.VirtualNodeContext;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.KNIMEServerHostnameVerifier;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.auth.Authenticator;
import org.knime.core.util.auth.CouldNotAuthorizeException;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.proxy.URLConnectionFactory;
import org.knime.core.util.urlresolve.KnimeUrlResolver;
import org.knime.workbench.explorer.ServerRequestModifier;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * Handler for the <tt>knime</tt> protocol. It can resolved three types of URLs:
 * <ul>
 * <li>workflow-relative URLs using the magic hostname <tt>knime.workflow</tt> (see
 * {@link CoreConstants#WORKFLOW_RELATIVE})</li>
 * <li>mountpoint-relative URLs using the magic hostname <tt>knime.mountpoint</tt> (see
 * {@link CoreConstants#MOUNTPOINT_RELATIVE})</li>
 * <li>mount point in the KNIME Explorer using the mount point name as hostname</li>
 * </ul>
 *
 * @author ohl, University of Konstanz
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 5.8
 * @noreference This class is not intended to be referenced by clients.
 */
public class ExplorerURLStreamHandler extends AbstractURLStreamHandlerService {

    private final ServerRequestModifier m_requestModifier;

    /**
     * Creates a new URL stream handler.
     */
    public ExplorerURLStreamHandler() {
        final var myself = FrameworkUtil.getBundle(getClass());
        if (myself != null) {
            final var ctx = myself.getBundleContext();
            final ServiceReference<?> ser = ctx.getServiceReference(ServerRequestModifier.class);
            if (ser != null) {
                try {
                    m_requestModifier = (ServerRequestModifier)ctx.getService(ser);
                } finally {
                    ctx.ungetService(ser);
                }
            } else {
                m_requestModifier = (p, c) -> {};
            }
        } else {
            m_requestModifier = (p, c) -> {};
        }
    }

    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        return openConnection(url, null);
    }

    @Override
    public URLConnection openConnection(final URL url, final Proxy p) throws IOException {
        var urlType = getUrlType(url);
        checkCanOpen(urlType);
        final var resolvedUrl = resolveKNIMEURL(url, urlType);
        if (p != null && !Proxy.NO_PROXY.equals(p)) {
            NodeLogger.getLogger(ExplorerURLStreamHandler.class).debug(String.format(
                "Ignoring proxy \"%s\" for unresolved URL \"%s\", will apply for proxy for " +
                        "resolved URL (could be the same as ignored here)", p, url));
        }
        // global proxy settings will be applied if and when an actual remote connection is opened
        return openConnectionForResolved(resolvedUrl);
    }

    private static void checkCanOpen(final KnimeUrlType urlType) throws IOException {
        if (urlType != KnimeUrlType.WORKFLOW_RELATIVE) {
            return;
        }
        var errorMessage = VirtualNodeContext.getContext().map(vnc -> {
            if (vnc.hasRestriction(VirtualNodeContext.Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS)) {
                return "Node is not allowed to access workflow-relative resources " +
                        "because it's executed within in a restricted (virtual) scope.";
            } else if (vnc.hasRestriction(VirtualNodeContext.Restriction.WORKFLOW_DATA_AREA_ACCESS)) {
                return "Node is not allowed to access workflow data area " +
                        "because it's executed within in a restricted (virtual) scope.";
            } else {
                return null;
            }
        }).orElse(null);
        if (errorMessage != null) {
            throw new IOException(errorMessage);
        }
    }

    /**
     * Opens the connection to an *already resolved* URL, distinguishes between KNIME URLs (opens the connection via the
     * mount table) and other URLs, like HTTP(S).
     *
     * @param resolvedUrl
     * @return opened connection to the given URL
     * @throws IOException
     */
    private URLConnection openConnectionForResolved(final URL resolvedUrl) throws IOException {
        if (CoreConstants.SCHEME.equals(resolvedUrl.getProtocol())) {
            return openExternalMountConnection(resolvedUrl);
        } else if ("http".equals(resolvedUrl.getProtocol()) || "https".equals(resolvedUrl.getProtocol())) {
            // neither the node context nor the workflow context can be null here, otherwise resolveKNIMEURL would have
            // already failed
            final var workflowContext =
                NodeContext.getContext().getContextObjectForClass(WorkflowManager.class).orElseThrow().getContextV2();
            final var conn = URLConnectionFactory.getConnection(resolvedUrl);
            authorizeClient(workflowContext, conn);

            getRemoteRepositoryAddress(workflowContext).ifPresent(u -> m_requestModifier.modifyRequest(u, conn));

            if (conn instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setHostnameVerifier(KNIMEServerHostnameVerifier.getInstance());
            }
            return conn;
        } else {
            return URLConnectionFactory.getConnection(resolvedUrl);
        }
    }

    /**
     * Resolves a knime-URL to the final address. The final address can be a local file-URL in case the workflow runs
     * locally, a KNIME server address, in case the workflow runs inside an executor, or the unaltered address in case
     * it points to a server mount point.
     *
     * @param url a KNIME URL
     * @return the resolved URL
     * @throws ResourceAccessException if an error occurs while resolving the URL
     */

    public static URL resolveKNIMEURL(final URL url) throws ResourceAccessException {
        return resolveKNIMEURL(url, getUrlType(url));
    }

    private static KnimeUrlType getUrlType(final URL url) throws ResourceAccessException {
        return KnimeUrlType.getType(url).orElseThrow(() -> new ResourceAccessException("Unexpected protocol: "
            + url.getProtocol() + ". Only " + KnimeUrlType.SCHEME + " is supported by this handler."));
    }

    private static URL resolveKNIMEURL(final URL url, final KnimeUrlType urlType) throws ResourceAccessException {
        final var nodeContext = NodeContext.getContextOptional();
        final var workflowContextV2Opt =
            nodeContext.flatMap(ctx -> ctx.getContextObjectForClass(WorkflowContextV2.class));
        if (urlType.isRelative()) {
            if (nodeContext.isEmpty()) {
                throw new ResourceAccessException("No context for relative URL available");
            } else if (workflowContextV2Opt.isEmpty()) {
                throw new ResourceAccessException("Workflow " + url.toString() + " does not have a context");
            }
        }

        final KnimeUrlResolver resolver;
        final var workflowContextV2 = workflowContextV2Opt.orElse(null);
        if (workflowContextV2 != null && workflowContextV2.getExecutorInfo() instanceof JobExecutorInfo jobExecutorInfo
                && jobExecutorInfo.isRemote()) {
            // old Remote Workflow Editor, resolving a KNIME URL inside a local dialog
            final var uriBuilder = new URIBuilder();
            uriBuilder.setScheme(CoreConstants.SCHEME);
            uriBuilder.setHost(jobExecutorInfo.getLocalMountId().orElseThrow());
            uriBuilder.setPath(jobExecutorInfo.getLocalWorkflowPath().toString());

            URI mountPointURI = null;
            try {
                mountPointURI = uriBuilder.build();
            } catch (URISyntaxException ex) {
                throw new ResourceAccessException(
                    "Couldn't create URL connection: Invalid URL: %s".formatted(uriBuilder.toString()), ex);
            }

            resolver = KnimeUrlResolver.getRemoteWorkflowResolver(mountPointURI, workflowContextV2);
        } else {
            resolver = KnimeUrlResolver.getResolver(workflowContextV2);
        }

        return resolver.resolve(url);
    }

    private static Optional<URI> getRemoteRepositoryAddress(final WorkflowContextV2 workflowContextV2) {
        if (workflowContextV2 != null && workflowContextV2.getExecutorInfo() instanceof JobExecutorInfo jobExecutorInfo
            && jobExecutorInfo.isRemote()) {
            return Optional.of(workflowContextV2)
                .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                .map(RestLocationInfo::getRepositoryAddress);
        }
        return Optional.empty();
    }

    private static Optional<Authenticator> getServerAuthenticator(final WorkflowContextV2 workflowContextV2) {
        if (workflowContextV2.getExecutorInfo() instanceof JobExecutorInfo jobExecutorInfo
            && jobExecutorInfo.isRemote()) {
            return Optional.of(workflowContextV2)
                .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                .map(RestLocationInfo::getAuthenticator);
        }
        return Optional.empty();
    }

    private static void authorizeClient(final WorkflowContextV2 workflowContext, final URLConnection conn)
        throws IOException {
        final Optional<Authenticator> authenticator = getServerAuthenticator(workflowContext);
        if (authenticator.isPresent()) {
            try {
                authenticator.get().authorizeClient(conn);
            } catch (CouldNotAuthorizeException e) {
                throw new IOException("Error while authenticating the client: " + e.getMessage(), e);
            }
        }
    }

    private static URLConnection openExternalMountConnection(final URL url) throws IOException {
        ItemVersion itemVersion =
                MountPointURLUtil.getVersionParam(url).map(ItemVersion::convertToItemVersion).orElse(null);
        return MountPointURLUtil.getMountPointURLService(url) //
            .newURLConnection(IPath.forPosix(URIPathEncoder.decodePath(url)), itemVersion);
    }
}
