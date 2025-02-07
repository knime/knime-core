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
 *   7 Feb 2025 (chaubold): created
 */
package org.knime.core.internal;

import java.awt.HeadlessException;
import java.io.IOException;
import java.util.EventObject;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.equinox.internal.p2.engine.PhaseEvent;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.knime.core.node.NodeLogger;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

/**
 * Utilities to check for and enable long path support on windows
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
class WindowsLongPathUtils {
    private static final String REGISTRY_PATH = "SYSTEM\\CurrentControlSet\\Control\\FileSystem";

    private static final String LONG_PATHS_VALUE_NAME = "LongPathsEnabled";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WindowsLongPathUtils.class);

    // opens a PowerShell with admin privileges and sets the `LongPathsEnabled` value
    private static final String POWERSHELL_SET_TEMPLATE =
        "powershell.exe -Command \"Start-Process powershell.exe -Verb runAs -ArgumentList '-NoProfile -Command "
            + "\\\"New-ItemProperty -Path HKLM:\\%s -Name %s -Value %d -PropertyType DWord -Force\\\"'\" -Wait";

    static class InstallationListener implements ProvisioningListener {

        public static InstallationListener INSTANCE = new InstallationListener();

        private InstallationListener() {
        }

        /**
         * Called whenever a ProvisioningEvent is fired by Eclipse's event bus
         */
        @Override
        public void notify(final EventObject o) {
            if (o instanceof PhaseEvent && isInstallOrUninstallPhase(o)
                && ((PhaseEvent)o).getType() == PhaseEvent.TYPE_START) {

                // check for long paths and enable
                tryEnablingLongPathSupport();
            }
        }

        private static boolean isInstallOrUninstallPhase(final EventObject o) {
            var phaseId = ((PhaseEvent)o).getPhaseId();
            return phaseId.equals(PhaseSetFactory.PHASE_INSTALL) || phaseId.equals(PhaseSetFactory.PHASE_UNINSTALL);
        }

    }

    private static void tryEnablingLongPathSupport() {
        if (!isLongPathsSettable()) {
            LOGGER.debug("Cannot configure long path support on this system");
            return;
        }

        if (isLongPathsEnabled()) {
            LOGGER.debug("Long path support is already enabled");
            return;
        }

        // ask the user whether long paths should be enabled
        if (!didUserConfirmEnablingLongPath()) {
            LOGGER.debug("User declined enabling long paths support");
            return;
        }

        final var command = POWERSHELL_SET_TEMPLATE.formatted(REGISTRY_PATH, LONG_PATHS_VALUE_NAME, 1);

        try {
            LOGGER.debug("Trying to enable long path support");
            if (Runtime.getRuntime().exec(command).waitFor() != 0) {
                LOGGER.debug("Could not enable long path support");
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Enabling long path support failed", e);
            return;
        }
    }

    private static boolean didUserConfirmEnablingLongPath() {
        try {
            LOGGER.debug("Asking user to enable long path support");
            Object[] options = {"Yes", "No"};

            int choice = JOptionPane.showOptionDialog(null, // Parent component (null centers on screen)
                "Do you want to enable long paths support for Windows to improve compatibility during installations and updates?",
                "Long path support is disabled", JOptionPane.YES_NO_CANCEL_OPTION, // Option type (can be any type, not used directly here)
                JOptionPane.QUESTION_MESSAGE, //
                null, // Icon (null for default)
                options, //
                options[0] // Default selection
            );

            return choice == 0;
        } catch (HeadlessException e) {
            return false;
        }
    }

    private static boolean isLongPathsSettable() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            return false;
        }
        try {
            final String buildNumberStr = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
                "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuildNumber");
            // Windows 10 version 1607 (Anniversary Update, build number 14393) is the first with Long-Paths support
            return Integer.parseInt(buildNumberStr) >= 14393;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isLongPathsEnabled() {
        if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH)
            || !Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH, LONG_PATHS_VALUE_NAME)) {
            return false;
        }
        return Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH, LONG_PATHS_VALUE_NAME) != 0;
    }
}
