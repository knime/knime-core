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

import java.nio.file.Path;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLService;
import org.knime.core.workbench.mountpoint.api.knimeurl.MountPointURLServiceFactory;
import org.knime.core.workbench.mountpoint.contribution.LocalDirURLMountPointService;

/**
 * URL service for the temp space mount point.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.8
 */
public final class TempSpaceURLMountPointService extends LocalDirURLMountPointService {

    private final TempSpaceMountPointState m_state;

    private TempSpaceURLMountPointService(final TempSpaceMountPointState state) {
        super(TempSpaceMountPointState.TYPE.getDefaultMountID().orElseThrow());
        m_state = state;
    }

    @Override
    protected Path getRootDirectory() {
        return m_state.getTempDir().toPath();
    }

    /** {@link MountPointURLServiceFactory} implementation for the {@link TempSpaceURLMountPointService}. */
    public static final class Factory implements MountPointURLServiceFactory {
        @Override
        public MountPointURLService createMountPointURLService(final WorkbenchMountPointState state) {
            final TempSpaceMountPointState tempState = CheckUtils.checkCast(state,
                TempSpaceMountPointState.class, IllegalArgumentException::new, "State is not of type %s but %s.",
                TempSpaceMountPointState.class.getName(), state.getClass().getName());
            return new TempSpaceURLMountPointService(tempState);
        }
    }
}
