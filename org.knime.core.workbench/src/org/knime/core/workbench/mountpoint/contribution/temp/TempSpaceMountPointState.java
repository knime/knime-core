/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by
 * KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 */
package org.knime.core.workbench.mountpoint.contribution.temp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.function.FailableFunction;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.User;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState;

/**
 * Settings for the temp space mount point, moved content out of old (explorer-based) TempSpaceContentProvider.
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class TempSpaceMountPointState implements WorkbenchMountPointState {

    private File m_tempDir;

    // TODO no commons lang in public API
    public File initTempDir(final FailableFunction<String, File, IOException> tempFileSupplier) throws Exception {
        if (m_tempDir == null) {
            final var prefix = "knime_temp_space_" + User.getUsername() + "_";
            m_tempDir = tempFileSupplier.apply(prefix);
        }
        return m_tempDir;
    }

    public File getTempDir() {
        CheckUtils.checkState(m_tempDir != null, "initTempDir() must be called before getTempDir()");
        return m_tempDir;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }
}
