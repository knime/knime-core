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
 *   Aug 19, 2025 (magnus): created
 */
package org.knime.core.internal.knimeurl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLServiceFactoryCollector;

/**
 * Utility class for handling mount point URLs in KNIME.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 */
final class MountPointURLUtil {

    public static final String VERSION_QUERY_PARAM = "version";

    private MountPointURLUtil() {
        // utility class
    }

    /**
     * Retrieves the {@link MountPointURLService} for the given KNIME URL.
     *
     * @param url the KNIME URL to retrieve the service for
     * @return the corresponding {@link MountPointURLService}
     * @throws IOException if the mount point is not mounted or
     * there does not exist a mount point URL service factory for the given mount point type
     */
    public static MountPointURLService getMountPointURLService(final URL url) throws IOException {
        return getMountPointURLService(resolveMountID(url));
    }

    /**
     * Retrieves the {@link MountPointURLService} for the given KNIME URI.
     *
     * @param uri the KNIME URI to retrieve the service for
     * @return the corresponding {@link MountPointURLService}
     * @throws IOException if the mount point is not mounted or
     * there does not exist a mount point URL service factory for the given mount point type
     */
    public static MountPointURLService getMountPointURLService(final URI uri) throws IOException {
        return getMountPointURLService(resolveMountID(uri));
    }

    private static MountPointURLService getMountPointURLService(final String mountPointId) throws IOException {
        final var mountPoint = WorkbenchMountTable.getMountPoint(mountPointId)
            .orElseThrow(() -> new IOException("Mount point with ID '" + mountPointId + "' is not mounted."));

        final var mountPointURLServiceFactory = MountPointURLServiceFactoryCollector.getInstance()
            .getMountPointURLServiceFactory(mountPoint.getType().getTypeIdentifier())
            .orElseThrow(() -> new ResourceAccessException("No mount point URL service factory found for type '"
                + mountPoint.getType().getTypeIdentifier() + "'"));

        return mountPoint.getProvider(MountPointURLService.class,
            () -> mountPointURLServiceFactory.createMountPointURLService(mountPoint.getState()));
    }

    /**
     * Extracts the mount point ID from a KNIME URL.
     *
     * @param url the KNIME URL to extract the mount point ID from
     * @return the mount point ID
     */
    public static String resolveMountID(final URL url) {
        return resolveMountID(url, url.getHost(), KnimeUrlType.getType(url));
    }

    /**
     * Extracts the mount point ID from a KNIME URI.
     *
     * @param uri the KNIME URI to extract the mount point ID from
     * @return the mount point ID
     */
    public static String resolveMountID(final URI uri) {
        return resolveMountID(uri, uri.getHost(), KnimeUrlType.getType(uri));
    }

    private static String resolveMountID(final Object url, final String host, final Optional<KnimeUrlType> optType) {
        return optType.map(urlType -> urlType.isRelative() ? getMountPointIdFromContext().orElse(host) : host) //
            .orElseThrow(() -> new IllegalArgumentException(
                "Invalid scheme in URL ('" + url + "'). " + "Only '" + CoreConstants.SCHEME + "' is allowed here."));
    }

    /**
     * Retrieves the mount point state from a KNIME URL.
     *
     * @param url the KNIME URL to retrieve the mount point state from
     * @return the corresponding {@link WorkbenchMountPointState}
     * @throws IOException if the mount point is not mounted
     */
    public static WorkbenchMountPointState resolveMountPointState(final URL url) throws IOException {
        final var mountPointId = resolveMountID(url);
        return WorkbenchMountTable.getMountPoint(mountPointId)
                .orElseThrow(() -> new IOException("Mount point with ID '" + mountPointId + "' is not mounted."))
                .getState();
    }

    /**
     * Retrieves the mount point state from a KNIME URI.
     *
     * @param uri the KNIME URI to retrieve the mount point state from
     * @return the corresponding {@link WorkbenchMountPointState}
     * @throws IOException if the mount point is not mounted
     */
    public static WorkbenchMountPointState resolveMountPointState(final URI uri) throws IOException {
        final var mountPointId = resolveMountID(uri);
        return WorkbenchMountTable.getMountPoint(mountPointId)
                .orElseThrow(() -> new IOException("Mount point with ID '" + mountPointId + "' is not mounted."))
                .getState();
    }

    private static Optional<String> getMountPointIdFromContext() {
        return NodeContext.getContextOptional() //
            .flatMap(ctx -> ctx.getContextObjectForClass(WorkflowContextV2.class)) //
            .flatMap(wfc -> ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, wfc.getExecutorInfo())) //
            .flatMap(AnalyticsPlatformExecutorInfo::getMountpoint) //
            .map(mp -> mp.getFirst().getAuthority());
    }

    static Optional<String> getVersionParam(final URL url) throws IOException {
        try {
            return new URIBuilder(url.toURI()).getQueryParams() //
                .stream() //
                .filter(nvp -> VERSION_QUERY_PARAM.equals(nvp.getName())) //
                .map(NameValuePair::getValue) //
                .findFirst();
        } catch (URISyntaxException ex) {
            throw new IOException("Malformed URL: " + url, ex);
        }
    }
}
