/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import org.eclipse.jface.action.Action;
import org.eclipse.ui.internal.ide.update.InstallWizardAction;

/**
 * Custom action to open the install wizard.
 * 
 * @author Christoph, University of Konstanz
 */
public class InvokeInstallSiteAction extends Action {

    /**
     * Constructor.
     */
    public InvokeInstallSiteAction() {
        // do nothing
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        openInstaller();
    }

    private void openInstaller() {
        InstallWizardAction installWizardAction = new InstallWizardAction();
        installWizardAction.run();
    }
}
