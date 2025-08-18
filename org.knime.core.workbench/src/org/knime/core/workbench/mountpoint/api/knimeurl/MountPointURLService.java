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
 *   Aug 12, 2025 (magnus): created
 */
package org.knime.core.workbench.mountpoint.api.knimeurl;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.workbench.mountpoint.api.MountPointProvider;

/**
 * A {@link URLConnection} with provided mount point service for KNIME URLs.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.7
 */
public interface MountPointURLService extends MountPointProvider {

    /**
     * Creates a new {@link URLConnection} for the given path and version. It's guaranteed that the path is not null
     * and is relative to the mount point root represented by the corresponding mount point state.
     *
     * @param path the path relative to the mount point root, not null
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @return a new {@link URLConnection} for the given path and version
     * @throws IOException if an I/O error occurs while creating the connection
     */
    URLConnection newURLConnection(IPath path, ItemVersion version) throws IOException;

    /**
     * Resolves the given path and version into a local file. If the path does not represent a local file
     * (e.g. on hub) it is downloaded first to a temporary directory and then the temporary copy is returned.
     *
     * @param path the path relative to the mount point root, not null
     * @param version the version of the item, possibly null if "latest" or not applicable
     * @param monitor a progress monitor to report progress, may be {@link NullProgressMonitor}
     * @return a local or temporary file {@link File} represented by the given path and version
     * @throws IOException if an I/O error occurs while resolving the file
     */
    File toLocalOrTempFile(IPath path, ItemVersion version, IProgressMonitor monitor) throws IOException;
}
