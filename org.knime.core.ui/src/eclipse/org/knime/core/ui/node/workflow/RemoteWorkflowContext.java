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
 */
package org.knime.core.ui.node.workflow;

import java.net.URI;
import java.util.Optional;

import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.Version;
import org.knime.core.util.auth.Authenticator;
import org.knime.core.util.auth.SimpleTokenAuthenticator;

/**
 * This class holds information about the context in which a remote workflow (e.g. a workflow on a KNIME server)
 * currently resides. It includes information such as the server URL and the workflow path.
 *
 * See also {@link WorkflowContextV2}.
 *
 * <b>This class is not intended to be used by clients.</b>
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class RemoteWorkflowContext implements WorkflowContextUI {

    private URI m_repositoryAddress;

    private String m_relativePath;

    private String m_authToken;

    private String m_mountId;

    private URI m_mountpointURI;

    private Version m_clientVersion;

    private Version m_serverVersion;

    private Authenticator m_authenticator;

    private final WorkflowContextV2 m_workflowContext;

    /**
     * Creates a new context for purely remote workflows (i.e. workflows running on a server).
     *
     * @param repositoryAddress the base address of the server repository (the REST endpoint)
     * @param relativePath the path of the workflow relative to the repository root
     * @param authToken the JWT to be for authentification
     * @param mountId the (default) mount id of the server
     * @param mountpointURI the uri of the workflow inside the mount point
     * @param clientVersion the version of the client talking to the 'workflow' server
     * @param serverVersion the version of the server hosting the remote workflow
     * @deprecated use {@link #RemoteWorkflowContext(WorkflowContextV2, URI, String, Authenticator, String, URI,
     * Version, Version)} instead
     */
    @Deprecated(since = "4.5.0")
    public RemoteWorkflowContext(final URI repositoryAddress, final String relativePath, final String authToken,
            final String mountId, final URI mountpointURI, final Version clientVersion, final Version serverVersion) {
        this(repositoryAddress, relativePath, new SimpleTokenAuthenticator(authToken), mountId, mountpointURI,
            clientVersion, serverVersion);
        m_authToken = authToken;
    }

    /**
     * Creates a new context for purely remote workflows (i.e. workflows running on a server or Hub).
     *
     * @param repositoryAddress the base address of the server repository (the REST endpoint)
     * @param relativePath the path of the workflow relative to the repository root
     * @param authenticator the authenticator
     * @param mountId the (default) mount id of the server
     * @param mountpointURI the uri of the workflow inside the mount point
     * @param clientVersion the version of the client talking to the 'workflow' server
     * @param serverVersion the version of the server hosting the remote workflow
     * @since 4.5.0
     * @deprecated use {@link #RemoteWorkflowContext(WorkflowContextV2, URI, String, Authenticator, String, URI,
     * Version, Version)} instead
     */
    @Deprecated(since = "4.7.0")
    public RemoteWorkflowContext(final URI repositoryAddress, final String relativePath,
            final Authenticator authenticator, final String mountId, final URI mountpointURI,
            final Version clientVersion, final Version serverVersion) {
        this(null, repositoryAddress, relativePath, authenticator, mountId, mountpointURI, clientVersion,
            serverVersion);
    }

    /**
     * Creates a new context for purely remote workflows (i.e. workflows running on a server or Hub).
     *
     * @param workflowContext the full workflow context
     * @param repositoryAddress the base address of the server repository (the REST endpoint)
     * @param relativePath the path of the workflow relative to the repository root
     * @param authenticator the authenticator
     * @param mountId the (default) mount id of the server
     * @param mountpointURI the uri of the workflow inside the mount point
     * @param clientVersion the version of the client talking to the 'workflow' server
     * @param serverVersion the version of the server hosting the remote workflow
     * @since 4.7.0
     */
    public RemoteWorkflowContext(final WorkflowContextV2 workflowContext, final URI repositoryAddress,
            final String relativePath, final Authenticator authenticator, final String mountId, final URI mountpointURI,
            final Version clientVersion, final Version serverVersion) {
        m_repositoryAddress = repositoryAddress;
        m_relativePath = relativePath;
        m_authenticator = authenticator;
        m_mountId = mountId;
        m_mountpointURI = mountpointURI;
        m_clientVersion = clientVersion;
        m_serverVersion = serverVersion;
        m_workflowContext = workflowContext;
    }

    /**
     * Returns the base address of the server repository (the REST endpoint).
     *
     * @return the repository base address
     */
    public URI getRepositoryAddress() {
        return m_repositoryAddress;
    }

    /**
     * Returns the path of the workflow relative to the repository root (see {@link #getRepositoryAddress()}.
     *
     * @return the relative path
     */
    public String getRelativePath() {
        return m_relativePath;
    }

    /**
     * Returns the JWT that should be used when talking to the server specified by {@link #getRepositoryAddress()}.
     *
     *
     * @return an authentication token
     * @deprecated use {@link #getServerAuthenticator()} instead
     */
    @Deprecated(since = "4.5.0")
    public String getServerAuthToken() {
        return m_authToken;
    }

    /**
     * Returns the server authenticator.
     *
     * @return the authenticator
     * @since 4.5.0
     */
    public Authenticator getServerAuthenticator() {
        return m_authenticator;
    }

    /**
     * Returns the (default) mount id of the remote server.
     *
     * @return a mount id
     */
    public String getMountId() {
        return m_mountId;
    }

    /**
     * Returns the URI of the workflow inside the mount point.
     *
     * @return the workflow URI
     */
    public URI getMountpointURI() {
        return m_mountpointURI;
    }

    /**
     * @return the version of the client talking to the server
     */
    public Version getClientVersion() {
        return m_clientVersion;
    }

    /**
     * @return the version of the server hosting the remote workflow
     */
    public Version getServerVersion() {
        return m_serverVersion;
    }

    /**
     * @return the full {@link WorkflowContextV2} if available, {@link Optional#empty()} otherwise
     * @since 4.7.0
     */
    public Optional<WorkflowContextV2> getWorkflowContextV2() {
        return Optional.ofNullable(m_workflowContext);
    }
}
