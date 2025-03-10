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


import java.util.Map;
import java.util.Optional;

import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointStateFactory;
import org.knime.core.workbench.preferences.MountSettings;

/**
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class TempSpaceMountPointStateFactory
    implements WorkbenchMountPointStateFactory<TempSpaceMountPointState> {

    public static final String ID = "com.knime.explorer.tempspace";

    @Override
    public TempSpaceMountPointState newInstance(final MountSettings settings) {
        return new TempSpaceMountPointState();
    }

    @Override
    public Optional<Map<String, String>> getDefaultSettings() {
        return Optional.of(Map.of());
    }
}
