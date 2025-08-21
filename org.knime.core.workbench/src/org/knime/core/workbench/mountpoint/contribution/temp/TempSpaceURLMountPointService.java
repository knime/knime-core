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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 12, 2025 (copilot): created
 */
package org.knime.core.workbench.mountpoint.contribution.temp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;
import org.knime.core.workbench.mountpoint.api.knimeurl.KnimeURLConnection;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLServiceFactory;

/**
 * URL service for the temp space mount point.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.7
 */
public final class TempSpaceURLMountPointService implements MountPointURLService {

    private final TempSpaceMountPointState m_state;

    private TempSpaceURLMountPointService(final TempSpaceMountPointState state) {
        m_state = state;
    }

    @Override
    public void dispose() {
      // no resources to dispose
    }

    @Override
    public KnimeURLConnection newURLConnection(final IPath path,
        final ItemVersion version) throws IOException {
        final var tempFile = toLocalOrTempFile(path, null, null);

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
                return new FileOutputStream(tempFile);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(tempFile);
            }

            @Override
            public int getContentLength() {
               final var fileLength = tempFile.length();
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
        final Path rootDirPath = m_state.getTempDir().toPath();
        return rootDirPath.resolve(path.toOSString()).toFile();
    }

    @Override
    public List<NamedItemVersion> getVersions(final IPath path) throws UnsupportedOperationException, IOException {
        throw new UnsupportedOperationException("Item versions are not supported in the temp space");
    }

    /**
     * {@link MountPointURLServiceFactory} implementation for the {@link TempSpaceURLMountPointService}.
     *
     * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
     */
    public static final class Factory implements MountPointURLServiceFactory {
        @Override
        public MountPointURLService createMountPointURLService(final WorkbenchMountPointState state) {
            final TempSpaceMountPointState tempState = CheckUtils.checkCast(state,
                TempSpaceMountPointState.class, IllegalArgumentException::new, "State is not of type %s but %s. ",
                TempSpaceMountPointState.class.getName(), state.getClass().getName());
            return new TempSpaceURLMountPointService(tempState);
        }
    }

}
