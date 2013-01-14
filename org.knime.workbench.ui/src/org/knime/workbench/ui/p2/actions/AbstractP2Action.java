/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   03.10.2010 (meinl): created
 */
package org.knime.workbench.ui.p2.actions;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class AbstractP2Action extends Action {
    protected AbstractP2Action(final String text, final String description,
            final String id) {
        super(text);
        setDescription(description);
        setId(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        if (provUI.getRepositoryTracker() == null) {
            MessageBox mbox =
                    new MessageBox(ProvUI.getDefaultParentShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mbox.setText("Action impossible");
            mbox.setMessage("It seems you are running KNIME from an SDK. "
                    + "Installing extension is not possible in this case.");
            mbox.open();
            return;
        }
        String installLocation = Platform.getInstallLocation().getURL().toString();
        String configurationLocation = Platform.getConfigurationLocation().getURL().toString();

        if (!configurationLocation.contains(installLocation)) {
            MessageBox mbox =
                    new MessageBox(ProvUI.getDefaultParentShell(),
                            SWT.ICON_WARNING | SWT.YES  | SWT.NO);
            mbox.setText("Permission problem");
            mbox.setMessage("Your KNIME installation directory seems to be "
                    + "read-only, maybe because KNIME was installed by a "
                    + "different user. Installing extensions or updating KNIME "
                    + "may cause problems. Do you really want to continue?");
            if (mbox.open() == SWT.NO) {
                return;
            }
        }

        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final LoadMetadataRepositoryJob loadJob =
                new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS,
                Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning()) {
                    if (event.getResult().isOK()) {
                        openWizard(loadJob, provUI);
                    }
                }
            }
        });
        loadJob.setUser(true);
        loadJob.schedule();
    }

    /**
     * This is called when a wizard (install, update, ...) should be opened.
     * Subclasses must override this method and open the desired wizard.
     *
     * @param job the repository job
     * @param provUI the provisioning UI instance
     */
    protected abstract void openWizard(final LoadMetadataRepositoryJob job,
            ProvisioningUI provUI);
}
