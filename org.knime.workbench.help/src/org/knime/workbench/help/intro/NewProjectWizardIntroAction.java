/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.knime.workbench.ui.wizards.project.NewProjectWizard;

/**
 * This action is called when the user clicks "Open KNIME Workbench" in the
 * intro page. It creates a new project with the standard name "KNIME_project".
 * Since the workspace must be empty (otherwise the intro page won't show up)
 * this name can be securely used.
 * 
 * @see NewProjectWizard
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class NewProjectWizardIntroAction implements IIntroAction {

    /**
     * {@inheritDoc}
     */
    public void run(final IIntroSite site, final Properties params) {

        try {
            // close the intro page
            IIntroManager introManager =
                    PlatformUI.getWorkbench().getIntroManager();
            IIntroPart introPart = introManager.getIntro();
            if (introPart != null) {
                introManager.closeIntro(introPart);
            }
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {
                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                // call static method on NewProjectWizard
                                NewProjectWizard.doFinish("KNIME_project",
                                        monitor);
                            } catch (CoreException ce) {
                                throw new RuntimeException(ce);
                            }
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
