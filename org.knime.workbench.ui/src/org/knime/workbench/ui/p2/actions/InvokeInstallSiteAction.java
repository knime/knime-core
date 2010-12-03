/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   Dec 18, 2006 (sieb): created
 */
package org.knime.workbench.ui.p2.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;

/**
 * Custom action to open the install wizard.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class InvokeInstallSiteAction extends AbstractP2Action {
    private static final String ID = "INVOKE_INSTALL_SITE_ACTION";

    /**
     *
     */
    public InvokeInstallSiteAction() {
        super("Install KNIME Extensions...", "Opens the KNIME update site to "
                + "install additional KNIME features.", ID);
    }

    @Override
    protected void openWizard(final LoadMetadataRepositoryJob job,
            final ProvisioningUI provUI) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                boolean isWin64 =
                        Platform.OS_WIN32.equals(Platform.getOS())
                                && Platform.ARCH_X86_64.equals(Platform
                                        .getOSArch());
                int defaultRestartPolicy =
                    provUI.getPolicy().getRestartPolicy();

                provUI.getPolicy().setRepositoriesVisible(false);
                if (isWin64) {
                    NodeLogger.getLogger(InvokeInstallSiteAction.class).debug(
                            "Installing new features for Windows 64bit arch:"
                                    + " activating restart workaround");
                    provUI.getPolicy().setRestartPolicy(
                            ProvisioningJob.RESTART_NONE);
                }
                int retCode = provUI.openInstallWizard(null, null, job);

                if (isWin64) {
                    if (retCode == IStatus.OK) {
                    MessageBox box =
                            new MessageBox(PlatformUI.getWorkbench()
                                    .getDisplay().getActiveShell(),
                                    SWT.ICON_WARNING);
                    box.setText("PLEASE RE-START MANUALLY");
                    box.setMessage(
                            "Please re-start KNIME after "
                            + "the installation is complete.\n\n"
                            + "Due to a known issue with Windows 64bit the "
                            + "application must be re-started manually "
                            + "after installing new features.");
                    box.open();
                    }
                    provUI.getPolicy().setRestartPolicy(defaultRestartPolicy);
                }
                provUI.getPolicy().setRepositoriesVisible(true);
            }
        });

    }
}
