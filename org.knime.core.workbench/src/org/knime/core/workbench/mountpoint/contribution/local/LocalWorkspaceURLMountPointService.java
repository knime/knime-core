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
 *   Aug 12, 2025 (wiswedel): created
 */
package org.knime.core.workbench.mountpoint.contribution.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.workbench.KNIMEWorkspacePath;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;
import org.knime.core.workbench.mountpoint.api.knimeurl.KnimeURLConnection;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLServiceFactory;

/**
 * URL service for the local workspace mount point.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.7
 */
public final class LocalWorkspaceURLMountPointService implements MountPointURLService {

    private LocalWorkspaceMountPointState m_state;

    private LocalWorkspaceURLMountPointService(final LocalWorkspaceMountPointState state) {
        m_state = state;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public KnimeURLConnection newURLConnection(final IPath path, final ItemVersion version)
        throws IOException {
        final var localFile = toLocalOrTempFile(path, null, null);

        // Rebuild knime URL for URL connection
        final var uriBuilder = new URIBuilder();
        uriBuilder.setScheme(CoreConstants.SCHEME);
        uriBuilder.setHost(m_state.getMountID());
        uriBuilder.setPathSegments(path.segments());

        URL url = null;
        try {
            url = uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IOException(
                "Couldn't create URL connection: Invalid URL: %s".formatted(uriBuilder.toString()), e);
        }

        return new KnimeURLConnection(url) {

            @Override
            public OutputStream getOutputStream() throws IOException {
                return new FileOutputStream(localFile);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(localFile);
            }

            @Override
            public int getContentLength() {
               final var fileLength = localFile.length();
               return fileLength > Integer.MAX_VALUE ? -1 : (int)fileLength;
            }

            @Override
            public void connect() throws IOException {
                // noop
            }
        };
    }

    @Override
    public File toLocalOrTempFile(final IPath path, final ItemVersion version, final IProgressMonitor monitor)
        throws IOException, CancellationException {
        IPath rootPath = Path.fromPortableString(KNIMEWorkspacePath.getWorkspaceDirPath().getAbsolutePath());
        return rootPath.append(path).toFile();
    }

    @Override
    public List<NamedItemVersion> getVersions(final IPath path) throws UnsupportedOperationException, IOException {
        throw new UnsupportedOperationException("Item versions are not supported in the local workspace");
    }

    /**
     * {@link MountPointURLServiceFactory} implementation for the {@link LocalWorkspaceURLMountPointService}.
     *
     * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
     */
    public static final class Factory implements MountPointURLServiceFactory {
        @Override
        public MountPointURLService createMountPointURLService(final WorkbenchMountPointState state) {
            final LocalWorkspaceMountPointState localState = CheckUtils.checkCast(state,
                LocalWorkspaceMountPointState.class, IllegalArgumentException::new, "State is not of type %s but %s. ",
                LocalWorkspaceMountPointState.class.getName(), state.getClass().getName());
            return new LocalWorkspaceURLMountPointService(localState);
        }
    }

}
