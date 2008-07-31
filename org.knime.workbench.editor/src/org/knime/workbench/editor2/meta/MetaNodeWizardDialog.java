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
 *   26.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeWizardDialog extends WizardDialog {

    /**
     * @param parentShell
     * @param newWizard
     */
    public MetaNodeWizardDialog(final Shell parentShell, 
            final IWizard newWizard) {
        super(parentShell, newWizard);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        Button nextBtn = getButton(IDialogConstants.NEXT_ID);
        nextBtn.setText("Customize >");
    }

}
