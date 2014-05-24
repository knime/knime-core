/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on 22.11.2013 by thor
 */
package org.knime.core.util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.knime.core.node.NodeLogger;

/**
 * This class contains some misc utility methods around basic Eclipse funtionality.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public final class EclipseUtil {
    private static final boolean RUN_FROM_SDK;

    static {
        boolean b = false;
        try {
            b = checkSDK();
        } catch (RuntimeException ex) {
            NodeLogger.getLogger("org.knime.core.util.EclipseUtil").error(
                "Could not determine if we are run from the SDK: " + ex.getMessage(), ex);
            throw ex;
        } finally {
            RUN_FROM_SDK = b;
        }
    }

    private EclipseUtil() { }

    /**
     * Returns the whether the current application has been started from an SDK or if it is a standalone instance. Note
     * that this method only guesses based on some indications and may be wrong (i.e. it returns <code>false</code>
     * even though the application has been started from the SDK) in some rare cases.
     *
     * @return <code>true</code> if the application has been started from an SDK, <code>false</code> otherwise
     */
    public static boolean isRunFromSDK() {
        return RUN_FROM_SDK;
    }

    private static boolean checkSDK() {
        Location installLocation = Platform.getInstallLocation();
        if (installLocation == null) {
            return true;
        }
        Location configurationLocation = Platform.getConfigurationLocation();
        if (configurationLocation == null) {
            return true;
        }

        return configurationLocation.getURL().getPath().contains("/.metadata/.plugins/org.eclipse.pde.core/");
    }
}
