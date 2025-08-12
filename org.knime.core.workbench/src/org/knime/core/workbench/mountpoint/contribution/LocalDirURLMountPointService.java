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
package org.knime.core.workbench.mountpoint.contribution;

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

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Owning;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.workbench.mountpoint.api.knimeurl.KnimeURLConnection;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;

/**
 * URL service for the local workspace mount point.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.8
 */
public abstract class LocalDirURLMountPointService implements MountPointURLService {

    private final String m_mountId;

    /**
     * Constructor.
     *
     * @param mountId mount ID for recreating the KNIME URL
     */
    protected LocalDirURLMountPointService(final String mountId) {
        m_mountId = mountId;
    }

    /**
     * Returns the root directory for this mount point.
     *
     * @return the root directory
     */
    protected abstract Path getRootDirectory();

    @Override
    public final String getMountId() {
        return m_mountId;
    }

    @Override
    public final void dispose() {
        // nothing to do
    }

    @Override
    public final KnimeURLConnection newURLConnection(final IPath path, final ItemVersion version) throws IOException {
        // Rebuild knime URL for URL connection
        final var uriBuilder = new URIBuilder() //
            .setScheme(CoreConstants.SCHEME) //
            .setHost(m_mountId) //
            .setPathSegments(path.segments());
        URL url = null;
        try {
            url = uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IOException(
                "Couldn't create URL connection: Invalid URL: %s".formatted(uriBuilder.toString()), e);
        }

        final var localFile = toLocalOrTempFile(path, null, null);
        return new KnimeURLConnection(url) {

            @Override
            public @Owning OutputStream getOutputStream() throws IOException {
                return new FileOutputStream(localFile);
            }

            @Override
            public @Owning InputStream getInputStream() throws IOException {
                return new FileInputStream(localFile);
            }

            @Override
            public long getContentLengthLong() {
                return localFile.length();
            }

            @Override
            public void connect() throws IOException {
                // noop
            }
        };
    }

    @Override
    public final File toLocalOrTempFile(final IPath path, final ItemVersion version, final IProgressMonitor monitor)
        throws IOException {
        return getRootDirectory().resolve(path.makeRelative().toOSString()).toFile();
    }

    @Override
    public final List<NamedItemVersion> getVersions(final IPath path) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Item versions are not supported for local items");
    }
}
