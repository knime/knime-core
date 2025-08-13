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

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;

import org.eclipse.core.runtime.IPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLServiceFactory;

/**
 * URL service for the temp space mount point.
 *
 * @author GitHub Copilot
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
    public URLConnection newURLConnection(final IPath relativePath, final ItemVersion version) throws IOException {
        final Path rootDirPath = m_state.getTempDir().toPath();
        final Path filePath = rootDirPath.resolve(relativePath.toOSString());
        return filePath.toUri().toURL().openConnection();
    }

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
