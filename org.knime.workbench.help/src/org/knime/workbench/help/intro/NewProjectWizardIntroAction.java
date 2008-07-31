/* @(#)$RCSfile$ 
 * $Revision: 5409 $ $Date: 2006-08-08 16:17:11 +0200 (Di, 08 Aug 2006) $ $Author: meinl $
 * 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   28.08.2005 (Florian Georg): created
 */
package org.knime.workbench.help.intro;

import java.util.Properties;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import org.knime.workbench.ui.wizards.project.NewProjectWizard;

/**
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewProjectWizardIntroAction implements IIntroAction {

    /**
     * {@inheritDoc}
     */
    public void run(final IIntroSite site, final Properties params) {

        // create a new NewProjectWizard
        INewWizard wizard = new NewProjectWizard();

        // get the active workbench window
        IWorkbenchWindow window = Workbench.getInstance()
                .getActiveWorkbenchWindow();

        // init the wizard
        wizard.init(window.getWorkbench(), null);

        // get the parent shell and create a dialog from the wizard
        Shell parent = window.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        dialog.create();
        window.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
                IWorkbenchHelpContextIds.NEW_WIZARD_SHORTCUT);

        dialog.open();
    }

}
