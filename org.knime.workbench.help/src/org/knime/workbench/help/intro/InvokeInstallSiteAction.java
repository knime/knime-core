/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Dec 18, 2006 (sieb): created
 */
package org.knime.workbench.help.intro;

import java.net.URL;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.update.internal.search.SiteSearchCategory;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.wizards.InstallWizardOperation;
import org.eclipse.update.search.BackLevelFilter;
import org.eclipse.update.search.EnvironmentFilter;
import org.eclipse.update.search.UpdateSearchRequest;
import org.eclipse.update.search.UpdateSearchScope;
import org.eclipse.update.ui.UpdateJob;

/**
 * Custom action to open the install wizard.
 * 
 * @author Christoph, University of Konstanz
 */
public class InvokeInstallSiteAction extends Action {

    private final static String ID = "INVOKE_INSTALL_SITE_ACTION";

    /**
     * Constructor.
     */
    public InvokeInstallSiteAction() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        openInstaller();
    }

    private InstallWizardOperation getOperation() {
        return new InstallWizardOperation();
    }

    private void openInstaller() {

        try {
            SiteSearchCategory category = new SiteSearchCategory();
            category.setId("org.eclipse.update.core.unified-search");
            category.setLiteFeaturesAreOK(true);

            UpdateSearchScope scope = new UpdateSearchScope();
            scope.setFeatureProvidedSitesEnabled(true);
            scope.addSearchSite("KNIME",
                    new URL("http://www.knime.org/update"), new String[0]);

            UpdateSearchRequest searchRequest =
                    new UpdateSearchRequest(category, scope);
            searchRequest.addFilter(new BackLevelFilter());
            searchRequest.addFilter(new EnvironmentFilter());
            UpdateJob job =
                    new UpdateJob(UpdateUIMessages.InstallWizard_jobName,
                            searchRequest);
            job.setUser(true);
            job.setPriority(Job.INTERACTIVE);

            getOperation().run(UpdateUI.getActiveWorkbenchShell(), job);
            
            // OLD WAY: Started to early (user had to select update or install)
            // InstallWizardAction installWizardAction = new
            // InstallWizardAction();
            // installWizardAction.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Opens the KNIME update site to install "
                + "additional KNIMIE features.";
    }

    @Override
    public String getText() {
        return "Update KNIME...";
    }

    /**
     * Returns the id of this action
     */
    @Override
    public String getId() {
        return ID;
    }
}
